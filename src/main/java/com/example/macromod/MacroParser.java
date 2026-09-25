package com.example.macromod;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MacroParser {

    public static List<MacroAction> parse(Path file) throws IOException {
        List<MacroAction> actions = new ArrayList<>();
        for (String raw : Files.readAllLines(file)) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            for (String stmt : splitByCommas(line)) {
                String s = stmt.trim();
                if (s.isEmpty() || s.startsWith("#")) continue;
                try {
                    parseLine(s, actions);
                } catch (Exception e) {
                    System.err.println("[MacroMod] Parse error: " + s);
                    System.err.println("[MacroMod]   " + e.getMessage());
                }
            }
        }
        return actions;
    }

    private static List<String> splitByCommas(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
                cur.append(c);
            } else if (c == ',' && !inQuote) {
                if (cur.length() > 0) { out.add(cur.toString()); cur.setLength(0); }
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    private static void parseLine(String line, List<MacroAction> actions) {
        List<String> t = tokenize(line);
        if (t.isEmpty()) return;
        String cmd = t.get(0).toLowerCase();

        switch (cmd) {
            case "goto", "walk", "walkto", "pathfind", "go" ->
                actions.add(parsePathfind(t.subList(1, t.size())));
            case "nolookgoto", "nolookwalk", "nolookgo" -> {
                double[] c = parseCoords(t.subList(1, t.size()));
                actions.add(MacroAction.nolookGoto(c[0], c[1], c[2]));
            }
            case "nolookautogoto", "nolookautowalk", "nolocksnapgoto" -> {
                double[] c = parseCoords(t.subList(1, t.size()));
                actions.add(MacroAction.nolookAutoGoto(c[0], c[1], c[2]));
            }
            case "gotohere", "walkhere", "pathhere" ->
                actions.add(parsePathfindFromCurrent());
            case "tp", "teleport" ->
                actions.add(parseGoto(t.subList(1, t.size())));
            case "pathstop", "stopwalking", "stopgoto" ->
                actions.add(MacroAction.pathfindStop());

            case "snap", "auto" -> parseSnapOrAuto(t, actions);
            case "look" -> parseLook(t, actions);
            case "lookhere" -> actions.add(parseLookFromCurrent());

            case "hold" -> {
                String k = t.get(1);
                actions.add(isMouse(k) ? MacroAction.mouseHold(k.toLowerCase()) : MacroAction.keyHold(k));
            }
            case "release" -> {
                String k = t.get(1);
                actions.add(isMouse(k) ? MacroAction.mouseRelease(k.toLowerCase()) : MacroAction.keyRelease(k));
            }
            case "press" -> actions.add(MacroAction.keyPress(t.get(1)));
            case "click" -> actions.add(MacroAction.mouseClick(t.get(1).toLowerCase()));

            case "key" -> parseKeyLong(t, actions);
            case "mouse" -> parseMouseLong(t, actions);

            case "chat", "say" -> actions.add(MacroAction.chat(joinFrom(t, 1)));
            case "cmd", "command" -> actions.add(MacroAction.cmd(joinFrom(t, 1)));

            case "cycle" -> parseCycle(t, 1, actions);
            case "bind" -> parseBind(t, actions);

            case "wait" -> actions.add(MacroAction.delay(parseWait(t.get(1))));
            case "loop" -> actions.add(MacroAction.loopStart(t.size() > 1 ? Integer.parseInt(t.get(1)) : -1));
            case "endloop" -> actions.add(MacroAction.loopEnd());

            case "key_hold" -> actions.add(MacroAction.keyHold(t.get(1)));
            case "key_release" -> actions.add(MacroAction.keyRelease(t.get(1)));
            case "key_press" -> actions.add(MacroAction.keyPress(t.get(1)));
            case "mouse_hold" -> actions.add(MacroAction.mouseHold(t.get(1).toLowerCase()));
            case "mouse_release" -> actions.add(MacroAction.mouseRelease(t.get(1).toLowerCase()));
            case "mouse_click" -> actions.add(MacroAction.mouseClick(t.get(1).toLowerCase()));
            case "snap_goto" -> actions.add(parseSnapGoto(t.subList(1, t.size())));
            case "snap_look" -> actions.add(parseSnapLook(t.subList(1, t.size())));

            default -> System.out.println("[MacroMod] Unknown command: " + cmd);
        }
    }

    private static MacroAction parsePathfind(List<String> parts) {
        double[] c = parseCoords(parts);
        return MacroAction.pathfind(c[0], c[1], c[2]);
    }

    private static MacroAction parsePathfindFromCurrent() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) throw new IllegalStateException("No player");
        return MacroAction.pathfind(mc.player.getX(), mc.player.getY(), mc.player.getZ());
    }

    private static MacroAction parseLookFromCurrent() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) throw new IllegalStateException("No player");
        return MacroAction.look(mc.player.getYRot(), mc.player.getXRot());
    }

    private static MacroAction parseGoto(List<String> parts) {
        double[] c = parseCoords(parts);
        return MacroAction.gotoPos(c[0], c[1], c[2]);
    }

    private static MacroAction parseSnapGoto(List<String> parts) {
        double[] c = parseCoords(parts);
        return MacroAction.snapGoto(c[0], c[1], c[2]);
    }

    private static void parseSnapOrAuto(List<String> t, List<MacroAction> out) {
        String sub = t.size() > 1 ? t.get(1).toLowerCase() : "";
        List<String> rest = t.subList(2, t.size());
        switch (sub) {
            case "goto", "position", "pos" -> out.add(parseSnapGoto(rest));
            case "look", "direction", "dir", "at" -> out.add(parseSnapLook(rest));
            default -> System.out.println("[MacroMod] Unknown snap/auto target: " + sub);
        }
    }

    private static void parseLook(List<String> t, List<MacroAction> out) {
        List<String> rest = t.subList(1, t.size());
        if (!rest.isEmpty() && rest.get(0).equalsIgnoreCase("at")) rest = rest.subList(1, rest.size());
        out.add(parseLookAction(rest));
    }

    private static MacroAction parseLookAction(List<String> parts) {
        float[] l = parseLookValues(parts);
        return MacroAction.look(l[0], l[1]);
    }

    private static MacroAction parseSnapLook(List<String> parts) {
        float[] l = parseLookValues(parts);
        return MacroAction.snapLook(l[0], l[1]);
    }

    private static void parseCycle(List<String> t, int start, List<MacroAction> out) {
        String kind = t.get(start).toLowerCase();
        List<String> msgs = collectMessages(t, start + 1);
        switch (kind) {
            case "chat", "say" -> out.add(MacroAction.cycleChat(msgs));
            case "cmd", "command" -> out.add(MacroAction.cycleCmd(msgs));
            default -> System.out.println("[MacroMod] Unknown cycle kind: " + kind);
        }
    }

    private static void parseBind(List<String> t, List<MacroAction> out) {
        int i = 1;
        if (t.get(i).equalsIgnoreCase("key")) i++;
        String key = t.get(i);
        i++;
        String sub = t.get(i).toLowerCase();
        switch (sub) {
            case "chat", "say" -> out.add(MacroAction.bind(key, collectMessages(t, i + 1), false, false));
            case "cmd", "command" -> out.add(MacroAction.bind(key, collectMessages(t, i + 1), true, false));
            case "cycle" -> {
                String cycleKind = t.get(i + 1).toLowerCase();
                boolean isCmd = cycleKind.equals("cmd") || cycleKind.equals("command");
                out.add(MacroAction.bind(key, collectMessages(t, i + 2), isCmd, true));
            }
            default -> System.out.println("[MacroMod] Unknown bind action: " + sub);
        }
    }

    private static List<String> collectMessages(List<String> t, int start) {
        List<String> msgs = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = start; i < t.size(); i++) {
            String tok = t.get(i);
            if (tok.equals("|")) {
                msgs.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                if (cur.length() > 0) cur.append(' ');
                cur.append(tok);
            }
        }
        if (cur.length() > 0) msgs.add(cur.toString().trim());
        return msgs;
    }

    private static void parseKeyLong(List<String> t, List<MacroAction> out) {
        String op = t.get(1).toLowerCase();
        String k = t.get(2);
        switch (op) {
            case "hold" -> out.add(MacroAction.keyHold(k));
            case "release" -> out.add(MacroAction.keyRelease(k));
            case "press" -> out.add(MacroAction.keyPress(k));
        }
    }

    private static void parseMouseLong(List<String> t, List<MacroAction> out) {
        String op = t.get(1).toLowerCase();
        String b = t.get(2).toLowerCase();
        switch (op) {
            case "hold" -> out.add(MacroAction.mouseHold(b));
            case "release" -> out.add(MacroAction.mouseRelease(b));
            case "click" -> out.add(MacroAction.mouseClick(b));
        }
    }

    private static boolean isMouse(String s) {
        s = s.toLowerCase();
        return s.equals("left") || s.equals("right") || s.equals("middle");
    }

    private static String joinFrom(List<String> t, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < t.size(); i++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(t.get(i));
        }
        return sb.toString();
    }

    private static double[] parseCoords(List<String> parts) {
        String joined = String.join(" ", parts);
        String[] nums = joined.split("[\\s,]+");
        if (nums.length != 3) throw new IllegalArgumentException("Need 3 coordinates");
        return new double[] {
            Double.parseDouble(nums[0]),
            Double.parseDouble(nums[1]),
            Double.parseDouble(nums[2])
        };
    }

    private static float[] parseLookValues(List<String> parts) {
        String joined = String.join(" ", parts);
        String[] nums = joined.split("\\s*/\\s*");
        if (nums.length != 2) throw new IllegalArgumentException("Need yaw / pitch");
        return new float[] {
            Float.parseFloat(nums[0].trim()),
            Float.parseFloat(nums[1].trim())
        };
    }

    private static long parseWait(String s) {
        s = s.toLowerCase();
        if (s.endsWith("ms")) return (long) Double.parseDouble(s.substring(0, s.length() - 2));
        if (s.endsWith("s"))  return (long) (Double.parseDouble(s.substring(0, s.length() - 1)) * 1000);
        if (s.endsWith("m"))  return (long) (Double.parseDouble(s.substring(0, s.length() - 1)) * 60000);
        return Long.parseLong(s);
    }

    private static List<String> tokenize(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (c == '|' && !inQuote) {
                if (cur.length() > 0) { out.add(cur.toString().trim()); cur.setLength(0); }
                out.add("|");
            } else if (Character.isWhitespace(c) && !inQuote) {
                if (cur.length() > 0) { out.add(cur.toString()); cur.setLength(0); }
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) out.add(cur.toString().trim());
        return out;
    }
}