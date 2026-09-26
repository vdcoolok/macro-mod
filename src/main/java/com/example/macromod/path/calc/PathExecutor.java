package com.example.macromod.path.calc;

import com.example.macromod.path.behavior.PathingBehavior;
import com.example.macromod.path.goal.Goal;
import com.example.macromod.path.goal.GoalBlock;
import com.example.macromod.path.movement.Movement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

public class PathExecutor {

    private static final int FINE_APPROACH_MAX_TICKS = 20;

    private Path currentPath;
    private final Goal goal;
    private boolean recalculationNeeded = false;
    private int fineTicks = 0;

    public PathExecutor(Path initialPath, Goal goal) {
        this.currentPath = initialPath;
        this.goal = goal;
    }

    public boolean tick(CalculationContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return true;

        if (recalculationNeeded) return false;

        if (currentPath == null || currentPath.isFinished()) {
            if (goal instanceof GoalBlock gb && gb.hasExact()) {
                double dx = gb.getExactX() - p.getX();
                double dz = gb.getExactZ() - p.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);

                if (dist <= 0.35) {
                    ctx.releaseAllInputs();
                    return true;
                }

                fineTicks++;
                if (fineTicks > FINE_APPROACH_MAX_TICKS) {
                    System.out.println("[MacroMod] Goal position not reached exactly ("
                        + String.format("%.2f", dist) + " blocks away), stopping pathing");
                    ctx.releaseAllInputs();
                    return true;
                }

                if (!fineApproachClear(ctx, p, gb)) {
                    System.out.println("[MacroMod] Final approach to goal is blocked, stopping pathing");
                    ctx.releaseAllInputs();
                    return true;
                }

                ctx.lookAt(gb.getExactX(), p.getY() + p.getEyeHeight(), gb.getExactZ());
                ctx.setInput("sprint", false);
                ctx.setInput("jump", false);
                ctx.setInput("back", false);
                ctx.setInput("forward", true);
                if (dist <= 0.6) {
                    ctx.setInput("sneak", true);
                } else {
                    ctx.setInput("sneak", false);
                }
                return false;
            }

            ctx.releaseAllInputs();
            return true;
        }

        Movement current = currentPath.getCurrent();

        int skip = shouldSkipAhead(ctx);
        if (skip > 0) {
            currentPath.skipTo(currentPath.getIndex() + skip);
            if (currentPath.isFinished()) {
                return tick(ctx);
            }
            current = currentPath.getCurrent();
        }

        Movement.MovementState state = current.tick(ctx);

        switch (state) {
            case SUCCESS -> {
                currentPath.advance();
                if (currentPath.isFinished()) {
                    return tick(ctx);
                }
            }
            case FAILED -> {
                System.out.println("[MacroMod] " + current.getClass().getSimpleName()
                    + " failed at " + current.getTo() + " from " + current.getFrom());
                PathingBehavior.blacklistEdge(current.getFrom(), current.getTo());
                ctx.releaseAllInputs();
                currentPath = null;
                recalculationNeeded = true;
                return false;
            }
            case RUNNING, PREPARE -> {}
        }

        if (currentPath.isFinished()) {
            return tick(ctx);
        }

        return false;
    }

    private boolean fineApproachClear(CalculationContext ctx, LocalPlayer p, GoalBlock gb) {
        double dx = gb.getExactX() - p.getX();
        double dz = gb.getExactZ() - p.getZ();
        int blockX = (int) Math.floor(p.getX() + dx * 0.25);
        int blockZ = (int) Math.floor(p.getZ() + dz * 0.25);
        int feetY = p.blockPosition().getY();

        if (!ctx.isPassable(blockX, feetY, blockZ)) return false;
        if (!ctx.isPassable(blockX, feetY + 1, blockZ)) return false;
        return true;
    }

    private int shouldSkipAhead(CalculationContext ctx) {
        if (currentPath == null) return 0;

        int skip = 0;
        for (int i = currentPath.getIndex(); i < currentPath.size(); i++) {
            BlockPos pos = currentPath.getMovements().get(i).getTo();
            BlockPos feet = ctx.playerFeetBlock();
            if (feet != null && feet.equals(pos)) {
                skip = i - currentPath.getIndex() + 1;
            } else break;
        }
        return skip;
    }

    public void setPath(Path path) { this.currentPath = path; }
    public Path getPath() { return currentPath; }
    public boolean needsRecalculation() { return recalculationNeeded; }
    public void clearRecalculation() { recalculationNeeded = false; }
}