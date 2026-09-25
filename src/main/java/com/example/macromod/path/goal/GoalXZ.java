package com.example.macromod.path.goal;

public class GoalXZ implements Goal {

    private final int x, z;
    private static final double SQRT_2 = Math.sqrt(2);

    public GoalXZ(int x, int z) {
        this.x = x;
        this.z = z;
    }

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
        return SQRT_2 * min + (max - min);
    }
}