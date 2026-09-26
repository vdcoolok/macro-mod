package com.example.macromod.mixin;

import com.example.macromod.FreeCamera;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "onScroll", at = @At("HEAD"))
    private void macromod$onScroll(long window, double xDelta, double yDelta, CallbackInfo ci) {
        FreeCamera.onScroll(yDelta);
    }
}
