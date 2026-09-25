package com.example.macromod.path.movement;

import com.example.macromod.path.calc.ActionCosts;
import com.example.macromod.path.calc.CalculationContext;
import net.minecraft.core.BlockPos;

public class TraverseMovement extends Movement implements ActionCosts {

    private static final int MAX_TICKS = 120;

    private int totalTicks = 0;

    public TraverseMovement(BlockPos from, BlockPos to) {
        super(from, to);
    }

    @Override
    public double calculateCost(CalculationContext ctx) {
        if (!ctx.isPassable(to.getX(), to.getY(), to.getZ())) return COST_INF;
        if (!ctx.isPassable(to.getX(), to.getY() + 1, to.getZ())) return COST_INF;
        if (!ctx.isSolidGround(to.getX(), to.getY() - 1, to.getZ())) return COST_INF;
        if (ctx.isHazard(to.getX(), to.getY(), to.getZ())) return COST_INF;

        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        if (dx != 0 && dz != 0) {
            if (!ctx.canFitThroughDiagonal(from.getX(), from.getY(), from.getZ(), dx, dz)) return COST_INF;
            if (!ctx.canExitDiagonal(to.getX(), from.getY(), to.getZ(), dx, dz)) return COST_INF;
            cost = WALK_ONE_BLOCK_COST * 1.41421356;
        } else {
            cost = WALK_ONE_BLOCK_COST;
        }
        return cost;
    }

    @Override
    public MovementState tick(CalculationContext ctx) {
        if (totalTicks > MAX_TICKS) {
            ctx.releaseAllInputs();
            return MovementState.FAILED;
        }
        totalTicks++;

        ctx.lookAt(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
        ctx.setInput("forward", true);
        ctx.setInput("sprint", true);
        ctx.setInput("jump", false);
        ctx.setInput("back", false);

        BlockPos feet = ctx.playerFeetBlock();
        if (feet != null && feet.equals(to)) {
            return MovementState.SUCCESS;
        }
        return MovementState.RUNNING;
    }

    @Override
    public boolean isValid(CalculationContext ctx) {
        return calculateCost(ctx) < COST_INF;
    }
}