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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
    private static final double SURFACE_EPSILON = 0.00020D;
    private static final double PLANE_EPSILON = 0.0020D;
    /*
     * Flat-wall cells are allowed to stay coarse; geometry discontinuities are
     * clipped analytically below instead of forcing the entire projector to a
     * dense mesh. This is the main CPU win for multiple active Alarms.
     */
    private static final int BASE_MESH_CELLS = 5;
    private static final int MAX_BOUNDARY_SUBDIVISIONS = 1;
    private static final int SURFACE_EDGE_BISECTIONS = 5;
    /*
     * Flat regions stay at 5x5 cells. Only cells whose nine probes disagree
     * recurse once around a discontinuity. No synthetic
     * clipping vertices are ever fabricated: every rendered vertex is an
     * actual sampled world hit. This is the important invariant that prevents
     * floating shards, emissive rods and the saw-tooth holes caused by the old
     * polygon clipper.
     */
    private static final int MESH_RESOLUTION =
            BASE_MESH_CELLS << MAX_BOUNDARY_SUBDIVISIONS;
    private static final int BASE_MESH_STEP =
            1 << MAX_BOUNDARY_SUBDIVISIONS;
    // Projection geometry is rebuilt at Minecraft's 20 Hz world tick rate.
    // Recasting the complete surface mesh every render frame was the source of
    // the severe FPS regression, especially with shaders enabled.

    private static final int PROJECTION_PHASES = 20;
    private static final int GEOMETRY_SIGNATURE_INTERVAL = 40;
    private static final int GEOMETRY_SIGNATURE_RADIUS = 4;

    private static final Map<ClientLevel, Map<BlockPos, ProjectionPhaseCache>>
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
        /*
         * The wash must be committed before the HDR eyes pass. Buffering both
         * together lets shader packs reorder/combine the translucent receiver
         * mesh and is what turned the cone into a solid emissive plate.
         */
        flush(buffers, washType);
        /*
         * HDR bloom is intentionally restricted to the Alarm's mounting-wall
         * receiver plane.
         *
         * RenderType.eyes is excellent for closed emissive meshes such as the
         * lamp itself, but shader packs do not treat it like an ordinary
         * depth-behaved translucent decal. When the same eyes pass is wrapped
         * onto ceiling, side-wall, floor or frame faces, those grazing triangles
         * are composited as bright HDR blades at the physical boundary. This is
         * exactly the artifact visible in the screenshots; changing UV density
         * or clipping thresholds cannot fix a render-pass semantic mismatch.
         *
         * Secondary receiver faces still render in the FULL_BRIGHT wash above,
         * so the beam continues naturally around geometry. Only the HDR copy is
         * limited to the coplanar authored surface where it is depth-stable.
         */
        Direction bloomFace = alarm.getBlockState()
                .getValue(AlarmModule.FACING);
        RenderType bloomType = RenderType.eyes(SPLASH_EMISSIVE);
        VertexConsumer bloom = buffers.getBuffer(bloomType);
        for (ProjectedTriangle triangle : projected.triangles) {
            if (!triangle.bloomAllowed()
                    || !triangle.isOnFace(bloomFace)) {
                continue;
            }
            emitProjectionTriangle(bloom, poseStack, alarm.getBlockPos(),
                    triangle, true);
        }
        // Do not endBatch here. Let the shared BufferSource batch every Alarm
        // projection in the frame; forcing two flushes per Alarm was a major
        // shader-side performance penalty.
    }

    private static ProjectionCache projection(ClientLevel level,
            AlarmModule.AlarmBlockEntity alarm, Entity camera,
            float rotorAngle, Vec3 mountOffset, boolean perFrame) {
        Map<BlockPos, ProjectionPhaseCache> byPos =
                PROJECTIONS.computeIfAbsent(level,
                        ignored -> new HashMap<>());
        BlockPos pos = alarm.getBlockPos();
        long tick = level.getGameTime();

        ProjectionPhaseCache cache = byPos.computeIfAbsent(
                pos.immutable(), ignored -> new ProjectionPhaseCache());
        cache.lastTouchedTick = tick;

        if (cache.signatureTick == Long.MIN_VALUE
                || tick - cache.signatureTick
                        >= GEOMETRY_SIGNATURE_INTERVAL) {
            long signature = projectionGeometrySignature(level, pos);
            if (cache.signatureTick != Long.MIN_VALUE
                    && cache.signature != signature) {
                cache.phases.clear();
            }
            cache.signature = signature;
            cache.signatureTick = tick;

            if ((tick & 127L) == 0L && byPos.size() > 16) {
                byPos.entrySet().removeIf(entry ->
                        tick - entry.getValue().lastTouchedTick > 200L);
            }
        }

        int phase = Math.floorMod((int) Math.floor(
                alarm.projectionPhase(0.0F)
                        * PROJECTION_PHASES + 1.0E-4F),
                PROJECTION_PHASES);

        List<ProjectedTriangle> cached = cache.phases.get(phase);
        if (!perFrame && cached != null) {
            return new ProjectionCache(tick, cached);
        }

        BlockState state = alarm.getBlockState();
        Direction facing = state.getValue(AlarmModule.FACING);
        Vec3 outward = direction(facing);
        Vec3 inward = outward.scale(-1.0D);
        Vec3 right = direction(facing.getClockWise());
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);

        /*
         * Geometry is intentionally quantized to the same 20 samples per
         * second that the previous cache already exposed visually. The model
         * itself remains smoothly animated; only the projected collision mesh
         * reuses one of the 20 deterministic revolution phases.
         */
        double rotorRadians = -phase
                * (Math.PI * 2.0D / PROJECTION_PHASES);
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
        List<ProjectedTriangle> triangles =
                List.copyOf(builder.build());

        if (!perFrame) {
            cache.phases.put(phase, triangles);
        }
        return new ProjectionCache(tick, triangles);
    }

    private static long projectionGeometrySignature(
            ClientLevel level, BlockPos origin) {
        long hash = 0xcbf29ce484222325L;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int x = -GEOMETRY_SIGNATURE_RADIUS;
                x <= GEOMETRY_SIGNATURE_RADIUS; x++) {
            for (int y = -GEOMETRY_SIGNATURE_RADIUS;
                    y <= GEOMETRY_SIGNATURE_RADIUS; y++) {
                for (int z = -GEOMETRY_SIGNATURE_RADIUS;
                        z <= GEOMETRY_SIGNATURE_RADIUS; z++) {
                    cursor.set(origin.getX() + x,
                            origin.getY() + y,
                            origin.getZ() + z);
                    BlockState state = level.getBlockState(cursor);
                    hash ^= state.hashCode();
                    hash *= 0x100000001b3L;
                }
            }
        }
        return hash;
    }

    /**
     * Adaptive surface tessellation for the alarm footprint.
     *
     * Flat surfaces stay at a coarse 5x5 topology. Only cells that contain a
     * real receiver discontinuity subdivide. Boundary polygons are assembled
     * from verified ray hits, and each emitted triangle rechecks its edge
     * midpoints and centroid so disconnected coplanar surfaces cannot be
     * bridged. The texture remains the sole owner of the beam's visual shape.
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
        private final BlockPos blastDoorController;
        private final Map<Integer, ProjectedSample> samples = new HashMap<>();
        private final Map<Long, ProjectedSample> edgeSamples = new HashMap<>();

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
            this.blastDoorController = blastDoorController;
        }

        private List<ProjectedTriangle> build() {
            List<ProjectedTriangle> result = new ArrayList<>();
            for (int v = 0; v < MESH_RESOLUTION; v += BASE_MESH_STEP) {
                for (int u = 0; u < MESH_RESOLUTION;
                        u += BASE_MESH_STEP) {
                    tessellate(u, v, u + BASE_MESH_STEP,
                            v + BASE_MESH_STEP, 0, result);
                }
            }
            return result;
        }

        private void tessellate(int u0, int v0, int u1, int v1,
                int depth, List<ProjectedTriangle> result) {
            ProjectedSample a = sample(u0, v0);
            ProjectedSample b = sample(u1, v0);
            ProjectedSample c = sample(u1, v1);
            ProjectedSample d = sample(u0, v1);

            int um = (u0 + u1) >>> 1;
            int vm = (v0 + v1) >>> 1;
            ProjectedSample top = sample(um, v0);
            ProjectedSample right = sample(u1, vm);
            ProjectedSample bottom = sample(um, v1);
            ProjectedSample left = sample(u0, vm);
            ProjectedSample center = sample(um, vm);

            ProjectedSample[] probes = {
                    a, b, c, d, top, right, bottom, left, center
            };

            if (sameReceiver(probes)) {
                addTriangle(result, a, b, c);
                addTriangle(result, a, c, d);
                return;
            }

            if (!hasRenderable(probes)) {
                return;
            }

            if (depth < MAX_BOUNDARY_SUBDIVISIONS
                    && u1 - u0 >= 2 && v1 - v0 >= 2) {
                tessellate(u0, v0, um, vm, depth + 1, result);
                tessellate(um, v0, u1, vm, depth + 1, result);
                tessellate(um, vm, u1, v1, depth + 1, result);
                tessellate(u0, vm, um, v1, depth + 1, result);
                return;
            }

            /*
             * Finest boundary cell. Clip each micro-triangle against the real
             * receivers sampled by the world. A boundary vertex is always the
             * last ACTUAL hit on that receiver, found by bisection along the UV
             * edge. Nothing is projected onto an infinite plane, nothing is
             * extended underneath an obstacle, and legitimate partial wedges
             * are not deleted.
             */
            clipLeafTriangle(result, a, top, center);
            clipLeafTriangle(result, a, center, left);
            clipLeafTriangle(result, top, b, right);
            clipLeafTriangle(result, top, right, center);
            clipLeafTriangle(result, center, right, c);
            clipLeafTriangle(result, center, c, bottom);
            clipLeafTriangle(result, left, center, bottom);
            clipLeafTriangle(result, left, bottom, d);
        }

        private boolean sameReceiver(ProjectedSample... probes) {
            ProjectedSample reference = null;
            for (ProjectedSample probe : probes) {
                if (!isRenderable(probe)) return false;
                if (reference == null) {
                    reference = probe;
                } else if (!sameSurface(reference, probe)) {
                    return false;
                }
            }
            return reference != null;
        }

        private static boolean hasRenderable(ProjectedSample... probes) {
            for (ProjectedSample probe : probes) {
                if (isRenderable(probe)) return true;
            }
            return false;
        }

        private void clipLeafTriangle(
                List<ProjectedTriangle> result,
                ProjectedSample a, ProjectedSample b, ProjectedSample c) {
            ProjectedSample[] triangle = { a, b, c };

            for (ProjectedSample target : triangle) {
                if (!isRenderable(target)) continue;

                boolean duplicate = false;
                for (ProjectedSample previous : triangle) {
                    if (previous == target) break;
                    if (isRenderable(previous)
                            && sameSurface(previous, target)) {
                        duplicate = true;
                        break;
                    }
                }
                if (duplicate) continue;

                clipTriangleToReceiver(result, triangle, target);
            }
        }

        private void clipTriangleToReceiver(
                List<ProjectedTriangle> result,
                ProjectedSample[] triangle, ProjectedSample target) {
            List<ProjectedSample> polygon = new ArrayList<>(5);

            for (int i = 0; i < 3; i++) {
                ProjectedSample current = triangle[i];
                ProjectedSample next = triangle[(i + 1) % 3];
                boolean currentInside = isRenderable(current)
                        && sameSurface(current, target);
                boolean nextInside = isRenderable(next)
                        && sameSurface(next, target);

                if (currentInside && nextInside) {
                    polygon.add(next);
                } else if (currentInside) {
                    ProjectedSample edge =
                            receiverBoundary(current, next, target);
                    if (edge != null) polygon.add(edge);
                } else if (nextInside) {
                    ProjectedSample edge =
                            receiverBoundary(next, current, target);
                    if (edge != null) polygon.add(edge);
                    polygon.add(next);
                }
            }

            if (polygon.size() < 3) return;
            ProjectedSample first = polygon.get(0);
            for (int i = 1; i + 1 < polygon.size(); i++) {
                addTriangle(result, first,
                        polygon.get(i), polygon.get(i + 1));
            }
        }

        /**
         * Returns the last verified sample that still belongs to {@code target}.
         * This is a real world hit, never a point reconstructed on an infinite
         * mathematical plane. The invariant matters more than another round of
         * visual band-aids: every rendered vertex must correspond to geometry
         * that the raycaster actually found.
         */
        private ProjectedSample receiverBoundary(
                ProjectedSample inside, ProjectedSample outside,
                ProjectedSample target) {
            if (!isRenderable(inside)
                    || !sameSurface(inside, target)
                    || outside == null) {
                return inside;
            }

            float inU = inside.u;
            float inV = inside.v;
            float outU = outside.u;
            float outV = outside.v;
            ProjectedSample lastValid = inside;

            for (int i = 0; i < SURFACE_EDGE_BISECTIONS; i++) {
                float midU = (inU + outU) * 0.5F;
                float midV = (inV + outV) * 0.5F;
                ProjectedSample probe = sampleAtCached(midU, midV);

                if (isRenderable(probe)
                        && sameSurface(probe, target)) {
                    inU = midU;
                    inV = midV;
                    lastValid = probe;
                } else {
                    outU = midU;
                    outV = midV;
                }
            }

            /*
             * Keep the boundary point on the verified receiver, but make it
             * transparent. Interpolation from the interior sample to this
             * zero-alpha vertex gives us a real geometric feather inside the
             * valid surface. Nothing is pushed under the obstacle, so there is
             * no overlap strip for BSL to turn into a bright blade.
             */
            return lastValid.withOpacity(0.0F);
        }

        private static boolean isBlastDoorOccluder(ProjectedSample sample) {
            return sample != null && sample.blastDoorOccluder;
        }

        private static boolean isRenderable(ProjectedSample sample) {
            return sample != null
                    && sample.receiver
                    && !sample.blastDoorOccluder;
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

        private void addTriangle(List<ProjectedTriangle> result,
                ProjectedSample a, ProjectedSample b, ProjectedSample c) {
            if (!compatibleTriangle(a, b, c)) return;
            result.add(new ProjectedTriangle(
                    a, b, c, 1.0F, 1.0F));
        }

        private ProjectedSample sample(int uIndex, int vIndex) {
            int key = (vIndex << 16) | uIndex;
            if (samples.containsKey(key)) return samples.get(key);

            float u01 = uIndex / (float) MESH_RESOLUTION;
            float v01 = vIndex / (float) MESH_RESOLUTION;
            ProjectedSample sample = sampleAtCached(u01, v01);
            samples.put(key, sample);
            return sample;
        }

        private ProjectedSample sampleAtCached(float u01, float v01) {
            int qu = Math.round(u01 * 16384.0F);
            int qv = Math.round(v01 * 16384.0F);
            long key = ((long) qu << 32) | (qv & 0xffffffffL);
            ProjectedSample cached = edgeSamples.get(key);
            if (cached != null) return cached;

            ProjectedSample sample = sampleAt(u01, v01);
            edgeSamples.put(key, sample);
            return sample;
        }

        private ProjectedSample sampleAt(float u01, float v01) {
            /*
             * Geometry is deliberately boring: a broad projector strip. The
             * final shape and feathering remain entirely texture-driven.
             */
            Vec3 wallSurface = projectedWallPoint(u01, v01);
            Vec3 intended = wallSurface.add(
                    inward.scale(RAY_OVERSHOOT));

            /*
             * Top-mounted Alarms now sit on real wall blocks, not Blast Door
             * copycats. Test the authored frame silhouette explicitly before
             * the normal world raycast so the metal can still clip the wash
             * without forcing the wall itself through a BlockEntity renderer.
             */
            if (blastDoorController != null) {
                BlockState doorState =
                        level.getBlockState(blastDoorController);
                if (BlastDoorModule.isController(doorState)) {
                    Vec3 visualHit =
                            BlastDoorStructure.visualOcclusionHit(
                                    level, blastDoorController, doorState,
                                    rayStart, wallSurface);
                    if (visualHit != null) {
                        return new ProjectedSample(
                                visualHit, wallFace, u01, v01,
                                false, true, false, 1.0F);
                    }
                }
            }

            FastWallResult fast = fastWallPlaneHit(
                    wallSurface, intended);
            ProjectedHit hit = fast.hit;
            if (!fast.handled) {
                hit = cast(level, context, alarmPos,
                        rayStart, intended, wallSurface, wallFace);
            }
            if (hit == null) {
                return new ProjectedSample(
                        wallSurface, wallFace, u01, v01,
                        false, false, false, 1.0F);
            }

            /*
             * The authored beam lives on the mounting wall and may fold onto a
             * ceiling/floor when the wall ends. A vertical face perpendicular
             * to the mounting wall is instead an obstacle silhouette (door
             * return, column side, adjacent room wall). Treating that grazing
             * face as a texture receiver is what creates the huge orange
             * "blades": a tiny UV interval gets stretched over the side face.
             *
             * It still blocks the ray; it simply does not receive a decal.
             */
            if (!isStableReceiverFace(hit.face, wallFace, rayStart,
                    hit.position)) {
                return new ProjectedSample(hit.position, hit.face,
                        u01, v01, false,
                        hit.blastDoorOccluder, false, 1.0F);
            }

            return new ProjectedSample(hit.position, hit.face,
                    u01, v01, hit.bloomAllowed,
                    hit.blastDoorOccluder, true, 1.0F);
        }

        private static boolean isStableReceiverFace(
                Direction face, Direction wallFace,
                Vec3 rayStart, Vec3 hitPosition) {
            if (face == wallFace) return true;
            if (face.getAxis() != Direction.Axis.Y) return false;

            Vec3 toSource = rayStart.subtract(hitPosition);
            double lengthSqr = toSource.lengthSqr();
            if (lengthSqr < 1.0E-10D) return false;

            double incidence = Math.abs(direction(face).dot(
                    toSource.scale(1.0D / Math.sqrt(lengthSqr))));
            return incidence >= 0.12D;
        }

        /**
         * Most samples are much simpler than a Minecraft raycast makes them
         * look. The projector starts in the Alarm's wall cell and ends on the
         * mounting plane only ~0.4 blocks away. If that front cell is empty (or
         * is one of this Alarm's invisible reservation helpers) and the block
         * immediately behind the plane exposes a sturdy opaque face, the hit is
         * mathematically known: it is the mounting plane itself.
         *
         * <p>This removes the expensive collision traversal from the common
         * flat-wall case. Partial shapes, glass and real obstacles still fall
         * back to the full cast, so the surface-aware behaviour is preserved.</p>
         */
        private FastWallResult fastWallPlaneHit(
                Vec3 wallSurface, Vec3 intended) {
            Vec3 outward = direction(wallFace);
            BlockPos frontPos = BlockPos.containing(
                    wallSurface.add(outward.scale(0.01D)));
            BlockState frontState = level.getBlockState(frontPos);

            boolean ownAlarmCell = frontPos.equals(alarmPos);
            if (!ownAlarmCell && AlarmModule.isPart(frontState)) {
                try {
                    ownAlarmCell = AlarmMountStructure.controllerPosition(
                            frontPos, frontState).equals(alarmPos);
                } catch (RuntimeException ignored) {
                    ownAlarmCell = false;
                }
            }

            if (!ownAlarmCell
                    && !frontState.isAir()
                    && !letsProjectedLightPass(frontState)
                    && !frontState.getCollisionShape(level, frontPos)
                            .isEmpty()) {
                return FastWallResult.NEEDS_CAST;
            }

            BlockPos supportPos = BlockPos.containing(
                    wallSurface.add(inward.scale(0.01D)));
            BlockState supportState = level.getBlockState(supportPos);

            if (BlastDoorModule.isStructureState(supportState)) {
                BlockHitResult mimicHit = BlastDoorStructure.clipLowerMimic(
                        level, supportPos, supportState, rayStart, intended);
                if (mimicHit == null) {
                    return FastWallResult.NEEDS_CAST;
                }
                Direction face = mimicHit.getDirection();
                Vec3 normal = direction(face);
                return FastWallResult.hit(new ProjectedHit(
                        mimicHit.getLocation().add(
                                normal.scale(SURFACE_EPSILON)),
                        face, false, false));
            }

            /*
             * The requested projection plane ends here. Air or a translucent
             * support cell is therefore a definitive miss; there is no reason
             * to ask Level.clip and then rediscover the same fact. This matters
             * especially during the seven cheap UV bisections at an open edge.
             */
            if (supportState.isAir()
                    || letsProjectedLightPass(supportState)) {
                return FastWallResult.MISS;
            }

            if (!supportState.isFaceSturdy(
                    level, supportPos, wallFace)) {
                return FastWallResult.NEEDS_CAST;
            }

            Vec3 position = wallSurface.add(
                    outward.scale(SURFACE_EPSILON));
            return FastWallResult.hit(new ProjectedHit(
                    position, wallFace, true, false));
        }

        private record FastWallResult(ProjectedHit hit, boolean handled) {
            private static final FastWallResult NEEDS_CAST =
                    new FastWallResult(null, false);
            private static final FastWallResult MISS =
                    new FastWallResult(null, true);

            private static FastWallResult hit(ProjectedHit hit) {
                return new FastWallResult(hit, true);
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

    private static boolean compatibleQuad(ProjectedSample a,
            ProjectedSample b, ProjectedSample c, ProjectedSample d) {
        return compatibleTriangle(a, b, c)
                && compatibleTriangle(a, c, d);
    }

    private static boolean sameSurface(ProjectedSample a,
            ProjectedSample b) {
        if (!isRenderableSample(a) || !isRenderableSample(b)
                || a.blastDoorOccluder != b.blastDoorOccluder
                || a.face != b.face) return false;
        if (Math.abs(planeCoordinate(a.position, a.face)
                - planeCoordinate(b.position, b.face)) > PLANE_EPSILON) {
            return false;
        }

        BlockPos cellA = receiverCell(a);
        BlockPos cellB = receiverCell(b);
        return Math.abs(cellA.getX() - cellB.getX()) <= 1
                && Math.abs(cellA.getY() - cellB.getY()) <= 1
                && Math.abs(cellA.getZ() - cellB.getZ()) <= 1;
    }

    private static BlockPos receiverCell(ProjectedSample sample) {
        Vec3 intoReceiver = sample.position.subtract(
                direction(sample.face).scale(0.01D));
        return BlockPos.containing(intoReceiver);
    }

    private static boolean compatibleTriangle(ProjectedSample a,
            ProjectedSample b, ProjectedSample c) {
        if (!isRenderableSample(a)
                || !isRenderableSample(b)
                || !isRenderableSample(c)) return false;
        if (a.blastDoorOccluder || b.blastDoorOccluder
                || c.blastDoorOccluder) return false;
        if (a.face != b.face || a.face != c.face) return false;

        double planeA = planeCoordinate(a.position, a.face);
        if (Math.abs(planeA - planeCoordinate(b.position, b.face))
                        > PLANE_EPSILON
                || Math.abs(planeA - planeCoordinate(c.position, c.face))
                        > PLANE_EPSILON) {
            return false;
        }

        Vec3 ab = b.position.subtract(a.position);
        Vec3 ac = c.position.subtract(a.position);
        return ab.cross(ac).lengthSqr() > 1.0E-12D;
    }

    private static boolean isRenderableSample(ProjectedSample sample) {
        return sample != null && sample.receiver;
    }

    private static double planeCoordinate(Vec3 point, Direction face) {
        return switch (face.getAxis()) {
            case X -> point.x;
            case Y -> point.y;
            case Z -> point.z;
        };
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
        projectionVertex(consumer, poseStack, blockOrigin,
                triangle.c, bloomPass, energyScale);
    }

    private static void projectionVertex(VertexConsumer consumer,
            PoseStack poseStack, BlockPos blockOrigin,
            ProjectedSample sample, boolean bloomPass,
            float energyScale) {
        Vec3 normal = direction(sample.face);
        /*
         * Wash and HDR bloom MUST be depth-identical. A second shell offset
         * away from the receiver protrudes past wall/ceiling/door silhouettes
         * and is exactly what produced the bright rods at projection ends.
         * BSL still sees a separate emissive texture pass; it simply occupies
         * the same physical decal surface.
         */
        Vec3 point = local(sample.position, blockOrigin);

        consumer.vertex(poseStack.last().pose(),
                        (float) point.x, (float) point.y, (float) point.z)
                .color(255, 255, 255,
                        Math.max(0, Math.min(255,
                                Math.round(sample.opacity
                                        * energyScale * 255.0F))))
                .uv(clampProjectorUv(sample.u),
                        clampProjectorUv(sample.v))
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(poseStack.last().normal(),
                        (float) normal.x, (float) normal.y,
                        (float) normal.z)
                .endVertex();
    }

    private static float clampProjectorUv(float uv) {
        return Math.max(0.0005F, Math.min(0.9995F, uv));
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

        private boolean isOnFace(Direction face) {
            return a.face == face && b.face == face && c.face == face;
        }
    }

    private record ProjectionCache(long tick,
            List<ProjectedTriangle> triangles) {
    }

    private static final class ProjectionPhaseCache {
        private long signature;
        private long signatureTick = Long.MIN_VALUE;
        private long lastTouchedTick;
        private final Map<Integer, List<ProjectedTriangle>> phases =
                new HashMap<>();
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
