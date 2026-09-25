package com.example.macromod;

import com.example.macromod.path.behavior.PathingBehavior;
import com.example.macromod.path.calc.CalculationContext;
import com.example.macromod.path.goal.GoalBlock;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class MacroExecutor {

    private static final List<MacroAction> actions = new ArrayList<>();
    private static int index = 0;
    private static long waitUntil = 0;
    private static final Deque<int[]> loopStack = new ArrayDeque<>();

    private static MacroAction activeSnapGoto = null;
    private static MacroAction activeSnapLook = null;
    private static long lastSnapAttempt = 0;

    private static final double SNAP_TOLERANCE = 1.0;

    private static final List<MacroBinding> activeBindings = new ArrayList<>();
    private static final Set<MacroAction> registeredBindings = new HashSet<>();

    private static final Map<String, Boolean> keyStates = new HashMap<>();
    private static final Map<String, Boolean> mouseStates = new HashMap<>();

    private static final List<PendingRelease> pendingReleases = new ArrayList<>();
    private static long tickCounter = 0;

    private record PendingRelease(String name, boolean isMouse, long releaseTick) {}

    public static boolean isRunning() { return !actions.isEmpty(); }

    public static void start(List<MacroAction> parsed) {
        stop();
        actions.addAll(parsed);
        index = 0;
        waitUntil = 0;
    }

    public static void stop() {
        actions.clear();
        index = 0;
        loopStack.clear();
        activeSnapGoto = null;
        activeSnapLook = null;
        lastSnapAttempt = 0;
        activeBindings.clear();
        registeredBindings.clear();
        pendingReleases.clear();
        NolookController.setMode(NolookController.Mode.NONE);
        PathingBehavior.get().stop();
        releaseAllKeys();
    }

    public static void tick(Minecraft client) {
        tickCounter++;
        processPendingReleases();
        if (client.player == null) return;

        reapplyActiveStates();
        tickBindings(client);
        handleSnapBack(client);

        if (PathingBehavior.get().isPathing()) {
            PathingBehavior.get().tick();
            return;
        }

        if (actions.isEmpty()) return;

        if (System.currentTimeMillis() < waitUntil) return;

        if (index >= actions.size()) {
            if (loopStack.isEmpty()) {
                if (activeSnapGoto != null || activeSnapLook != null) return;
                stop();
            }
            return;
        }

        MacroAction a = actions.get(index);
        try {
            switch (a.type) {
                case KEY_HOLD -> setKeyState(a.target, true);
                case KEY_RELEASE -> setKeyState(a.target, false);
                case KEY_PRESS -> {
                    setKeyState(a.target, true);
                    pendingReleases.add(new PendingRelease(a.target, false, tickCounter + 1));
                }
                case MOUSE_HOLD -> setMouseState(a.target, true);
                case MOUSE_RELEASE -> setMouseState(a.target, false);
                case MOUSE_CLICK -> {
                    setMouseState(a.target, true);
                    pendingReleases.add(new PendingRelease(a.target, true, tickCounter + 1));
                }
                case GOTO -> client.player.setPos(a.x, a.y, a.z);
                case LOOK -> {
                    client.player.setYRot(a.yaw);
                    client.player.setXRot(a.pitch);
                }
                case WAIT -> waitUntil = System.currentTimeMillis() + a.waitMs;
                case LOOP_START -> loopStack.push(new int[]{index, a.loopCount});
                case LOOP_END -> {
                    if (!loopStack.isEmpty()) {
                        int[] loop = loopStack.peek();
                        if (loop[1] == -1) { index = loop[0] + 1; return; }
                        loop[1]--;
                        if (loop[1] > 0) { index = loop[0] + 1; return; }
                        loopStack.pop();
                    }
                }
                case SNAP_GOTO -> activeSnapGoto = a;
                case SNAP_LOOK -> activeSnapLook = a;
                case CHAT -> sendChat(client, a.message);
                case CMD -> sendCommand(client, a.message);
                case CYCLE_CHAT -> {
                    if (a.messages == null || a.messages.isEmpty()) break;
                    sendChat(client, a.messages.get(a.cycleIndex));
                    a.cycleIndex = (a.cycleIndex + 1) % a.messages.size();
                }
                case CYCLE_CMD -> {
                    if (a.messages == null || a.messages.isEmpty()) break;
                    sendCommand(client, a.messages.get(a.cycleIndex));
                    a.cycleIndex = (a.cycleIndex + 1) % a.messages.size();
                }
                case BIND -> {
                    if (registeredBindings.add(a)) {
                        activeBindings.add(new MacroBinding(a.keyName, a.messages, a.bindIsCommand));
                    }
                }
                case PATHFIND -> {
                    NolookController.setMode(NolookController.Mode.NONE);
                    PathingBehavior.get().setGoal(new GoalBlock(a.x, a.y, a.z));
                    index++;
                    return;
                }
                case NLOOK_GOTO -> {
                    NolookController.setMode(NolookController.Mode.CAMERA);
                    PathingBehavior.get().setGoal(new GoalBlock(a.x, a.y, a.z));
                    index++;
                    return;
                }
                case NLOOK_AUTO_GOTO -> {
                    NolookController.setMode(NolookController.Mode.LOCKED);
                    PathingBehavior.get().setGoal(new GoalBlock(a.x, a.y, a.z));
                    index++;
                    return;
                }
                case PATHFIND_STOP -> {
                    PathingBehavior.get().stop();
                }
            }
        } catch (Exception e) {
            System.err.println("[MacroMod] Error executing action " + a.type + ": " + e.getMessage());
        }
        index++;
    }

    private static void handleSnapBack(Minecraft client) {
        LocalPlayer p = client.player;
        if (p == null) return;

        if (activeSnapGoto != null && !PathingBehavior.get().isPathing()) {
            double dx = activeSnapGoto.x - p.getX();
            double dy = activeSnapGoto.y - p.getY();
            double dz = activeSnapGoto.z - p.getZ();
            double distSq = dx * dx + dz * dz;

            if (distSq > 1.5 * 1.5 || Math.abs(dy) > 1.0) {
                long now = System.currentTimeMillis();
                if (now - lastSnapAttempt > 500) {
                    lastSnapAttempt = now;
                    PathingBehavior.get().setGoal(new GoalBlock(
                        activeSnapGoto.x, activeSnapGoto.y, activeSnapGoto.z));
                }
            } else {
                double dist = Math.sqrt(distSq);
                if (dist > 0.03) {
                    CalculationContext ctx = new CalculationContext();
                    ctx.lookAt(activeSnapGoto.x, p.getY() + p.getEyeHeight(), activeSnapGoto.z);
                    ctx.setInput("sprint", false);
                    ctx.setInput("jump", false);
                    ctx.setInput("back", false);
                    ctx.setInput("forward", true);
                    if (dist <= 0.25) {
                        ctx.setInput("sneak", true);
                    } else {
                        ctx.setInput("sneak", false);
                    }
                } else {
                    CalculationContext ctx = new CalculationContext();
                    ctx.releaseAllInputs();
                }
            }
        }

        boolean nolookPathing = NolookController.getMode() != NolookController.Mode.NONE;

        if (activeSnapLook != null && !nolookPathing) {
            if (Math.abs(p.getYRot() - activeSnapLook.yaw) > 0.1f ||
                Math.abs(p.getXRot() - activeSnapLook.pitch) > 0.1f) {
                p.setYRot(activeSnapLook.yaw);
                p.setXRot(activeSnapLook.pitch);
            }
        }
    }

    private static void processPendingReleases() {
        Iterator<PendingRelease> it = pendingReleases.iterator();
        while (it.hasNext()) {
            PendingRelease pr = it.next();
            if (pr.releaseTick <= tickCounter) {
                try {
                    if (pr.isMouse) setMouseState(pr.name, false);
                    else setKeyState(pr.name, false);
                } catch (Exception ignored) {}
                it.remove();
            }
        }
    }

    private static void tickBindings(Minecraft client) {
        if (activeBindings.isEmpty()) return;
        Window window = client.getWindow();
        for (MacroBinding b : activeBindings) {
            int code = keyNameToGlfw(b.keyName);
            if (code == -1) continue;
            boolean down = InputConstants.isKeyDown(window, code);
            if (down && !b.wasDown) {
                if (b.messages == null || b.messages.isEmpty()) continue;
                String msg = b.messages.get(b.index);
                if (b.isCommand) sendCommand(client, msg);
                else sendChat(client, msg);
                b.index = (b.index + 1) % b.messages.size();
            }
            b.wasDown = down;
        }
    }

    public static boolean isMouseHeld(String button) {
        return Boolean.TRUE.equals(mouseStates.get(button.toLowerCase()));
    }

    public static MacroAction getActiveSnapLook() {
        return activeSnapLook;
    }

    private static void reapplyActiveStates() {
        Minecraft c = Minecraft.getInstance();
        if (c == null || c.options == null) return;

        for (Map.Entry<String, Boolean> e : keyStates.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) {
                KeyMapping mapping = resolveKeyMapping(e.getKey());
                if (mapping != null) {
                    mapping.setDown(true);
                    InputConstants.Key key = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(mapping);
                    if (key == null) key = mapping.getDefaultKey();
                    KeyMapping.set(key, true);
                }
            }
        }
        for (Map.Entry<String, Boolean> e : mouseStates.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) {
                KeyMapping mapping = switch (e.getKey().toLowerCase()) {
                    case "left" -> c.options.keyAttack;
                    case "right" -> c.options.keyUse;
                    case "middle" -> c.options.keyPickItem;
                    default -> null;
                };
                if (mapping != null) {
                    mapping.setDown(true);
                    InputConstants.Key key = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(mapping);
                    if (key == null) key = mapping.getDefaultKey();
                    KeyMapping.set(key, true);
                    if (c.gameMode != null && !c.gameMode.isDestroying()) {
                        KeyMapping.click(key);
                    }
                }
            }
        }
    }

    private static void setKeyState(String name, boolean pressed) {
        KeyMapping mapping = resolveKeyMapping(name);
        if (mapping == null) {
            System.err.println("[MacroMod] Unknown key: " + name);
            return;
        }
        mapping.setDown(pressed);
        InputConstants.Key key = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(mapping);
        if (key == null) key = mapping.getDefaultKey();
        KeyMapping.set(key, pressed);
        ForcedInputState.forceKey(key, pressed);
        keyStates.put(name, pressed);
    }

    private static void setMouseState(String button, boolean pressed) {
        Minecraft c = Minecraft.getInstance();
        if (c == null || c.options == null) return;
        KeyMapping mapping = null;
        int glfwButton = -1;
        if (button.equalsIgnoreCase("left")) {
            mapping = c.options.keyAttack;
            glfwButton = 0;
        } else if (button.equalsIgnoreCase("right")) {
            mapping = c.options.keyUse;
            glfwButton = 1;
        } else if (button.equalsIgnoreCase("middle")) {
            mapping = c.options.keyPickItem;
            glfwButton = 2;
        }
        if (mapping != null) {
            mapping.setDown(pressed);
            InputConstants.Key key = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(mapping);
            if (key == null) key = mapping.getDefaultKey();
            KeyMapping.set(key, pressed);
            if (pressed) {
                KeyMapping.click(key);
            }
            ForcedInputState.forceKey(key, pressed);
            if (glfwButton >= 0) {
                ForcedInputState.forceMouse(glfwButton, pressed);
            }
        }
        mouseStates.put(button, pressed);
    }

    private static KeyMapping resolveKeyMapping(String name) {
        Minecraft c = Minecraft.getInstance();
        if (c == null || c.options == null) return null;
        return switch (name.toLowerCase()) {
            case "w", "forward" -> c.options.keyUp;
            case "s", "back", "backward" -> c.options.keyDown;
            case "a", "left" -> c.options.keyLeft;
            case "d", "right" -> c.options.keyRight;
            case "space", "jump" -> c.options.keyJump;
            case "shift", "sneak" -> c.options.keyShift;
            case "ctrl", "sprint" -> c.options.keySprint;
            case "e", "inventory" -> c.options.keyInventory;
            case "q", "drop" -> c.options.keyDrop;
            case "f", "swap", "swapoffhand" -> c.options.keySwapOffhand;
            case "t", "chat" -> c.options.keyChat;
            case "tab", "playerlist" -> c.options.keyPlayerList;
            case "1" -> c.options.keyHotbarSlots[0];
            case "2" -> c.options.keyHotbarSlots[1];
            case "3" -> c.options.keyHotbarSlots[2];
            case "4" -> c.options.keyHotbarSlots[3];
            case "5" -> c.options.keyHotbarSlots[4];
            case "6" -> c.options.keyHotbarSlots[5];
            case "7" -> c.options.keyHotbarSlots[6];
            case "8" -> c.options.keyHotbarSlots[7];
            case "9" -> c.options.keyHotbarSlots[8];
            default -> null;
        };
    }

    private static void releaseAllKeys() {
        try {
            keyStates.keySet().forEach(k -> setKeyState(k, false));
            mouseStates.keySet().forEach(k -> setMouseState(k, false));
        } catch (Exception ignored) {}
        ForcedInputState.clear();
        keyStates.clear();
        mouseStates.clear();
    }

    private static void sendChat(Minecraft c, String msg) {
        if (c.player == null || c.player.connection == null) return;
        c.player.connection.sendChat(msg);
    }

    private static void sendCommand(Minecraft c, String cmd) {
        if (c.player == null || c.player.connection == null) return;
        String s = cmd.startsWith("/") ? cmd.substring(1) : cmd;
        c.player.connection.sendCommand(s);
    }

    private static int keyNameToGlfw(String name) {
        name = name.toLowerCase();
        switch (name) {
            case "space": return GLFW.GLFW_KEY_SPACE;
            case "shift": return GLFW.GLFW_KEY_LEFT_SHIFT;
            case "ctrl": case "control": return GLFW.GLFW_KEY_LEFT_CONTROL;
            case "alt": return GLFW.GLFW_KEY_LEFT_ALT;
            case "tab": return GLFW.GLFW_KEY_TAB;
            case "enter": case "return": return GLFW.GLFW_KEY_ENTER;
            case "escape": case "esc": return GLFW.GLFW_KEY_ESCAPE;
            case "backspace": return GLFW.GLFW_KEY_BACKSPACE;
            case "delete": case "del": return GLFW.GLFW_KEY_DELETE;
            case "up": return GLFW.GLFW_KEY_UP;
            case "down": return GLFW.GLFW_KEY_DOWN;
            case "arrow_left": return GLFW.GLFW_KEY_LEFT;
            case "arrow_right": return GLFW.GLFW_KEY_RIGHT;
        }
        if (name.startsWith("f") && name.length() <= 3) {
            try {
                int n = Integer.parseInt(name.substring(1));
                if (n >= 1 && n <= 25) return GLFW.GLFW_KEY_F1 + (n - 1);
            } catch (NumberFormatException ignored) {}
        }
        if (name.length() == 1) {
            char c = name.charAt(0);
            if (c >= 'a' && c <= 'z') return GLFW.GLFW_KEY_A + (c - 'a');
            if (c >= '0' && c <= '9') return GLFW.GLFW_KEY_0 + (c - '0');
        }
        return -1;
    }
}