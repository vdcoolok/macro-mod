package com.example.macromod;

import com.example.macromod.path.render.RotationController;
import net.minecraft.client.player.LocalPlayer;

public final class NolookController {

    public enum Mode { NONE, CAMERA, LOCKED }

    private static Mode mode = Mode.NONE;
    private static boolean swapApplied = false;
    private static float swapYaw;
    private static float swapPitch;

    private NolookController() {}

    public static void setMode(Mode m) {
        mode = m == null ? Mode.NONE : m;
    }

    public static Mode getMode() {
        return mode;
    }

    public static void beforeMovementPacket(LocalPlayer p) {
        if (mode == Mode.NONE || swapApplied) return;

        swapYaw = p.getYRot();
        swapPitch = p.getXRot();
        swapApplied = true;

        float[] forced = forcedRotation(p);
        if (forced == null) return;
        p.setYRot(forced[0]);
        p.setXRot(forced[1]);
    }

    public static void afterMovementPacket(LocalPlayer p) {
        if (!swapApplied) return;
        p.setYRot(swapYaw);
        p.setXRot(swapPitch);
        swapApplied = false;
    }

    private static float[] forcedRotation(LocalPlayer p) {
        return switch (mode) {
            case CAMERA -> RotationController.getLastCameraRotation(p);
            case LOCKED -> {
                MacroAction lock = MacroExecutor.getActiveSnapLook();
                if (lock != null) yield new float[] { lock.yaw, lock.pitch };
                yield RotationController.getLastCameraRotation(p);
            }
            case NONE -> null;
        };
    }
}
