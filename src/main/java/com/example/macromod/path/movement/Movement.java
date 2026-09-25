package com.example.macromod.path.movement;

import com.example.macromod.path.calc.CalculationContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public abstract class Movement {

    public final BlockPos from;
    public final BlockPos to;
    protected double cost = -1;
    protected int ticksRunning = 0;
    private double prevX = Double.NaN;
    private double prevZ = Double.NaN;
    private int stillTicks = 0;

    public Movement(BlockPos from, BlockPos to) {
        this.from = from;
        this.to = to;
    }

    public abstract double calculateCost(CalculationContext ctx);
    public abstract MovementState tick(CalculationContext ctx);
    public abstract boolean isValid(CalculationContext ctx);

    public double getCost() { return cost; }
    public BlockPos getFrom() { return from; }
    public BlockPos getTo() { return to; }

    protected boolean arrived(CalculationContext ctx) {
        BlockPos feet = ctx.playerFeetBlock();
        return feet != null && feet.equals(to);
    }

    protected boolean stuck(int maxTicks) {
        ticksRunning++;
        if (ticksRunning > maxTicks) return true;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        double px = mc.player.getX();
        double pz = mc.player.getZ();

        if (!Double.isNaN(prevX)) {
            double movedSq = (px - prevX) * (px - prevX) + (pz - prevZ) * (pz - prevZ);
            if (movedSq < 0.0009) {
                stillTicks++;
                if (stillTicks > 20) return true;
            } else {
                stillTicks = 0;
            }
        }
        prevX = px;
        prevZ = pz;
        return false;
    }

    public enum MovementState {
        PREPARE, RUNNING, SUCCESS, FAILED
    }
}