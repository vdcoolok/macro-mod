package com.example.macromod;

import net.minecraft.client.Minecraft;

public final class BackgroundInputHandler {

    private static boolean breaking = false;

    private BackgroundInputHandler() {}

    public static void tick(Minecraft client) {
        if (client.player == null || client.gameMode == null) {
            breaking = false;
            return;
        }

        boolean wantBreak = MacroExecutor.isMouseHeld("left")
                || ForcedInputState.isMouseForced(0);

        if (wantBreak && client.screen != null) {
            client.gameMode.continueAttack(true);
            breaking = true;
        } else if (breaking) {
            if (!wantBreak) {
                client.gameMode.stopDestroyBlock();
            }
            breaking = wantBreak;
        }
    }
}
