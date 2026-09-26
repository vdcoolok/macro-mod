package com.example.macromod.path.cache;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.shapes.VoxelShape;

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
                    cached.setBreakTicks(lx, y, lz, breakTicksFor(state, level, pos));
                }
            }
        }
        cached.markLoaded();
        return cached;
    }

    public static PathingBlockType getPathingBlockType(BlockState state) {
        return getPathingBlockType(state, null, null);
    }

    public static PathingBlockType getPathingBlockType(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.isAir()) return PathingBlockType.AIR;

        if (state.is(Blocks.WATER)) return PathingBlockType.WATER;
        if (state.is(Blocks.LAVA)) return PathingBlockType.AVOID;

        if (!state.getFluidState().isEmpty()) {
            return state.getFluidState().isSource()
                ? PathingBlockType.WATER
                : PathingBlockType.AVOID;
        }

        if (state.getBlock() instanceof SnowLayerBlock) {
            return state.getValue(SnowLayerBlock.LAYERS) <= 3
                ? PathingBlockType.AIR
                : PathingBlockType.AVOID;
        }

        if (state.getBlock() instanceof net.minecraft.world.level.block.StairBlock) {
            return PathingBlockType.SOLID;
        }

        if (state.is(Blocks.POWDER_SNOW)) return PathingBlockType.AVOID;

        if (state.getBlock() instanceof LeavesBlock) return PathingBlockType.SOLID;

        if (isTinyCollisionPlant(state)) return PathingBlockType.AIR;

        if (isWalkableGround(state, level, pos)) return PathingBlockType.SOLID;

        try {
            return state.blocksMotion() ? PathingBlockType.SOLID : PathingBlockType.AIR;
        } catch (Exception e) {
            return PathingBlockType.SOLID;
        }
    }

    private static boolean isTinyCollisionPlant(BlockState state) {
        try {
            if (!(state.getBlock() instanceof net.minecraft.world.level.block.BushBlock)
                    && !(state.getBlock() instanceof net.minecraft.world.level.block.CropBlock)
                    && !(state.getBlock() instanceof net.minecraft.world.level.block.NetherWartBlock)) {
                return false;
            }
            if (isHazardBlock(state)) return false;
            return collisionHeight(state, null, null) < 0.2;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isWalkableGround(BlockState state, BlockGetter level, BlockPos pos) {
        if (level == null || pos == null) return false;
        if (isHazardBlock(state)) return false;

        double height = collisionHeight(state, level, pos);
        if (height <= 0.0 || height > 0.9) return false;

        if (state.getBlock() instanceof TrapDoorBlock && Boolean.TRUE.equals(state.getValue(TrapDoorBlock.OPEN))) {
            return false;
        }

        if (state.getBlock() instanceof net.minecraft.world.level.block.SlabBlock
                && state.getValue(net.minecraft.world.level.block.SlabBlock.TYPE) == SlabType.TOP) {
            return false;
        }

        return true;
    }

    private static double collisionHeight(BlockState state, BlockGetter level, BlockPos pos) {
        VoxelShape shape;
        try {
            shape = state.getCollisionShape(level, pos);
        } catch (Exception e) {
            return 0.0;
        }
        try {
            return shape.max(Direction.Axis.Y);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static boolean isSolidGround(BlockState state, BlockGetter level, BlockPos pos) {
        return getPathingBlockType(state, level, pos) == PathingBlockType.SOLID;
    }

    public static byte breakTicksFor(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.isAir()) return 0;
        if (state.is(Blocks.BEDROCK)
                || state.is(Blocks.BARRIER)
                || state.is(Blocks.REINFORCED_DEEPSLATE)
                || state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.CRYING_OBSIDIAN)
                || state.is(Blocks.NETHERITE_BLOCK)
                || state.is(Blocks.END_PORTAL_FRAME)
                || state.is(Blocks.SPAWNER)) {
            return CachedChunk.UNBREAKABLE;
        }

        float hardness;
        try {
            hardness = state.getDestroySpeed(level, pos);
        } catch (Exception e) {
            return CachedChunk.UNBREAKABLE;
        }
        if (hardness < 0) return CachedChunk.UNBREAKABLE;

        float seconds = hardness * 1.5f;
        if (state.is(Blocks.CHEST)) seconds += 1.0f;

        return (byte) Math.min(120, Math.round(seconds * 20.0f));
    }

    private static boolean isHazardBlock(BlockState state) {
        return state.is(Blocks.SWEET_BERRY_BUSH)
            || state.is(Blocks.CACTUS)
            || state.is(Blocks.FIRE)
            || state.is(Blocks.SOUL_FIRE)
            || state.is(Blocks.MAGMA_BLOCK)
            || state.is(Blocks.CAMPFIRE)
            || state.is(Blocks.SOUL_CAMPFIRE)
            || state.is(Blocks.COBWEB)
            || state.is(Blocks.WITHER_ROSE)
            || state.is(Blocks.POINTED_DRIPSTONE);
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
