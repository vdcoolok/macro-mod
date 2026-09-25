package com.example.macromod.path.calc;

public interface ActionCosts {
    double WALK_ONE_BLOCK_COST = 20.0 / 4.317;
    double SPRINT_ONE_BLOCK_COST = 20.0 / 5.612;
    double SNEAK_ONE_BLOCK_COST = 20.0 / 1.3;
    double WALK_OFF_BLOCK_COST = WALK_ONE_BLOCK_COST * 0.8;
    double JUMP_ONE_BLOCK_COST = 5.72854;
    double FALL_1_25_BLOCKS_COST = 5.72854;
    double FALL_0_25_BLOCKS_COST = 0.61512;
    double FALL_ONE_BLOCK_COST = 5.11354;
    double FALL_TWO_BLOCK_COST = 7.28283;
    double FALL_THREE_BLOCK_COST = 8.96862;
    double[] FALL_N_BLOCKS_COST = {
        0.0,
        FALL_ONE_BLOCK_COST,
        FALL_TWO_BLOCK_COST,
        FALL_THREE_BLOCK_COST
    };
    double CENTER_AFTER_FALL_COST = WALK_ONE_BLOCK_COST - WALK_OFF_BLOCK_COST;
    double JUMP_PENALTY = 2.0;
    double BLOCK_PLACEMENT_PENALTY = 20.0;
    double COST_INF = 1000000.0;
}