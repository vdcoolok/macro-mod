package com.example.macromod;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;

public final class BackgroundInputHandler {

    private static boolean breaking = false;
    private static boolean wantBreakPrev = false;

    private BackgroundInputHandler() {}

    public static void tick(Minecraft client) {
        if (client.player == null || client.gameMode == null) {
            breaking = false;
            wantBreakPrev = false;
            return;
        }

        boolean wantBreak = MacroExecutor.isMouseHeld("left")
                || ForcedInputState.isMouseForced(0);

        boolean screenOpen = client.gui != null && client.gui.screen() != null;

        if (!wantBreak) {
            if (breaking) {
                client.gameMode.stopDestroyBlock();
                breaking = false;
            }
            wantBreakPrev = false;
            return;
        }

        if (!wantBreakPrev && !screenOpen && !client.gameMode.isDestroying()) {
            resumeBreaking(client);
        }

        if (screenOpen) {
            keepBreaking(client);
        }

        breaking = true;
        wantBreakPrev = true;
    }

    private static void resumeBreaking(Minecraft client) {
        if (!(client.hitResult instanceof BlockHitResult hit)) return;
        client.gameMode.startDestroyBlock(hit.getBlockPos(), hit.getDirection());
    }

    private static void keepBreaking(Minecraft client) {
        if (!(client.hitResult instanceof BlockHitResult hit)) return;

        BlockPos pos = hit.getBlockPos();

        if (client.gameMode.isDestroying()) {
            client.gameMode.continueDestroyBlock(pos, hit.getDirection());
        } else {
            client.gameMode.startDestroyBlock(pos, hit.getDirection());
        }
    }
}
