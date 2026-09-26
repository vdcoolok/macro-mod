package com.example.macromod.path.goal;

import com.example.macromod.path.calc.ActionCosts;

public class GoalXZ implements Goal, ActionCosts {

    private final int x, z;
    private static final double SQRT_2 = Math.sqrt(2);
    private static final double COST_HEURISTIC = SPRINT_ONE_BLOCK_COST;

    public GoalXZ(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int getX() { return x; }
    public int getZ() { return z; }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        return this.x == x && this.z == z;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        int xDiff = this.x - x;
        int zDiff = this.z - z;
        int min = Math.min(Math.abs(xDiff), Math.abs(zDiff));
        int max = Math.max(Math.abs(xDiff), Math.abs(zDiff));
        return (SQRT_2 * min + (max - min)) * COST_HEURISTIC;
    }
}
