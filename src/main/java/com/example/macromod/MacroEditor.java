package com.example.macromod;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MacroEditor {

    private static String currentName = null;
    private static Path currentFile = null;
    private static final List<String> lines = new ArrayList<>();

    private MacroEditor() {}

    public static Path getMacrosDir() {
        Path dir = FabricLoader.getInstance().getGameDir().resolve("mods").resolve("macros");
        if (!Files.exists(dir)) {
            try { Files.createDirectories(dir); } catch (IOException ignored) {}
        }
        return dir;
    }

    public static List<String> listMacroNames() {
        List<String> names = new ArrayList<>();
        Path dir = getMacrosDir();
        try {
            if (Files.exists(dir)) {
                try (var stream = Files.list(dir)) {
                    stream.filter(p -> p.getFileName().toString().endsWith(".macro"))
                          .map(p -> p.getFileName().toString())
                          .map(n -> n.substring(0, n.length() - ".macro".length()))
                          .sorted()
                          .forEach(names::add);
                }
            }
        } catch (IOException ignored) {}
        return names;
    }

    public static boolean isEditing() { return currentName != null; }
    public static String getCurrentName() { return currentName; }
    public static List<String> getLines() { return lines; }
    public static int lineCount() { return lines.size(); }

    public static boolean create(String name) {
        if (name == null || name.isBlank()) return false;
        Path file = getMacrosDir().resolve(name + ".macro");
        if (Files.exists(file)) return false;
        try {
            Files.createFile(file);
            return open(name);
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean open(String name) {
        Path file = getMacrosDir().resolve(name + ".macro");
        if (!Files.exists(file)) return false;
        try {
            lines.clear();
            lines.addAll(Files.readAllLines(file));
            currentName = name;
            currentFile = file;
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void close() {
        currentName = null;
        currentFile = null;
        lines.clear();
    }

    public static boolean save() {
        if (currentFile == null) return false;
        try {
            Files.write(currentFile, lines);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean addLine(String line) {
        if (!isEditing()) return false;
        lines.add(line);
        save();
        return true;
    }

    public static boolean removeLine(int index1based) {
        if (!isEditing()) return false;
        int idx = index1based - 1;
        if (idx < 0 || idx >= lines.size()) return false;
        lines.remove(idx);
        save();
        return true;
    }

    public static boolean moveLine(int from1based, int to1based) {
        if (!isEditing()) return false;
        int from = from1based - 1;
        int to = to1based - 1;
        if (from < 0 || from >= lines.size()) return false;
        if (to < 0 || to >= lines.size()) return false;
        String l = lines.remove(from);
        lines.add(to, l);
        save();
        return true;
    }
}