package com.bl4ues.scpclassifieddirective.inventory.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.mixin.client.LevelRendererEntityTargetAccessor;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.util.Map;

/**
 * Produces a thin Secret Lab-style pickup outline.
 *
 * <p>The target item is rendered into an off-screen outline mask while the
 * world rendering pipeline is still in a geometry-safe stage. The completed
 * mask is then processed and composited over the final scene at AFTER_LEVEL.
 * Nothing is scaled, full-brightened, or recolored in the visible item render.</p>
 */
public final class PickupOutlineRenderer {
    private static final ResourceLocation POST_CHAIN = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "shaders/post/pickup_outline.json");
    private static final String MASK_TARGET = "pickup_mask";
    private static final String EDGE_TARGET = "pickup_edge";

    private static final MultiBufferSource.BufferSource DISCARD_BUFFER =
            new DiscardBufferSource();
    private static final OutlineBufferSource OUTLINE_BUFFER =
            new OutlineBufferSource(DISCARD_BUFFER);

    private static PostChain postChain;
    private static int framebufferWidth = -1;
    private static int framebufferHeight = -1;
    private static boolean unavailable;
    private static boolean maskReady;

    private PickupOutlineRenderer() {
    }

    /** Capture world-space item geometry while entity-like rendering is valid. */
    public static void captureMask(PoseStack poseStack, Camera camera) {
        maskReady = false;

        Minecraft minecraft = Minecraft.getInstance();
        ItemEntity item = PickupPromptClient.outlineTarget();
        if (!canRender(minecraft, item) || !ensurePostChain(minecraft)) {
            return;
        }

        RenderTarget mask = postChain.getTempTarget(MASK_TARGET);
        RenderTarget edge = postChain.getTempTarget(EDGE_TARGET);
        if (mask == null || edge == null) return;

        mask.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        edge.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        mask.clear(Minecraft.ON_OSX);
        edge.clear(Minecraft.ON_OSX);

        LevelRendererEntityTargetAccessor accessor =
                (LevelRendererEntityTargetAccessor) minecraft.levelRenderer;
        RenderTarget previousEntityTarget =
                accessor.scpclassifieddirective$getEntityTarget();

        try {
            accessor.scpclassifieddirective$setEntityTarget(mask);
            renderMask(minecraft, item, poseStack, camera);
            maskReady = true;
        } finally {
            accessor.scpclassifieddirective$setEntityTarget(previousEntityTarget);
            minecraft.getMainRenderTarget().bindWrite(false);
        }
    }

    /** Composite the captured outline over the fully rendered scene. */
    public static void composite() {
        if (!maskReady) return;

        Minecraft minecraft = Minecraft.getInstance();
        ItemEntity item = PickupPromptClient.outlineTarget();
        if (!canRender(minecraft, item) || !ensurePostChain(minecraft)) {
            maskReady = false;
            return;
        }

        postChain.process(minecraft.getFrameTime());
        minecraft.getMainRenderTarget().bindWrite(false);
        maskReady = false;
    }

    private static boolean canRender(Minecraft minecraft, ItemEntity item) {
        return minecraft.level != null
                && minecraft.player != null
                && minecraft.screen == null
                && !minecraft.options.hideGui
                && item != null
                && item.isAlive()
                && !unavailable;
    }

    private static void renderMask(Minecraft minecraft, ItemEntity item,
            PoseStack poseStack, Camera camera) {
        Vec3 cameraPosition = camera.getPosition();
        float partialTick = minecraft.getFrameTime();
        double x = Mth.lerp(partialTick, item.xOld, item.getX())
                - cameraPosition.x;
        double y = Mth.lerp(partialTick, item.yOld, item.getY())
                - cameraPosition.y;
        double z = Mth.lerp(partialTick, item.zOld, item.getZ())
                - cameraPosition.z;

        OUTLINE_BUFFER.setColor(255, 255, 255, 255);
        minecraft.getEntityRenderDispatcher().render(item, x, y, z,
                item.getYRot(), partialTick, poseStack, OUTLINE_BUFFER,
                LightTexture.FULL_BRIGHT);
        OUTLINE_BUFFER.endOutlineBatch();
    }

    private static boolean ensurePostChain(Minecraft minecraft) {
        int width = minecraft.getWindow().getWidth();
        int height = minecraft.getWindow().getHeight();
        if (width <= 0 || height <= 0) return false;

        if (postChain == null) {
            try {
                postChain = new PostChain(minecraft.getTextureManager(),
                        minecraft.getResourceManager(),
                        minecraft.getMainRenderTarget(), POST_CHAIN);
                framebufferWidth = width;
                framebufferHeight = height;
                postChain.resize(width, height);
            } catch (IOException | RuntimeException exception) {
                unavailable = true;
                ScpClassifiedDirectiveMod.LOGGER.error(
                        "Could not initialize the thin pickup outline shader",
                        exception);
                return false;
            }
        } else if (width != framebufferWidth || height != framebufferHeight) {
            framebufferWidth = width;
            framebufferHeight = height;
            postChain.resize(width, height);
        }
        return true;
    }

    private static final class DiscardBufferSource
            extends MultiBufferSource.BufferSource {
        private DiscardBufferSource() {
            super(new BufferBuilder(128), Map.of());
        }

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            return DiscardVertexConsumer.INSTANCE;
        }

        @Override
        public void endBatch() {
        }

        @Override
        public void endBatch(RenderType renderType) {
        }

        @Override
        public void endLastBatch() {
        }
    }

    private enum DiscardVertexConsumer implements VertexConsumer {
        INSTANCE;

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }

        @Override
        public void endVertex() {
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
        }

        @Override
        public void unsetDefaultColor() {
        }
    }
}
