package com.example.macromod.path.calc;

import com.example.macromod.path.behavior.PathingBehavior;
import com.example.macromod.path.goal.Goal;
import com.example.macromod.path.goal.GoalBlock;
import com.example.macromod.path.movement.Movement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

public class PathExecutor {

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

                fineTicks++;
                if (dist <= 0.03 || fineTicks > 30) {
                    ctx.releaseAllInputs();
                    return true;
                }

                ctx.lookAt(gb.getExactX(), p.getY() + p.getEyeHeight(), gb.getExactZ());
                ctx.setInput("sprint", false);
                ctx.setInput("jump", false);
                ctx.setInput("back", false);
                ctx.setInput("forward", true);
                if (dist <= 0.25) {
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