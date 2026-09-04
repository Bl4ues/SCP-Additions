package com.bl4ues.scpclassifieddirective.inventory.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.elevator.CoreRoomElevatorCarriageEntity;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.io.IOException;
import java.util.Map;

/**
 * Produces the thin SCP Unity / Secret Lab-style outline shared by physical
 * prompts. Pickup items take priority; otherwise the active contextual block,
 * elevator control, or interactable player corpse is captured.
 *
 * <p>The selected geometry is rendered into an off-screen outline mask while
 * world-space rendering is still valid. A one-pixel post pass extracts only
 * the external silhouette and composites it over the completed scene at
 * AFTER_LEVEL. The visible object itself is never scaled, recolored or drawn
 * full-bright.</p>
 */
public final class PickupOutlineRenderer {
    private static final double BUTTON_MASK_PADDING = 0.003D;
    private static final double BUTTON_FACE_SIZE = 1.5D / 16.0D;
    private static final double STATION_BUTTON_DEPTH = 0.75D / 16.0D;
    private static final double CARRIAGE_BUTTON_DEPTH = 0.5D / 16.0D;
    private static final ResourceLocation POST_CHAIN = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "shaders/post/pickup_outline.json");
    private static final ResourceLocation BUTTON_MASK_TEXTURE =
            new ResourceLocation("minecraft", "textures/block/white_concrete.png");
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
            } else if (context != null && context.isElevatorButton()) {
                renderButtonMask(minecraft, context, poseStack, camera);
            } else if (context != null && context.isCorpse()) {
                renderEntityMask(minecraft, context.entity(), poseStack, camera);
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

    /**
     * GeckoLib/ENTITYBLOCK_ANIMATED blocks must not also feed their baked
     * placeholder model into the mask. Doing so produced the large detached
     * polygons visible around SCP-902, the OCU, terminals, and other prompts.
     */
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

            BlockEntity blockEntity = minecraft.level.getBlockEntity(pos);
            if (state.getRenderShape() != RenderShape.ENTITYBLOCK_ANIMATED) {
                int packedLight = LevelRenderer.getLightColor(
                        minecraft.level, state, pos);
                minecraft.getBlockRenderer().renderSingleBlock(state, poseStack,
                        OUTLINE_BUFFER, packedLight, OverlayTexture.NO_OVERLAY);
            }

            if (blockEntity != null) {
                minecraft.getBlockEntityRenderDispatcher().render(
                        blockEntity, partialTick, poseStack, OUTLINE_BUFFER);
            }
        } finally {
            poseStack.popPose();
        }
    }

    /**
     * Floor-station and moving-carriage prompts intentionally highlight only
     * the physical button currently selected by the prompt rather than the
     * complete elevator model.
     */
    private static void renderButtonMask(Minecraft minecraft,
            ContextPromptOutlineTarget.Target context, PoseStack poseStack,
            Camera camera) {
        Vec3 anchor = context.anchor();
        if (anchor == null) return;

        boolean carriageButton = context.entity()
                instanceof CoreRoomElevatorCarriageEntity;
        Direction facing = Direction.NORTH;
        Vec3 faceNormal;
        if (context.entity()
                instanceof CoreRoomElevatorCarriageEntity carriage) {
            facing = carriage.facing();
            faceNormal = carriage.position().subtract(anchor)
                    .multiply(1.0D, 0.0D, 1.0D).normalize();
        } else if (context.blockPos() != null && minecraft.level != null) {
            BlockState state = minecraft.level.getBlockState(context.blockPos());
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            }
            faceNormal = new Vec3(facing.getStepX(), 0.0D,
                    facing.getStepZ());
        } else {
            faceNormal = new Vec3(facing.getStepX(), 0.0D,
                    facing.getStepZ());
        }
        if (faceNormal.lengthSqr() < 1.0E-6D) {
            faceNormal = new Vec3(facing.getStepX(), 0.0D,
                    facing.getStepZ());
        }
        faceNormal = faceNormal.normalize();
        double faceSize = BUTTON_FACE_SIZE + BUTTON_MASK_PADDING * 2.0D;
        double depth = (carriageButton
                ? CARRIAGE_BUTTON_DEPTH : STATION_BUTTON_DEPTH)
                + BUTTON_MASK_PADDING * 2.0D;
        Vec3 maskFront = anchor.add(faceNormal.scale(BUTTON_MASK_PADDING));
        Vec3 cameraPosition = camera.getPosition();

        poseStack.pushPose();
        try {
            poseStack.translate(maskFront.x - cameraPosition.x,
                    maskFront.y - cameraPosition.y,
                    maskFront.z - cameraPosition.z);
            VertexConsumer consumer = OUTLINE_BUFFER.getBuffer(
                    RenderType.entityCutoutNoCull(BUTTON_MASK_TEXTURE));
            emitOrientedBox(consumer, poseStack.last(), faceNormal,
                    faceSize, faceSize, depth);
        } finally {
            poseStack.popPose();
        }
    }

    /** The anchor is the center of the button's exposed face. */
    private static void emitOrientedBox(VertexConsumer consumer,
            PoseStack.Pose pose, Vec3 faceNormal, double width,
            double height, double depth) {
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        Vec3 outward = faceNormal.normalize();
        Vec3 right = new Vec3(outward.z, 0.0D, -outward.x)
                .scale(width * 0.5D);
        Vec3 up = new Vec3(0.0D, height * 0.5D, 0.0D);
        Vec3 back = outward.scale(-depth);

        Vec3 frontBottomLeft = right.scale(-1.0D).subtract(up);
        Vec3 frontBottomRight = right.subtract(up);
        Vec3 frontTopRight = right.add(up);
        Vec3 frontTopLeft = right.scale(-1.0D).add(up);
        Vec3 backBottomLeft = frontBottomLeft.add(back);
        Vec3 backBottomRight = frontBottomRight.add(back);
        Vec3 backTopRight = frontTopRight.add(back);
        Vec3 backTopLeft = frontTopLeft.add(back);

        quad(consumer, matrix, normal, frontBottomLeft, frontBottomRight,
                frontTopRight, frontTopLeft, outward);
        quad(consumer, matrix, normal, backBottomRight, backBottomLeft,
                backTopLeft, backTopRight, outward.scale(-1.0D));
        Vec3 rightNormal = right.normalize();
        quad(consumer, matrix, normal, frontBottomRight, backBottomRight,
                backTopRight, frontTopRight, rightNormal);
        quad(consumer, matrix, normal, backBottomLeft, frontBottomLeft,
                frontTopLeft, backTopLeft, rightNormal.scale(-1.0D));
        quad(consumer, matrix, normal, frontTopLeft, frontTopRight,
                backTopRight, backTopLeft, new Vec3(0.0D, 1.0D, 0.0D));
        quad(consumer, matrix, normal, backBottomLeft, backBottomRight,
                frontBottomRight, frontBottomLeft,
                new Vec3(0.0D, -1.0D, 0.0D));
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix,
            Matrix3f normal, Vec3 first, Vec3 second, Vec3 third,
            Vec3 fourth, Vec3 faceNormal) {
        quad(consumer, matrix, normal,
                (float) first.x, (float) first.y, (float) first.z,
                (float) second.x, (float) second.y, (float) second.z,
                (float) third.x, (float) third.y, (float) third.z,
                (float) fourth.x, (float) fourth.y, (float) fourth.z,
                (float) faceNormal.x, (float) faceNormal.y,
                (float) faceNormal.z);
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix,
            Matrix3f normal, float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float nx, float ny, float nz) {
        vertex(consumer, matrix, normal, x0, y0, z0, 0.0F, 0.0F, nx, ny, nz);
        vertex(consumer, matrix, normal, x1, y1, z1, 1.0F, 0.0F, nx, ny, nz);
        vertex(consumer, matrix, normal, x2, y2, z2, 1.0F, 1.0F, nx, ny, nz);
        vertex(consumer, matrix, normal, x3, y3, z3, 0.0F, 1.0F, nx, ny, nz);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
            Matrix3f normal, float x, float y, float z, float u, float v,
            float nx, float ny, float nz) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, nx, ny, nz)
                .endVertex();
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
