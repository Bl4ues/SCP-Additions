package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.alarm.AlarmModule;
import com.bl4ues.scpclassifieddirective.facility.alarm.AlarmMountStructure;
import com.bl4ues.scpclassifieddirective.facility.blastdoor.BlastDoorModule;
import com.bl4ues.scpclassifieddirective.facility.blastdoor.BlastDoorStructure;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * GeckoLib body plus a client-only projected alarm splash. The projection is
 * clipped against real world collision surfaces; it never exists as a floating
 * sprite and never mutates Minecraft's light engine while the rotor turns.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AlarmClient {
    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;
    private static final ResourceLocation BASE_GEO = id(
            "geo/block/alarm_base.geo.json");
    private static final ResourceLocation LAMP_GEO = id(
            "geo/block/alarm_lamp.geo.json");
    private static final ResourceLocation COVER_GEO = id(
            "geo/block/alarm_cover.geo.json");
    private static final ResourceLocation ITEM_GEO = id(
            "geo/item/alarm.geo.json");
    private static final ResourceLocation TEXTURE = id(
            "textures/block/alarm.png");
    private static final ResourceLocation GLOWMASK = id(
            "textures/block/alarm_glowmask.png");
    private static final ResourceLocation LAMP_EMISSIVE = id(
            "textures/effect/alarm_lamp_emissive.png");
    private static final ResourceLocation LAMP_OUTLINE = id(
            "textures/effect/alarm_lamp_outline.png");
    private static final ResourceLocation LAMP_OUTLINE_EMISSIVE = id(
            "textures/effect/alarm_lamp_outline_emissive.png");
    private static final ResourceLocation GLASS_BLOOM = id(
            "textures/effect/alarm_glass_bloom.png");
    private static final ResourceLocation SPLASH = id(
            "textures/effect/alarm_light_splash.png");
    private static final ResourceLocation SPLASH_EMISSIVE = id(
            "textures/effect/alarm_light_emissive.png");
    private static final ResourceLocation ANIMATION = id(
            "animations/block/alarm.animation.json");

    private static final double PROJECTOR_DISTANCE = 24.0D;
    private static final double PROJECTOR_DISTANCE_SQR =
            PROJECTOR_DISTANCE * PROJECTOR_DISTANCE;
    private static final double MIN_SPLASH_RADIUS = 0.06D;
    private static final double MAX_SPLASH_RADIUS = 3.58D;
    private static final double MAX_SPLASH_HALF_WIDTH = 1.72D;
    private static final double PROJECTOR_OUTSET = 0.34D;
    private static final double WALL_PLANE_INSET = 0.0625D;
    private static final double RAY_OVERSHOOT = 0.06D;
    private static final double SURFACE_EPSILON = 0.0030D;
    private static final double PLANE_EPSILON = 0.018D;
    private static final double BLOOM_SURFACE_EPSILON = 0.0016D;
    /*
     * Receiver-first projection. World faces are exact, then only shadow/beam
     * boundaries on those faces subdivide. This avoids the fundamental failure
     * mode of the old UV-first mesh: triangles can no longer bridge unrelated
     * surfaces or collapse a large UV region onto a tiny perpendicular face.
     */
    private static final int SURFACE_PATCH_SUBDIVISIONS = 3;
    private static final int SURFACE_EDGE_BISECTIONS = 6;
    private static final double PROJECTION_BOUNDS_PADDING = 0.12D;
    private static final double RECEIVER_HIT_EPSILON_SQR = 0.0016D;
    private static final float UV_EPSILON = 0.0010F;
    // Projection geometry is rebuilt at Minecraft's 20 Hz world tick rate.
    // Recasting the complete surface mesh every render frame was the source of
    // the severe FPS regression, especially with shaders enabled.

    private static final Map<ClientLevel, Map<BlockPos, ProjectionCache>>
            PROJECTIONS = new WeakHashMap<>();

    private AlarmClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AlarmModule.BLOCK_ENTITY.get(),
                BlockRenderer::new);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path);
    }

    private static void forceLamp(CoreGeoBone bone, boolean visible) {
        if (bone == null) return;
        bone.setHidden(!visible);
        float scale = visible ? 1.0F : 0.0F;
        bone.setScaleX(scale);
        bone.setScaleY(scale);
        bone.setScaleZ(scale);
    }

    private static float rotorAngle(
            AlarmModule.AlarmBlockEntity animatable, float partialTick) {
        return (float) (-animatable.projectionPhase(partialTick)
                * Math.PI * 2.0D);
    }

    private static final class BaseModel
            extends GeoModel<AlarmModule.AlarmBlockEntity> {
        @Override
        public ResourceLocation getModelResource(
                AlarmModule.AlarmBlockEntity animatable) {
            return BASE_GEO;
        }

        @Override
        public ResourceLocation getTextureResource(
                AlarmModule.AlarmBlockEntity animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(
                AlarmModule.AlarmBlockEntity animatable) {
            return ANIMATION;
        }

        @Override
        public void setCustomAnimations(AlarmModule.AlarmBlockEntity animatable,
                long instanceId,
                AnimationState<AlarmModule.AlarmBlockEntity> state) {
            super.setCustomAnimations(animatable, instanceId, state);
            boolean active = animatable.getBlockState()
                    .getValue(AlarmModule.ACTIVE);
            forceLamp(getAnimationProcessor().getBone("unlit"), !active);
            CoreGeoBone rotor = getAnimationProcessor().getBone("rotor");
            if (rotor != null) {
                rotor.setRotZ(rotorAngle(animatable, state.getPartialTick()));
            }
        }
    }

    private static class LampModel
            extends GeoModel<AlarmModule.AlarmBlockEntity> {
        private final ResourceLocation texture;

        private LampModel(ResourceLocation texture) {
            this.texture = texture;
        }

        @Override
        public ResourceLocation getModelResource(
                AlarmModule.AlarmBlockEntity animatable) {
            return LAMP_GEO;
        }

        @Override
        public ResourceLocation getTextureResource(
                AlarmModule.AlarmBlockEntity animatable) {
            return texture;
        }

        @Override
        public ResourceLocation getAnimationResource(
                AlarmModule.AlarmBlockEntity animatable) {
            return ANIMATION;
        }

        @Override
        public void setCustomAnimations(AlarmModule.AlarmBlockEntity animatable,
                long instanceId,
                AnimationState<AlarmModule.AlarmBlockEntity> state) {
            super.setCustomAnimations(animatable, instanceId, state);
            forceLamp(getAnimationProcessor().getBone("lit"), true);
            CoreGeoBone rotor = getAnimationProcessor().getBone("rotor");
            if (rotor != null) {
                rotor.setRotZ(rotorAngle(animatable, state.getPartialTick()));
            }
        }
    }

    private static final class CoverModel
            extends GeoModel<AlarmModule.AlarmBlockEntity> {
        @Override
        public ResourceLocation getModelResource(
                AlarmModule.AlarmBlockEntity animatable) {
            return COVER_GEO;
        }

        @Override
        public ResourceLocation getTextureResource(
                AlarmModule.AlarmBlockEntity animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(
                AlarmModule.AlarmBlockEntity animatable) {
            return ANIMATION;
        }
    }

    private static final class BaseRenderer
            extends GeoBlockRenderer<AlarmModule.AlarmBlockEntity> {
        private BaseRenderer() {
            super(new BaseModel());
        }

        @Override
        public RenderType getRenderType(AlarmModule.AlarmBlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }
    }

    private static final class LampRenderer
            extends GeoBlockRenderer<AlarmModule.AlarmBlockEntity> {
        private final boolean glowMask;

        private LampRenderer(boolean glowMask) {
            super(new LampModel(glowMask ? GLOWMASK : TEXTURE));
            this.glowMask = glowMask;
        }

        @Override
        public RenderType getRenderType(AlarmModule.AlarmBlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return glowMask
                    ? RenderType.eyes(texture)
                    : RenderType.entityTranslucentEmissive(texture);
        }
    }

    private static final class CoverRenderer
            extends GeoBlockRenderer<AlarmModule.AlarmBlockEntity> {
        private CoverRenderer() {
            super(new CoverModel());
        }

        @Override
        public RenderType getRenderType(AlarmModule.AlarmBlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityTranslucent(texture, true);
        }
    }

    /**
     * Explicit render ordering is important here:
     * opaque mechanism -> real lit cube -> glowmask -> translucent orange cover
     * -> projected surface light. No visibility state is shared between passes.
     */
    public static final class BlockRenderer
            implements BlockEntityRenderer<AlarmModule.AlarmBlockEntity> {
        private final BaseRenderer base = new BaseRenderer();
        private final CoverRenderer cover = new CoverRenderer();

        public BlockRenderer(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(AlarmModule.AlarmBlockEntity alarm,
                float partialTick, PoseStack poseStack,
                MultiBufferSource bufferSource, int packedLight,
                int packedOverlay) {
            Vec3 mountOffset = alarm.getLevel() == null ? Vec3.ZERO
                    : AlarmModule.visualOffset(alarm.getLevel(),
                            alarm.getBlockPos(), alarm.getBlockState());
            boolean active = alarm.getBlockState()
                    .getValue(AlarmModule.ACTIVE);
            float angle = rotorAngle(alarm, partialTick);

            poseStack.pushPose();
            poseStack.translate(mountOffset.x, mountOffset.y, mountOffset.z);
            base.render(alarm, partialTick, poseStack, bufferSource,
                    packedLight, packedOverlay);
            flush(bufferSource, RenderType.entityCutoutNoCull(TEXTURE));
            poseStack.popPose();

            if (active) {
                // Render the authored lit cube manually from its exact model
                // coordinates. This pass has no GeckoLib model state to leak
                // between block entities, so a single Alarm behaves identically
                // to two Alarms facing opposite sides of the same wall.
                renderLitLamp(alarm, poseStack, bufferSource,
                        angle, mountOffset);
            }

            poseStack.pushPose();
            poseStack.translate(mountOffset.x, mountOffset.y, mountOffset.z);
            cover.render(alarm, partialTick, poseStack, bufferSource,
                    packedLight, packedOverlay);
            flush(bufferSource, RenderType.entityTranslucent(TEXTURE, true));
            poseStack.popPose();

            if (active) {
                /*
                 * The physical orange cover is translucent, but shader packs
                 * composite that glass over the inner HDR lamp and can suppress
                 * most of its bloom. Re-emit only a soft transmission hotspot on
                 * the OUTER glass surface. The source lamp remains the real
                 * emissive object; this pass represents light that made it
                 * through the cover instead of drawing the lamp through walls.
                 */
                renderGlassTransmission(alarm, poseStack, bufferSource,
                        mountOffset);
                renderProjection(alarm, poseStack, bufferSource,
                        angle, mountOffset);
            }
        }

        @Override
        public boolean shouldRenderOffScreen(
                AlarmModule.AlarmBlockEntity blockEntity) {
            return true;
        }
    }

    private static void renderLitLamp(
            AlarmModule.AlarmBlockEntity alarm, PoseStack poseStack,
            MultiBufferSource buffers, float rotorAngle,
            Vec3 mountOffset) {
        /*
         * The orange shell is slightly larger than the pale lit cube, but only
         * the current camera-facing SILHOUETTE is submitted. Shared fold edges
         * between two visible faces are intentionally omitted; the orange layer
         * therefore reads as one outline around the whole lamp, not wireframe
         * around every face. Both layers share the exact rotor transform.
         */
        RenderType outlineType = RenderType.entityCutoutNoCull(LAMP_OUTLINE);
        VertexConsumer outline = buffers.getBuffer(outlineType);
        emitLampOutline(alarm, poseStack, outline, rotorAngle,
                mountOffset);
        flush(buffers, outlineType);

        RenderType outlineGlowType = RenderType.eyes(
                LAMP_OUTLINE_EMISSIVE);
        VertexConsumer outlineGlow = buffers.getBuffer(outlineGlowType);
        emitLampOutline(alarm, poseStack, outlineGlow, rotorAngle,
                mountOffset);
        flush(buffers, outlineGlowType);

        RenderType lampType = RenderType.entityCutoutNoCull(TEXTURE);
        VertexConsumer lamp = buffers.getBuffer(lampType);
        emitLitLampCube(alarm, poseStack, lamp, rotorAngle,
                mountOffset);
        flush(buffers, lampType);

        // Preserve the already-correct inner-lamp HDR path. The new orange
        // outline uses the same full-bright + eyes strategy, with its own color
        // and matching alpha strength.
        RenderType lampGlowType = RenderType.eyes(LAMP_EMISSIVE);
        VertexConsumer lampGlow = buffers.getBuffer(lampGlowType);
        emitLitLampCube(alarm, poseStack, lampGlow, rotorAngle,
                mountOffset);
        flush(buffers, lampGlowType);

        RenderType glowType = RenderType.eyes(GLOWMASK);
        VertexConsumer glow = buffers.getBuffer(glowType);
        emitLitLampCube(alarm, poseStack, glow, rotorAngle,
                mountOffset);
        flush(buffers, glowType);
    }

    private static void renderGlassTransmission(
            AlarmModule.AlarmBlockEntity alarm, PoseStack poseStack,
            MultiBufferSource buffers, Vec3 mountOffset) {
        Direction facing = alarm.getBlockState().getValue(AlarmModule.FACING);
        BlockPos origin = alarm.getBlockPos();

        /*
         * Front face of the authored cover is model Z=4.75. Put the bloom a
         * fraction outward (lower model Z) so it survives the translucent cover
         * depth pass without z-fighting. Its footprint is intentionally smaller
         * than the 2.9x2.9 glass face.
         */
        final double z = 4.72D;
        Vec3 p0 = modelPointToWorld(origin, facing,
                -1.15D, 6.85D, z, mountOffset);
        Vec3 p1 = modelPointToWorld(origin, facing,
                1.15D, 6.85D, z, mountOffset);
        Vec3 p2 = modelPointToWorld(origin, facing,
                1.15D, 9.15D, z, mountOffset);
        Vec3 p3 = modelPointToWorld(origin, facing,
                -1.15D, 9.15D, z, mountOffset);
        Vec3 normal = direction(facing);

        RenderType type = RenderType.eyes(GLASS_BLOOM);
        VertexConsumer consumer = buffers.getBuffer(type);
        glassBloomVertex(consumer, poseStack, local(p0, origin),
                normal, 0.0F, 1.0F);
        glassBloomVertex(consumer, poseStack, local(p1, origin),
                normal, 1.0F, 1.0F);
        glassBloomVertex(consumer, poseStack, local(p2, origin),
                normal, 1.0F, 0.0F);
        glassBloomVertex(consumer, poseStack, local(p3, origin),
                normal, 0.0F, 0.0F);
        flush(buffers, type);
    }

    private static void glassBloomVertex(VertexConsumer consumer,
            PoseStack poseStack, Vec3 point, Vec3 normal, float u, float v) {
        consumer.vertex(poseStack.last().pose(),
                        (float) point.x, (float) point.y, (float) point.z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(poseStack.last().normal(),
                        (float) normal.x, (float) normal.y,
                        (float) normal.z)
                .endVertex();
    }

    private static void emitLampOutline(
            AlarmModule.AlarmBlockEntity alarm, PoseStack poseStack,
            VertexConsumer consumer, float angle, Vec3 mountOffset) {
        Direction facing = alarm.getBlockState().getValue(AlarmModule.FACING);
        BlockPos origin = alarm.getBlockPos();
        Vec3 cameraPosition = Minecraft.getInstance().gameRenderer
                .getMainCamera().getPosition();

        // Original lit cube:
        // X [-0.70, 0.70], Y [7.30, 8.70], Z [6.25, 7.75].
        // Keep the requested larger cube, but make the outline only 0.10 model
        // pixels thick so it frames rather than cages the pale lamp.
        final double ox0 = -0.80D, ox1 = 0.80D;
        final double oy0 = 7.20D, oy1 = 8.80D;
        final double oz0 = 6.15D, oz1 = 7.85D;
        final double ix0 = -0.70D, ix1 = 0.70D;
        final double iy0 = 7.30D, iy1 = 8.70D;
        final double iz0 = 6.25D, iz1 = 7.75D;

        // A convex cuboid silhouette consists only of edges whose two adjacent
        // faces have opposite visibility relative to the camera. This is the
        // crucial difference from the previous wireframe-like twelve-edge pass.

        // X-axis edges: adjacent +/-Y and +/-Z faces.
        silhouetteLampEdge(consumer, poseStack, alarm, facing, origin,
                cameraPosition, angle, mountOffset,
                ox0, oy0, oz0, ox1, iy0, iz0,
                new Vec3(0.0D, -1.0D, 0.0D),
                new Vec3(0.0D, 0.0D, -1.0D));
        silhouetteLampEdge(consumer, poseStack, alarm, facing, origin,
                cameraPosition, angle, mountOffset,
                ox0, iy1, oz0, ox1, oy1, iz0,
                new Vec3(0.0D, 1.0D, 0.0D),
                new Vec3(0.0D, 0.0D, -1.0D));
        silhouetteLampEdge(consumer, poseStack, alarm, facing, origin,
                cameraPosition, angle, mountOffset,
                ox0, oy0, iz1, ox1, iy0, oz1,
                new Vec3(0.0D, -1.0D, 0.0D),
                new Vec3(0.0D, 0.0D, 1.0D));
        silhouetteLampEdge(consumer, poseStack, alarm, facing, origin,
                cameraPosition, angle, mountOffset,
                ox0, iy1, iz1, ox1, oy1, oz1,
                new Vec3(0.0D, 1.0D, 0.0D),
                new Vec3(0.0D, 0.0D, 1.0D));

        // Y-axis edges: adjacent +/-X and +/-Z faces.
        silhouetteLampEdge(consumer, poseStack, alarm, facing, origin,
                cameraPosition, angle, mountOffset,
                ox0, iy0, oz0, ix0, iy1, iz0,
                new Vec3(-1.0D, 0.0D, 0.0D),
                new Vec3(0.0D, 0.0D, -1.0D));
        silhouetteLampEdge(consumer, poseStack, alarm, facing, origin,
                cameraPosition, angle, mountOffset,
                ix1, iy0, oz0, ox1, iy1, iz0,
                new Vec3(1.0D, 0.0D, 0.0D),
                new Vec3(0.0D, 0.0D, -1.0D));
        silhouetteLampEdge(consumer, poseStack, alarm, facing, origin,
                cameraPosition, angle, mountOffset,
                ox0, iy0, iz1, ix0, iy1, oz1,
                new Vec3(-1.0D, 0.0D, 0.0D),
                new Vec3(0.0D, 0.0D, 1.0D));
        silhouetteLampEdge(consumer, poseStack, alarm, facing, origin,
                cameraPosition, angle, mountOffset,
                ix1, iy0, iz1, ox1, iy1, oz1,
                new Vec3(1.0D, 0.0D, 0.0D),
                new Vec3(0.0D, 0.0D, 1.0D));

        // Z-axis edges: adjacent +/-X and +/-Y faces.
        silhouetteLampEdge(consumer, poseStack, alarm, facing, origin,
                cameraPosition, angle, mountOffset,
                ox0, oy0, iz0, ix0, iy0, iz1,
                new Vec3(-1.0D, 0.0D, 0.0D),
                new Vec3(0.0D, -1.0D, 0.0D));
        silhouetteLampEdge(consumer, poseStack, alarm, facing, origin,
                cameraPosition, angle, mountOffset,
                ix1, oy0, iz0, ox1, iy0, iz1,
                new Vec3(1.0D, 0.0D, 0.0D),
                new Vec3(0.0D, -1.0D, 0.0D));
        silhouetteLampEdge(consumer, poseStack, alarm, facing, origin,
                cameraPosition, angle, mountOffset,
                ox0, iy1, iz0, ix0, oy1, iz1,
                new Vec3(-1.0D, 0.0D, 0.0D),
                new Vec3(0.0D, 1.0D, 0.0D));
        silhouetteLampEdge(consumer, poseStack, alarm, facing, origin,
                cameraPosition, angle, mountOffset,
                ix1, iy1, iz0, ox1, oy1, iz1,
                new Vec3(1.0D, 0.0D, 0.0D),
                new Vec3(0.0D, 1.0D, 0.0D));
    }

    private static void silhouetteLampEdge(VertexConsumer consumer,
            PoseStack poseStack, AlarmModule.AlarmBlockEntity alarm,
            Direction facing, BlockPos origin, Vec3 cameraPosition,
            float angle, Vec3 mountOffset,
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            Vec3 adjacentNormalA, Vec3 adjacentNormalB) {
        Vec3 midpoint = new Vec3((x0 + x1) * 0.5D,
                (y0 + y1) * 0.5D, (z0 + z1) * 0.5D);
        Vec3 rotatedMidpoint = rotateModelPoint(midpoint, angle);
        Vec3 worldMidpoint = modelPointToWorld(origin, facing,
                rotatedMidpoint.x, rotatedMidpoint.y, rotatedMidpoint.z,
                mountOffset);
        Vec3 toCamera = cameraPosition.subtract(worldMidpoint);

        boolean faceAVisible = rotateModelVector(adjacentNormalA,
                angle, facing).dot(toCamera) >= 0.0D;
        boolean faceBVisible = rotateModelVector(adjacentNormalB,
                angle, facing).dot(toCamera) >= 0.0D;

        if (faceAVisible == faceBVisible) return;

        lampBox(consumer, poseStack, alarm, angle, mountOffset,
                x0, y0, z0, x1, y1, z1);
    }

    private static void lampBox(VertexConsumer consumer, PoseStack poseStack,
            AlarmModule.AlarmBlockEntity alarm, float angle,
            Vec3 mountOffset, double x0, double y0, double z0,
            double x1, double y1, double z1) {
        Direction facing = alarm.getBlockState().getValue(AlarmModule.FACING);
        BlockPos origin = alarm.getBlockPos();

        lampFace(consumer, poseStack, origin, facing, angle, mountOffset,
                new Vec3(x0, y0, z0), new Vec3(x1, y0, z0),
                new Vec3(x1, y1, z0), new Vec3(x0, y1, z0),
                new Vec3(0.0D, 0.0D, -1.0D),
                0.0F, 0.0F, 32.0F, 32.0F);
        lampFace(consumer, poseStack, origin, facing, angle, mountOffset,
                new Vec3(x1, y0, z1), new Vec3(x0, y0, z1),
                new Vec3(x0, y1, z1), new Vec3(x1, y1, z1),
                new Vec3(0.0D, 0.0D, 1.0D),
                0.0F, 0.0F, 32.0F, 32.0F);
        lampFace(consumer, poseStack, origin, facing, angle, mountOffset,
                new Vec3(x1, y0, z0), new Vec3(x1, y0, z1),
                new Vec3(x1, y1, z1), new Vec3(x1, y1, z0),
                new Vec3(1.0D, 0.0D, 0.0D),
                0.0F, 0.0F, 32.0F, 32.0F);
        lampFace(consumer, poseStack, origin, facing, angle, mountOffset,
                new Vec3(x0, y0, z1), new Vec3(x0, y0, z0),
                new Vec3(x0, y1, z0), new Vec3(x0, y1, z1),
                new Vec3(-1.0D, 0.0D, 0.0D),
                0.0F, 0.0F, 32.0F, 32.0F);
        lampFace(consumer, poseStack, origin, facing, angle, mountOffset,
                new Vec3(x0, y1, z0), new Vec3(x1, y1, z0),
                new Vec3(x1, y1, z1), new Vec3(x0, y1, z1),
                new Vec3(0.0D, 1.0D, 0.0D),
                0.0F, 0.0F, 32.0F, 32.0F);
        lampFace(consumer, poseStack, origin, facing, angle, mountOffset,
                new Vec3(x0, y0, z1), new Vec3(x1, y0, z1),
                new Vec3(x1, y0, z0), new Vec3(x0, y0, z0),
                new Vec3(0.0D, -1.0D, 0.0D),
                0.0F, 0.0F, 32.0F, 32.0F);
    }

    private static void emitLitLampCube(
            AlarmModule.AlarmBlockEntity alarm, PoseStack poseStack,
            VertexConsumer consumer, float angle, Vec3 mountOffset) {
        Direction facing = alarm.getBlockState().getValue(AlarmModule.FACING);
        BlockPos origin = alarm.getBlockPos();

        final double x0 = -0.7D, x1 = 0.7D;
        final double y0 = 7.3D, y1 = 8.7D;
        final double z0 = 6.25D, z1 = 7.75D;

        lampFace(consumer, poseStack, origin, facing, angle, mountOffset,
                new Vec3(x0, y0, z0), new Vec3(x1, y0, z0),
                new Vec3(x1, y1, z0), new Vec3(x0, y1, z0),
                new Vec3(0.0D, 0.0D, -1.0D),
                0.0F, 10.0F, 1.5F, 11.5F);
        lampFace(consumer, poseStack, origin, facing, angle, mountOffset,
                new Vec3(x1, y0, z1), new Vec3(x0, y0, z1),
                new Vec3(x0, y1, z1), new Vec3(x1, y1, z1),
                new Vec3(0.0D, 0.0D, 1.0D),
                6.0F, 12.0F, 7.5F, 13.5F);
        lampFace(consumer, poseStack, origin, facing, angle, mountOffset,
                new Vec3(x1, y0, z0), new Vec3(x1, y0, z1),
                new Vec3(x1, y1, z1), new Vec3(x1, y1, z0),
                new Vec3(1.0D, 0.0D, 0.0D),
                3.0F, 12.0F, 4.5F, 13.5F);
        lampFace(consumer, poseStack, origin, facing, angle, mountOffset,
                new Vec3(x0, y0, z1), new Vec3(x0, y0, z0),
                new Vec3(x0, y1, z0), new Vec3(x0, y1, z1),
                new Vec3(-1.0D, 0.0D, 0.0D),
                9.0F, 12.0F, 10.5F, 13.5F);
        lampFace(consumer, poseStack, origin, facing, angle, mountOffset,
                new Vec3(x0, y1, z0), new Vec3(x1, y1, z0),
                new Vec3(x1, y1, z1), new Vec3(x0, y1, z1),
                new Vec3(0.0D, 1.0D, 0.0D),
                12.0F, 12.0F, 13.5F, 13.5F);
        lampFace(consumer, poseStack, origin, facing, angle, mountOffset,
                new Vec3(x0, y0, z1), new Vec3(x1, y0, z1),
                new Vec3(x1, y0, z0), new Vec3(x0, y0, z0),
                new Vec3(0.0D, -1.0D, 0.0D),
                0.0F, 14.5F, 1.5F, 13.0F);
    }

    private static void lampFace(VertexConsumer consumer, PoseStack poseStack,
            BlockPos blockOrigin, Direction facing, float angle,
            Vec3 mountOffset, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
            Vec3 normal, float u0, float v0, float u1, float v1) {
        Vec3 rp0 = rotateModelPoint(p0, angle);
        Vec3 rp1 = rotateModelPoint(p1, angle);
        Vec3 rp2 = rotateModelPoint(p2, angle);
        Vec3 rp3 = rotateModelPoint(p3, angle);
        Vec3 worldNormal = rotateModelVector(normal, angle, facing);

        lampVertex(consumer, poseStack,
                local(modelPointToWorld(blockOrigin, facing,
                        rp0.x, rp0.y, rp0.z, mountOffset), blockOrigin),
                worldNormal, u0 / 32.0F, v1 / 32.0F);
        lampVertex(consumer, poseStack,
                local(modelPointToWorld(blockOrigin, facing,
                        rp1.x, rp1.y, rp1.z, mountOffset), blockOrigin),
                worldNormal, u1 / 32.0F, v1 / 32.0F);
        lampVertex(consumer, poseStack,
                local(modelPointToWorld(blockOrigin, facing,
                        rp2.x, rp2.y, rp2.z, mountOffset), blockOrigin),
                worldNormal, u1 / 32.0F, v0 / 32.0F);
        lampVertex(consumer, poseStack,
                local(modelPointToWorld(blockOrigin, facing,
                        rp3.x, rp3.y, rp3.z, mountOffset), blockOrigin),
                worldNormal, u0 / 32.0F, v0 / 32.0F);
    }

    private static void lampVertex(VertexConsumer consumer,
            PoseStack poseStack, Vec3 point, Vec3 normal, float u, float v) {
        consumer.vertex(poseStack.last().pose(),
                        (float) point.x, (float) point.y, (float) point.z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(poseStack.last().normal(),
                        (float) normal.x, (float) normal.y,
                        (float) normal.z)
                .endVertex();
    }

    private static Vec3 rotateModelPoint(Vec3 point, float angle) {
        double dx = point.x;
        double dy = point.y - 8.0D;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(dx * cos - dy * sin,
                8.0D + dx * sin + dy * cos, point.z);
    }

    private static Vec3 rotateModelVector(Vec3 vector, float angle,
            Direction facing) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double rx = vector.x * cos - vector.y * sin;
        double ry = vector.x * sin + vector.y * cos;
        Vec3 right = direction(facing.getClockWise());
        Vec3 back = direction(facing.getOpposite());
        return right.scale(rx)
                .add(0.0D, ry, 0.0D)
                .add(back.scale(vector.z))
                .normalize();
    }

    private static void renderProjection(AlarmModule.AlarmBlockEntity alarm,
            PoseStack poseStack, MultiBufferSource buffers,
            float rotorAngle, Vec3 mountOffset) {
        if (!(alarm.getLevel() instanceof ClientLevel level)) return;

        Minecraft minecraft = Minecraft.getInstance();
        Entity camera = minecraft.getCameraEntity();
        if (camera == null) return;

        Vec3 center = Vec3.atCenterOf(alarm.getBlockPos());
        double cameraDistanceSqr = camera.position().distanceToSqr(center);
        if (cameraDistanceSqr > PROJECTOR_DISTANCE_SQR) return;

        /*
         * One continuous mask is projected over an adaptive, surface-clipped
         * mesh. Samples describe geometry only; they are not separate light
         * sources. This is what removes the honeycomb pattern while retaining
         * real occlusion by walls, ceilings, pillars and door structure.
         */
        ProjectionCache projected = projection(level, alarm, camera,
                rotorAngle, mountOffset, false);
        if (projected.triangles.isEmpty()) return;

        RenderType washType = RenderType.entityTranslucent(SPLASH, true);
        VertexConsumer wash = buffers.getBuffer(washType);
        for (ProjectedTriangle triangle : projected.triangles) {
            emitProjectionTriangle(wash, poseStack, alarm.getBlockPos(),
                    triangle, false);
        }
        flush(buffers, washType);

        // Separate, deliberately weak shader-emissive copy. The texture itself
        // carries a much lower alpha than the visible wash, so BSL receives a
        // soft bloom signal instead of another opaque cone.
        RenderType bloomType = RenderType.eyes(SPLASH_EMISSIVE);
        VertexConsumer bloom = buffers.getBuffer(bloomType);
        for (ProjectedTriangle triangle : projected.triangles) {
            if (!triangle.bloomAllowed()) continue;
            emitProjectionTriangle(bloom, poseStack, alarm.getBlockPos(),
                    triangle, true);
        }
        flush(buffers, bloomType);
    }

    private static ProjectionCache projection(ClientLevel level,
            AlarmModule.AlarmBlockEntity alarm, Entity camera,
            float rotorAngle, Vec3 mountOffset, boolean perFrame) {
        Map<BlockPos, ProjectionCache> byPos = PROJECTIONS.computeIfAbsent(
                level, ignored -> new HashMap<>());
        BlockPos pos = alarm.getBlockPos();
        long tick = level.getGameTime();
        ProjectionCache cached = byPos.get(pos);
        if (!perFrame && cached != null && cached.tick == tick) {
            return cached;
        }

        BlockState state = alarm.getBlockState();
        Direction facing = state.getValue(AlarmModule.FACING);
        Vec3 outward = direction(facing);
        Vec3 inward = outward.scale(-1.0D);
        Vec3 right = direction(facing.getClockWise());
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);

        double rotorRadians = rotorAngle;
        Vec3 tangent = right.scale(Math.sin(rotorRadians))
                .add(up.scale(-Math.cos(rotorRadians))).normalize();
        Vec3 fanSide = outward.cross(tangent);
        if (fanSide.lengthSqr() < 1.0E-6D) fanSide = right;
        fanSide = fanSide.normalize();

        Vec3 rotorCenter = modelPointToWorld(pos, facing,
                0.0D, 8.0D, 7.0D, mountOffset);
        Vec3 wallOrigin = rotorCenter.add(
                inward.scale(WALL_PLANE_INSET));
        Vec3 rayStart = wallOrigin.add(
                outward.scale(PROJECTOR_OUTSET));

        BlockPos blastDoorController =
                AlarmModule.blastDoorTopMountController(
                        level, pos, state);
        ProjectionBuilder builder = new ProjectionBuilder(level, camera, pos,
                rayStart, wallOrigin, tangent, fanSide, inward, facing,
                blastDoorController);
        List<ProjectedTriangle> triangles = builder.build();

        ProjectionCache fresh = new ProjectionCache(tick,
                List.copyOf(triangles));
        byPos.put(pos.immutable(), fresh);
        return fresh;
    }

    /**
     * Receiver-first projected light.
     *
     * <p>The old renderer started with a UV grid and tried to infer world
     * surfaces from ray samples. That is mathematically unstable exactly where
     * this effect matters most: corners, door openings, thin frames and
     * wall-to-ceiling transitions. One UV triangle could straddle two unrelated
     * planes, so every attempted "fix" merely chose between holes, neon slivers
     * or geometry floating in empty space.</p>
     *
     * <p>This builder inverts the problem. It enumerates the ACTUAL collision
     * faces that can receive light, maps each face point back into projector UV,
     * and ray-tests visibility on that same face. Geometry boundaries therefore
     * come from world geometry itself; adaptive subdivision is used only for the
     * 2-D visibility boundary inside one already-known receiver plane.</p>
     */
    private static final class ProjectionBuilder {
        private final ClientLevel level;
        private final Entity context;
        private final BlockPos alarmPos;
        private final Vec3 rayStart;
        private final Vec3 wallOrigin;
        private final Vec3 tangent;
        private final Vec3 fanSide;
        private final Vec3 inward;
        private final Direction wallFace;
        private final Vec3 outward;
        private final BlockPos blastDoorController;
        private final Map<SurfaceKey, FaceProbe> probes = new HashMap<>();

        private ProjectionBuilder(ClientLevel level, Entity context,
                BlockPos alarmPos, Vec3 rayStart, Vec3 wallOrigin,
                Vec3 tangent, Vec3 fanSide, Vec3 inward,
                Direction wallFace, BlockPos blastDoorController) {
            this.level = level;
            this.context = context;
            this.alarmPos = alarmPos;
            this.rayStart = rayStart;
            this.wallOrigin = wallOrigin;
            this.tangent = tangent;
            this.fanSide = fanSide;
            this.inward = inward;
            this.wallFace = wallFace;
            this.outward = direction(wallFace);
            this.blastDoorController = blastDoorController;
        }

        private List<ProjectedTriangle> build() {
            List<ProjectedTriangle> result = new ArrayList<>();
            AABB bounds = projectionBounds();

            int minX = (int) Math.floor(bounds.minX);
            int minY = (int) Math.floor(bounds.minY);
            int minZ = (int) Math.floor(bounds.minZ);
            int maxX = (int) Math.floor(bounds.maxX);
            int maxY = (int) Math.floor(bounds.maxY);
            int maxZ = (int) Math.floor(bounds.maxZ);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        if (state.isAir() || isOwnAlarmCell(pos, state)) {
                            continue;
                        }

                        VoxelShape shape;
                        boolean bloomAllowed = true;

                        if (BlastDoorModule.isStructureState(state)) {
                            /*
                             * Blast Door metal is an occluder-only GeckoLib
                             * surface. Rendering the emissive decal on its coarse
                             * collision envelope is what used to make it appear
                             * transparent. Only the authored lower wall-mimic is
                             * a legitimate receiver here.
                             */
                            shape = BlastDoorStructure.lowerMimicShape(
                                    level, pos, state);
                            if (shape.isEmpty()) continue;
                        } else {
                            if (letsProjectedLightPass(state)) continue;
                            shape = state.getCollisionShape(level, pos);
                            if (shape.isEmpty()) continue;
                        }

                        for (AABB localBox : shape.toAabbs()) {
                            AABB box = localBox.move(
                                    pos.getX(), pos.getY(), pos.getZ());
                            emitBox(box, bloomAllowed, result);
                        }
                    }
                }
            }

            return result;
        }

        private AABB projectionBounds() {
            Vec3 p00 = projectedWallPoint(0.0F, 0.0F);
            Vec3 p10 = projectedWallPoint(1.0F, 0.0F);
            Vec3 p01 = projectedWallPoint(0.0F, 1.0F);
            Vec3 p11 = projectedWallPoint(1.0F, 1.0F);

            double minX = Math.min(rayStart.x,
                    Math.min(Math.min(p00.x, p10.x),
                            Math.min(p01.x, p11.x)));
            double minY = Math.min(rayStart.y,
                    Math.min(Math.min(p00.y, p10.y),
                            Math.min(p01.y, p11.y)));
            double minZ = Math.min(rayStart.z,
                    Math.min(Math.min(p00.z, p10.z),
                            Math.min(p01.z, p11.z)));
            double maxX = Math.max(rayStart.x,
                    Math.max(Math.max(p00.x, p10.x),
                            Math.max(p01.x, p11.x)));
            double maxY = Math.max(rayStart.y,
                    Math.max(Math.max(p00.y, p10.y),
                            Math.max(p01.y, p11.y)));
            double maxZ = Math.max(rayStart.z,
                    Math.max(Math.max(p00.z, p10.z),
                            Math.max(p01.z, p11.z)));

            return new AABB(minX, minY, minZ,
                    maxX, maxY, maxZ).inflate(PROJECTION_BOUNDS_PADDING);
        }

        private boolean isOwnAlarmCell(BlockPos pos, BlockState state) {
            if (pos.equals(alarmPos)) return true;
            if (!AlarmModule.isPart(state)) return false;
            try {
                return AlarmMountStructure.controllerPosition(
                        pos, state).equals(alarmPos);
            } catch (RuntimeException ignored) {
                return false;
            }
        }

        private void emitBox(AABB box, boolean bloomAllowed,
                List<ProjectedTriangle> result) {
            for (Direction face : Direction.values()) {
                FaceRect rect = faceRect(box, face);
                Vec3 center = rect.center();
                if (direction(face).dot(
                        rayStart.subtract(center)) <= 1.0E-6D) {
                    continue;
                }
                if (!mayIntersectProjector(rect)) continue;

                tessellateFace(rect, face, bloomAllowed,
                        0, result);
            }
        }

        private boolean mayIntersectProjector(FaceRect rect) {
            Vec3[] points = {
                    rect.p00, rect.p10, rect.p11, rect.p01, rect.center()
            };
            boolean mapped = false;
            float minU = Float.POSITIVE_INFINITY;
            float maxU = Float.NEGATIVE_INFINITY;
            float minV = Float.POSITIVE_INFINITY;
            float maxV = Float.NEGATIVE_INFINITY;

            for (Vec3 point : points) {
                ProjectorUv uv = projectorUv(point);
                if (uv == null) continue;
                mapped = true;
                minU = Math.min(minU, uv.u);
                maxU = Math.max(maxU, uv.u);
                minV = Math.min(minV, uv.v);
                maxV = Math.max(maxV, uv.v);
            }

            return mapped
                    && maxU >= -UV_EPSILON
                    && minU <= 1.0F + UV_EPSILON
                    && maxV >= -UV_EPSILON
                    && minV <= 1.0F + UV_EPSILON;
        }

        private void tessellateFace(FaceRect rect, Direction face,
                boolean bloomAllowed, int depth,
                List<ProjectedTriangle> result) {
            Vec3 topMid = midpoint(rect.p00, rect.p10);
            Vec3 rightMid = midpoint(rect.p10, rect.p11);
            Vec3 bottomMid = midpoint(rect.p01, rect.p11);
            Vec3 leftMid = midpoint(rect.p00, rect.p01);
            Vec3 center = rect.center();

            FaceProbe a = probe(rect.p00, face, bloomAllowed);
            FaceProbe b = probe(rect.p10, face, bloomAllowed);
            FaceProbe c = probe(rect.p11, face, bloomAllowed);
            FaceProbe d = probe(rect.p01, face, bloomAllowed);
            FaceProbe top = probe(topMid, face, bloomAllowed);
            FaceProbe right = probe(rightMid, face, bloomAllowed);
            FaceProbe bottom = probe(bottomMid, face, bloomAllowed);
            FaceProbe left = probe(leftMid, face, bloomAllowed);
            FaceProbe mid = probe(center, face, bloomAllowed);

            FaceProbe[] nine = {
                    a, b, c, d, top, right, bottom, left, mid
            };

            if (allActive(nine)) {
                addSurfaceTriangle(result, a.sample, b.sample, c.sample);
                addSurfaceTriangle(result, a.sample, c.sample, d.sample);
                return;
            }

            if (!anyActive(nine)) {
                return;
            }

            if (depth < SURFACE_PATCH_SUBDIVISIONS) {
                tessellateFace(new FaceRect(
                                rect.p00, topMid, center, leftMid),
                        face, bloomAllowed, depth + 1, result);
                tessellateFace(new FaceRect(
                                topMid, rect.p10, rightMid, center),
                        face, bloomAllowed, depth + 1, result);
                tessellateFace(new FaceRect(
                                center, rightMid, rect.p11, bottomMid),
                        face, bloomAllowed, depth + 1, result);
                tessellateFace(new FaceRect(
                                leftMid, center, bottomMid, rect.p01),
                        face, bloomAllowed, depth + 1, result);
                return;
            }

            clipFaceTriangle(result, a, b, c, face, bloomAllowed);
            clipFaceTriangle(result, a, c, d, face, bloomAllowed);
        }

        private static boolean allActive(FaceProbe[] probes) {
            for (FaceProbe probe : probes) {
                if (!probe.active) return false;
            }
            return true;
        }

        private static boolean anyActive(FaceProbe[] probes) {
            for (FaceProbe probe : probes) {
                if (probe.active) return true;
            }
            return false;
        }

        private void clipFaceTriangle(List<ProjectedTriangle> result,
                FaceProbe a, FaceProbe b, FaceProbe c,
                Direction face, boolean bloomAllowed) {
            FaceProbe[] triangle = { a, b, c };
            List<FaceProbe> polygon = new ArrayList<>(5);

            for (int i = 0; i < 3; i++) {
                FaceProbe current = triangle[i];
                FaceProbe next = triangle[(i + 1) % 3];

                if (current.active && next.active) {
                    polygon.add(next);
                } else if (current.active) {
                    FaceProbe edge = faceBoundary(
                            current, next, face, bloomAllowed);
                    if (edge != null) polygon.add(edge);
                } else if (next.active) {
                    FaceProbe edge = faceBoundary(
                            next, current, face, bloomAllowed);
                    if (edge != null) polygon.add(edge);
                    polygon.add(next);
                }
            }

            if (polygon.size() < 3) return;
            ProjectedSample first = polygon.get(0).sample;
            for (int i = 1; i + 1 < polygon.size(); i++) {
                addSurfaceTriangle(result, first,
                        polygon.get(i).sample,
                        polygon.get(i + 1).sample);
            }
        }

        private FaceProbe faceBoundary(FaceProbe inside, FaceProbe outside,
                Direction face, boolean bloomAllowed) {
            if (!inside.active) return null;

            Vec3 in = inside.position;
            Vec3 out = outside.position;
            FaceProbe lastValid = inside;

            for (int i = 0; i < SURFACE_EDGE_BISECTIONS; i++) {
                Vec3 mid = midpoint(in, out);
                FaceProbe probe = probe(mid, face, bloomAllowed);
                if (probe.active) {
                    in = mid;
                    lastValid = probe;
                } else {
                    out = mid;
                }
            }

            return lastValid;
        }

        private FaceProbe probe(Vec3 point, Direction face,
                boolean bloomAllowed) {
            SurfaceKey key = SurfaceKey.of(point, face, bloomAllowed);
            FaceProbe cached = probes.get(key);
            if (cached != null) return cached;

            ProjectorUv uv = projectorUv(point);
            if (uv == null
                    || uv.u < -UV_EPSILON
                    || uv.u > 1.0F + UV_EPSILON
                    || uv.v < -UV_EPSILON
                    || uv.v > 1.0F + UV_EPSILON) {
                FaceProbe miss = new FaceProbe(
                        point, uv, false, null);
                probes.put(key, miss);
                return miss;
            }

            float u = Math.max(0.0F, Math.min(1.0F, uv.u));
            float v = Math.max(0.0F, Math.min(1.0F, uv.v));
            boolean visible = receiverVisible(point, face);
            ProjectedSample sample = visible
                    ? new ProjectedSample(
                            point.add(direction(face)
                                    .scale(SURFACE_EPSILON)),
                            face, u, v, bloomAllowed,
                            false, true, 1.0F)
                    : null;
            FaceProbe result = new FaceProbe(
                    point, new ProjectorUv(u, v), visible, sample);
            probes.put(key, result);
            return result;
        }

        private boolean receiverVisible(Vec3 point, Direction face) {
            /*
             * visualOcclusionHit is intentionally a WALL-SPACE silhouette test.
             * Applying it to ceiling/side receivers turns their X/Y into fake
             * door-metal hits and recreates holes around the Blast Door. Other
             * faces use the real collision ray below.
             */
            if (face == wallFace && blastDoorController != null) {
                BlockState controllerState =
                        level.getBlockState(blastDoorController);
                if (BlastDoorModule.isController(controllerState)
                        && BlastDoorStructure.visualOcclusionHit(
                                level, blastDoorController, controllerState,
                                rayStart, point) != null) {
                    return false;
                }
            }

            Vec3 ray = point.subtract(rayStart);
            double length = ray.length();
            if (length < 1.0E-6D) return false;
            Vec3 end = point.add(ray.scale(0.025D / length));

            ProjectedHit hit = cast(level, context, alarmPos,
                    rayStart, end, point, wallFace);
            if (hit == null || hit.blastDoorOccluder) return false;

            Vec3 expected = point.add(
                    direction(face).scale(SURFACE_EPSILON));
            return hit.position.distanceToSqr(expected)
                    <= RECEIVER_HIT_EPSILON_SQR;
        }

        private ProjectorUv projectorUv(Vec3 point) {
            Vec3 ray = point.subtract(rayStart);
            double denominator = ray.dot(outward);
            if (Math.abs(denominator) < 1.0E-6D) return null;

            double numerator = wallOrigin.subtract(rayStart).dot(outward);
            double t = numerator / denominator;
            if (!Double.isFinite(t) || t <= 0.0D) return null;

            Vec3 projected = rayStart.add(ray.scale(t));
            Vec3 rel = projected.subtract(wallOrigin);
            double radius = rel.dot(tangent);
            float v = (float) ((radius - MIN_SPLASH_RADIUS)
                    / (MAX_SPLASH_RADIUS - MIN_SPLASH_RADIUS));
            if (!Float.isFinite(v)) return null;

            double widthScale = 0.55D
                    + 0.45D * Math.sqrt(Math.max(0.0D,
                            Math.min(1.0D, v)));
            double halfWidth = MAX_SPLASH_HALF_WIDTH * widthScale;
            if (halfWidth < 1.0E-6D) return null;

            double lateral = rel.dot(fanSide) / halfWidth;
            float u = (float) ((lateral + 1.0D) * 0.5D);
            return Float.isFinite(u) ? new ProjectorUv(u, v) : null;
        }

        private Vec3 projectedWallPoint(float u01, float v01) {
            double lateral = -1.0D + 2.0D * u01;
            double radius = MIN_SPLASH_RADIUS
                    + (MAX_SPLASH_RADIUS - MIN_SPLASH_RADIUS) * v01;
            double widthScale = 0.55D
                    + 0.45D * Math.sqrt(Math.max(0.0D, v01));
            return wallOrigin
                    .add(tangent.scale(radius))
                    .add(fanSide.scale(
                            MAX_SPLASH_HALF_WIDTH
                                    * widthScale * lateral));
        }

        private void addSurfaceTriangle(List<ProjectedTriangle> result,
                ProjectedSample a, ProjectedSample b, ProjectedSample c) {
            if (a == null || b == null || c == null) return;

            Vec3 ab = b.position.subtract(a.position);
            Vec3 ac = c.position.subtract(a.position);
            double worldArea = ab.cross(ac).length() * 0.5D;
            if (worldArea <= 1.0E-10D) return;

            Vec3 center = a.position.add(b.position).add(c.position)
                    .scale(1.0D / 3.0D);
            Direction face = a.face;
            double incidence = Math.max(0.0D,
                    direction(face).dot(
                            rayStart.subtract(center).normalize()));

            float washScale = 1.0F;
            float bloomScale = 1.0F;

            if (face != wallFace) {
                washScale *= smoothStep(0.035F, 0.34F,
                        (float) incidence);
                bloomScale *= smoothStep(0.14F, 0.52F,
                        (float) incidence);
            }

            double idealArea = triangleArea(
                    projectedWallPoint(a.u, a.v),
                    projectedWallPoint(b.u, b.v),
                    projectedWallPoint(c.u, c.v));
            if (idealArea > 1.0E-9D) {
                float compression = (float) Math.max(0.0D,
                        Math.min(1.0D, worldArea / idealArea));
                if (face != wallFace) {
                    washScale *= smoothStep(0.025F, 0.24F, compression);
                    bloomScale *= smoothStep(0.12F, 0.50F, compression);
                }
            }

            if (washScale <= 0.002F) return;
            result.add(new ProjectedTriangle(
                    a, b, c, washScale, bloomScale));
        }

        private static double triangleArea(
                Vec3 a, Vec3 b, Vec3 c) {
            return b.subtract(a).cross(c.subtract(a)).length() * 0.5D;
        }

        private static float smoothStep(
                float edge0, float edge1, float value) {
            if (edge1 <= edge0) {
                return value >= edge1 ? 1.0F : 0.0F;
            }
            float x = Math.max(0.0F, Math.min(1.0F,
                    (value - edge0) / (edge1 - edge0)));
            return x * x * (3.0F - 2.0F * x);
        }

        private static FaceRect faceRect(AABB box, Direction face) {
            return switch (face) {
                case NORTH -> new FaceRect(
                        new Vec3(box.maxX, box.minY, box.minZ),
                        new Vec3(box.minX, box.minY, box.minZ),
                        new Vec3(box.minX, box.maxY, box.minZ),
                        new Vec3(box.maxX, box.maxY, box.minZ));
                case SOUTH -> new FaceRect(
                        new Vec3(box.minX, box.minY, box.maxZ),
                        new Vec3(box.maxX, box.minY, box.maxZ),
                        new Vec3(box.maxX, box.maxY, box.maxZ),
                        new Vec3(box.minX, box.maxY, box.maxZ));
                case WEST -> new FaceRect(
                        new Vec3(box.minX, box.minY, box.minZ),
                        new Vec3(box.minX, box.minY, box.maxZ),
                        new Vec3(box.minX, box.maxY, box.maxZ),
                        new Vec3(box.minX, box.maxY, box.minZ));
                case EAST -> new FaceRect(
                        new Vec3(box.maxX, box.minY, box.maxZ),
                        new Vec3(box.maxX, box.minY, box.minZ),
                        new Vec3(box.maxX, box.maxY, box.minZ),
                        new Vec3(box.maxX, box.maxY, box.maxZ));
                case DOWN -> new FaceRect(
                        new Vec3(box.minX, box.minY, box.minZ),
                        new Vec3(box.maxX, box.minY, box.minZ),
                        new Vec3(box.maxX, box.minY, box.maxZ),
                        new Vec3(box.minX, box.minY, box.maxZ));
                case UP -> new FaceRect(
                        new Vec3(box.minX, box.maxY, box.minZ),
                        new Vec3(box.minX, box.maxY, box.maxZ),
                        new Vec3(box.maxX, box.maxY, box.maxZ),
                        new Vec3(box.maxX, box.maxY, box.minZ));
            };
        }

        private static Vec3 midpoint(Vec3 a, Vec3 b) {
            return new Vec3(
                    (a.x + b.x) * 0.5D,
                    (a.y + b.y) * 0.5D,
                    (a.z + b.z) * 0.5D);
        }

        private record FaceRect(
                Vec3 p00, Vec3 p10, Vec3 p11, Vec3 p01) {
            private Vec3 center() {
                return new Vec3(
                        (p00.x + p10.x + p11.x + p01.x) * 0.25D,
                        (p00.y + p10.y + p11.y + p01.y) * 0.25D,
                        (p00.z + p10.z + p11.z + p01.z) * 0.25D);
            }
        }

        private record ProjectorUv(float u, float v) {
        }

        private record FaceProbe(Vec3 position, ProjectorUv uv,
                boolean active, ProjectedSample sample) {
        }

        private record SurfaceKey(long x, long y, long z,
                int face, boolean bloomAllowed) {
            private static SurfaceKey of(Vec3 point, Direction face,
                    boolean bloomAllowed) {
                return new SurfaceKey(
                        Math.round(point.x * 16384.0D),
                        Math.round(point.y * 16384.0D),
                        Math.round(point.z * 16384.0D),
                        face.ordinal(), bloomAllowed);
            }
        }
    }

    private static ProjectedHit cast(ClientLevel level, Entity context,
            BlockPos alarmPos, Vec3 start, Vec3 end,
            Vec3 wallSurface, Direction wallFace) {
        Vec3 ray = end.subtract(start);
        if (ray.lengthSqr() < 1.0E-8D) return null;
        Vec3 rayDirection = ray.normalize();
        Vec3 cursor = start;

        /*
         * The Alarm body itself and genuinely translucent block render layers
         * do not terminate the projected light. This makes ordinary glass,
         * stained glass, panes, ice and other translucent blocks behave like
         * optical media instead of opaque walls. We advance past the complete
         * block cell, not a tiny epsilon, so a row of glass panes cannot trap a
         * ray in repeated self-hits.
         *
         * Blast Door structure is deliberately different. Its visible body is
         * a large translucent GeckoLib render even though the metal is physically
         * opaque. Drawing an emissive projection on its collision face lets BSL
         * composite that projection through the model and creates the white
         * see-through artifact. Treat the structure as a pure occluder: a ray
         * that reaches it stops there and contributes no surface polygon.
         */
        for (int attempt = 0; attempt < 16; attempt++) {
            BlockHitResult hit = level.clip(new ClipContext(cursor, end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, context));
            if (hit.getType() != HitResult.Type.BLOCK) return null;

            BlockPos hitPos = hit.getBlockPos();
            BlockState hitState = level.getBlockState(hitPos);
            boolean ownAlarmCell = hitPos.equals(alarmPos);
            if (!ownAlarmCell && AlarmModule.isPart(hitState)) {
                try {
                    ownAlarmCell = AlarmMountStructure.controllerPosition(
                            hitPos, hitState).equals(alarmPos);
                } catch (RuntimeException ignored) {
                    ownAlarmCell = false;
                }
            }
            if (ownAlarmCell) {
                cursor = skipPastBlockCell(hit.getLocation(),
                        rayDirection, hitPos);
                if (!rayCursorStillValid(cursor, start, end, ray)) {
                    return null;
                }
                continue;
            }


            if (BlastDoorModule.isStructureState(hitState)) {
                /*
                 * The gameplay collision envelope is intentionally conservative
                 * and includes reserved/mimic cells above the visible frame.
                 * The visual test below is silhouette-based, so a top-mounted
                 * Alarm only loses light where the projected point actually
                 * enters visible Blast Door metal. This matters because that
                 * Alarm is raised by BLAST_DOOR_TOP_MOUNT_Y_OFFSET; using a
                 * volumetric/parallax shadow makes the cutoff look exactly like
                 * the lamp were still in its old, lower position.
                 */
                Vec3 visualHit = BlastDoorStructure.visualOcclusionHit(
                        level, hitPos, hitState, cursor, end);
                if (visualHit != null) {
                    return new ProjectedHit(visualHit,
                            hit.getDirection(), false, true);
                }

                BlockHitResult mimicHit = BlastDoorStructure.clipLowerMimic(
                        level, hitPos, hitState, cursor, end);
                if (mimicHit != null) {
                    Direction mimicFace = mimicHit.getDirection();
                    Vec3 mimicNormal = direction(mimicFace);
                    return new ProjectedHit(
                            mimicHit.getLocation().add(
                                    mimicNormal.scale(SURFACE_EPSILON)),
                            mimicFace, false, false);
                }

                cursor = skipPastBlockCell(hit.getLocation(),
                        rayDirection, hitPos);
                if (!rayCursorStillValid(cursor, start, end, ray)) {
                    return null;
                }
                continue;
            }

            if (letsProjectedLightPass(hitState)) {
                cursor = skipPastBlockCell(hit.getLocation(),
                        rayDirection, hitPos);
                if (!rayCursorStillValid(cursor, start, end, ray)) {
                    return null;
                }
                continue;
            }

            Direction face = hit.getDirection();
            Vec3 normal = direction(face);
            Vec3 position = hit.getLocation().add(
                    normal.scale(SURFACE_EPSILON));
            return new ProjectedHit(position, face, true, false);
        }
        return null;
    }

    private static boolean letsProjectedLightPass(BlockState state) {
        if (state.isAir()) return true;
        // Vanilla and Forge register glass/ice/panes on the translucent chunk
        // layer. Using the render layer rather than collision/occlusion flags is
        // important: slabs, stairs and fences may be non-occluding but are still
        // opaque material and must cast a real shadow.
        return ItemBlockRenderTypes.getChunkRenderType(state)
                == RenderType.translucent();
    }

    private static boolean rayCursorStillValid(Vec3 cursor, Vec3 start,
            Vec3 end, Vec3 ray) {
        return cursor.distanceToSqr(end) >= 1.0E-6D
                && cursor.subtract(start).dot(ray) >= 0.0D
                && cursor.subtract(end).dot(ray) <= 0.0D;
    }

    private static Vec3 skipPastBlockCell(Vec3 point, Vec3 direction,
            BlockPos blockPos) {
        double tx = exitDistance(point.x, direction.x,
                blockPos.getX(), blockPos.getX() + 1.0D);
        double ty = exitDistance(point.y, direction.y,
                blockPos.getY(), blockPos.getY() + 1.0D);
        double tz = exitDistance(point.z, direction.z,
                blockPos.getZ(), blockPos.getZ() + 1.0D);
        double distance = Math.min(tx, Math.min(ty, tz));
        if (!Double.isFinite(distance)) {
            return point.add(direction.scale(1.001D));
        }
        return point.add(direction.scale(distance + 0.002D));
    }

    private static double exitDistance(double coordinate, double direction,
            double min, double max) {
        if (Math.abs(direction) < 1.0E-9D) {
            return Double.POSITIVE_INFINITY;
        }
        double boundary = direction > 0.0D ? max : min;
        double distance = (boundary - coordinate) / direction;
        return distance >= 0.0D
                ? distance : Double.POSITIVE_INFINITY;
    }

    private static void emitProjectionTriangle(VertexConsumer consumer,
            PoseStack poseStack, BlockPos blockOrigin,
            ProjectedTriangle triangle, boolean bloomPass) {
        float energyScale = bloomPass
                ? triangle.bloomScale : triangle.washScale;
        projectionVertex(consumer, poseStack, blockOrigin,
                triangle.a, bloomPass, energyScale);
        projectionVertex(consumer, poseStack, blockOrigin,
                triangle.b, bloomPass, energyScale);
        projectionVertex(consumer, poseStack, blockOrigin,
                triangle.c, bloomPass, energyScale);
        // Both selected render types use QUADS. Repeating the final corner
        // creates a degenerate quad with exactly the triangle's visible area.
        projectionVertex(consumer, poseStack, blockOrigin,
                triangle.c, bloomPass, energyScale);
    }

    private static void projectionVertex(VertexConsumer consumer,
            PoseStack poseStack, BlockPos blockOrigin,
            ProjectedSample sample, boolean bloomPass,
            float energyScale) {
        Vec3 normal = direction(sample.face);
        Vec3 worldPoint = bloomPass
                ? sample.position.add(normal.scale(BLOOM_SURFACE_EPSILON))
                : sample.position;
        Vec3 point = local(worldPoint, blockOrigin);

        consumer.vertex(poseStack.last().pose(),
                        (float) point.x, (float) point.y, (float) point.z)
                .color(255, 255, 255,
                        Math.max(0, Math.min(255,
                                Math.round(sample.opacity
                                        * energyScale * 255.0F))))
                .uv(sample.u, sample.v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(poseStack.last().normal(),
                        (float) normal.x, (float) normal.y,
                        (float) normal.z)
                .endVertex();
    }

    private static void flush(MultiBufferSource buffers, RenderType type) {
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(type);
        }
    }

    private static Vec3 modelPointToWorld(BlockPos pos, Direction facing,
            double modelX, double modelY, double modelZ,
            Vec3 mountOffset) {
        Vec3 center = Vec3.atCenterOf(pos).add(mountOffset);
        Vec3 right = direction(facing.getClockWise());
        Vec3 back = direction(facing.getOpposite());
        return center
                .add(right.scale(modelX / 16.0D))
                .add(0.0D, (modelY - 8.0D) / 16.0D, 0.0D)
                .add(back.scale(modelZ / 16.0D));
    }

    private static Vec3 local(Vec3 world, BlockPos blockOrigin) {
        return world.subtract(blockOrigin.getX(),
                blockOrigin.getY(), blockOrigin.getZ());
    }

    private static Vec3 direction(Direction direction) {
        return new Vec3(direction.getStepX(),
                direction.getStepY(), direction.getStepZ());
    }

    private enum BlastDoorCoverage {
        NONE,
        FULL,
        MIXED
    }

    private record ProjectedHit(Vec3 position, Direction face,
            boolean bloomAllowed, boolean blastDoorOccluder) {
    }

    private record ProjectedSample(Vec3 position, Direction face,
            float u, float v, boolean bloomAllowed,
            boolean blastDoorOccluder, boolean receiver, float opacity) {
        private ProjectedSample withOpacity(float opacity) {
            return new ProjectedSample(position, face, u, v,
                    bloomAllowed, blastDoorOccluder, receiver, opacity);
        }
    }

    private record ProjectedTriangle(ProjectedSample a,
            ProjectedSample b, ProjectedSample c,
            float washScale, float bloomScale) {
        private boolean bloomAllowed() {
            return bloomScale > 0.01F
                    && a.bloomAllowed
                    && b.bloomAllowed
                    && c.bloomAllowed;
        }
    }

    private record ProjectionCache(long tick,
            List<ProjectedTriangle> triangles) {
    }

    private static final class ItemModel
            extends GeoModel<AlarmModule.AlarmItem> {
        @Override
        public ResourceLocation getModelResource(
                AlarmModule.AlarmItem animatable) {
            return ITEM_GEO;
        }

        @Override
        public ResourceLocation getTextureResource(
                AlarmModule.AlarmItem animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(
                AlarmModule.AlarmItem animatable) {
            return ANIMATION;
        }

        @Override
        public void setCustomAnimations(AlarmModule.AlarmItem animatable,
                long instanceId,
                AnimationState<AlarmModule.AlarmItem> animationState) {
            super.setCustomAnimations(animatable, instanceId, animationState);
            // Inventory/hand representation is always the authored dormant pose.
            forceLamp(getAnimationProcessor().getBone("lit"), false);
            forceLamp(getAnimationProcessor().getBone("unlit"), true);
            CoreGeoBone rotor = getAnimationProcessor().getBone("rotor");
            if (rotor != null) rotor.setRotZ(0.0F);
        }
    }

    public static final class ItemRenderer
            extends GeoItemRenderer<AlarmModule.AlarmItem> {
        public ItemRenderer() {
            super(new ItemModel());
        }

        @Override
        public RenderType getRenderType(AlarmModule.AlarmItem animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityTranslucent(texture, true);
        }
    }
}
