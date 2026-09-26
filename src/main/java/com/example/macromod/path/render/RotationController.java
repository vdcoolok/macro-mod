package com.example.macromod.path.render;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class RotationController {

    private static final float MAX_YAW_STEP = 22.0f;
    private static final float MAX_PITCH_STEP = 14.0f;

    private static float targetYaw = 0f;
    private static float targetPitch = 0f;
    private static float bodyYaw = 0f;
    private static float bodyPitch = 0f;
    private static boolean hasTarget = false;
    private static boolean hasBody = false;
    private static boolean smoothing = false;
    private static boolean smoothRequested = false;
    private static boolean smoothLookEnabled = true;

    private static boolean saved = false;
    private static float savedYaw = 0f;
    private static float savedPitch = 0f;

    private static float cameraYaw = 0f;
    private static float cameraPitch = 0f;
    private static boolean hasCamera = false;

    private RotationController() {}

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
        if (hasBody && !smoothRequested) {
            bodyYaw = targetYaw;
            bodyPitch = targetPitch;
            smoothing = false;
        }
    }

    public static void applyBeforeTick() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;

        captureCamera(p);

        if (!hasBody) {
            bodyYaw = p.getYRot();
            bodyPitch = p.getXRot();
            hasBody = true;
        }

        if (hasTarget) {
            if (!smoothRequested) {
                bodyYaw = targetYaw;
                bodyPitch = targetPitch;
                smoothing = false;
            } else if (!smoothing && !atTarget()) {
                smoothing = true;
            }
            hasTarget = false;
        }

        if (smoothing) {
            bodyYaw = stepAngle(bodyYaw, targetYaw, MAX_YAW_STEP);
            bodyPitch = step(bodyPitch, targetPitch, MAX_PITCH_STEP);
            if (atTarget()) smoothing = false;
        }

        if (!saved) {
            savedYaw = p.getYRot();
            savedPitch = p.getXRot();
            saved = true;
        }

        p.setYRot(bodyYaw);
        p.setXRot(bodyPitch);
    }

    public static void restoreAfterTick() {
        if (isThirdPerson()) {
            if (!hasTarget && !smoothing) {
                hasBody = false;
                saved = false;
            }
            return;
        }

        if (!saved) return;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) {
            saved = false;
            hasBody = false;
            return;
        }
        p.setYRot(savedYaw);
        p.setXRot(savedPitch);
        saved = false;
        if (!hasTarget && !smoothing) hasBody = false;
    }

    public static boolean isSettling() {
        return smoothing || hasTarget;
    }

    public static void reset() {
        hasTarget = false;
        hasBody = false;
        smoothing = false;
        saved = false;
    }

    public static boolean isThirdPerson() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) return false;
        CameraType type = mc.options.getCameraType();
        return type != null && !type.isFirstPerson();
    }

    public static boolean overrideCamera() {
        return isThirdPerson() && (hasTarget || smoothing || saved);
    }

    public static float getCameraYaw() {
        return cameraYaw;
    }

    public static float getCameraPitch() {
        return cameraPitch;
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

    private static boolean atTarget() {
        return Math.abs(wrapYaw(targetYaw - bodyYaw)) < 0.1f && Math.abs(targetPitch - bodyPitch) < 0.1f;
    }

    private static float stepAngle(float current, float target, float maxStep) {
        float diff = wrapYaw(target - current);
        if (Math.abs(diff) <= maxStep) return wrapYaw(target);
        return wrapYaw(current + Math.signum(diff) * maxStep);
    }

    private static float step(float current, float target, float maxStep) {
        float diff = target - current;
        if (Math.abs(diff) <= maxStep) return target;
        return current + Math.signum(diff) * maxStep;
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
