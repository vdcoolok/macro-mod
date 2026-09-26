package com.example.macromod;

import java.util.Locale;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public final class FreeCamera {

    public enum Mode {

        OFF,
        ORBIT,
        FREE;

        public Mode next() {
            Mode[] all = values();
            return all[(ordinal() + 1) % all.length];
        }

        public String label() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Mode parse(String raw) {
            if (raw == null) return OFF;
            for (Mode mode : values()) {
                if (mode.name().equalsIgnoreCase(raw.trim())) return mode;
            }
            return OFF;
        }
    }

    private static final double MIN_DISTANCE = 0.6;
    private static final double MAX_DISTANCE = 64.0;
    private static final double ZOOM_STEP = 0.45;
    private static final double MOVE_SPEED = 1.8;
    private static final double FAST_MULTIPLIER = 3.5;

    private static Mode mode = Mode.OFF;
    private static double distance = 4.0;
    private static double x = 0.0;
    private static double y = 0.0;
    private static double z = 0.0;
    private static boolean placed = false;
    private static boolean warned = false;
    private static KeyMapping toggleKey = null;
    private static final double[] view = new double[5];

    private FreeCamera() {}

    public static void register() {
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.macromod.freecam",
            GLFW.GLFW_KEY_F6,
            KeyMapping.Category.MISC
        ));
    }

    public static Mode getMode() {
        return mode;
    }

    public static void setMode(Mode next) {
        mode = next == null ? Mode.OFF : next;
        placed = false;
    }

    public static double getDistance() {
        return distance;
    }

    public static void setDistance(double value) {
        distance = clamp(value, MIN_DISTANCE, MAX_DISTANCE);
    }

    public static boolean isActive() {
        return mode != Mode.OFF && !isBlocked();
    }

    public static boolean isBlocked() {
        return CombatController.isActive();
    }

    public static boolean isFree() {
        return mode == Mode.FREE && !isBlocked();
    }

    public static void onScroll(double amount) {
        if (!isActive()) return;
        distance = clamp(distance + amount * ZOOM_STEP, MIN_DISTANCE, MAX_DISTANCE);
    }

    public static void cycle() {
        if (isBlocked()) {
            setMode(Mode.OFF);
            return;
        }
        setMode(mode.next());
    }

    public static boolean setModeChecked(Mode next) {
        if (next != Mode.OFF && isBlocked()) {
            setMode(Mode.OFF);
            return false;
        }
        setMode(next);
        return true;
    }

    public static void reset() {
        mode = Mode.OFF;
        placed = false;
        warned = false;
    }

    public static void tick(Minecraft mc) {
        if (isBlocked() && mode != Mode.OFF) {
            setMode(Mode.OFF);
        }

        if (!isBlocked()) {
            warned = false;
        }

        if (toggleKey != null) {
            while (toggleKey.consumeClick()) {
                if (isBlocked() && mode == Mode.OFF) {
                    warn(mc, "Free camera is off while attacking");
                    continue;
                }
                cycle();
            }
        }

        if (mc.player == null || mc.level == null) {
            placed = false;
            return;
        }

        if (!isActive()) {
            placed = false;
            return;
        }

        if (mc.gui != null && mc.gui.screen() != null) return;
        if (!isFree()) return;

        Vec3 forward = forward(mc.player.getYRot(), mc.player.getXRot());
        Vec3 right = right(mc.player.getYRot());

        double step = MOVE_SPEED * (mc.options.keyShift.isDown() ? FAST_MULTIPLIER : 1.0);
        if (mc.options.keyUp.isDown()) {
            x += forward.x * step;
            y += forward.y * step;
            z += forward.z * step;
        }
        if (mc.options.keyDown.isDown()) {
            x -= forward.x * step;
            y -= forward.y * step;
            z -= forward.z * step;
        }
        if (mc.options.keyLeft.isDown()) {
            x -= right.x * step;
            z -= right.z * step;
        }
        if (mc.options.keyRight.isDown()) {
            x += right.x * step;
            z += right.z * step;
        }
        if (mc.options.keyJump.isDown()) y += step;
    }

    public static double[] computeView(DeltaTracker tracker) {
        if (!isActive()) return null;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return null;
        if (mc.gui != null && mc.gui.screen() != null) return null;

        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();
        float partial = tracker.getGameTimeDeltaPartialTick(false);
        Vec3 eye = mc.player.getEyePosition(partial);

        if (mode == Mode.ORBIT) {
            Vec3 back = forward(yaw, pitch);
            view[0] = eye.x - back.x * distance;
            view[1] = eye.y - back.y * distance;
            view[2] = eye.z - back.z * distance;
            placed = false;
        } else {
            if (!placed) {
                Vec3 back = forward(yaw, pitch);
                x = eye.x - back.x * distance;
                y = eye.y - back.y * distance;
                z = eye.z - back.z * distance;
                placed = true;
            }
            view[0] = x;
            view[1] = y;
            view[2] = z;
        }

        view[3] = yaw;
        view[4] = pitch;
        return view;
    }

    private static Vec3 forward(float yaw, float pitch) {
        double y = Math.toRadians(yaw);
        double p = Math.toRadians(pitch);
        double cp = Math.cos(p);
        return new Vec3(-Math.sin(y) * cp, -Math.sin(p), Math.cos(y) * cp);
    }

    private static Vec3 right(float yaw) {
        double y = Math.toRadians(yaw);
        return new Vec3(-Math.cos(y), 0.0, -Math.sin(y));
    }

    private static void warn(Minecraft mc, String message) {
        if (warned) return;
        warned = true;
        if (mc.gui == null || mc.gui.chatListener() == null) return;
        mc.gui.chatListener().handleSystemMessage(
            net.minecraft.network.chat.Component.literal("§c" + message),
            false
        );
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
