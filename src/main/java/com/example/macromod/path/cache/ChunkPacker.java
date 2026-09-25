package com.example.macromod.path.cache;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public class ChunkPacker {

    public static CachedChunk pack(LevelChunk chunk, Level level) {
        CachedChunk cached = new CachedChunk(chunk.getPos().x(), chunk.getPos().z());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int y = level.getMinY(); y < level.getMaxY(); y++) {
                    pos.set(chunk.getPos().getMinBlockX() + lx, y, chunk.getPos().getMinBlockZ() + lz);
                    BlockState state = chunk.getBlockState(pos);
                    cached.set(lx, y, lz, getPathingBlockType(state, level, pos));

                    if (isSpecialBlock(state)) {
                        cached.setSpecialBlock(lx, y, lz, state.getBlock());
                    }
                }
            }
        }
        cached.markLoaded();
        return cached;
    }

    public static PathingBlockType getPathingBlockType(BlockState state, Level level, BlockPos pos) {
        if (state.isAir()) return PathingBlockType.AIR;
        if (state.is(Blocks.WATER)) return PathingBlockType.WATER;
        if (state.is(Blocks.LAVA)) return PathingBlockType.AVOID;

        if (state.getFluidState().isSource() && !state.getFluidState().isEmpty()) {
            return PathingBlockType.WATER;
        }
        if (!state.getFluidState().isEmpty()) return PathingBlockType.AVOID;

        try {
            return state.getCollisionShape(level, pos).isEmpty()
                    ? PathingBlockType.AIR
                    : PathingBlockType.SOLID;
        } catch (Exception e) {
            return PathingBlockType.SOLID;
        }
    }

    private static boolean isSpecialBlock(BlockState state) {
        return state.is(Blocks.DIAMOND_ORE)
            || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)
            || state.is(Blocks.CHEST)
            || state.is(Blocks.SPAWNER)
            || state.is(Blocks.IRON_ORE)
            || state.is(Blocks.GOLD_ORE)
            || state.is(Blocks.ANCIENT_DEBRIS);
    }
}