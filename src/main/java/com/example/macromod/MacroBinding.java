package com.example.macromod;

import java.util.List;

public class MacroBinding {
    public final String keyName;
    public final List<String> messages;
    public final boolean isCommand;
    public int index = 0;
    public boolean wasDown = false;

    public MacroBinding(String keyName, List<String> messages, boolean isCommand) {
        this.keyName = keyName;
        this.messages = messages;
        this.isCommand = isCommand;
    }
}