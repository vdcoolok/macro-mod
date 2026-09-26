package com.example.macromod.mixin;

import com.example.macromod.NolookController;
import com.example.macromod.path.render.RotationController;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V",
            shift = At.Shift.AFTER
        )
    )
    private void macromod$applyRotationBeforeTick(CallbackInfo ci) {
        RotationController.applyBeforeTick();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void macromod$restoreRotationAfterTick(CallbackInfo ci) {
        RotationController.restoreAfterTick();
    }

    @Inject(
        method = "sendPosition",
        at = @At("HEAD")
    )
    private void macromod$beforeMovementPacket(CallbackInfo ci) {
        NolookController.beforeMovementPacket((LocalPlayer) (Object) this);
    }

    @Inject(
        method = "sendPosition",
        at = @At("RETURN")
    )
    private void macromod$afterMovementPacket(CallbackInfo ci) {
        NolookController.afterMovementPacket((LocalPlayer) (Object) this);
    }
}
