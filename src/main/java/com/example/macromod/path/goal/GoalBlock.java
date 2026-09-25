package com.example.macromod.path.goal;

import com.example.macromod.path.calc.ActionCosts;
import net.minecraft.core.BlockPos;

public class GoalBlock implements Goal, ActionCosts {

    private static final double SQRT_2 = Math.sqrt(2);

    private final int x, y, z;
    private final double exactX, exactY, exactZ;
    private final boolean hasExact;

    public GoalBlock(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.exactX = x + 0.5;
        this.exactY = y;
        this.exactZ = z + 0.5;
        this.hasExact = false;
    }

    public GoalBlock(double exactX, double exactY, double exactZ) {
        this.x = (int) Math.floor(exactX);
        this.y = (int) Math.floor(exactY);
        this.z = (int) Math.floor(exactZ);
        this.exactX = exactX;
        this.exactY = exactY;
        this.exactZ = exactZ;
        this.hasExact = true;
    }

    public GoalBlock(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    public double getExactX() { return exactX; }
    public double getExactY() { return exactY; }
    public double getExactZ() { return exactZ; }
    public boolean hasExact() { return hasExact; }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        return this.x == x && this.y == y && this.z == z;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        return goalYLevel(y - this.y) + goalXZ(x - this.x, z - this.z);
    }

    private static double goalYLevel(int yDiff) {
        if (yDiff > 0) return yDiff * FALL_ONE_BLOCK_COST;
        if (yDiff < 0) return -yDiff * JUMP_ONE_BLOCK_COST;
        return 0;
    }

    private static double goalXZ(double xDiff, double zDiff) {
        double ax = Math.abs(xDiff);
        double az = Math.abs(zDiff);
        double straight = Math.max(ax, az) - Math.min(ax, az);
        double diagonal = Math.min(ax, az) * SQRT_2;
        return diagonal + straight;
    }
}
