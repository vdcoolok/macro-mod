package com.example.macromod.path.render;

import net.minecraft.client.Camera;

public final class AimCamera extends Camera {

    public void aim(float yaw, float pitch) {
        setRotation(yaw, pitch);
    }

    public void place(double x, double y, double z) {
        setPosition(x, y, z);
    }
}
