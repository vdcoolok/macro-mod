package com.example.macromod.path.calc;

import com.example.macromod.path.movement.Movement;
import com.example.macromod.path.movement.ParkourMovement;
import net.minecraft.core.BlockPos;

import java.util.List;

public class Path {

    private final List<Movement> movements;
    private final int nodesExplored;
    private int index = 0;

    public Path(List<Movement> movements, int nodesExplored) {
        this.movements = movements;
        this.nodesExplored = nodesExplored;
    }

    public boolean isFinished() { return index >= movements.size(); }
    public Movement getCurrent() { return isFinished() ? null : movements.get(index); }
    public Movement getNext() {
        return index + 1 < movements.size() ? movements.get(index + 1) : null;
    }
    public void advance() { index++; }
    public int getIndex() { return index; }
    public int size() { return movements.size(); }
    public List<Movement> getMovements() { return movements; }
    public int getNodesExplored() { return nodesExplored; }

    public BlockPos getFinalDestination() {
        return movements.isEmpty() ? null : movements.get(movements.size() - 1).getTo();
    }

    public void skipTo(int newIndex) {
        if (newIndex > index) index = newIndex;
    }

    public double getTotalCost() {
        double total = 0;
        for (Movement m : movements) {
            double c = m.getCost();
            if (c < 0 || c >= Double.POSITIVE_INFINITY) return Double.POSITIVE_INFINITY;
            total += c;
        }
        return total;
    }

    public double getRemainingCost() {
        double total = 0;
        for (int i = index; i < movements.size(); i++) {
            double c = movements.get(i).getCost();
            if (c < 0 || c >= Double.POSITIVE_INFINITY) return Double.POSITIVE_INFINITY;
            total += c;
        }
        return total;
    }

    public boolean validateApproach(int index) {
        return true;
    }
}