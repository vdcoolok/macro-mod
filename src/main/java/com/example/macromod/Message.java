package com.example.macromod;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class Message {

    private Message() {}

    public static void info(String text) {
        send("§6[MacroMod] §f" + text);
    }

    public static void success(String text) {
        send("§6[MacroMod] §a" + text);
    }

    public static void error(String text) {
        send("§6[MacroMod] §c" + text);
    }

    private static void send(String text) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        client.player.sendSystemMessage(Component.literal(text));
    }
}
