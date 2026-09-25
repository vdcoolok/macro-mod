package com.example.macromod.path.movement;

import com.example.macromod.path.calc.CalculationContext;
import net.minecraft.core.BlockPos;

public class SprintChainMovement extends Movement {

    private final int chainLength;
    private final boolean ascend;

    public SprintChainMovement(BlockPos from, BlockPos to, int chainLength, boolean ascend) {
        super(from, to);
        this.chainLength = chainLength;
        this.ascend = ascend;
    }

    @Override
    public double calculateCost(CalculationContext ctx) {
        if (!ctx.isPassable(to.getX(), to.getY(), to.getZ())) return Double.POSITIVE_INFINITY;
        if (!ctx.isSolidGround(to.getX(), to.getY() - 1, to.getZ())) return Double.POSITIVE_INFINITY;

        cost = chainLength * 3.564 + (ascend ? 2.5 : 0);
        return cost;
    }

    @Override
    public MovementState tick(CalculationContext ctx) {
        ctx.setInput("forward", true);
        ctx.setInput("sprint", true);
        ctx.lookAt(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);

        if (ctx.playerNear(to, 1.0) && ctx.playerOnGround()) {
            ctx.setInput("forward", false);
            ctx.setInput("sprint", false);
            return MovementState.SUCCESS;
        }
        return MovementState.RUNNING;
    }

    @Override
    public boolean isValid(CalculationContext ctx) {
        return calculateCost(ctx) < Double.POSITIVE_INFINITY;
    }
}