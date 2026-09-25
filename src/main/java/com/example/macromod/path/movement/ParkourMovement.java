package com.example.macromod.path.movement;

import com.example.macromod.path.calc.ActionCosts;
import com.example.macromod.path.calc.CalculationContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class ParkourMovement extends Movement implements ActionCosts {

    public static final int MAX_GAP = 4;
    private static final int MAX_TICKS = 100;

    private final int gapSize;
    private final int dirX;
    private final int dirZ;
    private final int dy;

    private int totalTicks = 0;
    private boolean jumped = false;

    public ParkourMovement(BlockPos from, BlockPos to, int gapSize) {
        super(from, to);
        this.gapSize = gapSize;
        this.dirX = Integer.signum(to.getX() - from.getX());
        this.dirZ = Integer.signum(to.getZ() - from.getZ());
        this.dy = to.getY() - from.getY();
    }

    @Override
    public double calculateCost(CalculationContext ctx) {
        if (gapSize < 1 || gapSize > MAX_GAP) return COST_INF;
        if (dy > 1 || dy < -3) return COST_INF;
        if (dy == 1 && gapSize > 3) return COST_INF;

        if (!ctx.isSolidGround(from.getX(), from.getY() - 1, from.getZ())) return COST_INF;
        if (!ctx.isPassable(from.getX(), from.getY(), from.getZ())) return COST_INF;
        if (!ctx.isPassable(from.getX(), from.getY() + 1, from.getZ())) return COST_INF;
        if (!ctx.isPassable(from.getX(), from.getY() + 2, from.getZ())) return COST_INF;

        if (!ctx.isSolidGround(to.getX(), to.getY() - 1, to.getZ())) return COST_INF;
        if (!ctx.isPassable(to.getX(), to.getY(), to.getZ())) return COST_INF;
        if (!ctx.isPassable(to.getX(), to.getY() + 1, to.getZ())) return COST_INF;

        int maxFlightY = Math.max(from.getY(), to.getY()) + 1;

        for (int i = 1; i <= gapSize; i++) {
            int cx = from.getX() + dirX * i;
            int cz = from.getZ() + dirZ * i;

            for (int y = Math.min(from.getY(), to.getY()); y <= maxFlightY + 1; y++) {
                if (!ctx.isPassable(cx, y, cz)) return COST_INF;
            }

            if (ctx.isHazard(cx, from.getY(), cz)) return COST_INF;

            if (ctx.isSolidGround(cx, from.getY() - 1, cz)) {
                return COST_INF;
            }
        }

        double base = SPRINT_ONE_BLOCK_COST * (gapSize + 1);
        if (dy > 0) base += JUMP_ONE_BLOCK_COST;
        else if (dy < 0) base += FALL_N_BLOCKS_COST[-dy] + 5.0;

        cost = base + JUMP_PENALTY;
        if (gapSize == 4) cost += 1.0;
        return cost;
    }

    @Override
    public MovementState tick(CalculationContext ctx) {
        totalTicks++;
        if (totalTicks > MAX_TICKS) {
            ctx.releaseAllInputs();
            return MovementState.FAILED;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return MovementState.FAILED;

        BlockPos feet = ctx.playerFeetBlock();
        if (feet == null) return MovementState.FAILED;

        double px = p.getX();
        double py = p.getY();
        double pz = p.getZ();

        boolean atDestFeet = feet.equals(to);
        boolean atDestBox = Math.abs(px - (to.getX() + 0.5)) <= 0.6
                         && Math.abs(pz - (to.getZ() + 0.5)) <= 0.6
                         && Math.abs(py - to.getY()) <= 0.5;

        if (jumped && p.onGround() && (atDestFeet || atDestBox)) {
            ctx.setInput("jump", false);
            return MovementState.SUCCESS;
        }
        if (!jumped && atDestFeet) {
            ctx.setInput("jump", false);
            return MovementState.SUCCESS;
        }

        double fallLimit = Math.min(from.getY(), to.getY()) - 0.6;
        if (py < fallLimit) {
            ctx.releaseAllInputs();
            return MovementState.FAILED;
        }

        if (jumped) {
            ctx.lookAt(to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5);
            ctx.setInput("forward", true);
            ctx.setInput("sprint", true);
            ctx.setInput("jump", false);
            ctx.setInput("back", false);
            return MovementState.RUNNING;
        }

        ctx.lookAt(to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5);

        Vec3 vel = p.getDeltaMovement();
        double vx = vel.x;
        double vz = vel.z;

        double cliffEdgeX = (dirX > 0) ? (from.getX() + 1.28) : (from.getX() - 0.28);
        double distToCliffX = (dirX > 0) ? (cliffEdgeX - px) : (px - cliffEdgeX);

        double cliffEdgeZ = (dirZ > 0) ? (from.getZ() + 1.28) : (from.getZ() - 0.28);
        double distToCliffZ = (dirZ > 0) ? (cliffEdgeZ - pz) : (pz - cliffEdgeZ);

        double distToCliff = (dirX != 0) ? distToCliffX : distToCliffZ;
        double forwardSpeed = Math.max(0.0, vx * dirX + vz * dirZ);

        ctx.setInput("back", false);
        ctx.setInput("forward", true);
        ctx.setInput("sprint", true);

        boolean shouldJump = p.onGround() && forwardSpeed > 0.05
                && distToCliff <= (forwardSpeed + 0.03);

        if (shouldJump) {
            ctx.setInput("jump", true);
            jumped = true;
            return MovementState.RUNNING;
        }

        ctx.setInput("jump", false);
        return MovementState.RUNNING;
    }

    @Override
    public boolean isValid(CalculationContext ctx) {
        return calculateCost(ctx) < COST_INF;
    }
}