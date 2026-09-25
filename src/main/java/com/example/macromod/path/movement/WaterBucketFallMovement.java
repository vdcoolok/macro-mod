package com.example.macromod.path.movement;

import com.example.macromod.path.calc.CalculationContext;
import net.minecraft.core.BlockPos;

public class WaterBucketFallMovement extends Movement {

    private final int fallDistance;

    public WaterBucketFallMovement(BlockPos from, BlockPos to, int fallDistance) {
        super(from, to);
        this.fallDistance = fallDistance;
    }

    @Override
    public double calculateCost(CalculationContext ctx) {
        if (!ctx.hasWaterBucket()) return Double.POSITIVE_INFINITY;
        if (!ctx.isSolidGround(to.getX(), to.getY() - 1, to.getZ())) return Double.POSITIVE_INFINITY;
        if (!ctx.isPassable(to.getX(), to.getY(), to.getZ())) return Double.POSITIVE_INFINITY;

        cost = 3.0 + fallDistance * 0.5 + 10.0;
        return cost;
    }

    @Override
    public MovementState tick(CalculationContext ctx) {
        ctx.setInput("forward", true);
        ctx.lookAt(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);

        if (ctx.playerNear(to, 0.9) && ctx.playerOnGround()) {
            ctx.setInput("forward", false);
            return MovementState.SUCCESS;
        }
        return MovementState.RUNNING;
    }

    @Override
    public boolean isValid(CalculationContext ctx) {
        return calculateCost(ctx) < Double.POSITIVE_INFINITY;
    }
}