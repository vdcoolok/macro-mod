package com.example.macromod.path.calc;

import com.example.macromod.ForcedInputState;
import com.example.macromod.path.cache.CachedChunk;
import com.example.macromod.path.cache.CachedWorld;
import com.example.macromod.path.cache.PathingBlockType;
import com.example.macromod.path.render.RotationController;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;

public class CalculationContext implements ActionCosts {

    private final Minecraft mc;
    private final CachedWorld cache;
    private final int minY, maxY;
    private final Map<BlockPos, Double> costCache = new HashMap<>();

    public CalculationContext() {
        this.mc = Minecraft.getInstance();
        this.cache = CachedWorld.get();
        this.minY = mc.level != null ? mc.level.getMinY() : -64;
        this.maxY = mc.level != null ? mc.level.getMaxY() : 320;
    }

    public boolean isLoaded(int x, int z) {
        CachedChunk c = cache.getChunk(x >> 4, z >> 4);
        return c != null && c.isLoaded();
    }

    public PathingBlockType getBlockType(int x, int y, int z) {
        CachedChunk c = cache.getChunk(x >> 4, z >> 4);
        if (c == null || !c.isLoaded()) return PathingBlockType.AVOID;
        return c.get(x & 15, y, z & 15);
    }

    public boolean isPassable(int x, int y, int z) {
        if (y < minY || y >= maxY) return false;
        return getBlockType(x, y, z) == PathingBlockType.AIR;
    }

    public boolean isSolidGround(int x, int y, int z) {
        if (y < minY || y >= maxY) return false;
        return getBlockType(x, y, z) == PathingBlockType.SOLID;
    }

    public boolean isHazard(int x, int y, int z) {
        return getBlockType(x, y, z) == PathingBlockType.AVOID;
    }

    public boolean isObstructing(int x, int y, int z) {
        if (isPassable(x, y, z)) return false;
        return isSolidGround(x, y - 1, z) && isPassable(x, y + 1, z);
    }

    public boolean canFitThroughDiagonal(int x, int y, int z, int dx, int dz) {
        if (dx == 0 || dz == 0) return true;
        boolean sideX = isPassable(x + dx, y, z)
                     && isPassable(x + dx, y + 1, z);
        boolean sideZ = isPassable(x, y, z + dz)
                     && isPassable(x, y + 1, z + dz);
        return sideX || sideZ;
    }

    public boolean canExitDiagonal(int destX, int y, int destZ, int dx, int dz) {
        if (dx == 0 || dz == 0) return true;
        boolean sideX = isPassable(destX - dx, y, destZ)
                     && isPassable(destX - dx, y + 1, destZ);
        boolean sideZ = isPassable(destX, y, destZ - dz)
                     && isPassable(destX, y + 1, destZ - dz);
        return sideX || sideZ;
    }

    public boolean canBreak(BlockPos pos) {
        CachedChunk c = cache.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        if (c == null) return false;
        Block block = c.getSpecialBlock(pos.getX() & 15, pos.getY(), pos.getZ() & 15);
        if (block == null) return true;
        return block != Blocks.CHEST && block != Blocks.SPAWNER;
    }

    public double breakCost(BlockPos pos) {
        if (!isObstructing(pos.getX(), pos.getY(), pos.getZ())) return 0.0;
        return 20.0;
    }

    public double blockPlacePenalty() { return BLOCK_PLACEMENT_PENALTY; }
    public double jumpPenalty() { return JUMP_PENALTY; }
    public int maxFallDistance() { return 3; }

    public boolean canSprint() {
        LocalPlayer p = mc.player;
        if (p == null) return false;
        return p.getFoodData().getFoodLevel() > 6;
    }

    public boolean hasSprintRunUp(int x, int y, int z, int dirX, int dirZ) {
        if (dirX == 0 && dirZ == 0) return false;
        for (int i = 1; i <= 3; i++) {
            int cx = x - dirX * i;
            int cz = z - dirZ * i;
            if (!isSolidGround(cx, y - 1, cz)) return false;
            if (!isPassable(cx, y, cz)) return false;
            if (!isPassable(cx, y + 1, cz)) return false;
        }
        return true;
    }

    public boolean hasThrowawayBlock() {
        if (mc.player == null) return false;
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            String id = stack.getItem().getDescriptionId();
            if (id.contains("cobblestone") || id.contains("dirt")
                    || id.contains("netherrack") || id.contains("scaffolding")
                    || id.contains("planks") || id.contains("stone")) {
                return true;
            }
        }
        return false;
    }

    public boolean hasWaterBucket() {
        if (mc.player == null) return false;
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getItem(i);
            if (stack.getItem().getDescriptionId().contains("water_bucket")) return true;
        }
        return false;
    }

    public boolean isNearEdge(int x, int y, int z) {
        if (!isSolidGround(x, y - 1, z)) return false;
        if (!isPassable(x, y, z)) return false;
        if (!isSolidGround(x + 1, y - 1, z) && !isSolidGround(x + 1, y - 2, z)) return true;
        if (!isSolidGround(x - 1, y - 1, z) && !isSolidGround(x - 1, y - 2, z)) return true;
        if (!isSolidGround(x, y - 1, z + 1) && !isSolidGround(x, y - 2, z + 1)) return true;
        if (!isSolidGround(x, y - 1, z - 1) && !isSolidGround(x, y - 2, z - 1)) return true;
        return false;
    }

    public boolean canAscendTo(int x, int y, int z) {
        if (!isSolidGround(x, y - 1, z)) return false;
        if (!isPassable(x, y, z)) return false;
        if (!isPassable(x, y + 1, z)) return false;
        if (!isPassable(x, y + 2, z)) return false;
        return true;
    }

    public boolean playerNear(BlockPos pos, double threshold) {
        LocalPlayer p = mc.player;
        if (p == null) return false;
        double dx = p.getX() - (pos.getX() + 0.5);
        double dz = p.getZ() - (pos.getZ() + 0.5);
        double dy = p.getY() - pos.getY();
        return Math.sqrt(dx * dx + dz * dz + dy * dy) < threshold;
    }

    public boolean playerOnGround() {
        return mc.player != null && mc.player.onGround();
    }

    public BlockPos playerFeetBlock() {
        LocalPlayer p = mc.player;
        if (p == null) return null;
        return BlockPos.containing(p.getX(), p.getY() + 0.1251, p.getZ());
    }

    public void setInput(String name, boolean pressed) {
        if (mc.options == null) return;
        KeyMapping mapping = switch (name.toLowerCase()) {
            case "forward" -> mc.options.keyUp;
            case "back" -> mc.options.keyDown;
            case "left" -> mc.options.keyLeft;
            case "right" -> mc.options.keyRight;
            case "jump" -> mc.options.keyJump;
            case "sneak" -> mc.options.keyShift;
            case "sprint" -> mc.options.keySprint;
            case "attack" -> mc.options.keyAttack;
            case "use" -> mc.options.keyUse;
            default -> null;
        };
        if (mapping == null) return;
        mapping.setDown(pressed);
        InputConstants.Key bound = KeyMappingHelper.getBoundKeyOf(mapping);
        if (bound != null) {
            KeyMapping.set(bound, pressed);
            ForcedInputState.forceKey(bound, pressed);
        }
    }

    public void releaseAllInputs() {
        setInput("forward", false);
        setInput("back", false);
        setInput("left", false);
        setInput("right", false);
        setInput("jump", false);
        setInput("sneak", false);
        setInput("sprint", false);
    }

    public void lookAt(double x, double y, double z) {
        LocalPlayer p = mc.player;
        if (p == null) return;
        double dx = x - p.getX();
        double dy = y - (p.getY() + p.getEyeHeight());
        double dz = z - p.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, horizontal));
        RotationController.setTarget(yaw, pitch);
    }

    public double getCachedCost(BlockPos pos) {
        return costCache.getOrDefault(pos, -1.0);
    }

    public void cacheCost(BlockPos pos, double cost) {
        costCache.put(pos, cost);
    }
}