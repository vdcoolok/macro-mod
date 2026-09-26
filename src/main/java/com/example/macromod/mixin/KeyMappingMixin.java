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
        method = "set(Lcom/mojang/blaze3d/platform/InputConstants$Key;Z)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void macromod$guardSet(InputConstants.Key key, boolean held, CallbackInfo ci) {
        Boolean forced = ForcedInputState.forcedState(key);
        if (forced != null && forced != held) {
            ci.cancel();
            KeyMapping.set(key, forced);
        }
    }

    @Inject(
        method = "setDown",
        at = @At("HEAD"),
        cancellable = true
    )
    private void macromod$guardSetDown(boolean held, CallbackInfo ci) {
        InputConstants.Key key = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf((KeyMapping) (Object) this);
        if (key != null) {
            Boolean forced = ForcedInputState.forcedState(key);
            if (forced != null && forced != held) {
                ci.cancel();
            }
        }
    }

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
