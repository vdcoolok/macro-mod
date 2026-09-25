package com.example.macromod;

import com.example.macromod.path.behavior.PathingBehavior;
import com.example.macromod.path.calc.CalculationContext;
import com.example.macromod.path.goal.GoalBlock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CombatController {

    public enum AttackMode { SPAM, CRIT }

    private static final double ATTACK_RANGE_SQ = 9.0;
    private static final double FOLLOW_STOP_DIST_SQ = 1.0;
    private static final double CRIT_MIN_DIST = 2.0;
    private static final double CRIT_MAX_DIST = 3.0;
    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final long DEFAULT_SPAM_INTERVAL_MS = 500;
    private static final double MAX_SCAN_RANGE = 64.0;

    private static boolean active = false;
    private static boolean hostileOnly = false;
    private static final List<String> nameFilters = new ArrayList<>();

    private static AttackMode mode = AttackMode.SPAM;
    private static long spamIntervalMs = DEFAULT_SPAM_INTERVAL_MS;

    private static LivingEntity target;
    private static long lastAttackMs = 0;
    private static int scanCooldown = 0;
    private static boolean critJumpQueued = false;

    private CombatController() {}

    public static void start(boolean hostile, List<String> filters) {
        hostileOnly = hostile;
        nameFilters.clear();
        if (filters != null) {
            for (String f : filters) {
                if (f != null && !f.isBlank()) nameFilters.add(f.trim().toLowerCase(Locale.ROOT));
            }
        }
        target = null;
        lastAttackMs = 0;
        scanCooldown = 0;
        critJumpQueued = false;
        active = true;
    }

    public static void stop() {
        active = false;
        target = null;
        nameFilters.clear();
        hostileOnly = false;
        critJumpQueued = false;
        PathingBehavior.get().stop();
        CalculationContext ctx = new CalculationContext();
        ctx.setInput("attack", false);
        ctx.setInput("jump", false);
        ctx.releaseAllInputs();
    }

    public static boolean isActive() {
        return active;
    }

    public static void setMode(AttackMode m) {
        mode = m == null ? AttackMode.SPAM : m;
    }

    public static AttackMode getMode() {
        return mode;
    }

    public static void setSpamInterval(long ms) {
        spamIntervalMs = Math.max(50, ms);
    }

    public static long getSpamInterval() {
        return spamIntervalMs;
    }

    public static void tick(Minecraft client) {
        if (!active) return;
        if (client.player == null || client.level == null) {
            stop();
            return;
        }

        scanCooldown--;
        if (scanCooldown <= 0 || !isTargetValid(client, target)) {
            scanCooldown = SCAN_INTERVAL_TICKS;
            target = findTarget(client);
            if (target == null) {
                Message.error("Can't find loaded mobs matching criteria.");
                stop();
                return;
            }
        }

        double distSq = client.player.distanceToSqr(target);

        updatePathGoal(client, distSq);

        if (mode == AttackMode.CRIT) {
            tickCrit(client, distSq);
        } else {
            tickSpam(client, distSq);
        }
    }

    private static void updatePathGoal(Minecraft client, double distSq) {
        PathingBehavior pathing = PathingBehavior.get();
        if (distSq > ATTACK_RANGE_SQ) {
            Vec3 tp = target.position();
            pathing.setGoal(new GoalBlock(
                Math.floor(tp.x),
                Math.floor(tp.y),
                Math.floor(tp.z)));
        } else if (pathing.isPathing()) {
            pathing.stop();
        }
    }

    private static void tickSpam(Minecraft client, double distSq) {
        if (distSq > ATTACK_RANGE_SQ) return;

        CalculationContext ctx = new CalculationContext();
        lookAtTarget(ctx, client, target);

        long now = System.currentTimeMillis();
        if (now - lastAttackMs >= spamIntervalMs) {
            performAttack(client);
            lastAttackMs = now;
        }
    }

    private static void tickCrit(Minecraft client, double distSq) {
        LocalPlayer player = client.player;
        double dist = Math.sqrt(distSq);
        CalculationContext ctx = new CalculationContext();

        if (!player.onGround()) {
            if (critJumpQueued) {
                Vec3 delta = player.getDeltaMovement();
                if (delta.y < 0 && dist <= CRIT_MAX_DIST) {
                    lookAtTarget(ctx, client, target);
                    performAttack(client);
                    critJumpQueued = false;
                    lastAttackMs = System.currentTimeMillis();
                }
            }
            ctx.setInput("jump", false);
            return;
        }

        critJumpQueued = false;

        if (distSq > ATTACK_RANGE_SQ) return;

        lookAtTarget(ctx, client, target);

        boolean inCritWindow = dist >= CRIT_MIN_DIST && dist <= CRIT_MAX_DIST;
        if (!inCritWindow) return;

        long now = System.currentTimeMillis();
        if (now - lastAttackMs >= spamIntervalMs) {
            ctx.setInput("jump", true);
            player.jumpFromGround();
            critJumpQueued = true;
            lastAttackMs = now;
        }
    }

    private static void performAttack(Minecraft client) {
        if (client.player == null || client.gameMode == null) return;
        client.gameMode.attack(client.player, target);
        client.player.swing(InteractionHand.MAIN_HAND);
    }

    private static void lookAtTarget(CalculationContext ctx, Minecraft client, LivingEntity target) {
        Vec3 tp = target.position();
        ctx.lookAtDirect(tp.x, tp.y + target.getBbHeight() * 0.85, tp.z);
    }

    private static boolean isTargetValid(Minecraft client, LivingEntity candidate) {
        if (candidate == null) return false;
        if (!candidate.isAlive() || candidate.isRemoved()) return false;
        if (client.level == null || client.player == null) return false;
        if (candidate.getId() == client.player.getId()) return false;
        return client.level.getEntity(candidate.getId()) == candidate;
    }

    private static LivingEntity findTarget(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) return null;

        LivingEntity best = null;
        double bestDistSq = MAX_SCAN_RANGE * MAX_SCAN_RANGE;

        for (Entity e : client.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity living)) continue;
            if (living.getId() == player.getId()) continue;
            if (!living.isAlive()) continue;
            if (!matchesCriteria(living)) continue;

            double d = player.distanceToSqr(living);
            if (d < bestDistSq) {
                bestDistSq = d;
                best = living;
            }
        }
        return best;
    }

    private static boolean matchesCriteria(LivingEntity entity) {
        if (hostileOnly && !(entity instanceof Monster)) return false;

        if (nameFilters.isEmpty()) return true;

        EntityType<?> type = entity.getType();
        String shortName = TargetRegistry.shortName(type);
        String translationKey = type.getDescriptionId();

        for (String filter : nameFilters) {
            if (FuzzyMatcher.matches(filter, shortName)) return true;
            if (FuzzyMatcher.matches(filter, translationKey)) return true;
        }
        return false;
    }
}
