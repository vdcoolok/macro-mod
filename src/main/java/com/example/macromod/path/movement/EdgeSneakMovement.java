package com.example.macromod.path.movement;

import com.example.macromod.path.calc.CalculationContext;
import net.minecraft.core.BlockPos;

public class EdgeSneakMovement extends Movement {

    public static final double COST = 8.0;

    public EdgeSneakMovement(BlockPos from, BlockPos to) {
        super(from, to);
    }

    @Override
    public double calculateCost(CalculationContext ctx) {
        if (!ctx.isPassable(to.getX(), to.getY(), to.getZ())) return Double.POSITIVE_INFINITY;
        if (!ctx.isSolidGround(to.getX(), to.getY() - 1, to.getZ())) return Double.POSITIVE_INFINITY;
        cost = COST;
        return cost;
    }

    @Override
    public MovementState tick(CalculationContext ctx) {
        ctx.setInput("forward", true);
        ctx.setInput("sneak", true);
        ctx.lookAt(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);

        if (ctx.playerNear(to, 0.5)) {
            ctx.releaseAllInputs();
            return MovementState.SUCCESS;
        }
        return MovementState.RUNNING;
    }

    @Override
    public boolean isValid(CalculationContext ctx) {
        return calculateCost(ctx) < Double.POSITIVE_INFINITY;
    }
}