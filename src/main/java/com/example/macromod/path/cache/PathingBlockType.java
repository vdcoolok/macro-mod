package com.example.macromod.path.cache;

public enum PathingBlockType {
    AIR(0),
    WATER(1),
    AVOID(2),
    SOLID(3);

    public final int bits;
    PathingBlockType(int bits) { this.bits = bits; }

    public static PathingBlockType fromOrdinal(int o) {
        return values()[o & 3];
    }
}