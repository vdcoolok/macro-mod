package com.example.macromod.mixin;

import com.example.macromod.path.render.PathRenderer;
import com.example.macromod.path.render.SecondCamera;
import com.mojang.blaze3d.pipeline.RenderTarget;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "close", at = @At("RETURN"))
    private void macromod$onGameRendererClose(CallbackInfo ci) {
        PathRenderer.close();
        SecondCamera.close();
    }

    @Inject(method = "mainRenderTarget", at = @At("HEAD"), cancellable = true)
    private void macromod$redirectMainTarget(CallbackInfoReturnable<RenderTarget> cir) {
        RenderTarget alternate = SecondCamera.redirectedTarget();
        if (alternate != null) {
            cir.setReturnValue(alternate);
        }
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void macromod$secondCameraPass(DeltaTracker tracker, CallbackInfo ci) {
        SecondCamera.renderLevel(tracker);
    }

    @Inject(method = "extract", at = @At("TAIL"))
    private void macromod$submitSecondCamera(DeltaTracker tracker, boolean renderLevel, CallbackInfo ci) {
        GameRenderState game = ((GameRenderer) (Object) this).gameRenderState();
        if (game == null) return;
        SecondCamera.submitOverlay(game.guiRenderState);
    }
}
