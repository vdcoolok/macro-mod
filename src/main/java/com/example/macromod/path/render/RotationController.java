package com.example.macromod.path.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class RotationController {

    private static float targetYaw = 0f;
    private static float targetPitch = 0f;
    private static boolean hasPending = false;
    private static boolean saved = false;
    private static float savedYaw = 0f;
    private static float savedPitch = 0f;

    private RotationController() {}

    public static void setTarget(float yaw, float pitch) {
        targetYaw = yaw;
        targetPitch = pitch;
        hasPending = true;
    }

    public static void applyBeforeTick() {
        if (!hasPending) return;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;

        if (!saved) {
            savedYaw = p.getYRot();
            savedPitch = p.getXRot();
            saved = true;
        }
        p.setYRot(targetYaw);
        p.setXRot(targetPitch);
        hasPending = false;
    }

    public static void restoreAfterTick() {
        if (!saved) return;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) {
            saved = false;
            return;
        }
        p.setYRot(savedYaw);
        p.setXRot(savedPitch);
        saved = false;
    }
}