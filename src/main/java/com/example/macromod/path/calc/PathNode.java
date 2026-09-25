package com.example.macromod.path.calc;

import com.example.macromod.path.movement.Movement;
import net.minecraft.core.BlockPos;

public class PathNode implements Comparable<PathNode> {

    public final int x, y, z;
    public PathNode parent;
    public double cost;
    public double estimatedCost;
    public boolean closed;
    public Movement movementToReach;

    public PathNode(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double combinedCost() { return cost + estimatedCost; }

    public BlockPos toBlockPos() { return new BlockPos(x, y, z); }

    @Override
    public int compareTo(PathNode o) {
        return Double.compare(this.combinedCost(), o.combinedCost());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PathNode n)) return false;
        return x == n.x && y == n.y && z == n.z;
    }

    @Override
    public int hashCode() {
        return (x * 31 + y) * 31 + z;
    }
}