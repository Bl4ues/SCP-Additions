package com.bl4ues.scpclassifieddirective.inventory.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.mixin.client.LevelRendererEntityTargetAccessor;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.util.Map;

/**
 * Produces the thin SCP Unity / Secret Lab-style outline shared by physical
 * prompts. Pickup items take priority; otherwise the active contextual block
 * or interactable player corpse is captured with its real renderer geometry.
 *
 * <p>The selected geometry is rendered into an off-screen outline mask while
 * world-space rendering is still valid. A one-pixel post pass extracts only
 * the external silhouette and composites it over the completed scene at
 * AFTER_LEVEL. The visible object itself is never scaled, recolored or drawn
 * full-bright.</p>
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

    /** Capture the current physical prompt target while geometry rendering is valid. */
    public static void captureMask(PoseStack poseStack, Camera camera) {
        maskReady = false;

        Minecraft minecraft = Minecraft.getInstance();
        if (!canRender(minecraft) || !ensurePostChain(minecraft)) return;

        ItemEntity pickup = PickupPromptClient.outlineTarget();
        ContextPromptOutlineTarget.Target context = pickup == null
                ? ContextPromptOutlineTarget.current(minecraft) : null;
        if ((pickup == null || !pickup.isAlive()) && context == null) return;

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
            OUTLINE_BUFFER.setColor(255, 255, 255, 255);
            if (pickup != null && pickup.isAlive()) {
                renderEntityMask(minecraft, pickup, poseStack, camera);
            } else if (context != null && context.isCorpse()) {
                renderEntityMask(minecraft, context.corpse(), poseStack, camera);
            } else if (context != null && context.isBlock()) {
                renderBlockMask(minecraft, context.blockPos(), poseStack, camera);
            }
            OUTLINE_BUFFER.endOutlineBatch();
            maskReady = true;
        } finally {
            accessor.scpclassifieddirective$setEntityTarget(previousEntityTarget);
            minecraft.getMainRenderTarget().bindWrite(false);
        }
    }

    /** Composite the captured one-pixel silhouette over the fully rendered scene. */
    public static void composite() {
        if (!maskReady) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (!canRender(minecraft) || !ensurePostChain(minecraft)) {
            maskReady = false;
            return;
        }

        postChain.process(minecraft.getFrameTime());
        minecraft.getMainRenderTarget().bindWrite(false);
        maskReady = false;
    }

    private static boolean canRender(Minecraft minecraft) {
        return minecraft.level != null
                && minecraft.player != null
                && minecraft.screen == null
                && !minecraft.options.hideGui
                && !unavailable;
    }

    private static void renderEntityMask(Minecraft minecraft, Entity entity,
            PoseStack poseStack, Camera camera) {
        if (entity == null || entity.isRemoved()) return;

        Vec3 cameraPosition = camera.getPosition();
        float partialTick = minecraft.getFrameTime();
        double x = Mth.lerp(partialTick, entity.xOld, entity.getX())
                - cameraPosition.x;
        double y = Mth.lerp(partialTick, entity.yOld, entity.getY())
                - cameraPosition.y;
        double z = Mth.lerp(partialTick, entity.zOld, entity.getZ())
                - cameraPosition.z;
        float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());

        minecraft.getEntityRenderDispatcher().render(entity, x, y, z,
                yaw, partialTick, poseStack, OUTLINE_BUFFER,
                LightTexture.FULL_BRIGHT);
    }

    private static void renderBlockMask(Minecraft minecraft, BlockPos pos,
            PoseStack poseStack, Camera camera) {
        if (minecraft.level == null || pos == null) return;
        BlockState state = minecraft.level.getBlockState(pos);
        if (state.isAir()) return;

        Vec3 cameraPosition = camera.getPosition();
        float partialTick = minecraft.getFrameTime();
        poseStack.pushPose();
        try {
            poseStack.translate(pos.getX() - cameraPosition.x,
                    pos.getY() - cameraPosition.y,
                    pos.getZ() - cameraPosition.z);

            int packedLight = LevelRenderer.getLightColor(
                    minecraft.level, state, pos);
            minecraft.getBlockRenderer().renderSingleBlock(state, poseStack,
                    OUTLINE_BUFFER, packedLight, OverlayTexture.NO_OVERLAY);

            BlockEntity blockEntity = minecraft.level.getBlockEntity(pos);
            if (blockEntity != null) {
                minecraft.getBlockEntityRenderDispatcher().render(
                        blockEntity, partialTick, poseStack, OUTLINE_BUFFER);
            }
        } finally {
            poseStack.popPose();
        }
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
                        "Could not initialize the thin physical prompt outline shader",
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
