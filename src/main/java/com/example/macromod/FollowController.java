package com.example.macromod;

import com.example.macromod.path.behavior.PathingBehavior;
import com.example.macromod.path.goal.GoalBlock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class FollowController {

    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final int RESCAN_AFTER_LOST_TICKS = 40;
    private static final double MAX_SCAN_RANGE = 128.0;

    private static boolean active = false;
    private static final List<String> nameFilters = new ArrayList<>();
    private static UUID targetId;

    private static Entity target;
    private static int scanCooldown = 0;
    private static int lostTicks = 0;

    private FollowController() {}

    public static void start(String name) {
        nameFilters.clear();
        if (name != null && !name.isBlank()) {
            nameFilters.add(name.trim().toLowerCase(Locale.ROOT));
        }
        targetId = null;
        target = null;
        scanCooldown = 0;
        lostTicks = 0;
        active = true;
    }

    public static void stop() {
        active = false;
        target = null;
        targetId = null;
        nameFilters.clear();
        lostTicks = 0;
        PathingBehavior.get().stop();
    }

    public static boolean isActive() {
        return active;
    }

    public static void tick(Minecraft client) {
        if (!active) return;
        if (client.player == null || client.level == null) {
            stop();
            return;
        }

        ClientLevel level = client.level;

        if (target != null && isTargetValid(level, target)) {
            lostTicks = 0;
        } else {
            target = null;
            lostTicks++;
            if (lostTicks > RESCAN_AFTER_LOST_TICKS) {
                Message.error("Can't find loaded mobs matching criteria.");
                stop();
                return;
            }
        }

        scanCooldown--;
        if (target == null && scanCooldown <= 0) {
            scanCooldown = SCAN_INTERVAL_TICKS;
            target = findTarget(client);
            if (target == null) return;
            targetId = target.getUUID();
        }

        if (target == null) return;

        updatePathGoal(client);
    }

    private static void updatePathGoal(Minecraft client) {
        PathingBehavior pathing = PathingBehavior.get();

        var pos = target.blockPosition();
        GoalBlock goal = new GoalBlock(pos.getX(), pos.getY(), pos.getZ());

        if (!isFollowingGoal(pathing, goal)) {
            pathing.setGoal(goal);
        }
    }

    private static boolean isFollowingGoal(PathingBehavior pathing, GoalBlock goal) {
        com.example.macromod.path.calc.Path current = pathing.getCurrentPath();
        if (current == null) return false;
        var dest = current.getFinalDestination();
        if (dest == null) return false;
        return dest.getX() == goal.getX() && dest.getY() == goal.getY()
                && dest.getZ() == goal.getZ();
    }

    private static boolean isTargetValid(ClientLevel level, Entity candidate) {
        if (candidate == null) return false;
        if (!candidate.isAlive() || candidate.isRemoved()) return false;
        return level.getEntity(candidate.getId()) == candidate;
    }

    private static Entity findTarget(Minecraft client) {
        if (client.player == null || client.level == null) return null;
        if (nameFilters.isEmpty()) return null;

        Entity best = null;
        double bestDistSq = MAX_SCAN_RANGE * MAX_SCAN_RANGE;

        for (Entity e : client.level.entitiesForRendering()) {
            if (e.getId() == client.player.getId()) continue;
            if (!matchesName(e)) continue;

            double d = client.player.distanceToSqr(e);
            if (d < bestDistSq) {
                bestDistSq = d;
                best = e;
            }
        }
        return best;
    }

    private static boolean matchesName(Entity entity) {
        String shortName = TargetRegistry.shortName(entity.getType());
        String custom = entity.hasCustomName()
            ? entity.getCustomName().getString()
            : null;

        for (String filter : nameFilters) {
            if (FuzzyMatcher.matches(filter, shortName)) return true;
            if (custom != null && FuzzyMatcher.matches(filter, custom)) return true;
        }
        return false;
    }
}
