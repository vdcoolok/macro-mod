package com.example.macromod.mixin;

import com.example.macromod.ForcedInputState;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyMapping.class)
public class KeyMappingMixin {

    @Inject(
        method = {"releaseAll", "restoreToggleStatesOnScreenClosed"},
        at = @At("TAIL")
    )
    private static void macromod$reapplyForcedInputs(CallbackInfo ci) {
        for (var entry : ForcedInputState.getForcedKeyStates().entrySet()) {
            KeyMapping.set(entry.getKey(), entry.getValue());
        }
        for (int button : ForcedInputState.getForcedMouseButtons()) {
            KeyMapping.set(InputConstants.Type.MOUSE.getOrCreate(button), true);
        }
    }
}
