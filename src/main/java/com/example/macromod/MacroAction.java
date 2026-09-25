package com.example.macromod;

import java.util.List;

public class MacroAction {

    public enum Type {
        KEY_HOLD, KEY_RELEASE, KEY_PRESS,
        MOUSE_HOLD, MOUSE_RELEASE, MOUSE_CLICK,
        GOTO, LOOK, WAIT,
        LOOP_START, LOOP_END,
        SNAP_GOTO, SNAP_LOOK,
        CHAT, CMD,
        CYCLE_CHAT, CYCLE_CMD,
        BIND,
        PATHFIND, PATHFIND_STOP
    }

    public Type type;
    public String target;
    public double x, y, z;
    public float yaw, pitch;
    public long waitMs;
    public int loopCount;

    public String message;
    public List<String> messages;
    public int cycleIndex = 0;

    public String keyName;
    public boolean bindIsCommand;
    public boolean bindIsCycle;

    public static MacroAction keyHold(String k)     { MacroAction a = new MacroAction(); a.type = Type.KEY_HOLD; a.target = k; return a; }
    public static MacroAction keyRelease(String k)  { MacroAction a = new MacroAction(); a.type = Type.KEY_RELEASE; a.target = k; return a; }
    public static MacroAction keyPress(String k)    { MacroAction a = new MacroAction(); a.type = Type.KEY_PRESS; a.target = k; return a; }
    public static MacroAction mouseHold(String b)   { MacroAction a = new MacroAction(); a.type = Type.MOUSE_HOLD; a.target = b; return a; }
    public static MacroAction mouseRelease(String b){ MacroAction a = new MacroAction(); a.type = Type.MOUSE_RELEASE; a.target = b; return a; }
    public static MacroAction mouseClick(String b)  { MacroAction a = new MacroAction(); a.type = Type.MOUSE_CLICK; a.target = b; return a; }
    public static MacroAction gotoPos(double x, double y, double z) { MacroAction a = new MacroAction(); a.type = Type.GOTO; a.x = x; a.y = y; a.z = z; return a; }
    public static MacroAction look(float yaw, float pitch) { MacroAction a = new MacroAction(); a.type = Type.LOOK; a.yaw = yaw; a.pitch = pitch; return a; }
    public static MacroAction delay(long ms)        { MacroAction a = new MacroAction(); a.type = Type.WAIT; a.waitMs = ms; return a; }
    public static MacroAction loopStart(int c)      { MacroAction a = new MacroAction(); a.type = Type.LOOP_START; a.loopCount = c; return a; }
    public static MacroAction loopEnd()             { MacroAction a = new MacroAction(); a.type = Type.LOOP_END; return a; }
    public static MacroAction snapGoto(double x, double y, double z) { MacroAction a = new MacroAction(); a.type = Type.SNAP_GOTO; a.x = x; a.y = y; a.z = z; return a; }
    public static MacroAction snapLook(float yaw, float pitch) { MacroAction a = new MacroAction(); a.type = Type.SNAP_LOOK; a.yaw = yaw; a.pitch = pitch; return a; }
    public static MacroAction chat(String msg)      { MacroAction a = new MacroAction(); a.type = Type.CHAT; a.message = msg; return a; }
    public static MacroAction cmd(String c)         { MacroAction a = new MacroAction(); a.type = Type.CMD; a.message = c; return a; }
    public static MacroAction cycleChat(List<String> msgs) { MacroAction a = new MacroAction(); a.type = Type.CYCLE_CHAT; a.messages = msgs; return a; }
    public static MacroAction cycleCmd(List<String> msgs)  { MacroAction a = new MacroAction(); a.type = Type.CYCLE_CMD; a.messages = msgs; return a; }
    public static MacroAction bind(String key, List<String> msgs, boolean isCmd, boolean isCycle) {
        MacroAction a = new MacroAction();
        a.type = Type.BIND;
        a.keyName = key;
        a.messages = msgs;
        a.bindIsCommand = isCmd;
        a.bindIsCycle = isCycle;
        return a;
    }

    public static MacroAction pathfind(double x, double y, double z) {
        MacroAction a = new MacroAction();
        a.type = Type.PATHFIND;
        a.x = x;
        a.y = y;
        a.z = z;
        return a;
    }

    public static MacroAction pathfindStop() {
        MacroAction a = new MacroAction();
        a.type = Type.PATHFIND_STOP;
        return a;
    }
}