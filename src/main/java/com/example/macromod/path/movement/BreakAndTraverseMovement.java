package com.example.macromod.path.movement;

import com.example.macromod.path.calc.CalculationContext;
import net.minecraft.core.BlockPos;

public class BreakAndTraverseMovement extends Movement {

    public static final double COST = 4.633 + 12.0;

    public BreakAndTraverseMovement(BlockPos from, BlockPos to) {
        super(from, to);
    }

    @Override
    public double calculateCost(CalculationContext ctx) {
        if (!ctx.canBreak(to)) return Double.POSITIVE_INFINITY;
        if (!ctx.isSolidGround(to.getX(), to.getY() - 1, to.getZ())) return Double.POSITIVE_INFINITY;

        boolean obstruction = !ctx.isPassable(to.getX(), to.getY(), to.getZ())
                || !ctx.isPassable(to.getX(), to.getY() + 1, to.getZ());
        if (!obstruction) return Double.POSITIVE_INFINITY;

        cost = COST;
        return cost;
    }

    @Override
    public MovementState tick(CalculationContext ctx) {
        ctx.lookAt(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
        ctx.setInput("attack", true);

        if (ctx.isPassable(to.getX(), to.getY(), to.getZ())
                && ctx.isPassable(to.getX(), to.getY() + 1, to.getZ())) {
            ctx.setInput("attack", false);
            ctx.setInput("forward", true);
            if (ctx.playerNear(to, 0.9)) {
                ctx.setInput("forward", false);
                return MovementState.SUCCESS;
            }
            return MovementState.RUNNING;
        }
        return MovementState.RUNNING;
    }

    @Override
    public boolean isValid(CalculationContext ctx) {
        return calculateCost(ctx) < Double.POSITIVE_INFINITY;
    }
}