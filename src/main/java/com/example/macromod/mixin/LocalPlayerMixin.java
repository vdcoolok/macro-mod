package com.example.macromod.mixin;

import com.example.macromod.path.render.RotationController;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void macromod$applyRotationBeforeTick(CallbackInfo ci) {
        RotationController.applyBeforeTick();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void macromod$restoreRotationAfterTick(CallbackInfo ci) {
        RotationController.restoreAfterTick();
    }
}