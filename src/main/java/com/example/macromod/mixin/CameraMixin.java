package com.example.macromod.mixin;

import com.example.macromod.FreeCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    public abstract Vec3 position();

    @Shadow
    public abstract Frustum getCullFrustum();

    @Inject(method = "update", at = @At("TAIL"))
    private void macromod$applyFreeCamera(DeltaTracker tracker, CallbackInfo ci) {
        double[] view = FreeCamera.computeView(tracker);
        if (view == null) return;

        setPosition(view[0], view[1], view[2]);
        setRotation((float) view[3], (float) view[4]);

        Vec3 at = position();
        Frustum frustum = getCullFrustum();
        if (frustum != null) {
            frustum.prepare(at.x, at.y, at.z);
        }
    }
}
