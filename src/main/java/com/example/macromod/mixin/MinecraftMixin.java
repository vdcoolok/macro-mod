package com.example.macromod.mixin;

import com.example.macromod.path.render.RotationController;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "getViewYRot", at = @At("HEAD"), cancellable = true, require = 0)
    private void macromod$freeCameraYaw(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (RotationController.overrideCamera()) {
            cir.setReturnValue(RotationController.getCameraYaw());
        }
    }

    @Inject(method = "getViewXRot", at = @At("HEAD"), cancellable = true, require = 0)
    private void macromod$freeCameraPitch(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (RotationController.overrideCamera()) {
            cir.setReturnValue(RotationController.getCameraPitch());
        }
    }
}
