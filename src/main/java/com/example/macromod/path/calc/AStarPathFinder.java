package com.example.macromod.path.calc;

import com.example.macromod.path.behavior.PathingBehavior;
import com.example.macromod.path.goal.Goal;
import com.example.macromod.path.movement.*;

import net.minecraft.core.BlockPos;

import java.util.*;

public class AStarPathFinder {

    private static final double[] COEFFICIENTS = {
        1.5, 2.0, 2.5, 3.0, 4.0, 5.0, 10.0
    };

    private final CalculationContext ctx;
    private final Goal goal;
    private final int startX, startY, startZ;

    private static final int MAX_NODES = 200_000;
    private static final long PRIMARY_TIMEOUT_MS = 3000;
    private static final long FAILURE_TIMEOUT_MS = 8000;
    private static final double MIN_PARTIAL_DIST_SQ = 9.0;

    public AStarPathFinder(CalculationContext ctx, Goal goal, int startX, int startY, int startZ) {
        this.ctx = ctx;
        this.goal = goal;
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
    }

    public Path calculate() {
        long startTime = System.currentTimeMillis();
        long primaryTimeout = startTime + PRIMARY_TIMEOUT_MS;
        long failureTimeout = startTime + FAILURE_TIMEOUT_MS;

        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        Map<Long, PathNode> allNodes = new HashMap<>();

        PathNode start = new PathNode(startX, startY, startZ);
        start.cost = 0;
        start.estimatedCost = goal.heuristic(startX, startY, startZ);
        openSet.add(start);
        allNodes.put(key(startX, startY, startZ), start);

        double[] bestHeuristic = new double[COEFFICIENTS.length];
        PathNode[] bestNode = new PathNode[COEFFICIENTS.length];
        for (int i = 0; i < COEFFICIENTS.length; i++) {
            bestHeuristic[i] = start.estimatedCost;
            bestNode[i] = start;
        }

        int nodesExplored = 0;

        while (!openSet.isEmpty() && nodesExplored < MAX_NODES) {
            if ((nodesExplored & 63) == 0) {
                long now = System.currentTimeMillis();
                if (now > failureTimeout || now > primaryTimeout) break;
            }

            PathNode current = openSet.poll();
            if (current.closed) continue;
            current.closed = true;
            nodesExplored++;

            for (int i = 0; i < COEFFICIENTS.length; i++) {
                double metric = current.estimatedCost + current.cost / COEFFICIENTS[i];
                if (metric < bestHeuristic[i]) {
                    bestHeuristic[i] = metric;
                    bestNode[i] = current;
                }
            }

            if (goal.isInGoal(current.x, current.y, current.z)) {
                return reconstructPath(current, nodesExplored);
            }

            List<Movement> movements = generateMovements(current);
            for (Movement m : movements) {
                BlockPos dest = m.getTo();
                BlockPos from = m.getFrom();

                if (!ctx.isLoaded(dest.getX(), dest.getZ())) continue;
                if (PathingBehavior.isBlacklisted(dest.getX(), dest.getY(), dest.getZ())
                        && !goal.isInGoal(dest.getX(), dest.getY(), dest.getZ())) {
                    continue;
                }
                if (PathingBehavior.isEdgeBlacklisted(from, dest)) continue;

                double actionCost = m.calculateCost(ctx);
                if (actionCost >= ActionCosts.COST_INF) continue;

                long k = key(dest.getX(), dest.getY(), dest.getZ());
                PathNode node = allNodes.get(k);
                if (node == null) {
                    node = new PathNode(dest.getX(), dest.getY(), dest.getZ());
                    allNodes.put(k, node);
                } else if (node.closed) {
                    continue;
                }

                double tentativeG = current.cost + actionCost;
                if (tentativeG < node.cost || node.parent == null) {
                    node.parent = current;
                    node.cost = tentativeG;
                    node.movementToReach = m;
                    node.estimatedCost = goal.heuristic(dest.getX(), dest.getY(), dest.getZ());
                    openSet.add(node);
                }
            }
        }

        return selectBestPartial(bestNode, bestHeuristic, nodesExplored);
    }

    private List<Movement> generateMovements(PathNode current) {
        List<Movement> moves = new ArrayList<>();
        BlockPos from = current.toBlockPos();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                BlockPos flat = new BlockPos(from.getX() + dx, from.getY(), from.getZ() + dz);
                addIfValid(moves, new TraverseMovement(from, flat));

                BlockPos up = new BlockPos(from.getX() + dx, from.getY() + 1, from.getZ() + dz);
                addIfValid(moves, new AscendMovement(from, up));

                BlockPos down = new BlockPos(from.getX() + dx, from.getY() - 1, from.getZ() + dz);
                if (ctx.isSolidGround(down.getX(), down.getY() - 1, down.getZ())) {
                    addIfValid(moves, new FallMovement(from, down, 1));
                }
                for (int dy = 2; dy <= 3; dy++) {
                    BlockPos deeper = new BlockPos(from.getX() + dx, from.getY() - dy, from.getZ() + dz);
                    if (ctx.isSolidGround(deeper.getX(), deeper.getY() - 1, deeper.getZ())) {
                        addIfValid(moves, new FallMovement(from, deeper, dy));
                    }
                }

                if (dx == 0 || dz == 0) {
                    for (int gap = 1; gap <= ParkourMovement.MAX_GAP; gap++) {
                        BlockPos sameLevel = new BlockPos(
                            from.getX() + dx * (gap + 1),
                            from.getY(),
                            from.getZ() + dz * (gap + 1)
                        );
                        addIfValid(moves, new ParkourMovement(from, sameLevel, gap));

                        if (gap <= 3) {
                            BlockPos oneUp = new BlockPos(
                                from.getX() + dx * (gap + 1),
                                from.getY() + 1,
                                from.getZ() + dz * (gap + 1)
                            );
                            addIfValid(moves, new ParkourMovement(from, oneUp, gap));
                        }

                        for (int dy = 1; dy <= 3; dy++) {
                            BlockPos downPos = new BlockPos(
                                from.getX() + dx * (gap + 1),
                                from.getY() - dy,
                                from.getZ() + dz * (gap + 1)
                            );
                            addIfValid(moves, new ParkourMovement(from, downPos, gap));
                        }
                    }
                }
            }
        }

        if (ctx.hasThrowawayBlock()) {
            BlockPos pillarUp = new BlockPos(from.getX(), from.getY() + 1, from.getZ());
            addIfValid(moves, new PillarMovement(from, pillarUp));
        }

        return moves;
    }

    private void addIfValid(List<Movement> moves, Movement m) {
        try {
            if (m.isValid(ctx)) moves.add(m);
        } catch (Exception ignored) {}
    }

    private Path selectBestPartial(PathNode[] bestNode, double[] bestHeuristic, int nodes) {
        PathNode selected = null;
        for (int i = 0; i < COEFFICIENTS.length; i++) {
            PathNode candidate = bestNode[i];
            if (candidate == null) continue;
            double distSq =
                Math.pow(candidate.x - startX, 2) +
                Math.pow(candidate.z - startZ, 2);
            if (distSq >= MIN_PARTIAL_DIST_SQ) {
                selected = candidate;
                break;
            }
        }
        if (selected == null) return null;
        return reconstructPath(selected, nodes);
    }

    private Path reconstructPath(PathNode end, int nodes) {
        LinkedList<Movement> movements = new LinkedList<>();
        PathNode current = end;
        while (current.parent != null) {
            if (current.movementToReach != null) movements.addFirst(current.movementToReach);
            current = current.parent;
        }
        return new Path(movements, nodes);
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38)
             | ((long) (y & 0xFFF) << 26)
             | (z & 0x3FFFFFF);
    }
}