package com.example.macromod;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;

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

        if (!wantBreak) {
            if (breaking) {
                client.gameMode.stopDestroyBlock();
                breaking = false;
            }
            return;
        }

        if (client.gui != null && client.gui.screen() != null) {
            keepBreaking(client);
        }

        breaking = true;
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
