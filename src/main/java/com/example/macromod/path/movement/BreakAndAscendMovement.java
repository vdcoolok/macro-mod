package com.example.macromod.path.movement;

import com.example.macromod.path.calc.ActionCosts;
import com.example.macromod.path.calc.CalculationContext;
import net.minecraft.core.BlockPos;

public class BreakAndAscendMovement extends Movement implements ActionCosts {

    private static final int MAX_TICKS = 160;

    private int totalTicks = 0;

    public BreakAndAscendMovement(BlockPos from, BlockPos to) {
        super(from, to);
    }

    @Override
    public double calculateCost(CalculationContext ctx) {
        if (!ctx.canAscendBreakingTo(to.getX(), to.getY(), to.getZ())) return COST_INF;

        double ticks = 0.0;
        for (int dy = 0; dy <= 1; dy++) {
            if (ctx.isPassable(to.getX(), to.getY() + dy, to.getZ())) continue;
            double t = ctx.blockBreakTicks(to.getX(), to.getY() + dy, to.getZ());
            if (Double.isInfinite(t)) return COST_INF;
            ticks += t;
        }

        double overhead = ctx.standingHeadroomBreakCost(to.getX(), to.getY(), to.getZ());
        if (Double.isInfinite(overhead)) return COST_INF;

        cost = JUMP_ONE_BLOCK_COST + ticks + overhead;
        return cost;
    }

    @Override
    public MovementState tick(CalculationContext ctx) {
        if (totalTicks > MAX_TICKS) {
            ctx.releaseAllInputs();
            return MovementState.FAILED;
        }
        totalTicks++;

        boolean lowerOpen = ctx.isLivePassable(to.getX(), to.getY(), to.getZ())
                || ctx.isPassable(to.getX(), to.getY(), to.getZ());
        boolean upperOpen = ctx.isLivePassable(to.getX(), to.getY() + 1, to.getZ())
                || ctx.isPassable(to.getX(), to.getY() + 1, to.getZ());
        boolean headroom = ctx.isLivePassable(to.getX(), to.getY() + 2, to.getZ())
                || ctx.isPassable(to.getX(), to.getY() + 2, to.getZ());

        if (lowerOpen && upperOpen && headroom) {
            ctx.setInput("attack", false);
            ctx.cache().markAir(to.getX(), to.getY(), to.getZ());
            ctx.cache().markAir(to.getX(), to.getY() + 1, to.getZ());
            ctx.cache().markAir(to.getX(), to.getY() + 2, to.getZ());

            if (ctx.playerFeetBlock() != null && ctx.playerFeetBlock().equals(to)
                    && ctx.playerOnGround()) {
                ctx.setInput("jump", false);
                return MovementState.SUCCESS;
            }

            ctx.lookAt(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
            ctx.setInput("forward", true);
            ctx.setInput("sprint", true);
            ctx.setInput("jump", true);
            return MovementState.RUNNING;
        }

        int targetY = lowerOpen ? to.getY() + 1 : to.getY();
        ctx.lookAtDirect(to.getX() + 0.5, targetY + 0.5, to.getZ() + 0.5);
        ctx.setInput("attack", true);
        return MovementState.RUNNING;
    }

    @Override
    public boolean isValid(CalculationContext ctx) {
        return calculateCost(ctx) < COST_INF;
    }
}
