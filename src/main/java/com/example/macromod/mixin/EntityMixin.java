package com.example.macromod.mixin;

import com.example.macromod.path.render.RotationController;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "moveRelative", at = @At("HEAD"))
    private void macromod$applyMovementRotation(CallbackInfo ci) {
        RotationController.applyForMovement((Entity) (Object) this);
    }

    @Inject(method = "moveRelative", at = @At("RETURN"))
    private void macromod$restoreMovementRotation(CallbackInfo ci) {
        RotationController.restoreAfterMovement((Entity) (Object) this);
    }
}
