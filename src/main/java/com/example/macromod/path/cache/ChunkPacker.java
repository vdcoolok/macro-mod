package com.example.macromod.path.cache;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
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
                    cached.set(lx, y, lz, getPathingBlockType(state));

                    if (isSpecialBlock(state)) {
                        cached.setSpecialBlock(lx, y, lz, state.getBlock());
                    }
                    cached.setBreakTicks(lx, y, lz, breakTicksFor(state));
                }
            }
        }
        cached.markLoaded();
        return cached;
    }

    public static PathingBlockType getPathingBlockType(BlockState state) {
        if (state.isAir()) return PathingBlockType.AIR;

        if (state.is(Blocks.WATER)) return PathingBlockType.WATER;
        if (state.is(Blocks.LAVA)) return PathingBlockType.AVOID;

        if (!state.getFluidState().isEmpty()) {
            return state.getFluidState().isSource()
                ? PathingBlockType.WATER
                : PathingBlockType.AVOID;
        }

        if (state.getBlock() instanceof LeavesBlock) return PathingBlockType.SOLID;

        try {
            return state.blocksMotion() ? PathingBlockType.SOLID : PathingBlockType.AIR;
        } catch (Exception e) {
            return PathingBlockType.SOLID;
        }
    }

    public static byte breakTicksFor(BlockState state) {
        if (state.isAir()) return 0;
        if (state.is(Blocks.BEDROCK)
                || state.is(Blocks.BARRIER)
                || state.is(Blocks.REINFORCED_DEEPSLATE)
                || state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.CRYING_OBSIDIAN)
                || state.is(Blocks.NETHERITE_BLOCK)
                || state.is(Blocks.END_PORTAL_FRAME)
                || state.is(Blocks.SPAWNER)
                || state.is(Blocks.END_CHEST)) {
            return CachedChunk.UNBREAKABLE;
        }

        float hardness;
        try {
            hardness = state.destroySpeed;
        } catch (Exception e) {
            return CachedChunk.UNBREAKABLE;
        }
        if (hardness < 0) return CachedChunk.UNBREAKABLE;

        float seconds = hardness * 1.5f;
        if (state.is(Blocks.CHEST)) seconds += 1.0f;

        return (byte) Math.min(120, Math.round(seconds * 20.0f));
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
