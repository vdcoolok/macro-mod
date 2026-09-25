package com.example.macromod.path.behavior;

import com.example.macromod.ForcedInputState;
import com.example.macromod.NolookController;
import com.example.macromod.path.calc.AStarPathFinder;
import com.example.macromod.path.calc.CalculationContext;
import com.example.macromod.path.calc.Path;
import com.example.macromod.path.calc.PathExecutor;
import com.example.macromod.path.goal.Goal;
import com.example.macromod.path.goal.GoalBlock;
import com.example.macromod.path.goal.GoalXZ;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PathingBehavior {

    private static PathingBehavior INSTANCE;

    private static final Map<Long, Long> blacklist = new ConcurrentHashMap<>();
    private static final Map<Long, Long> edgeBlacklist = new ConcurrentHashMap<>();
    private static final long BLACKLIST_TTL_MS = 10_000;
    private static final long EDGE_BLACKLIST_TTL_MS = 8000;

    private static final long RECALC_INTERVAL_MS = 5000;
    private static final long EXPLORE_RECALC_INTERVAL_MS = 1500;
    private static final double SWAP_MARGIN = 0.95;
    private static final int EXPLORE_CHUNK_MARGIN = 2;

    private Goal currentGoal;
    private Goal activeGoal;
    private PathExecutor executor;
    private CalculationContext context;
    private boolean calculating = false;
    private boolean exploring = false;

    private long lastRecalc = 0;

    private PathingBehavior() {}

    public static PathingBehavior get() {
        if (INSTANCE == null) INSTANCE = new PathingBehavior();
        return INSTANCE;
    }

    public static void blacklist(BlockPos pos) {
        blacklist.put(key(pos.getX(), pos.getY(), pos.getZ()),
                System.currentTimeMillis() + BLACKLIST_TTL_MS);
    }

    public static boolean isBlacklisted(int x, int y, int z) {
        Long expiry = blacklist.get(key(x, y, z));
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            blacklist.remove(key(x, y, z));
            return false;
        }
        return true;
    }

    public static void blacklistEdge(BlockPos from, BlockPos to) {
        edgeBlacklist.put(edgeKey(from, to),
                System.currentTimeMillis() + EDGE_BLACKLIST_TTL_MS);
    }

    public static boolean isEdgeBlacklisted(BlockPos from, BlockPos to) {
        long k = edgeKey(from, to);
        Long expiry = edgeBlacklist.get(k);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            edgeBlacklist.remove(k);
            return false;
        }
        return true;
    }

    public void setGoal(Goal goal) {
        this.currentGoal = goal;
        this.activeGoal = goal;
        this.executor = null;
        this.calculating = false;
        this.exploring = false;
        this.lastRecalc = System.currentTimeMillis();
        startCalculation();
    }

    public void stop() {
        if (context != null) context.releaseAllInputs();
        NolookController.setMode(NolookController.Mode.NONE);
        currentGoal = null;
        activeGoal = null;
        executor = null;
        calculating = false;
        exploring = false;
    }

    public boolean isPathing() {
        return currentGoal != null && (executor != null || calculating);
    }

    private void startCalculation() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || activeGoal == null) return;
        if (calculating) return;

        calculating = true;
        context = new CalculationContext();

        final Goal goal = activeGoal;
        final BlockPos startPos = context.playerFeetBlock();
        if (startPos == null) { calculating = false; return; }
        final int sx = startPos.getX();
        final int sy = startPos.getY();
        final int sz = startPos.getZ();

        Thread calcThread = new Thread(() -> {
            long t0 = System.currentTimeMillis();
            try {
                AStarPathFinder finder = new AStarPathFinder(context, goal, sx, sy, sz);
                Path path = finder.calculate();
                long dt = System.currentTimeMillis() - t0;

                if (path == null || path.isFinished()) {
                    calculating = false;

                    if (path != null && goal.isInGoal(sx, sy, sz)) {
                        System.out.println("[MacroMod] Already at " + goalDescription(goal));
                        stop();
                        return;
                    }

                    if (executor == null) {
                        System.out.println("[MacroMod] No path to ("
                                + goalDescription(goal) + ") found (" + dt + "ms)");
                        handleCalculationFailure();
                    }
                    return;
                }

                System.out.println("[MacroMod] Path found: " + path.size()
                        + " movements, cost " + String.format("%.2f", path.getTotalCost())
                        + ", " + dt + "ms");

                if (executor == null) {
                    executor = new PathExecutor(path, goal);
                } else {
                    double currentRemaining = executor.getPath().getRemainingCost();
                    if (newCost(path) < currentRemaining * SWAP_MARGIN) {
                        System.out.println("[MacroMod] Swapping path: "
                            + String.format("%.2f", currentRemaining) + " → "
                            + String.format("%.2f", newCost(path)));
                        executor = new PathExecutor(path, goal);
                    }
                }
                calculating = false;
            } catch (Throwable t) {
                System.err.println("[MacroMod] Path calculation crashed: " + t);
                calculating = false;
            }
        }, "MacroMod-PathCalc");
        calcThread.setDaemon(true);
        calcThread.start();
    }

    private static double newCost(Path path) {
        return path.getTotalCost();
    }

    private void handleCalculationFailure() {
        if (exploring) {
            System.out.println("[MacroMod] Explore target unreachable, stopping pathing");
            stop();
            return;
        }

        if (goalChunksCached()) {
            System.out.println("[MacroMod] Goal is in loaded chunks but no path was found, stopping pathing");
            stop();
            return;
        }

        GoalXZ step = nextExploreStep();
        if (step == null) {
            System.out.println("[MacroMod] Goal is in unloaded chunks and no straight-line "
                    + "step could be computed, stopping pathing");
            stop();
            return;
        }

        System.out.println("[MacroMod] Goal area not cached, exploring toward ("
                + step.getX() + ", " + step.getZ() + ")");
        exploring = true;
        activeGoal = step;
        startCalculation();
    }

    private GoalXZ nextExploreStep() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || currentGoal == null) return null;

        int tx = goalX(currentGoal);
        int tz = goalZ(currentGoal);
        if (tx == Integer.MIN_VALUE || tz == Integer.MIN_VALUE) return null;

        BlockPos feet = context.playerFeetBlock();
        if (feet == null) return null;

        int dx = tx - feet.getX();
        int dz = tz - feet.getZ();
        int chebyshev = Math.max(Math.abs(dx), Math.abs(dz));
        if (chebyshev == 0) return null;

        int renderDistance = mc.options.getEffectiveRenderDistance();
        int maxStepBlocks = Math.max(16, (renderDistance - EXPLORE_CHUNK_MARGIN) * 16);
        int stepBlocks = Math.min(maxStepBlocks, chebyshev);

        double ux = dx / (double) chebyshev;
        double uz = dz / (double) chebyshev;

        int stepX = feet.getX() + (int) Math.round(ux * stepBlocks);
        int stepZ = feet.getZ() + (int) Math.round(uz * stepBlocks);

        if (stepX == feet.getX() && stepZ == feet.getZ()) return null;
        return new GoalXZ(stepX, stepZ);
    }

    private boolean goalChunksCached() {
        if (currentGoal == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        int gx = goalX(currentGoal);
        int gz = goalZ(currentGoal);
        if (gx == Integer.MIN_VALUE || gz == Integer.MIN_VALUE) return false;
        return mc.level.getChunkSource().getChunk(gx >> 4, gz >> 4, false) != null;
    }

    private static int goalX(Goal goal) {
        if (goal instanceof GoalXZ g) return g.getX();
        if (goal instanceof GoalBlock g) return g.getX();
        return Integer.MIN_VALUE;
    }

    private static int goalZ(Goal goal) {
        if (goal instanceof GoalXZ g) return g.getZ();
        if (goal instanceof GoalBlock g) return g.getZ();
        return Integer.MIN_VALUE;
    }

    private static String goalDescription(Goal goal) {
        if (goal instanceof GoalXZ g) return "xz " + g.getX() + ", " + g.getZ();
        if (goal instanceof GoalBlock g) return "block " + g.getX() + ", " + g.getY() + ", " + g.getZ();
        return goal.getClass().getSimpleName();
    }

    public void tick() {
        if (currentGoal == null) return;

        lockKeyboard();

        if (executor == null) {
            if (!calculating) startCalculation();
            return;
        }

        if (exploring && goalChunksCached()) {
            System.out.println("[MacroMod] Goal chunks now cached, switching to real path");
            if (context != null) context.releaseAllInputs();
            executor = null;
            exploring = false;
            activeGoal = currentGoal;
            startCalculation();
            return;
        }

        boolean done = executor.tick(context);
        if (done) {
            if (context != null) context.releaseAllInputs();
            executor = null;

            if (exploring) {
                GoalXZ step = nextExploreStep();
                if (step != null && !reached(step)) {
                    activeGoal = step;
                    startCalculation();
                    return;
                }
                exploring = false;
                activeGoal = currentGoal;
                startCalculation();
                return;
            }

            stop();
            return;
        }

        if (executor.needsRecalculation()) {
            if (context != null) context.releaseAllInputs();
            executor.clearRecalculation();
            executor = null;
            startCalculation();
            return;
        }

        long now = System.currentTimeMillis();
        long interval = exploring ? EXPLORE_RECALC_INTERVAL_MS : RECALC_INTERVAL_MS;
        if (!calculating && now - lastRecalc > interval) {
            lastRecalc = now;
            startCalculation();
        }
    }

    private boolean reached(GoalXZ step) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return true;
        return mc.player.blockPosition().getX() == step.getX()
                && mc.player.blockPosition().getZ() == step.getZ();
    }

    private static void lockKeyboard() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;

        for (Map.Entry<InputConstants.Key, Boolean> e : ForcedInputState.getForcedKeyStates().entrySet()) {
            InputConstants.Key key = e.getKey();
            if (key == null || !e.getValue()) continue;

            if (!InputConstants.isKeyDown(mc.getWindow(), key.getValue())) {
                KeyMapping.set(key, true);
            }
        }
    }

    public Goal getGoal() { return currentGoal; }
    public boolean isCalculating() { return calculating; }
    public boolean isExploring() { return exploring; }
    public Path getCurrentPath() { return executor != null ? executor.getPath() : null; }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38)
             | ((long) (y & 0xFFF) << 26)
             | (z & 0x3FFFFFF);
    }

    private static long edgeKey(BlockPos from, BlockPos to) {
        long a = key(from.getX(), from.getY(), from.getZ());
        long b = key(to.getX(), to.getY(), to.getZ());
        return a * 31 + b;
    }
}
