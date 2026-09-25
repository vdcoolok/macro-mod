package com.example.macromod.mixin;

import com.example.macromod.ForcedInputState;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Map;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(
        method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V",
        at = @At("TAIL")
    )
    private void macromod$reapplyForcedKeys(long window, int action, KeyEvent event, CallbackInfo ci) {
        for (Map.Entry<InputConstants.Key, Boolean> e : ForcedInputState.getForcedKeyStates().entrySet()) {
            KeyMapping.set(e.getKey(), e.getValue());
        }
    }
}
