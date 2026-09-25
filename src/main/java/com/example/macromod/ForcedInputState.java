package com.example.macromod;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ForcedInputState {

    private static final Set<InputConstants.Key> forcedKeys = new HashSet<>();
    private static final Set<Integer> forcedMouseButtons = new HashSet<>();

    private ForcedInputState() {}

    public static void forceKey(InputConstants.Key key, boolean held) {
        if (key == null) return;
        if (held) forcedKeys.add(key);
        else forcedKeys.remove(key);
    }

    public static void forceMouse(int button, boolean held) {
        if (held) forcedMouseButtons.add(button);
        else forcedMouseButtons.remove(button);
    }

    public static boolean isKeyForced(InputConstants.Key key) {
        return key != null && forcedKeys.contains(key);
    }

    public static boolean isMouseForced(int button) {
        return forcedMouseButtons.contains(button);
    }

    public static Set<InputConstants.Key> getForcedKeys() {
        return Collections.unmodifiableSet(forcedKeys);
    }

    public static Set<Integer> getForcedMouseButtons() {
        return Collections.unmodifiableSet(forcedMouseButtons);
    }

    public static void clear() {
        forcedKeys.clear();
        forcedMouseButtons.clear();
    }
}