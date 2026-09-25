package com.example.macromod.path.movement;

import com.example.macromod.path.calc.ActionCosts;
import com.example.macromod.path.calc.CalculationContext;
import net.minecraft.core.BlockPos;

public class FallMovement extends Movement implements ActionCosts {

    private static final int MAX_TICKS = 80;

    private final int fallDistance;
    private int totalTicks = 0;

    public FallMovement(BlockPos from, BlockPos to, int fallDistance) {
        super(from, to);
        this.fallDistance = fallDistance;
    }

    @Override
    public double calculateCost(CalculationContext ctx) {
        if (fallDistance < 1 || fallDistance > ctx.maxFallDistance()) return COST_INF;
        if (!ctx.isSolidGround(to.getX(), to.getY() - 1, to.getZ())) return COST_INF;
        if (!ctx.isPassable(to.getX(), to.getY(), to.getZ())) return COST_INF;
        if (!ctx.isPassable(to.getX(), to.getY() + 1, to.getZ())) return COST_INF;
        if (!ctx.hasStandingHeadroom(to.getX(), to.getY(), to.getZ())) return COST_INF;
        cost = WALK_OFF_BLOCK_COST + FALL_N_BLOCKS_COST[fallDistance];
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