package com.example.macromod.path.movement;

import com.example.macromod.path.calc.ActionCosts;
import com.example.macromod.path.calc.CalculationContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;

public class AscendMovement extends Movement implements ActionCosts {

    private static final int MAX_TICKS = 80;

    public AscendMovement(BlockPos from, BlockPos to) {
        super(from, to);
    }

    @Override
    public double calculateCost(CalculationContext ctx) {
        if (!ctx.canAscendTo(to.getX(), to.getY(), to.getZ())) return COST_INF;
        if (!ctx.isPassable(from.getX(), from.getY() + 2, from.getZ())) return COST_INF;
        cost = JUMP_ONE_BLOCK_COST;
        return cost;
    }

    @Override
    public MovementState tick(CalculationContext ctx) {
        if (stuck(MAX_TICKS)) {
            ctx.releaseAllInputs();
            return MovementState.FAILED;
        }

        if (arrived(ctx)) {
            ctx.setInput("jump", false);
            return MovementState.SUCCESS;
        }

        ctx.lookAt(to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5);
        ctx.setInput("forward", true);
        ctx.setInput("sprint", true);

        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return MovementState.FAILED;

        double dx = (to.getX() + 0.5) - p.getX();
        double dz = (to.getZ() + 0.5) - p.getZ();
        double distSq = dx * dx + dz * dz;

        if (!ctx.isPassable(from.getX(), from.getY() + 2, from.getZ())
                || !ctx.isPassable(to.getX(), to.getY() + 1, to.getZ())) {
            ctx.setInput("jump", false);
            return MovementState.RUNNING;
        }

        if (distSq > 1.44) {
            ctx.setInput("jump", false);
            return MovementState.RUNNING;
        }

        ctx.setInput("jump", true);
        return MovementState.RUNNING;
    }

    @Override
    public boolean isValid(CalculationContext ctx) {
        return calculateCost(ctx) < COST_INF;
    }
}