package com.example.macromod;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ForcedInputState {

    private static final Map<InputConstants.Key, Boolean> forcedKeyStates = new HashMap<>();
    private static final Set<Integer> forcedMouseButtons = new HashSet<>();

    private ForcedInputState() {}

    public static void forceKey(InputConstants.Key key, boolean held) {
        if (key == null) return;
        forcedKeyStates.put(key, held);
    }

    public static void forceMouse(int button, boolean held) {
        if (held) forcedMouseButtons.add(button);
        else forcedMouseButtons.remove(button);
    }

    public static boolean isKeyForced(InputConstants.Key key) {
        return forcedKeyStates.containsKey(key);
    }

    public static boolean isMouseForced(int button) {
        return forcedMouseButtons.contains(button);
    }

    public static Map<InputConstants.Key, Boolean> getForcedKeyStates() {
        return Collections.unmodifiableMap(forcedKeyStates);
    }

    public static Set<Integer> getForcedMouseButtons() {
        return Collections.unmodifiableSet(forcedMouseButtons);
    }

    public static void clear() {
        forcedKeyStates.clear();
        forcedMouseButtons.clear();
    }
}
