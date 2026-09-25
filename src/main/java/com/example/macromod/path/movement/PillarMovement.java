package com.example.macromod.path.movement;

import com.example.macromod.path.calc.CalculationContext;
import net.minecraft.core.BlockPos;

public class PillarMovement extends Movement {

    public static final double COST = 15.0;

    public PillarMovement(BlockPos from, BlockPos to) {
        super(from, to);
    }

    @Override
    public double calculateCost(CalculationContext ctx) {
        if (!ctx.hasThrowawayBlock()) return Double.POSITIVE_INFINITY;
        if (!ctx.isPassable(to.getX(), to.getY(), to.getZ())) return Double.POSITIVE_INFINITY;
        if (!ctx.isPassable(to.getX(), to.getY() + 1, to.getZ())) return Double.POSITIVE_INFINITY;

        cost = COST + ctx.blockPlacePenalty();
        return cost;
    }

    @Override
    public MovementState tick(CalculationContext ctx) {
        ctx.setInput("sneak", true);
        ctx.setInput("jump", true);
        ctx.lookAt(to.getX() + 0.5, to.getY() - 1.0, to.getZ() + 0.5);

        if (ctx.playerNear(to, 0.5)) {
            ctx.setInput("sneak", false);
            ctx.setInput("jump", false);
            return MovementState.SUCCESS;
        }
        return MovementState.RUNNING;
    }

    @Override
    public boolean isValid(CalculationContext ctx) {
        return calculateCost(ctx) < Double.POSITIVE_INFINITY;
    }
}