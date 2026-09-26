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

    private static final String DEFAULT_ATTACK_MODE = "spam";
    private static final long DEFAULT_SPAM_INTERVAL = 500L;
    private static final boolean DEFAULT_SMOOTH_LOOK = true;
    private static final boolean DEFAULT_BOT_VIEW = false;

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
