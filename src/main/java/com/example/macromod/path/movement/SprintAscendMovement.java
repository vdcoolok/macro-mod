package com.example.macromod.path.movement;

import com.example.macromod.path.calc.CalculationContext;
import net.minecraft.core.BlockPos;

public class SprintAscendMovement extends Movement {

    public static final double COST = 7.12;
    private final boolean sprintFromFlat;

    public SprintAscendMovement(BlockPos from, BlockPos to, boolean sprintFromFlat) {
        super(from, to);
        this.sprintFromFlat = sprintFromFlat;
    }

    @Override
    public double calculateCost(CalculationContext ctx) {
        if (!ctx.isPassable(to.getX(), to.getY(), to.getZ())) return Double.POSITIVE_INFINITY;
        if (!ctx.isPassable(to.getX(), to.getY() + 1, to.getZ())) return Double.POSITIVE_INFINITY;
        if (!ctx.isSolidGround(to.getX(), to.getY() - 1, to.getZ())) return Double.POSITIVE_INFINITY;
        if (!ctx.isPassable(from.getX(), from.getY() + 2, from.getZ())) return Double.POSITIVE_INFINITY;

        double base = COST;
        if (sprintFromFlat) base -= 1.2;
        cost = base;
        return cost;
    }

    @Override
    public MovementState tick(CalculationContext ctx) {
        ctx.setInput("forward", true);
        ctx.setInput("jump", true);
        ctx.setInput("sprint", true);
        ctx.lookAt(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);

        if (ctx.playerNear(to, 0.9) && ctx.playerOnGround()) {
            ctx.setInput("forward", false);
            ctx.setInput("jump", false);
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