package com.example.macromod.mixin;

import com.example.macromod.ForcedInputState;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "releaseAllMouseButtons", at = @At("TAIL"))
    private void macromod$reapplyForcedMouseButtons(CallbackInfo ci) {
        for (int button : ForcedInputState.getForcedMouseButtons()) {
            InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(button);
            KeyMapping.set(key, true);
        }
        for (var e : ForcedInputState.getForcedKeyStates().entrySet()) {
            if (e.getValue()) KeyMapping.set(e.getKey(), true);
        }
    }
}
