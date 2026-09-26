package com.example.macromod.path.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

public final class RotationController {

    private static final float MAX_YAW_STEP = 22.0f;
    private static final float MAX_PITCH_STEP = 14.0f;
    private static final float REACHED_EPS = 0.1f;

    private static float targetYaw = 0f;
    private static float targetPitch = 0f;
    private static boolean hasTarget = false;
    private static boolean smoothRequested = false;

    private static float botYaw = 0f;
    private static float botPitch = 0f;
    private static boolean hasBot = false;
    private static boolean smoothing = false;

    private static boolean saved = false;
    private static float savedYaw = 0f;
    private static float savedPitch = 0f;

    private static boolean moveSaved = false;
    private static float moveYaw = 0f;
    private static float movePitch = 0f;

    private static float cameraYaw = 0f;
    private static float cameraPitch = 0f;
    private static boolean hasCamera = false;

    private static boolean smoothLookEnabled = true;
    private static boolean botViewEnabled = false;
    private static float lastViewYaw = 0f;
    private static boolean hasViewYaw = false;

    private RotationController() {}

    public static boolean isBotViewEnabled() {
        return botViewEnabled;
    }

    public static void setBotViewEnabled(boolean enabled) {
        botViewEnabled = enabled;
        if (!enabled) hasViewYaw = false;
    }

    public static boolean isSmoothLookEnabled() {
        return smoothLookEnabled;
    }

    public static void setSmoothLookEnabled(boolean enabled) {
        smoothLookEnabled = enabled;
    }

    public static void setTarget(float yaw, float pitch) {
        setTargetInternal(yaw, pitch, false, false);
    }

    public static void setAdaptiveTarget(float yaw, float pitch) {
        setTargetInternal(yaw, pitch, smoothLookEnabled, false);
    }

    public static void setForcedSmoothTarget(float yaw, float pitch) {
        setTargetInternal(yaw, pitch, true, true);
    }

    private static void setTargetInternal(float yaw, float pitch, boolean smooth, boolean forceSmooth) {
        targetYaw = wrapYaw(yaw);
        targetPitch = clampPitch(pitch);
        smoothRequested = forceSmooth || smooth;
        hasTarget = true;
    }

    public static void applyBeforeTick() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;

        captureCamera(p);
        if (!hasBot) {
            botYaw = p.getYRot();
            botPitch = p.getXRot();
            hasBot = true;
        }
        save(p);
        p.setYRot(botYaw);
        p.setXRot(botPitch);
        applyBodyView(p);
    }

    private static void applyBodyView(LocalPlayer p) {
        if (!botViewEnabled) return;

        if (hasViewYaw) {
            p.yBodyRotO = lastViewYaw;
            p.yHeadRotO = lastViewYaw;
        } else {
            p.yBodyRotO = botYaw;
            p.yHeadRotO = botYaw;
        }

        p.setYBodyRot(botYaw);
        p.setYHeadRot(botYaw);

        lastViewYaw = botYaw;
        hasViewYaw = true;
    }

    public static void restoreAfterTick() {
        if (!saved) return;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) {
            saved = false;
            hasBot = false;
            return;
        }
        p.setYRot(savedYaw);
        p.setXRot(savedPitch);
        saved = false;
        settle();
    }

    public static void applyForMovement(Entity self) {
        LocalPlayer p = localPlayer(self);
        if (p == null || moveSaved) return;
        resolve(p);
        moveYaw = p.getYRot();
        movePitch = p.getXRot();
        moveSaved = true;
        p.setYRot(botYaw);
        p.setXRot(botPitch);
    }

    public static void restoreAfterMovement(Entity self) {
        if (!moveSaved) return;
        LocalPlayer p = localPlayer(self);
        if (p == null) {
            moveSaved = false;
            return;
        }
        p.setYRot(moveYaw);
        p.setXRot(movePitch);
        moveSaved = false;
    }

    public static boolean isSettling() {
        return smoothing || hasTarget;
    }

    public static void reset() {
        hasTarget = false;
        smoothRequested = false;
        hasBot = false;
        smoothing = false;
        saved = false;
        moveSaved = false;
        hasViewYaw = false;
    }

    public static void captureCamera(LocalPlayer p) {
        cameraYaw = p.getYRot();
        cameraPitch = p.getXRot();
        hasCamera = true;
    }

    public static float[] getLastCameraRotation(LocalPlayer p) {
        if (!hasCamera) captureCamera(p);
        return new float[] { cameraYaw, cameraPitch };
    }

    private static void resolve(LocalPlayer p) {
        if (!hasBot) {
            botYaw = p.getYRot();
            botPitch = p.getXRot();
            hasBot = true;
        }

        if (hasTarget) {
            if (smoothRequested) {
                if (!atTarget()) smoothing = true;
            } else {
                botYaw = targetYaw;
                botPitch = targetPitch;
                smoothing = false;
            }
            hasTarget = false;
            smoothRequested = false;
        }

        if (!smoothing) return;
        if (atTarget()) {
            botYaw = targetYaw;
            botPitch = targetPitch;
            smoothing = false;
            return;
        }

        botYaw = wrapYaw(botYaw + mouseToAngle(angleToMouse(clampStep(wrapYaw(targetYaw - botYaw), MAX_YAW_STEP))));
        botPitch = clampPitch(botPitch + mouseToAngle(angleToMouse(clampStep(targetPitch - botPitch, MAX_PITCH_STEP))));
        if (atTarget()) {
            botYaw = targetYaw;
            botPitch = targetPitch;
            smoothing = false;
        }
    }

    private static void save(LocalPlayer p) {
        if (saved) return;
        savedYaw = p.getYRot();
        savedPitch = p.getXRot();
        saved = true;
    }

    private static void settle() {
        if (!hasTarget && !smoothing) hasBot = false;
    }

    private static LocalPlayer localPlayer(Entity self) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc == null ? null : mc.player;
        if (p == null || p != self) return null;
        return p;
    }

    private static boolean atTarget() {
        return Math.abs(wrapYaw(targetYaw - botYaw)) < REACHED_EPS && Math.abs(targetPitch - botPitch) < REACHED_EPS;
    }

    private static float clampStep(float delta, float maxStep) {
        if (delta > maxStep) return maxStep;
        if (delta < -maxStep) return -maxStep;
        return delta;
    }

    private static float angleToMouse(float angleDelta) {
        float min = mouseToAngle(1f);
        if (min <= 0f) return angleDelta;
        return (float) Math.round(angleDelta / min);
    }

    private static float mouseToAngle(float mouseDelta) {
        Minecraft mc = Minecraft.getInstance();
        double sensitivity = 0.5d;
        if (mc != null && mc.options != null && mc.options.sensitivity() != null) {
            sensitivity = mc.options.sensitivity().get();
        }
        float f = (float) (sensitivity * 0.6d + 0.2d);
        return (float) (mouseDelta * f * f * f * 8.0d) * 0.15f;
    }

    private static float wrapYaw(float yaw) {
        float wrapped = yaw % 360.0f;
        if (wrapped >= 180.0f) wrapped -= 360.0f;
        if (wrapped < -180.0f) wrapped += 360.0f;
        return wrapped;
    }

    private static float clampPitch(float pitch) {
        if (pitch > 90.0f) return 90.0f;
        if (pitch < -90.0f) return -90.0f;
        return pitch;
    }
}
