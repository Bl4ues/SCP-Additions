package com.bl4ues.scpclassifieddirective.inventory.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.entity.Scp914BlockEntity;
import com.bl4ues.scpclassifieddirective.client.Scp294PhysicalClient;
import com.bl4ues.scpclassifieddirective.client.Scp914InteractionClient;
import com.bl4ues.scpclassifieddirective.facility.Scp714ContainmentStandModule;
import com.bl4ues.scpclassifieddirective.init.Scp714Items;
import com.bl4ues.scpclassifieddirective.facility.elevator.CoreRoomElevatorCarriageEntity;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceGeometry;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformContextTargetClient;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
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
 * elevator control, SCP-914 control, or interactable player corpse is captured.
 *
 * <p>The selected geometry is rendered into an off-screen outline mask while
 * world-space rendering is still valid. A one-pixel post pass extracts only
 * the external silhouette and composites it over the completed scene at
 * AFTER_LEVEL. The visible object itself is never scaled, recolored or drawn
 * full-bright.</p>
 */
public final class PickupOutlineRenderer {
    private static final double MODEL_UNIT = 1.0D / 16.0D;
    private static final double SCP_914_FLOOR_EPSILON = 0.1D / 16.0D;
    private static final float SCP_914_TEXTURE_SIZE = 512.0F;
    private static final ResourceLocation POST_CHAIN = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "shaders/post/pickup_outline.json");
    private static final ResourceLocation BUTTON_MASK_TEXTURE =
            new ResourceLocation("minecraft", "textures/block/white_concrete.png");
    private static final ResourceLocation SCP_914_TEXTURE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "textures/block/scp914.png");
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
            } else if (context != null && context.isTransformed()) {
                renderTransformedMask(minecraft, context.transformed(),
                        poseStack, camera);
            } else if (context != null && context.isBlock()
                    && Scp294PhysicalClient.isContextControl(
                    context.interactionKey())) {
                Scp294PhysicalClient.renderContextOutline(
                        context.blockPos(),
                        minecraft.level.getBlockState(context.blockPos()),
                        context.interactionKey(), poseStack, camera,
                        OUTLINE_BUFFER);
            } else if (context != null && context.isBlock()
                    && isScp714ContainmentStandControl(
                    context.interactionKey())) {
                renderScp714ContainmentStandMask(minecraft, context,
                        poseStack, camera);
            } else if (context != null && context.isScp914Control()) {
                renderScp914ControlMask(minecraft, context, poseStack, camera);
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
     * Render the exact logical payload used by transformed construction into
     * the same outline buffer as ordinary Context Interaction blocks. No fake
     * parent-world BlockPos participates in this path.
     */
    private static void renderTransformedMask(Minecraft minecraft,
            TransformContextTargetClient.Target target, PoseStack poseStack,
            Camera camera) {
        if (minecraft.level == null || target == null) return;
        BlockState state = target.state();
        if (state == null || state.isAir()
                || state.getRenderShape() == RenderShape.INVISIBLE) return;

        Vec3 cameraPosition = camera.getPosition();
        poseStack.pushPose();
        try {
            if (target.kind() == TransformContextTargetClient.Kind.GROUP) {
                TransformGroup group = TransformConstructionClientState.group(
                        target.groupId());
                if (group == null || target.groupCell() == null) return;
                poseStack.translate(group.origin().x - cameraPosition.x,
                        group.origin().y - cameraPosition.y,
                        group.origin().z - cameraPosition.z);
                poseStack.mulPose(TransformMath.quaternion(group.rotationX(),
                        group.rotationY(), group.rotationZ()));
                poseStack.translate(target.groupCell().x() - 0.5D,
                        target.groupCell().y() - 0.5D,
                        target.groupCell().z() - 0.5D);
            } else {
                ConstructionSurface surface =
                        TransformConstructionClientState.surface(
                                target.surfaceId());
                if (surface == null || target.surfaceSlot() == null) return;
                boolean overlay = target.kind()
                        == TransformContextTargetClient.Kind.SURFACE_OVERLAY;
                int side = overlay
                        ? (target.normalSign() < 0 ? -1 : 1)
                        : TransformSurfaceGeometry.MAIN_SIDE;
                double u = (target.surfaceSlot().column() + 0.5D)
                        / surface.columns();
                double v = (target.surfaceSlot().row() + 0.5D)
                        / surface.rows();
                Vec3 normal = surface.gridNormal(u, v).scale(side);
                Vec3 tangent = surface.gridFrameTangent(u, v).scale(side);
                Vec3 vertical = TransformMath.safeNormalize(
                        normal.cross(tangent), surface.gridVertical(u, v));
                Vec3 center = TransformSurfaceGeometry.cellCenter(surface,
                        target.surfaceSlot(), side, overlay);
                poseStack.translate(center.x - cameraPosition.x,
                        center.y - cameraPosition.y,
                        center.z - cameraPosition.z);
                poseStack.mulPose(TransformMath.frameQuaternion(
                        tangent, vertical, normal));
                poseStack.translate(-0.5D, -0.5D, -0.5D);
            }

            if (state.getRenderShape() != RenderShape.ENTITYBLOCK_ANIMATED) {
                minecraft.getBlockRenderer().renderSingleBlock(state,
                        poseStack, OUTLINE_BUFFER, LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY);
            }
        } finally {
            poseStack.popPose();
        }
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

    private static boolean isScp714ContainmentStandControl(
            String interactionKey) {
        return Scp714ContainmentStandModule.PLACE_INTERACTION.equals(
                interactionKey)
                || Scp714ContainmentStandModule.TAKE_INTERACTION.equals(
                interactionKey);
    }

    /**
     * Replays only the physical part selected by the stand interaction.
     * Place outlines the complete ring case, including its hinged lid. Take
     * reuses SCP-714's real baked item model so alpha cutouts and the item's
     * authored silhouette match the ordinary world pickup exactly.
     */
    private static void renderScp714ContainmentStandMask(
            Minecraft minecraft, ContextPromptOutlineTarget.Target context,
            PoseStack poseStack, Camera camera) {
        if (minecraft.level == null || context.blockPos() == null) return;
        BlockPos pos = context.blockPos();
        BlockState state = minecraft.level.getBlockState(pos);
        if (!state.is(Scp714ContainmentStandModule.BLOCK.get())) return;

        Direction facing = state.hasProperty(
                Scp714ContainmentStandModule.FACING)
                ? state.getValue(Scp714ContainmentStandModule.FACING)
                : Direction.NORTH;
        Vec3 cameraPosition = camera.getPosition();

        poseStack.pushPose();
        try {
            poseStack.translate(pos.getX() - cameraPosition.x,
                    pos.getY() - cameraPosition.y,
                    pos.getZ() - cameraPosition.z);
            poseStack.translate(0.5D, 0.0D, 0.5D);
            rotateForFacing(poseStack, facing);

            // Parent "bone": pivot [0, 13.5, 0], rotation [12.5, 0, 0].
            applyAuthoredBoneTransform(poseStack,
                    0.0D, 13.5D, 0.0D,
                    12.5D, 0.0D, 0.0D);

            VertexConsumer consumer = OUTLINE_BUFFER.getBuffer(
                    RenderType.entityCutoutNoCull(BUTTON_MASK_TEXTURE));

            if (Scp714ContainmentStandModule.PLACE_INTERACTION.equals(
                    context.interactionKey())) {
                // ring_box itself.
                emitAuthoredCube(consumer, poseStack.last(),
                        -1.0D, 15.5D, -1.0D,
                        2.0D, 1.0D, 2.0D);
                emitAuthoredCube(consumer, poseStack.last(),
                        -0.95D, 15.55D, -0.95D,
                        1.9D, 0.95D, 1.9D);
                emitAuthoredCube(consumer, poseStack.last(),
                        -0.25D, 16.4D, 0.9D,
                        0.5D, 0.2D, 0.2D);
                emitAuthoredCube(consumer, poseStack.last(),
                        -0.35D, 15.5D, -1.0D,
                        0.7D, 1.0D, 2.0D);

                // lid is a child of ring_box. The bone contributes -5 degrees
                // and each of its authored cubes contributes another -112.5
                // degrees around the same hinge pivot.
                poseStack.pushPose();
                try {
                    applyAuthoredBoneTransform(poseStack,
                            0.0D, 16.5D, 1.0D,
                            -5.0D, 0.0D, 0.0D);
                    emitTransformedAuthoredCube(consumer, poseStack,
                            -0.95D, 16.5D, -0.95D,
                            1.9D, 1.35D, 1.9D,
                            0.0D, 16.5D, 1.0D,
                            -112.5D, 0.0D, 0.0D);
                    emitTransformedAuthoredCube(consumer, poseStack,
                            -1.0D, 16.5D, -1.0D,
                            2.0D, 1.4D, 2.0D,
                            0.0D, 16.5D, 1.0D,
                            -112.5D, 0.0D, 0.0D);
                    emitTransformedAuthoredCube(consumer, poseStack,
                            -0.25D, 16.25D, -1.05D,
                            0.5D, 0.5D, 0.15D,
                            0.0D, 16.5D, 1.0D,
                            -112.5D, 0.0D, 0.0D);
                } finally {
                    poseStack.popPose();
                }
                return;
            }

            if (!Scp714ContainmentStandModule.TAKE_INTERACTION.equals(
                    context.interactionKey())) {
                return;
            }

            poseStack.pushPose();
            try {
                // The stand's 714 bone is the ordinary SCP-714 model translated
                // into Gecko coordinates, then rotated [0, 90, 90]. Render the
                // actual item model through the same ItemRenderer path used by
                // pickup entities instead of approximating it with white boxes.
                applyAuthoredBoneTransform(poseStack,
                        0.0D, 16.4975D, -0.025D,
                        0.0D, 90.0D, 90.0D);

                // ItemDisplayContext.NONE centers baked item coordinates by
                // subtracting 0.5 on every axis. These offsets reconstruct the
                // authored stand coordinates, including GeckoLib's mirrored X.
                poseStack.translate(0.0D, 1.515625D, 0.009375D);
                poseStack.scale(-1.0F, 1.0F, 1.0F);
                minecraft.getItemRenderer().renderStatic(
                        new ItemStack(Scp714Items.SCP_714.get()),
                        ItemDisplayContext.NONE, LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY, poseStack, OUTLINE_BUFFER,
                        minecraft.level, 0);
            } finally {
                poseStack.popPose();
            }
        } finally {
            poseStack.popPose();
        }
    }

    /**
     * SCP-914 shares one huge GeckoLib BlockEntity, but its normal prompts are
     * attached to two tiny physical controls. Replaying only those authored
     * controls prevents the complete machine from flashing when the player aims
     * at the dial or winding key.
     */
    private static void renderScp914ControlMask(Minecraft minecraft,
            ContextPromptOutlineTarget.Target context, PoseStack poseStack,
            Camera camera) {
        if (minecraft.level == null || context.blockPos() == null) return;
        BlockPos pos = context.blockPos();
        BlockState state = minecraft.level.getBlockState(pos);
        Direction facing = state.hasProperty(
                BlockStateProperties.HORIZONTAL_FACING)
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
        Vec3 cameraPosition = camera.getPosition();

        poseStack.pushPose();
        try {
            poseStack.translate(pos.getX() - cameraPosition.x,
                    pos.getY() - cameraPosition.y + SCP_914_FLOOR_EPSILON,
                    pos.getZ() - cameraPosition.z);
            poseStack.translate(0.5D, 0.0D, 0.5D);
            rotateForFacing(poseStack, facing);

            if ("scp_914_dial".equals(context.interactionKey())) {
                renderScp914DialMask(minecraft, pos, poseStack);
            } else if ("scp_914_start".equals(context.interactionKey())) {
                renderScp914WindingKeyMask(poseStack);
            }
        } finally {
            poseStack.popPose();
        }
    }

    /**
     * Replays every cube in the animated grab_dial bone from scp914.geo.json.
     * triangle_dial and dial_body are deliberately excluded: the triangle moves
     * in the opposite direction and dial_body is the stationary backing plate.
     */
    private static void renderScp914DialMask(Minecraft minecraft, BlockPos pos,
            PoseStack poseStack) {
        VertexConsumer consumer = OUTLINE_BUFFER.getBuffer(
                RenderType.entityCutoutNoCull(BUTTON_MASK_TEXTURE));
        BlockEntity blockEntity = minecraft.level.getBlockEntity(pos);
        float dialAngle = blockEntity instanceof Scp914BlockEntity machine
                ? Scp914InteractionClient.renderDialAngle(machine) : 0.0F;

        poseStack.pushPose();
        try {
            applyAuthoredBoneTransform(poseStack,
                    0.0D, 20.04D, -8.25D,
                    0.0D, 0.0D, dialAngle);

            emitAuthoredCube(consumer, poseStack.last(),
                    -1.07D, 18.97D, -8.785D,
                    2.14D, 2.14D, 0.535D);
            emitAuthoredCube(consumer, poseStack.last(),
                    -0.0535D, 19.612D, -9.534D,
                    0.107D, 0.856D, 0.107D);
            emitAuthoredCube(consumer, poseStack.last(),
                    0.428D, 19.505D, -9.534D,
                    0.107D, 1.07D, 0.107D);
            emitAuthoredCube(consumer, poseStack.last(),
                    -0.535D, 19.505D, -9.534D,
                    0.107D, 1.07D, 0.107D);
            emitAuthoredCube(consumer, poseStack.last(),
                    -0.428D, 20.468D, -9.534D,
                    0.856D, 0.107D, 0.107D);
            emitAuthoredCube(consumer, poseStack.last(),
                    -0.428D, 19.505D, -9.534D,
                    0.856D, 0.107D, 0.107D);
            emitAuthoredCube(consumer, poseStack.last(),
                    -0.535D, 19.505D, -9.427D,
                    1.07D, 1.07D, 0.0D);

            emitTransformedAuthoredCube(consumer, poseStack,
                    -0.8025D, 20.8425D, -9.5875D,
                    1.605D, 0.0D, 0.8025D,
                    0.0D, 20.8425D, -8.75825D,
                    22.5D, 0.0D, 0.0D);
            emitTransformedAuthoredCube(consumer, poseStack,
                    -0.8025D, 19.2375D, -9.5875D,
                    1.605D, 0.0D, 0.8025D,
                    0.0D, 19.2375D, -8.75825D,
                    -22.5D, 0.0D, 0.0D);
            emitTransformedAuthoredCube(consumer, poseStack,
                    0.0D, 20.04D, -9.5875D,
                    1.605D, 0.0D, 0.8025D,
                    0.8025D, 20.04D, -8.75825D,
                    -22.5D, 0.0D, -90.0D);
            emitTransformedAuthoredCube(consumer, poseStack,
                    -1.605D, 20.04D, -9.5875D,
                    1.605D, 0.0D, 0.8025D,
                    -0.8025D, 20.04D, -8.75825D,
                    -22.5D, 0.0D, 90.0D);

            // These are the two authored faces that form the arrow above the
            // draggable dial. They belong to grab_dial and rotate with it.
            emitTransformedAuthoredCube(consumer, poseStack,
                    -0.73064D, 21.41264D, -8.892D,
                    0.856D, 0.0D, 0.642D,
                    -0.30264D, 21.41264D, -8.25D,
                    45.0D, 0.0D, -45.0D);
            emitTransformedAuthoredCube(consumer, poseStack,
                    -0.12536D, 21.41264D, -8.892D,
                    0.856D, 0.0D, 0.642D,
                    0.30264D, 21.41264D, -8.25D,
                    45.0D, 0.0D, 45.0D);
        } finally {
            poseStack.popPose();
        }
    }

    /**
     * wind_key is a textured zero-thickness plane. Its visible key silhouette is
     * defined by alpha in scp914.png, not by the rectangular plane bounds, so the
     * outline must sample the same authored face instead of substituting a box.
     */
    private static void renderScp914WindingKeyMask(PoseStack poseStack) {
        VertexConsumer consumer = OUTLINE_BUFFER.getBuffer(
                RenderType.entityCutoutNoCull(SCP_914_TEXTURE));
        emitAuthoredTexturedXPlane(consumer, poseStack.last(),
                0.0D, 13.75D, -9.95D,
                1.5D, 1.75D,
                266.5F, 20.0F, 8.0F, 5.0F);
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

    private static void emitTransformedAuthoredCube(VertexConsumer consumer,
            PoseStack poseStack, double originX, double originY,
            double originZ, double sizeX, double sizeY, double sizeZ,
            double pivotX, double pivotY, double pivotZ,
            double rotationX, double rotationY, double rotationZ) {
        poseStack.pushPose();
        try {
            applyAuthoredBoneTransform(poseStack, pivotX, pivotY, pivotZ,
                    rotationX, rotationY, rotationZ);
            emitAuthoredCube(consumer, poseStack.last(), originX, originY,
                    originZ, sizeX, sizeY, sizeZ);
        } finally {
            poseStack.popPose();
        }
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

    /**
     * Emits the authored east face of a zero-thickness X cube while retaining
     * the atlas alpha. No-cull rendering makes the same physical silhouette
     * visible from either side without replacing it with artificial thickness.
     */
    private static void emitAuthoredTexturedXPlane(VertexConsumer consumer,
            PoseStack.Pose pose, double x, double originY, double originZ,
            double sizeY, double sizeZ, float uvX, float uvY,
            float uvWidth, float uvHeight) {
        float px = (float) (-x * MODEL_UNIT);
        float minY = (float) (originY * MODEL_UNIT);
        float maxY = (float) ((originY + sizeY) * MODEL_UNIT);
        float minZ = (float) (originZ * MODEL_UNIT);
        float maxZ = (float) ((originZ + sizeZ) * MODEL_UNIT);
        float u0 = uvX / SCP_914_TEXTURE_SIZE;
        float v0 = uvY / SCP_914_TEXTURE_SIZE;
        float u1 = (uvX + uvWidth) / SCP_914_TEXTURE_SIZE;
        float v1 = (uvY + uvHeight) / SCP_914_TEXTURE_SIZE;
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        // Bedrock's east face maps U across Z and V from top to bottom.
        vertex(consumer, matrix, normal, px, minY, maxZ,
                u0, v1, 1.0F, 0.0F, 0.0F);
        vertex(consumer, matrix, normal, px, minY, minZ,
                u1, v1, 1.0F, 0.0F, 0.0F);
        vertex(consumer, matrix, normal, px, maxY, minZ,
                u1, v0, 1.0F, 0.0F, 0.0F);
        vertex(consumer, matrix, normal, px, maxY, maxZ,
                u0, v0, 1.0F, 0.0F, 0.0F);
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
