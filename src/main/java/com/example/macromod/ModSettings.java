package com.example.macromod;

import com.example.macromod.path.render.RotationController;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public final class ModSettings {

    private static final String KEY_ATTACK_MODE = "attackmode";
    private static final String KEY_SPAM_INTERVAL = "spaminterval";
    private static final String KEY_SMOOTH_LOOK = "smoothlook";
    private static final String KEY_BOT_VIEW = "botview";
    private static final String KEY_FREE_CAM = "freecam";
    private static final String KEY_FCAM_DIST = "fcamdist";

    private static final String DEFAULT_ATTACK_MODE = "spam";
    private static final long DEFAULT_SPAM_INTERVAL = 500L;
    private static final boolean DEFAULT_SMOOTH_LOOK = true;
    private static final boolean DEFAULT_BOT_VIEW = false;
    private static final String DEFAULT_FREE_CAM = "off";
    private static final double DEFAULT_FCAM_DIST = 4.0;

    private static final Properties values = new Properties();
    private static Path file = null;

    private ModSettings() {}

    public static void load() {
        file = FabricLoader.getInstance().getGameDir()
            .resolve("mods")
            .resolve("macros")
            .resolve("settings.properties");

        values.clear();
        if (Files.exists(file)) {
            try (var reader = Files.newBufferedReader(file)) {
                values.load(reader);
            } catch (IOException ignored) {}
        }

        CombatController.setMode(readMode());
        CombatController.setSpamInterval(readLong(KEY_SPAM_INTERVAL, DEFAULT_SPAM_INTERVAL));
        RotationController.setSmoothLookEnabled(readBoolean(KEY_SMOOTH_LOOK, DEFAULT_SMOOTH_LOOK));
        RotationController.setBotViewEnabled(readBoolean(KEY_BOT_VIEW, DEFAULT_BOT_VIEW));
        FreeCamera.setMode(FreeCamera.Mode.parse(readString(KEY_FREE_CAM, DEFAULT_FREE_CAM)));
        FreeCamera.setDistance(readDouble(KEY_FCAM_DIST, DEFAULT_FCAM_DIST));
    }

    public static void setFreeCamera(FreeCamera.Mode next) {
        FreeCamera.setMode(next);
        put(KEY_FREE_CAM, FreeCamera.getMode().label());
    }

    public static void setFreeCameraDistance(double value) {
        FreeCamera.setDistance(value);
        put(KEY_FCAM_DIST, Double.toString(FreeCamera.getDistance()));
    }

    public static void setAttackMode(CombatController.AttackMode mode) {
        CombatController.AttackMode applied = mode == null ? CombatController.AttackMode.SPAM : mode;
        CombatController.setMode(applied);
        put(KEY_ATTACK_MODE, applied.name().toLowerCase(Locale.ROOT));
    }

    public static void setSpamInterval(long ms) {
        CombatController.setSpamInterval(ms);
        put(KEY_SPAM_INTERVAL, Long.toString(CombatController.getSpamInterval()));
    }

    public static void setSmoothLook(boolean enabled) {
        RotationController.setSmoothLookEnabled(enabled);
        put(KEY_SMOOTH_LOOK, Boolean.toString(enabled));
    }

    public static void setBotView(boolean enabled) {
        RotationController.setBotViewEnabled(enabled);
        put(KEY_BOT_VIEW, Boolean.toString(enabled));
    }

    public static Path getFile() {
        return file;
    }

    private static CombatController.AttackMode readMode() {
        String raw = values.getProperty(KEY_ATTACK_MODE, DEFAULT_ATTACK_MODE);
        try {
            return CombatController.AttackMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return CombatController.AttackMode.SPAM;
        }
    }

    private static long readLong(String key, long fallback) {
        String raw = values.getProperty(key);
        if (raw == null) return fallback;
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String readString(String key, String fallback) {
        String raw = values.getProperty(key);
        if (raw == null) return fallback;
        raw = raw.trim();
        return raw.isEmpty() ? fallback : raw;
    }

    private static double readDouble(String key, double fallback) {
        String raw = values.getProperty(key);
        if (raw == null) return fallback;
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean readBoolean(String key, boolean fallback) {
        String raw = values.getProperty(key);
        if (raw == null) return fallback;
        raw = raw.trim();
        if (raw.equalsIgnoreCase("true")) return true;
        if (raw.equalsIgnoreCase("false")) return false;
        return fallback;
    }

    private static void put(String key, String value) {
        values.setProperty(key, value);
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            try (var writer = Files.newBufferedWriter(file)) {
                values.store(writer, "MacroMod settings");
            }
        } catch (IOException ignored) {}
    }
}
