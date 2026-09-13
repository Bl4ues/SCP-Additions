package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.alarm.AlarmModule;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
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
    private static final ResourceLocation SPLASH = id(
            "textures/effect/alarm_light_splash.png");
    private static final ResourceLocation ANIMATION = id(
            "animations/block/alarm.animation.json");

    private static final double PROJECTOR_DISTANCE = 24.0D;
    private static final double PROJECTOR_DISTANCE_SQR =
            PROJECTOR_DISTANCE * PROJECTOR_DISTANCE;
    private static final double MIN_SPLASH_RADIUS = 0.035D;
    private static final double MAX_SPLASH_RADIUS = 3.55D;
    private static final double PROJECTOR_OUTSET = 0.34D;
    private static final double WALL_PLANE_INSET = 0.0625D;
    private static final double RAY_OVERSHOOT = 0.06D;
    private static final double SURFACE_EPSILON = 0.0030D;
    private static final double PLANE_EPSILON = 0.022D;
    private static final double MAX_TRIANGLE_EDGE_SQR = 1.10D;
    private static final int BASE_MESH_CELLS = 8;
    private static final int ADAPTIVE_SUBDIVISIONS = 2;
    private static final int MESH_RESOLUTION =
            BASE_MESH_CELLS << ADAPTIVE_SUBDIVISIONS;
    private static final int BASE_MESH_STEP =
            MESH_RESOLUTION / BASE_MESH_CELLS;
    private static final double PER_FRAME_PROJECTOR_DISTANCE_SQR = 64.0D;

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
            double mountYOffset = alarm.getLevel() == null ? 0.0D
                    : AlarmModule.visualYOffset(alarm.getLevel(),
                            alarm.getBlockPos(), alarm.getBlockState());
            boolean active = alarm.getBlockState()
                    .getValue(AlarmModule.ACTIVE);
            float angle = rotorAngle(alarm, partialTick);

            poseStack.pushPose();
            poseStack.translate(0.0D, mountYOffset, 0.0D);
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
                        angle, mountYOffset);
            }

            poseStack.pushPose();
            poseStack.translate(0.0D, mountYOffset, 0.0D);
            cover.render(alarm, partialTick, poseStack, bufferSource,
                    packedLight, packedOverlay);
            flush(bufferSource, RenderType.entityTranslucent(TEXTURE, true));
            poseStack.popPose();

            if (active) {
                renderProjection(alarm, poseStack, bufferSource,
                        angle, mountYOffset);
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
            double mountYOffset) {
        RenderType lampType = RenderType.entityCutoutNoCull(TEXTURE);
        VertexConsumer lamp = buffers.getBuffer(lampType);
        emitLitLampCube(alarm, poseStack, lamp, rotorAngle,
                mountYOffset);
        flush(buffers, lampType);

        // The exact same geometry is submitted through the eyes render type.
        // The first full-bright cutout pass provides stable depth; this second
        // pass is what shader packs such as BSL recognize as true emissive/bloom.
        RenderType glowType = RenderType.eyes(GLOWMASK);
        VertexConsumer glow = buffers.getBuffer(glowType);
        emitLitLampCube(alarm, poseStack, glow, rotorAngle,
                mountYOffset);
        flush(buffers, glowType);
    }

    private static void emitLitLampCube(
            AlarmModule.AlarmBlockEntity alarm, PoseStack poseStack,
            VertexConsumer consumer, float angle, double mountYOffset) {
        Direction facing = alarm.getBlockState().getValue(AlarmModule.FACING);
        BlockPos origin = alarm.getBlockPos();

        final double x0 = -0.7D, x1 = 0.7D;
        final double y0 = 7.3D, y1 = 8.7D;
        final double z0 = 6.25D, z1 = 7.75D;

        lampFace(consumer, poseStack, origin, facing, angle, mountYOffset,
                new Vec3(x0, y0, z0), new Vec3(x1, y0, z0),
                new Vec3(x1, y1, z0), new Vec3(x0, y1, z0),
                new Vec3(0.0D, 0.0D, -1.0D),
                0.0F, 10.0F, 1.5F, 11.5F);
        lampFace(consumer, poseStack, origin, facing, angle, mountYOffset,
                new Vec3(x1, y0, z1), new Vec3(x0, y0, z1),
                new Vec3(x0, y1, z1), new Vec3(x1, y1, z1),
                new Vec3(0.0D, 0.0D, 1.0D),
                6.0F, 12.0F, 7.5F, 13.5F);
        lampFace(consumer, poseStack, origin, facing, angle, mountYOffset,
                new Vec3(x1, y0, z0), new Vec3(x1, y0, z1),
                new Vec3(x1, y1, z1), new Vec3(x1, y1, z0),
                new Vec3(1.0D, 0.0D, 0.0D),
                3.0F, 12.0F, 4.5F, 13.5F);
        lampFace(consumer, poseStack, origin, facing, angle, mountYOffset,
                new Vec3(x0, y0, z1), new Vec3(x0, y0, z0),
                new Vec3(x0, y1, z0), new Vec3(x0, y1, z1),
                new Vec3(-1.0D, 0.0D, 0.0D),
                9.0F, 12.0F, 10.5F, 13.5F);
        lampFace(consumer, poseStack, origin, facing, angle, mountYOffset,
                new Vec3(x0, y1, z0), new Vec3(x1, y1, z0),
                new Vec3(x1, y1, z1), new Vec3(x0, y1, z1),
                new Vec3(0.0D, 1.0D, 0.0D),
                12.0F, 12.0F, 13.5F, 13.5F);
        lampFace(consumer, poseStack, origin, facing, angle, mountYOffset,
                new Vec3(x0, y0, z1), new Vec3(x1, y0, z1),
                new Vec3(x1, y0, z0), new Vec3(x0, y0, z0),
                new Vec3(0.0D, -1.0D, 0.0D),
                0.0F, 14.5F, 1.5F, 13.0F);
    }

    private static void lampFace(VertexConsumer consumer, PoseStack poseStack,
            BlockPos blockOrigin, Direction facing, float angle,
            double mountYOffset, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
            Vec3 normal, float u0, float v0, float u1, float v1) {
        Vec3 rp0 = rotateModelPoint(p0, angle);
        Vec3 rp1 = rotateModelPoint(p1, angle);
        Vec3 rp2 = rotateModelPoint(p2, angle);
        Vec3 rp3 = rotateModelPoint(p3, angle);
        Vec3 worldNormal = rotateModelVector(normal, angle, facing);

        lampVertex(consumer, poseStack,
                local(modelPointToWorld(blockOrigin, facing,
                        rp0.x, rp0.y, rp0.z, mountYOffset), blockOrigin),
                worldNormal, u0 / 32.0F, v1 / 32.0F);
        lampVertex(consumer, poseStack,
                local(modelPointToWorld(blockOrigin, facing,
                        rp1.x, rp1.y, rp1.z, mountYOffset), blockOrigin),
                worldNormal, u1 / 32.0F, v1 / 32.0F);
        lampVertex(consumer, poseStack,
                local(modelPointToWorld(blockOrigin, facing,
                        rp2.x, rp2.y, rp2.z, mountYOffset), blockOrigin),
                worldNormal, u1 / 32.0F, v0 / 32.0F);
        lampVertex(consumer, poseStack,
                local(modelPointToWorld(blockOrigin, facing,
                        rp3.x, rp3.y, rp3.z, mountYOffset), blockOrigin),
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
            float rotorAngle, double mountYOffset) {
        if (!(alarm.getLevel() instanceof ClientLevel level)) return;
        Minecraft minecraft = Minecraft.getInstance();
        Entity camera = minecraft.getCameraEntity();
        if (camera == null) return;

        Vec3 alarmCenter = Vec3.atCenterOf(alarm.getBlockPos());
        double cameraDistanceSqr =
                camera.position().distanceToSqr(alarmCenter);
        if (cameraDistanceSqr > PROJECTOR_DISTANCE_SQR) return;

        ProjectionCache cache = projection(level, alarm, camera,
                rotorAngle, mountYOffset,
                cameraDistanceSqr <= PER_FRAME_PROJECTOR_DISTANCE_SQR);
        if (cache == null || cache.triangles.isEmpty()) return;

        /*
         * A single continuous translucent-emissive pass is deliberate.
         *
         * The previous implementation stamped hundreds of radial quads over
         * each other and then submitted the same footprints again through the
         * eyes/bloom program. On shader packs the overlap accumulated into the
         * nearly-solid yellow beacon seen in-game. This mesh covers every
         * surface point only once. It is still full-bright/emissive, but no
         * longer asks BSL for an additional HDR bloom pass.
         */
        RenderType lightType = RenderType.entityTranslucentEmissive(SPLASH);
        VertexConsumer light = buffers.getBuffer(lightType);
        BlockPos origin = alarm.getBlockPos();
        for (ProjectedTriangle triangle : cache.triangles) {
            emitProjectionTriangle(light, poseStack, origin, triangle);
        }
        flush(buffers, lightType);
    }

    private static ProjectionCache projection(ClientLevel level,
            AlarmModule.AlarmBlockEntity alarm, Entity camera,
            float rotorAngle, double mountYOffset, boolean perFrame) {
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
                0.0D, 8.0D, 7.0D, mountYOffset);
        Vec3 wallOrigin = rotorCenter.add(
                inward.scale(WALL_PLANE_INSET));
        Vec3 rayStart = wallOrigin
                .add(tangent.scale(MIN_SPLASH_RADIUS))
                .add(outward.scale(PROJECTOR_OUTSET));

        ProjectionBuilder builder = new ProjectionBuilder(level, camera, pos,
                rayStart, wallOrigin, tangent, fanSide, inward);
        List<ProjectedTriangle> triangles = builder.build();

        ProjectionCache fresh = new ProjectionCache(tick,
                List.copyOf(triangles));
        byPos.put(pos.immutable(), fresh);
        return fresh;
    }

    /**
     * Adaptive surface tessellation for the alarm footprint.
     *
     * A flat 1x1 wall is resolved with only the coarse 8x8 lattice. Cells that
     * cross a VoxelShape discontinuity (door frame, ceiling fold, pillar, etc.)
     * are subdivided locally up to two more times. That gives an effective
     * 32x32 boundary resolution only where the geometry actually needs it,
     * instead of paying for a thousand raycasts on every ordinary wall.
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
        private final Map<Integer, ProjectedSample> samples = new HashMap<>();

        private ProjectionBuilder(ClientLevel level, Entity context,
                BlockPos alarmPos, Vec3 rayStart, Vec3 wallOrigin,
                Vec3 tangent, Vec3 fanSide, Vec3 inward) {
            this.level = level;
            this.context = context;
            this.alarmPos = alarmPos;
            this.rayStart = rayStart;
            this.wallOrigin = wallOrigin;
            this.tangent = tangent;
            this.fanSide = fanSide;
            this.inward = inward;
        }

        private List<ProjectedTriangle> build() {
            List<ProjectedTriangle> result = new ArrayList<>();
            for (int v = 0; v < MESH_RESOLUTION; v += BASE_MESH_STEP) {
                for (int u = 0; u < MESH_RESOLUTION;
                        u += BASE_MESH_STEP) {
                    subdivide(u, v, u + BASE_MESH_STEP,
                            v + BASE_MESH_STEP, 0, result);
                }
            }
            return result;
        }

        private void subdivide(int u0, int v0, int u1, int v1,
                int depth, List<ProjectedTriangle> result) {
            ProjectedSample a = sample(u0, v0);
            ProjectedSample b = sample(u1, v0);
            ProjectedSample c = sample(u1, v1);
            ProjectedSample d = sample(u0, v1);

            if (compatibleQuad(a, b, c, d)) {
                addTriangle(result, a, b, c);
                addTriangle(result, a, c, d);
                return;
            }

            if (depth < ADAPTIVE_SUBDIVISIONS) {
                int um = (u0 + u1) >>> 1;
                int vm = (v0 + v1) >>> 1;
                subdivide(u0, v0, um, vm, depth + 1, result);
                subdivide(um, v0, u1, vm, depth + 1, result);
                subdivide(um, vm, u1, v1, depth + 1, result);
                subdivide(u0, vm, um, v1, depth + 1, result);
                return;
            }

            // At the finest local resolution retain whichever triangle really
            // belongs to a single physical surface. This produces a small,
            // smooth seam around folds rather than a whole missing square.
            addTriangle(result, a, b, c);
            addTriangle(result, a, c, d);
        }

        private void addTriangle(List<ProjectedTriangle> result,
                ProjectedSample a, ProjectedSample b, ProjectedSample c) {
            if (compatibleTriangle(a, b, c)) {
                result.add(new ProjectedTriangle(a, b, c));
            }
        }

        private ProjectedSample sample(int uIndex, int vIndex) {
            int key = (vIndex << 16) | uIndex;
            if (samples.containsKey(key)) return samples.get(key);

            float u01 = uIndex / (float) MESH_RESOLUTION;
            float v01 = vIndex / (float) MESH_RESOLUTION;
            double lateral = -1.0D + 2.0D * u01;
            double t = v01;

            double radius = MIN_SPLASH_RADIUS
                    + (MAX_SPLASH_RADIUS - MIN_SPLASH_RADIUS)
                    * Math.pow(t, 1.04D);

            // Preserve the approved overall silhouette: narrow root, quickly
            // widening pear/fan body and a gently rounded far cap.
            double growth = smoothStep(0.0F, 0.24F, (float) t);
            double lobe = Math.sin(Math.PI * 0.70D * t);
            lobe = Math.pow(Math.max(0.0D, lobe), 0.56D);
            double halfWidth = 0.025D + 1.52D * growth * lobe;

            Vec3 intended = wallOrigin
                    .add(tangent.scale(radius))
                    .add(fanSide.scale(halfWidth * lateral))
                    .add(inward.scale(RAY_OVERSHOOT));
            ProjectedHit hit = cast(level, context, alarmPos,
                    rayStart, intended);

            ProjectedSample sample = hit == null ? null
                    : new ProjectedSample(hit.position, hit.face,
                            u01, v01);
            samples.put(key, sample);
            return sample;
        }
    }

    private static ProjectedHit cast(ClientLevel level, Entity context,
            BlockPos alarmPos, Vec3 start, Vec3 end) {
        Vec3 ray = end.subtract(start);
        if (ray.lengthSqr() < 1.0E-8D) return null;
        Vec3 advance = ray.normalize().scale(0.012D);
        Vec3 cursor = start;

        // Only the Alarm itself is transparent to its projector. Every other
        // collider, including the Blast Door's dynamic VoxelShape, is a real
        // receiving/occluding surface. This is what makes the light wrap around
        // its frame instead of painting straight through it.
        for (int attempt = 0; attempt < 3; attempt++) {
            BlockHitResult hit = level.clip(new ClipContext(cursor, end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, context));
            if (hit.getType() != HitResult.Type.BLOCK) return null;

            if (hit.getBlockPos().equals(alarmPos)) {
                cursor = hit.getLocation().add(advance);
                if (cursor.distanceToSqr(end) < 1.0E-6D) return null;
                continue;
            }

            Direction face = hit.getDirection();
            Vec3 normal = direction(face);
            Vec3 position = hit.getLocation().add(
                    normal.scale(SURFACE_EPSILON));
            return new ProjectedHit(position, face);
        }
        return null;
    }

    private static boolean compatibleQuad(ProjectedSample a,
            ProjectedSample b, ProjectedSample c, ProjectedSample d) {
        return compatibleTriangle(a, b, c)
                && compatibleTriangle(a, c, d);
    }

    private static boolean compatibleTriangle(ProjectedSample a,
            ProjectedSample b, ProjectedSample c) {
        if (a == null || b == null || c == null) return false;
        if (a.face != b.face || a.face != c.face) return false;

        double planeA = planeCoordinate(a.position, a.face);
        if (Math.abs(planeA - planeCoordinate(b.position, b.face))
                        > PLANE_EPSILON
                || Math.abs(planeA - planeCoordinate(c.position, c.face))
                        > PLANE_EPSILON) {
            return false;
        }

        return a.position.distanceToSqr(b.position)
                        <= MAX_TRIANGLE_EDGE_SQR
                && b.position.distanceToSqr(c.position)
                        <= MAX_TRIANGLE_EDGE_SQR
                && c.position.distanceToSqr(a.position)
                        <= MAX_TRIANGLE_EDGE_SQR;
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
            ProjectedTriangle triangle) {
        projectionVertex(consumer, poseStack, blockOrigin, triangle.a);
        projectionVertex(consumer, poseStack, blockOrigin, triangle.b);
        projectionVertex(consumer, poseStack, blockOrigin, triangle.c);
        // entityTranslucentEmissive uses QUADS. Repeating the last corner forms
        // a degenerate quad with the exact visible area of the triangle.
        projectionVertex(consumer, poseStack, blockOrigin, triangle.c);
    }

    private static void projectionVertex(VertexConsumer consumer,
            PoseStack poseStack, BlockPos blockOrigin,
            ProjectedSample sample) {
        Vec3 point = local(sample.position, blockOrigin);
        Vec3 normal = direction(sample.face);
        consumer.vertex(poseStack.last().pose(),
                        (float) point.x, (float) point.y, (float) point.z)
                .color(255, 190, 96, 150)
                .uv(sample.u, sample.v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(poseStack.last().normal(),
                        (float) normal.x, (float) normal.y,
                        (float) normal.z)
                .endVertex();
    }

    private static float smoothStep(float edge0, float edge1, float value) {
        if (edge1 <= edge0) return value >= edge1 ? 1.0F : 0.0F;
        float x = Math.max(0.0F, Math.min(1.0F,
                (value - edge0) / (edge1 - edge0)));
        return x * x * (3.0F - 2.0F * x);
    }

    private static void flush(MultiBufferSource buffers, RenderType type) {
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(type);
        }
    }

    private static Vec3 modelPointToWorld(BlockPos pos, Direction facing,
            double modelX, double modelY, double modelZ,
            double mountYOffset) {
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 right = direction(facing.getClockWise());
        Vec3 back = direction(facing.getOpposite());
        return center
                .add(right.scale(modelX / 16.0D))
                .add(0.0D, (modelY - 8.0D) / 16.0D + mountYOffset,
                        0.0D)
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

    private record ProjectedHit(Vec3 position, Direction face) {
    }

    private record ProjectedSample(Vec3 position, Direction face,
            float u, float v) {
    }

    private record ProjectedTriangle(ProjectedSample a,
            ProjectedSample b, ProjectedSample c) {
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
