package com.example.macromod.path.behavior;

import com.example.macromod.ForcedInputState;
import com.example.macromod.path.calc.*;
import com.example.macromod.path.goal.Goal;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PathingBehavior {

    private static PathingBehavior INSTANCE;

    private static final Map<Long, Long> blacklist = new ConcurrentHashMap<>();
    private static final Map<Long, Long> edgeBlacklist = new ConcurrentHashMap<>();
    private static final long BLACKLIST_TTL_MS = 10_000;
    private static final long EDGE_BLACKLIST_TTL_MS = 8000;

    private Goal currentGoal;
    private PathExecutor executor;
    private CalculationContext context;
    private boolean calculating = false;

    private long lastRecalc = 0;
    private static final long RECALC_INTERVAL_MS = 5000;
    private static final double SWAP_MARGIN = 0.95;

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
        this.executor = null;
        this.calculating = false;
        this.lastRecalc = System.currentTimeMillis();
        startCalculation();
    }

    public void stop() {
        if (context != null) context.releaseAllInputs();
        currentGoal = null;
        executor = null;
        calculating = false;
    }

    public boolean isPathing() {
        return currentGoal != null && (executor != null || calculating);
    }

    private void startCalculation() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || currentGoal == null) return;
        if (calculating) return;

        calculating = true;
        context = new CalculationContext();

        final Goal goal = currentGoal;
        final BlockPos startPos = context.playerFeetBlock();
        if (startPos == null) { calculating = false; return; }
        final int sx = startPos.getX();
        final int sy = startPos.getY();
        final int sz = startPos.getZ();

        Thread calcThread = new Thread(() -> {
            long t0 = System.currentTimeMillis();
            AStarPathFinder finder = new AStarPathFinder(context, goal, sx, sy, sz);
            Path path = finder.calculate();
            long dt = System.currentTimeMillis() - t0;

            if (path == null || path.isFinished()) {
                if (executor == null) {
                    System.out.println("[MacroMod] No path found (" + dt + "ms)");
                    currentGoal = null;
                }
                calculating = false;
                return;
            }

            double newCost = path.getTotalCost();
            System.out.println("[MacroMod] Path found: " + path.size()
                    + " movements, cost " + String.format("%.2f", newCost)
                    + ", " + dt + "ms");

            if (executor == null) {
                executor = new PathExecutor(path, goal);
            } else {
                double currentRemaining = executor.getPath().getRemainingCost();
                if (newCost < currentRemaining * SWAP_MARGIN) {
                    System.out.println("[MacroMod] Swapping path: "
                        + String.format("%.2f", currentRemaining) + " → "
                        + String.format("%.2f", newCost));
                    executor = new PathExecutor(path, goal);
                }
            }
            calculating = false;
        }, "MacroMod-PathCalc");
        calcThread.setDaemon(true);
        calcThread.start();
    }

    public void tick() {
        if (currentGoal == null) return;

        if (executor == null) {
            if (!calculating) currentGoal = null;
            return;
        }

        boolean done = executor.tick(context);
        if (done) {
            if (context != null) context.releaseAllInputs();
            stop();
            return;
        }

        if (executor.needsRecalculation()) {
            if (context != null) context.releaseAllInputs();
            executor.clearRecalculation();
            startCalculation();
            return;
        }

        long now = System.currentTimeMillis();
        if (!calculating && now - lastRecalc > RECALC_INTERVAL_MS) {
            lastRecalc = now;
            startCalculation();
        }
    }

    public Goal getGoal() { return currentGoal; }
    public boolean isCalculating() { return calculating; }
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