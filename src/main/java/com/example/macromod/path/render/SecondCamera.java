package com.example.macromod.path.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;

import java.util.OptionalDouble;

public final class SecondCamera {

    private static final String MOD_ID = "macromod";
    private static final String LABEL = "MacroMod Second Camera";
    private static final int MIN_WIDTH = 160;
    private static final int MAX_WIDTH = 480;
    private static final int MARGIN = 4;
    private static final float NEAR = 0.05f;

    private static boolean enabled = true;
    private static boolean rendered = false;
    private static RenderTarget redirect = null;

    private static TextureTarget target = null;
    private static ProjectionMatrixBuffer projectionBuffer = null;
    private static Projection projection = null;
    private static FogRenderer fogRenderer = null;
    private static CrossFrameResourcePool resourcePool = null;
    private static GpuSampler sampler = null;
    private static int samplerAnisotropy = -1;
    private static AimCamera camera = null;
    private static CameraRenderState state = null;
    private static final Matrix4f viewRotation = new Matrix4f();
    private static final Matrix3x2f pose = new Matrix3x2f();
    private static GpuBufferSlice fogBuffer = null;

    private SecondCamera() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) rendered = false;
    }

    public static RenderTarget redirectedTarget() {
        return redirect;
    }

    public static void renderLevel(DeltaTracker tracker) {
        if (!enabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameRenderer == null) return;

        GameRenderState game = mc.gameRenderer.gameRenderState();
        if (game == null || game.levelRenderState == null) return;

        CameraRenderState main = game.levelRenderState.cameraRenderState;
        if (main == null || !main.initialized) return;

        RenderTarget screen = mc.gameRenderer.mainRenderTarget();
        if (screen == null || screen.width <= 0 || screen.height <= 0) return;

        int scale = Math.max(1, mc.getWindow().getGuiScale());
        int guiWidth = screen.width / scale;
        int guiHeight = screen.height / scale;

        int blitWidth = clamp((int) (guiWidth / 3f), MIN_WIDTH, MAX_WIDTH);
        int blitHeight = Math.round(blitWidth * 9f / 16f);
        if (blitHeight > guiHeight - MARGIN * 2) {
            blitHeight = Math.max(1, guiHeight - MARGIN * 2);
            blitWidth = Math.round(blitHeight * 16f / 9f);
        }

        int viewWidth = blitWidth * scale;
        int viewHeight = blitHeight * scale;

        if (ensure(viewWidth, viewHeight) == null) return;

        float partial = tracker.getGameTimeDeltaPartialTick(false);
        Vec3 eye = mc.player.getEyePosition(partial);
        float[] bot = RotationController.getBotRotation();

        camera.setLevel(mc.level);
        camera.update(tracker);
        camera.aim(bot[0], bot[1]);
        camera.place(eye.x, eye.y, eye.z);
        camera.extractRenderState(state, partial);

        state.fogType = main.fogType;
        state.fogData = main.fogData;

        projection.setupPerspective(NEAR, state.depthFar, camera.getFov(), viewWidth, viewHeight);
        projection.getMatrix(state.projectionMatrix);
        camera.getViewRotationMatrix(viewRotation);

        Frustum frustum = new Frustum(viewRotation, state.projectionMatrix);
        frustum.prepare(eye.x, eye.y, eye.z);
        state.cullFrustum = frustum;

        fogRenderer.updateBuffer(state.fogData);
        fogBuffer = fogRenderer.getBuffer(FogRenderer.FogMode.WORLD);

        RenderSystem.setProjectionMatrix(
            projectionBuffer.getBuffer(state.projectionMatrix),
            ProjectionType.PERSPECTIVE
        );
        RenderSystem.setShaderFog(fogBuffer);

        redirect = target;
        try {
            mc.levelRenderer.render(
                resourcePool,
                tracker,
                false,
                state,
                viewRotation,
                fogBuffer,
                state.fogData.color,
                true
            );
            rendered = true;
        } finally {
            redirect = null;
        }

        fogRenderer.endFrame();
        resourcePool.endFrame();
    }

    public static void submitOverlay(GuiRenderState gui) {
        if (!rendered || gui == null) return;
        if (target == null || gui.isHudHidden) return;

        GpuTextureView view = target.getColorTextureView();
        if (view == null) return;

        Minecraft mc = Minecraft.getInstance();
        int want = anisotropy();
        if (sampler == null || samplerAnisotropy != want) {
            if (sampler != null) sampler.close();
            sampler = RenderSystem.getDevice().createSampler(
                AddressMode.CLAMP_TO_EDGE,
                AddressMode.CLAMP_TO_EDGE,
                FilterMode.LINEAR,
                FilterMode.LINEAR,
                want,
                OptionalDouble.empty()
            );
            samplerAnisotropy = want;
        }

        RenderTarget screen = mc.gameRenderer == null ? null : mc.gameRenderer.mainRenderTarget();
        if (screen == null || screen.width <= 0 || screen.height <= 0) return;

        int scale = Math.max(1, mc.getWindow().getGuiScale());
        int guiWidth = screen.width / scale;
        int guiHeight = screen.height / scale;

        int w = clamp((int) (guiWidth / 3f), MIN_WIDTH, MAX_WIDTH);
        int h = Math.round(w * 9f / 16f);
        if (h > guiHeight - MARGIN * 2) {
            h = Math.max(1, guiHeight - MARGIN * 2);
            w = Math.round(h * 16f / 9f);
        }

        int x0 = guiWidth - w - MARGIN;
        int y0 = MARGIN;

        gui.addBlitToCurrentLayer(new BlitRenderState(
            RenderPipelines.GUI_TEXTURED,
            TextureSetup.singleTexture(view, sampler),
            pose,
            x0,
            y0,
            x0 + w,
            y0 + h,
            0f,
            1f,
            0f,
            1f,
            0xFFFFFFFF,
            ScreenRectangle.empty()
        ));
    }

    public static void close() {
        rendered = false;
        redirect = null;
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
        if (projectionBuffer != null) {
            projectionBuffer.close();
            projectionBuffer = null;
        }
        projection = null;
        if (fogRenderer != null) {
            fogRenderer.close();
            fogRenderer = null;
        }
        if (resourcePool != null) {
            resourcePool.close();
            resourcePool = null;
        }
        if (sampler != null) {
            sampler.close();
            sampler = null;
        }
        samplerAnisotropy = -1;
        camera = null;
        state = null;
        fogBuffer = null;
    }

    private static TextureTarget ensure(int width, int height) {
        if (target == null) {
            target = new TextureTarget(LABEL, width, height, true, GpuFormat.RGBA8_UNORM);
        } else if (target.width != width || target.height != height) {
            target.resize(width, height);
        }

        if (projectionBuffer == null) {
            projectionBuffer = new ProjectionMatrixBuffer(MOD_ID + " second camera projection");
        }
        if (projection == null) projection = new Projection();
        if (fogRenderer == null) fogRenderer = new FogRenderer();
        if (resourcePool == null) resourcePool = new CrossFrameResourcePool(4);
        if (camera == null) camera = new AimCamera();
        if (state == null) {
            state = new CameraRenderState();
            state.orientation.identity();
        }

        return target.getColorTextureView() == null ? null : target;
    }

    private static int anisotropy() {
        Minecraft mc = Minecraft.getInstance();
        GameRenderState game = mc.gameRenderer == null ? null : mc.gameRenderer.gameRenderState();
        if (game == null || game.optionsRenderState.textureFiltering != TextureFilteringMethod.ANISOTROPIC) {
            return 1;
        }
        return clamp(game.optionsRenderState.maxAnisotropyValue, 1, 16);
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
