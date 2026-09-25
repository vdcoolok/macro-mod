package com.example.macromod.path.cache;

import net.minecraft.world.level.block.Block;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class CachedChunk {

    public static final int SIZE = 16 * 16 * 384;
    public static final int Y_OFFSET = 64;
    public static final byte UNBREAKABLE = (byte) 255;

    private final BitSet data;
    private final byte[] breakTicks;
    private final Map<Integer, Block> specialBlocks;
    public final int x, z;
    private boolean loaded;

    public CachedChunk(int x, int z) {
        this.x = x;
        this.z = z;
        this.data = new BitSet(SIZE * 2);
        this.breakTicks = new byte[SIZE];
        this.specialBlocks = new HashMap<>();
        this.loaded = false;
    }

    public synchronized void set(int lx, int y, int lz, PathingBlockType type) {
        int index = index(lx, y, lz);
        if (index < 0 || index >= SIZE) return;
        setBits(index, type.bits);
    }

    public synchronized PathingBlockType get(int lx, int y, int lz) {
        int index = index(lx, y, lz);
        if (index < 0 || index >= SIZE) return PathingBlockType.AVOID;
        return PathingBlockType.fromOrdinal(getBits(index));
    }

    public boolean isLoaded() { return loaded; }
    public void markLoaded() { loaded = true; }

    public byte getBreakTicks(int lx, int y, int lz) {
        int index = index(lx, y, lz);
        if (index < 0 || index >= SIZE) return UNBREAKABLE;
        return breakTicks[index];
    }

    public void setBreakTicks(int lx, int y, int lz, byte ticks) {
        int index = index(lx, y, lz);
        if (index < 0 || index >= SIZE) return;
        breakTicks[index] = ticks;
    }

    public Block getSpecialBlock(int lx, int y, int lz) {
        int index = index(lx, y, lz);
        if (index < 0 || index >= SIZE) return null;
        return specialBlocks.get(index);
    }

    public void setSpecialBlock(int lx, int y, int lz, Block block) {
        int index = index(lx, y, lz);
        if (index < 0 || index >= SIZE) return;
        specialBlocks.put(index, block);
    }

    private static int index(int x, int y, int z) {
        int yo = y + Y_OFFSET;
        if (yo < 0 || yo >= 384) return -1;
        return (yo * 256) + (z * 16) + x;
    }

    private void setBits(int index, int value) {
        int bitIndex = index * 2;
        for (int i = 0; i < 2; i++) {
            data.set(bitIndex + i, ((value >> i) & 1) == 1);
        }
    }

    private int getBits(int index) {
        int bitIndex = index * 2;
        int value = 0;
        for (int i = 0; i < 2; i++) {
            if (data.get(bitIndex + i)) value |= (1 << i);
        }
        return value;
    }
}
