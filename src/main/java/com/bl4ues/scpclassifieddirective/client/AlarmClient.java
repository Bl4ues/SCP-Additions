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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
     * The projected light is built from real receiver faces, not from a UV
     * mesh that guesses world topology from ray samples. Full block faces use
     * a cheap 2x2 patch grid; partial/complex shapes use 4x4.
     */
    private static final int RECEIVER_SUBDIVISIONS = 2;
    private static final int COMPLEX_RECEIVER_SUBDIVISIONS = 4;
    private static final double RECEIVER_VISIBILITY_EPSILON_SQR = 0.0064D;
    // Twenty deterministic rotor phases are cached. Receiver visibility is
    // rebuilt only when nearby block geometry changes; phase generation itself
    // performs no world raycasts.

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
        if (projected.quads.isEmpty()) return;

        RenderType washType = RenderType.entityTranslucent(SPLASH, true);
        VertexConsumer wash = buffers.getBuffer(washType);
        for (ProjectedQuad quad : projected.quads) {
            emitProjectionQuad(wash, poseStack, alarm.getBlockPos(),
                    quad, false);
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
        for (ProjectedQuad quad : projected.quads) {
            if (!quad.bloomAllowed()
                    || !quad.isOnFace(bloomFace)) {
                continue;
            }
            emitProjectionQuad(bloom, poseStack, alarm.getBlockPos(),
                    quad, true);
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
                cache.receivers = null;
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

        List<ProjectedQuad> cached = cache.phases.get(phase);
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

        if (cache.receivers == null) {
            cache.receivers = collectReceiverPatches(level, camera, pos,
                    rayStart, facing, blastDoorController);
        }

        ProjectionBuilder builder = new ProjectionBuilder(
                cache.receivers, wallOrigin, rayStart,
                tangent, fanSide, facing);
        List<ProjectedQuad> quads = List.copyOf(builder.build());

        if (!perFrame) {
            cache.phases.put(phase, quads);
        }
        return new ProjectionCache(tick, quads);
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
    /**
     * Surface-first projection. Receiver topology is collected once from real
     * VoxelShape faces. Rotor phases only remap those verified patches into
     * projector UV space, so phase generation performs no collision raycasts.
     */
    private static final class ProjectionBuilder {
        private final List<ReceiverPatch> receivers;
        private final Vec3 wallOrigin;
        private final Vec3 rayStart;
        private final Vec3 tangent;
        private final Vec3 fanSide;
        private final Direction wallFace;
        private final Vec3 outward;

        private ProjectionBuilder(List<ReceiverPatch> receivers,
                Vec3 wallOrigin, Vec3 rayStart,
                Vec3 tangent, Vec3 fanSide, Direction wallFace) {
            this.receivers = receivers;
            this.wallOrigin = wallOrigin;
            this.rayStart = rayStart;
            this.tangent = tangent;
            this.fanSide = fanSide;
            this.wallFace = wallFace;
            this.outward = direction(wallFace);
        }

        private List<ProjectedQuad> build() {
            List<ProjectedQuad> result = new ArrayList<>();
            for (ReceiverPatch patch : receivers) {
                ProjectorUv aUv = projectorUv(patch.a);
                ProjectorUv bUv = projectorUv(patch.b);
                ProjectorUv cUv = projectorUv(patch.c);
                ProjectorUv dUv = projectorUv(patch.d);
                ProjectorUv centerUv = projectorUv(patch.center());

                if (!intersectsProjector(aUv, bUv, cUv, dUv, centerUv)) {
                    continue;
                }

                boolean bloom = patch.face == wallFace;
                ProjectedSample a = projectedSample(patch.a, patch.face, aUv, bloom);
                ProjectedSample b = projectedSample(patch.b, patch.face, bUv, bloom);
                ProjectedSample c = projectedSample(patch.c, patch.face, cUv, bloom);
                ProjectedSample d = projectedSample(patch.d, patch.face, dUv, bloom);
                if (a == null || b == null || c == null || d == null) continue;

                result.add(new ProjectedQuad(a, b, c, d));
            }
            return result;
        }

        private ProjectedSample projectedSample(Vec3 position, Direction face,
                ProjectorUv uv, boolean bloomAllowed) {
            if (uv == null) return null;
            return new ProjectedSample(
                    position.add(direction(face).scale(SURFACE_EPSILON)),
                    face,
                    clampProjectorUv(uv.u),
                    clampProjectorUv(uv.v),
                    bloomAllowed,
                    1.0F);
        }

        private boolean intersectsProjector(ProjectorUv... samples) {
            float minU = Float.POSITIVE_INFINITY;
            float maxU = Float.NEGATIVE_INFINITY;
            float minV = Float.POSITIVE_INFINITY;
            float maxV = Float.NEGATIVE_INFINITY;
            boolean any = false;
            for (ProjectorUv uv : samples) {
                if (uv == null) continue;
                any = true;
                minU = Math.min(minU, uv.u);
                maxU = Math.max(maxU, uv.u);
                minV = Math.min(minV, uv.v);
                maxV = Math.max(maxV, uv.v);
            }
            return any && maxU >= 0.0F && minU <= 1.0F
                    && maxV >= 0.0F && minV <= 1.0F;
        }

        private ProjectorUv projectorUv(Vec3 point) {
            Vec3 ray = point.subtract(rayStart);
            double denominator = ray.dot(outward);
            if (Math.abs(denominator) < 1.0E-7D) return null;

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
                    + 0.45D * Math.sqrt(Math.max(0.0D, Math.min(1.0D, v)));
            double halfWidth = MAX_SPLASH_HALF_WIDTH * widthScale;
            if (halfWidth < 1.0E-7D) return null;

            float u = (float) ((rel.dot(fanSide) / halfWidth + 1.0D) * 0.5D);
            return Float.isFinite(u) ? new ProjectorUv(u, v) : null;
        }
    }

    private static List<ReceiverPatch> collectReceiverPatches(
            ClientLevel level, Entity context, BlockPos alarmPos,
            Vec3 rayStart, Direction wallFace,
            BlockPos blastDoorController) {
        List<ReceiverPatch> result = new ArrayList<>();
        Set<ReceiverPatchKey> seen = new HashSet<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -GEOMETRY_SIGNATURE_RADIUS;
                dx <= GEOMETRY_SIGNATURE_RADIUS; dx++) {
            for (int dy = -GEOMETRY_SIGNATURE_RADIUS;
                    dy <= GEOMETRY_SIGNATURE_RADIUS; dy++) {
                for (int dz = -GEOMETRY_SIGNATURE_RADIUS;
                        dz <= GEOMETRY_SIGNATURE_RADIUS; dz++) {
                    cursor.set(alarmPos.getX() + dx,
                            alarmPos.getY() + dy,
                            alarmPos.getZ() + dz);
                    BlockPos pos = cursor.immutable();
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || letsProjectedLightPass(state)
                            || isOwnAlarmCell(level, alarmPos, pos, state)) {
                        continue;
                    }

                    boolean blastDoor = BlastDoorModule.isStructureState(state);
                    VoxelShape shape = blastDoor
                            ? BlastDoorStructure.lowerMimicShape(level, pos, state)
                            : state.getCollisionShape(level, pos);
                    if (shape.isEmpty()) continue;

                    List<AABB> boxes = shape.toAabbs();
                    boolean complex = blastDoor || boxes.size() > 1;
                    for (AABB localBox : boxes) {
                        AABB box = localBox.move(pos);
                        int divisions = complex
                                || box.getXsize() < 0.99D
                                || box.getYsize() < 0.99D
                                || box.getZsize() < 0.99D
                                ? COMPLEX_RECEIVER_SUBDIVISIONS
                                : RECEIVER_SUBDIVISIONS;

                        addReceiverFace(level, context, alarmPos, rayStart,
                                wallFace, blastDoorController, box, wallFace,
                                divisions, seen, result);

                        if (rayStart.y < box.minY - 1.0E-5D) {
                            addReceiverFace(level, context, alarmPos, rayStart,
                                    wallFace, blastDoorController, box,
                                    Direction.DOWN, divisions, seen, result);
                        } else if (rayStart.y > box.maxY + 1.0E-5D) {
                            addReceiverFace(level, context, alarmPos, rayStart,
                                    wallFace, blastDoorController, box,
                                    Direction.UP, divisions, seen, result);
                        }
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    private static boolean isOwnAlarmCell(ClientLevel level,
            BlockPos alarmPos, BlockPos pos, BlockState state) {
        if (pos.equals(alarmPos)) return true;
        if (!AlarmModule.isPart(state)) return false;
        try {
            return AlarmMountStructure.controllerPosition(pos, state)
                    .equals(alarmPos);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static void addReceiverFace(ClientLevel level, Entity context,
            BlockPos alarmPos, Vec3 rayStart, Direction wallFace,
            BlockPos blastDoorController, AABB box, Direction face,
            int divisions, Set<ReceiverPatchKey> seen,
            List<ReceiverPatch> result) {
        FaceRect rect = faceRect(box, face);
        if (direction(face).dot(rayStart.subtract(rect.center())) <= 1.0E-6D) {
            return;
        }

        for (int y = 0; y < divisions; y++) {
            double v0 = y / (double) divisions;
            double v1 = (y + 1) / (double) divisions;
            for (int x = 0; x < divisions; x++) {
                double u0 = x / (double) divisions;
                double u1 = (x + 1) / (double) divisions;
                ReceiverPatch patch = new ReceiverPatch(
                        rect.point(u0, v0), rect.point(u1, v0),
                        rect.point(u1, v1), rect.point(u0, v1), face);

                ReceiverPatchKey key = ReceiverPatchKey.of(patch);
                if (!seen.add(key)) continue;
                if (receiverPatchVisible(level, context, alarmPos, rayStart,
                        wallFace, blastDoorController, patch)) {
                    result.add(patch);
                }
            }
        }
    }

    private static boolean receiverPatchVisible(ClientLevel level,
            Entity context, BlockPos alarmPos, Vec3 rayStart,
            Direction wallFace, BlockPos blastDoorController,
            ReceiverPatch patch) {
        Vec3 target = patch.center();
        if (blastDoorController != null && patch.face == wallFace) {
            BlockState controllerState = level.getBlockState(blastDoorController);
            if (BlastDoorModule.isController(controllerState)
                    && BlastDoorStructure.visualOcclusionHit(
                            level, blastDoorController, controllerState,
                            rayStart, target) != null) {
                return false;
            }
        }

        /*
         * ClipContext does not reliably report a collision when the ray ends
         * exactly on the receiver plane. This was filtering every wall patch
         * out of the new surface-first cache, which is why the cones vanished
         * entirely. Cast a little past the target so the receiver lies strictly
         * inside the segment, while keeping the authored target separate for
         * Blast Door silhouette tests and hit validation.
         */
        Vec3 visibilityRay = target.subtract(rayStart);
        if (visibilityRay.lengthSqr() < 1.0E-8D) {
            return false;
        }
        Vec3 visibilityEnd = target.add(
                visibilityRay.normalize().scale(RAY_OVERSHOOT));

        ProjectedHit hit = cast(level, context, alarmPos,
                rayStart, visibilityEnd, target, patch.face);
        if (hit == null || hit.blastDoorOccluder || hit.face != patch.face) {
            return false;
        }
        Vec3 expected = target.add(direction(patch.face).scale(SURFACE_EPSILON));
        return hit.position.distanceToSqr(expected)
                <= RECEIVER_VISIBILITY_EPSILON_SQR;
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

    private record FaceRect(Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
        private Vec3 point(double u, double v) {
            return lerp(lerp(a, b, u), lerp(d, c, u), v);
        }
        private Vec3 center() {
            return point(0.5D, 0.5D);
        }
    }

    private static Vec3 lerp(Vec3 a, Vec3 b, double t) {
        return new Vec3(
                a.x + (b.x - a.x) * t,
                a.y + (b.y - a.y) * t,
                a.z + (b.z - a.z) * t);
    }

    private record ReceiverPatch(
            Vec3 a, Vec3 b, Vec3 c, Vec3 d, Direction face) {
        private Vec3 center() {
            return new Vec3(
                    (a.x + b.x + c.x + d.x) * 0.25D,
                    (a.y + b.y + c.y + d.y) * 0.25D,
                    (a.z + b.z + c.z + d.z) * 0.25D);
        }
    }

    private record ReceiverPatchKey(
            long ax, long ay, long az,
            long cx, long cy, long cz, int face) {
        private static ReceiverPatchKey of(ReceiverPatch patch) {
            return new ReceiverPatchKey(
                    quantize(patch.a.x), quantize(patch.a.y),
                    quantize(patch.a.z), quantize(patch.c.x),
                    quantize(patch.c.y), quantize(patch.c.z),
                    patch.face.ordinal());
        }
        private static long quantize(double value) {
            return Math.round(value * 4096.0D);
        }
    }

    private record ProjectorUv(float u, float v) {
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
                        level, hitPos, hitState, cursor, wallSurface);
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

    private static void emitProjectionQuad(VertexConsumer consumer,
            PoseStack poseStack, BlockPos blockOrigin,
            ProjectedQuad quad, boolean bloomPass) {
        projectionVertex(consumer, poseStack, blockOrigin,
                quad.a, bloomPass, 1.0F);
        projectionVertex(consumer, poseStack, blockOrigin,
                quad.b, bloomPass, 1.0F);
        projectionVertex(consumer, poseStack, blockOrigin,
                quad.c, bloomPass, 1.0F);
        projectionVertex(consumer, poseStack, blockOrigin,
                quad.d, bloomPass, 1.0F);
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

    private record ProjectedHit(Vec3 position, Direction face,
            boolean bloomAllowed, boolean blastDoorOccluder) {
    }

    private record ProjectedSample(Vec3 position, Direction face,
            float u, float v, boolean bloomAllowed, float opacity) {
    }

    private record ProjectedQuad(ProjectedSample a,
            ProjectedSample b, ProjectedSample c, ProjectedSample d) {
        private boolean bloomAllowed() {
            return a.bloomAllowed && b.bloomAllowed
                    && c.bloomAllowed && d.bloomAllowed;
        }
        private boolean isOnFace(Direction face) {
            return a.face == face && b.face == face
                    && c.face == face && d.face == face;
        }
    }

    private record ProjectionCache(long tick,
            List<ProjectedQuad> quads) {
    }

    private static final class ProjectionPhaseCache {
        private long signature;
        private long signatureTick = Long.MIN_VALUE;
        private long lastTouchedTick;
        private List<ReceiverPatch> receivers;
        private final Map<Integer, List<ProjectedQuad>> phases =
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
