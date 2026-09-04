package com.bl4ues.scpclassifieddirective.inventory.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.elevator.CoreRoomElevatorCarriageEntity;
import com.bl4ues.scpclassifieddirective.mixin.client.LevelRendererEntityTargetAccessor;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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
    private static final double MODEL_UNIT = 1.0D / 16.0D;
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
        boolean up = context.interactionKey() != null
                && context.interactionKey().endsWith("_up");
        if (context.entity() instanceof CoreRoomElevatorCarriageEntity carriage) {
            renderCarriageButtonMask(minecraft, carriage, up, poseStack,
                    camera);
        } else if (context.blockPos() != null && minecraft.level != null) {
            renderStationButtonMask(minecraft, context.blockPos(), up,
                    poseStack, camera);
        }
    }

    /**
     * Replays the authored station bone hierarchy instead of approximating the
     * button with a world-aligned box. Values below come directly from
     * core_room_elevator_floor_station.geo.json.
     */
    private static void renderStationButtonMask(Minecraft minecraft,
            BlockPos pos, boolean up, PoseStack poseStack, Camera camera) {
        BlockState state = minecraft.level.getBlockState(pos);
        Direction facing = state.hasProperty(
                BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
        Vec3 cameraPosition = camera.getPosition();

        poseStack.pushPose();
        try {
            poseStack.translate(pos.getX() - cameraPosition.x,
                    pos.getY() - cameraPosition.y,
                    pos.getZ() - cameraPosition.z);
            poseStack.translate(0.5D, 0.0D, 0.5D);
            rotateForFacing(poseStack, facing);

            applyAuthoredBoneTransform(poseStack,
                    14.48819D, 20.5D, -16.55101D,
                    0.0D, 45.0D, 0.0D);
            applyAuthoredBoneTransform(poseStack,
                    14.64492D, up ? 21.25D : 19.25D, -16.69749D,
                    up ? 0.0D : 180.0D, 45.0D, 0.0D);

            VertexConsumer consumer = OUTLINE_BUFFER.getBuffer(
                    RenderType.entityCutoutNoCull(BUTTON_MASK_TEXTURE));
            emitAuthoredCube(consumer, poseStack.last(),
                    13.89492D, up ? 20.5D : 18.5D, -17.44749D,
                    0.75D, 1.5D, 1.5D);
        } finally {
            poseStack.popPose();
        }
    }

    /**
     * Replays the same non-living-entity basis and button bone used by the
     * carriage renderer. Values come directly from
     * core_room_elevator_carriage.geo.json.
     */
    private static void renderCarriageButtonMask(Minecraft minecraft,
            CoreRoomElevatorCarriageEntity carriage, boolean up,
            PoseStack poseStack, Camera camera) {
        float partialTick = minecraft.getFrameTime();
        Vec3 renderPosition = carriage.getPosition(partialTick);
        Vec3 cameraPosition = camera.getPosition();

        poseStack.pushPose();
        try {
            poseStack.translate(renderPosition.x - cameraPosition.x,
                    renderPosition.y - cameraPosition.y,
                    renderPosition.z - cameraPosition.z);
            // GeoEntityRenderer supplies 180 degrees to non-living entities;
            // CarriageRenderer then applies its authored EAST basis and the
            // logical station facing.
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F
                    + rotationDegreesFor(carriage.facing())));
            poseStack.translate(0.0D, 0.01D, 0.0D);

            applyAuthoredBoneTransform(poseStack,
                    -10.95508D, up ? 21.25D : 19.25D, 11.00251D,
                    up ? 0.0D : 180.0D, 45.0D, 0.0D);

            VertexConsumer consumer = OUTLINE_BUFFER.getBuffer(
                    RenderType.entityCutoutNoCull(BUTTON_MASK_TEXTURE));
            emitAuthoredCube(consumer, poseStack.last(),
                    -11.45508D, up ? 20.5D : 18.5D, 10.25251D,
                    0.5D, 1.5D, 1.5D);
        } finally {
            poseStack.popPose();
        }
    }

    /** Apply GeckoLib's Bedrock-to-Minecraft pivot and rotation conversion. */
    private static void applyAuthoredBoneTransform(PoseStack poseStack,
            double pivotX, double pivotY, double pivotZ,
            double rotationX, double rotationY, double rotationZ) {
        double x = -pivotX * MODEL_UNIT;
        double y = pivotY * MODEL_UNIT;
        double z = pivotZ * MODEL_UNIT;
        poseStack.translate(x, y, z);
        if (rotationZ != 0.0D) {
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) rotationZ));
        }
        if (rotationY != 0.0D) {
            poseStack.mulPose(Axis.YP.rotationDegrees((float) -rotationY));
        }
        if (rotationX != 0.0D) {
            poseStack.mulPose(Axis.XP.rotationDegrees((float) -rotationX));
        }
        poseStack.translate(-x, -y, -z);
    }

    private static void rotateForFacing(PoseStack poseStack,
            Direction facing) {
        float degrees = rotationDegreesFor(facing);
        if (degrees != 0.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(degrees));
        }
    }

    private static float rotationDegreesFor(Direction facing) {
        return switch (facing) {
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    /** Emit the exact converted bounds of one cube from a GeckoLib geo file. */
    private static void emitAuthoredCube(VertexConsumer consumer,
            PoseStack.Pose pose, double originX, double originY,
            double originZ, double sizeX, double sizeY, double sizeZ) {
        double minX = -(originX + sizeX) * MODEL_UNIT;
        double minY = originY * MODEL_UNIT;
        double minZ = originZ * MODEL_UNIT;
        double maxX = -originX * MODEL_UNIT;
        double maxY = (originY + sizeY) * MODEL_UNIT;
        double maxZ = (originZ + sizeZ) * MODEL_UNIT;
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        quad(consumer, matrix, normal,
                (float) minX, (float) minY, (float) minZ,
                (float) minX, (float) minY, (float) maxZ,
                (float) minX, (float) maxY, (float) maxZ,
                (float) minX, (float) maxY, (float) minZ,
                -1.0F, 0.0F, 0.0F);
        quad(consumer, matrix, normal,
                (float) maxX, (float) minY, (float) maxZ,
                (float) maxX, (float) minY, (float) minZ,
                (float) maxX, (float) maxY, (float) minZ,
                (float) maxX, (float) maxY, (float) maxZ,
                1.0F, 0.0F, 0.0F);
        quad(consumer, matrix, normal,
                (float) maxX, (float) minY, (float) minZ,
                (float) minX, (float) minY, (float) minZ,
                (float) minX, (float) maxY, (float) minZ,
                (float) maxX, (float) maxY, (float) minZ,
                0.0F, 0.0F, -1.0F);
        quad(consumer, matrix, normal,
                (float) minX, (float) minY, (float) maxZ,
                (float) maxX, (float) minY, (float) maxZ,
                (float) maxX, (float) maxY, (float) maxZ,
                (float) minX, (float) maxY, (float) maxZ,
                0.0F, 0.0F, 1.0F);
        quad(consumer, matrix, normal,
                (float) minX, (float) maxY, (float) minZ,
                (float) minX, (float) maxY, (float) maxZ,
                (float) maxX, (float) maxY, (float) maxZ,
                (float) maxX, (float) maxY, (float) minZ,
                0.0F, 1.0F, 0.0F);
        quad(consumer, matrix, normal,
                (float) minX, (float) minY, (float) maxZ,
                (float) minX, (float) minY, (float) minZ,
                (float) maxX, (float) minY, (float) minZ,
                (float) maxX, (float) minY, (float) maxZ,
                0.0F, -1.0F, 0.0F);
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
