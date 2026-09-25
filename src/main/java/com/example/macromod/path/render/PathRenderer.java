package com.example.macromod.path.render;

import com.example.macromod.path.behavior.PathingBehavior;
import com.example.macromod.path.calc.Path;
import com.example.macromod.path.movement.Movement;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public class PathRenderer {

    private static final String MOD_ID = "macromod";
    private static final float LINE_HALF_WIDTH = 0.035f;

    private static final RenderPipeline FILLED_THROUGH_WALLS = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(MOD_ID, "pipeline/path_filled_through_walls"))
            .withDepthStencilState(Optional.empty())
            .build()
    );

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    private static final StagedVertexBuffer stagedBuffer = new StagedVertexBuffer(
            () -> MOD_ID + " path render buffer",
            RenderType.SMALL_BUFFER_SIZE
    );

    private static List<Vec3> cachedPoints = new ArrayList<>();
    private static BlockPos cachedGoal = null;

    public static void extract(LevelExtractionContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        PathingBehavior pb = PathingBehavior.get();
        cachedPoints = new ArrayList<>();
        cachedGoal = null;

        if (!pb.isPathing()) return;

        Path path = pb.getCurrentPath();
        if (path == null || path.isFinished()) return;

        cachedPoints.add(new Vec3(
                mc.player.getX(),
                mc.player.getY() + 0.5,
                mc.player.getZ()
        ));

        for (int i = path.getIndex(); i < path.size(); i++) {
            BlockPos to = path.getMovements().get(i).getTo();
            cachedPoints.add(new Vec3(to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5));
        }
        cachedGoal = path.getFinalDestination();
    }

    public static void render(LevelRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean hasAnything = cachedPoints.size() >= 2 || cachedGoal != null;
        if (!hasAnything) return;

        RenderPipeline pipeline = FILLED_THROUGH_WALLS;
        VertexFormat formatBinding = pipeline.getVertexFormatBinding(0);
        if (formatBinding == null) return;
        PrimitiveTopology primitive = pipeline.getPrimitiveTopology();

        StagedVertexBuffer.Draw draw = stagedBuffer.appendDraw(
                formatBinding, primitive,
                primitive == PrimitiveTopology.QUADS
                        ? RenderSystem.getProjectionType().vertexSorting() : null);

        PoseStack matrices = context.poseStack();
        Vec3 camera = context.levelState().cameraRenderState.pos;
        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        VertexConsumer builder = stagedBuffer.getVertexBuilder(draw);
        Matrix4fc matrix = matrices.last().pose();

        if (cachedPoints.size() >= 2) {
            for (int i = 0; i < cachedPoints.size() - 1; i++) {
                Vec3 a = cachedPoints.get(i);
                Vec3 b = cachedPoints.get(i + 1);
                drawRibbon(matrix, builder,
                        (float) a.x, (float) a.y, (float) a.z,
                        (float) b.x, (float) b.y, (float) b.z,
                        LINE_HALF_WIDTH,
                        0.2f, 1.0f, 0.3f, 1.0f);
            }
        }

        if (cachedGoal != null) {
            drawWireBox(matrix, builder, cachedGoal, 0.005f, 0.2f, 1.0f, 0.3f, 1.0f);
        }

        matrices.popPose();
        stagedBuffer.upload();
        StagedVertexBuffer.ExecuteInfo info = stagedBuffer.getExecuteInfo(draw);
        if (info != null) {
            drawBuffer(Minecraft.getInstance(), info, pipeline);
        }
        stagedBuffer.endFrame();
    }

    private static void drawRibbon(Matrix4fc matrix, VertexConsumer builder,
                                    float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    float halfWidth,
                                    float r, float g, float b, float a) {
        float dx = x2 - x1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-4f) return;

        float nx = (-dz / len) * halfWidth;
        float nz = (dx / len) * halfWidth;

        float rTop = halfWidth * 0.5f;

        float yTop1 = y1 + rTop;
        float yTop2 = y2 + rTop;

        float ax1 = x1 - nx, az1 = z1 - nz;
        float ax2 = x1 + nx, az2 = z1 + nz;
        float bx1 = x2 - nx, bz1 = z2 - nz;
        float bx2 = x2 + nx, bz2 = z2 + nz;

        builder.addVertex(matrix, ax1, yTop1, az1).setColor(r, g, b, a);
        builder.addVertex(matrix, bx1, yTop2, bz1).setColor(r, g, b, a);
        builder.addVertex(matrix, bx2, yTop2, bz2).setColor(r, g, b, a);
        builder.addVertex(matrix, ax2, yTop1, az2).setColor(r, g, b, a);

        builder.addVertex(matrix, ax2, yTop1, az2).setColor(r, g, b, a);
        builder.addVertex(matrix, bx2, yTop2, bz2).setColor(r, g, b, a);
        builder.addVertex(matrix, bx1, yTop2, bz1).setColor(r, g, b, a);
        builder.addVertex(matrix, ax1, yTop1, az1).setColor(r, g, b, a);
    }

    private static void drawWireBox(Matrix4fc matrix, VertexConsumer builder,
                                     BlockPos pos, float expand,
                                     float r, float g, float b, float a) {
        float minX = pos.getX() - expand;
        float minY = pos.getY() - expand;
        float minZ = pos.getZ() - expand;
        float maxX = pos.getX() + 1 + expand;
        float maxY = pos.getY() + 1 + expand;
        float maxZ = pos.getZ() + 1 + expand;
        float t = 0.004f;

        drawRibbon(matrix, builder, minX, minY, minZ, maxX, minY, minZ, t, r, g, b, a);
        drawRibbon(matrix, builder, maxX, minY, minZ, maxX, minY, maxZ, t, r, g, b, a);
        drawRibbon(matrix, builder, maxX, minY, maxZ, minX, minY, maxZ, t, r, g, b, a);
        drawRibbon(matrix, builder, minX, minY, maxZ, minX, minY, minZ, t, r, g, b, a);

        drawRibbon(matrix, builder, minX, maxY, minZ, maxX, maxY, minZ, t, r, g, b, a);
        drawRibbon(matrix, builder, maxX, maxY, minZ, maxX, maxY, maxZ, t, r, g, b, a);
        drawRibbon(matrix, builder, maxX, maxY, maxZ, minX, maxY, maxZ, t, r, g, b, a);
        drawRibbon(matrix, builder, minX, maxY, maxZ, minX, maxY, minZ, t, r, g, b, a);

        drawRibbon(matrix, builder, minX, minY, minZ, minX, maxY, minZ, t, r, g, b, a);
        drawRibbon(matrix, builder, maxX, minY, minZ, maxX, maxY, minZ, t, r, g, b, a);
        drawRibbon(matrix, builder, maxX, minY, maxZ, maxX, maxY, maxZ, t, r, g, b, a);
        drawRibbon(matrix, builder, minX, minY, maxZ, minX, maxY, maxZ, t, r, g, b, a);
    }

    private static void drawBuffer(Minecraft client, StagedVertexBuffer.ExecuteInfo info, RenderPipeline pipeline) {
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR,
                        MODEL_OFFSET, TEXTURE_MATRIX);

        RenderTarget mainTarget = client.gameRenderer.mainRenderTarget();
        GpuTextureView colorTexture = mainTarget.getColorTextureView();
        if (colorTexture == null) return;

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(
                        () -> MOD_ID + " path render pipeline rendering",
                        colorTexture,
                        Optional.empty(),
                        mainTarget.getDepthTextureView(),
                        OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setVertexBuffer(0, info.vertexBuffer().slice());
            renderPass.setIndexBuffer(info.indexBuffer(), info.indexType());
            renderPass.drawIndexed(info.indexCount(), 1, info.firstIndex(),
                    info.baseVertex(), 0);
        }
    }

    public static void close() {
        stagedBuffer.close();
    }
}