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
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.HashMap;
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
    private static final ResourceLocation BLOCK_GEO = id(
            "geo/block/alarm.geo.json");
    private static final ResourceLocation ITEM_GEO = id(
            "geo/item/alarm.geo.json");
    private static final ResourceLocation TEXTURE = id(
            "textures/block/alarm.png");
    private static final ResourceLocation GLOWMASK = id(
            "textures/block/alarm_glowmask.png");
    private static final ResourceLocation ANIMATION = id(
            "animations/block/alarm.animation.json");
    private static final ResourceLocation SPLASH = id(
            "textures/effect/alarm_light_splash.png");

    private static final double PROJECTOR_DISTANCE = 24.0D;
    private static final double PROJECTOR_DISTANCE_SQR =
            PROJECTOR_DISTANCE * PROJECTOR_DISTANCE;
    private static final double MIN_SPLASH_RADIUS = 0.28D;
    private static final double MAX_SPLASH_RADIUS = 2.35D;
    private static final double SPLASH_HALF_ANGLE =
            Math.toRadians(26.0D);
    private static final double PROJECTOR_OUTSET = 0.24D;
    private static final double WALL_PLANE_INSET = 0.0625D;
    private static final double RAY_OVERSHOOT = 0.06D;
    private static final double SURFACE_EPSILON = 0.0035D;
    private static final double MAX_PATCH_EDGE_SQR = 1.35D;
    private static final int RADIAL_RINGS = 6;
    private static final int ANGULAR_SAMPLES = 9;

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

    private static final class BlockModel
            extends GeoModel<AlarmModule.AlarmBlockEntity> {
        @Override
        public ResourceLocation getModelResource(
                AlarmModule.AlarmBlockEntity animatable) {
            return BLOCK_GEO;
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
                AnimationState<AlarmModule.AlarmBlockEntity> animationState) {
            super.setCustomAnimations(animatable, instanceId, animationState);

            // Like the Intercom, the authored lit/unlit meshes occupy exactly
            // the same space. Explicitly restore their state per block entity
            // so GeckoLib model reuse cannot leak a previous Alarm's scale.
            boolean active = animatable.getBlockState()
                    .getValue(AlarmModule.ACTIVE);
            forceLamp(getAnimationProcessor().getBone("lit"), active);
            forceLamp(getAnimationProcessor().getBone("unlit"), !active);
        }
    }

    private static final class BodyRenderer
            extends GeoBlockRenderer<AlarmModule.AlarmBlockEntity> {
        private BodyRenderer() {
            super(new BlockModel());
            addRenderLayer(new GeoRenderLayer<>(this) {
                @Override
                public void render(PoseStack poseStack,
                        AlarmModule.AlarmBlockEntity animatable,
                        BakedGeoModel bakedModel, RenderType renderType,
                        MultiBufferSource bufferSource, VertexConsumer buffer,
                        float partialTick, int packedLight, int packedOverlay) {
                    if (!animatable.getBlockState()
                            .getValue(AlarmModule.ACTIVE)) {
                        return;
                    }
                    RenderType emissive = RenderType.eyes(GLOWMASK);
                    getRenderer().reRender(bakedModel, poseStack, bufferSource,
                            animatable, emissive,
                            bufferSource.getBuffer(emissive), partialTick,
                            FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                            1.0F, 1.0F, 1.0F, 1.0F);
                }
            });
        }

        private float rotorAngle() {
            CoreGeoBone rotor = getGeoModel().getAnimationProcessor()
                    .getBone("rotor");
            return rotor == null ? 0.0F : rotor.getRotZ();
        }

        @Override
        public RenderType getRenderType(AlarmModule.AlarmBlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            // Block and item use isolated GeoModels, matching the Intercom fix
            // that prevents inventory render state from corrupting placed blocks.
            return RenderType.entityTranslucent(texture, true);
        }

        @Override
        public boolean shouldRenderOffScreen(
                AlarmModule.AlarmBlockEntity blockEntity) {
            return true;
        }
    }

    /**
     * Keep the GeckoLib body isolated from the custom world-space projection.
     * GeoBlockRenderer's 1.20.1 erased render signature cannot safely be
     * overridden with the concrete block-entity type.
     */
    public static final class BlockRenderer
            implements BlockEntityRenderer<AlarmModule.AlarmBlockEntity> {
        private final BodyRenderer body = new BodyRenderer();

        public BlockRenderer(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(AlarmModule.AlarmBlockEntity alarm,
                float partialTick, PoseStack poseStack,
                MultiBufferSource bufferSource, int packedLight,
                int packedOverlay) {
            body.render(alarm, partialTick, poseStack, bufferSource,
                    packedLight, packedOverlay);
            if (alarm.getBlockState().getValue(AlarmModule.ACTIVE)) {
                renderProjection(alarm, poseStack, bufferSource,
                        body.rotorAngle());
            }
        }

        @Override
        public boolean shouldRenderOffScreen(
                AlarmModule.AlarmBlockEntity blockEntity) {
            return true;
        }
    }

    private static void renderProjection(AlarmModule.AlarmBlockEntity alarm,
            PoseStack poseStack, MultiBufferSource buffers,
            float rotorAngle) {
        if (!(alarm.getLevel() instanceof ClientLevel level)) return;
        Minecraft minecraft = Minecraft.getInstance();
        Entity camera = minecraft.getCameraEntity();
        if (camera == null) return;

        Vec3 alarmCenter = Vec3.atCenterOf(alarm.getBlockPos());
        if (camera.position().distanceToSqr(alarmCenter)
                > PROJECTOR_DISTANCE_SQR) {
            return;
        }

        double cameraDistanceSqr = camera.position().distanceToSqr(alarmCenter);
        ProjectionCache cache = projection(level, alarm, camera,
                rotorAngle, cameraDistanceSqr <= 144.0D);
        if (cache == null) return;

        VertexConsumer consumer = buffers.getBuffer(
                RenderType.entityTranslucentEmissive(SPLASH));
        BlockPos originBlock = alarm.getBlockPos();

        for (int ring = 0; ring < RADIAL_RINGS - 1; ring++) {
            for (int slice = 0; slice < ANGULAR_SAMPLES - 1; slice++) {
                ProjectedHit a = cache.samples[ring][slice];
                ProjectedHit b = cache.samples[ring][slice + 1];
                ProjectedHit c = cache.samples[ring + 1][slice + 1];
                ProjectedHit d = cache.samples[ring + 1][slice];
                if (!compatible(a, b, c, d)) continue;

                float u0 = slice / (float) (ANGULAR_SAMPLES - 1);
                float u1 = (slice + 1)
                        / (float) (ANGULAR_SAMPLES - 1);
                float v0 = ring / (float) (RADIAL_RINGS - 1);
                float v1 = (ring + 1)
                        / (float) (RADIAL_RINGS - 1);
                emitProjectionQuad(consumer, poseStack, originBlock,
                        a, b, c, d, u0, v0, u1, v1);
            }
        }
    }

    private static ProjectionCache projection(ClientLevel level,
            AlarmModule.AlarmBlockEntity alarm, Entity camera,
            float rotorAngle, boolean perFrame) {
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

        // Read the actual GeckoLib bone after animation evaluation. The
        // reflector sits above the bulb at rest, while the emitted cone leaves
        // the opposite side, so zero rotation projects DOWN. The authored
        // animation is counter-clockwise and reaches -360 degrees in one second.
        double rotorRadians = rotorAngle;
        Vec3 tangent = right.scale(Math.sin(rotorRadians))
                .add(up.scale(-Math.cos(rotorRadians))).normalize();
        Vec3 fanSide = outward.cross(tangent);
        if (fanSide.lengthSqr() < 1.0E-6D) fanSide = right;
        fanSide = fanSide.normalize();

        Vec3 rotorCenter = modelPointToWorld(pos, facing,
                0.0D, 8.0D, 7.0D);
        Vec3 wallOrigin = rotorCenter.add(
                inward.scale(WALL_PLANE_INSET));

        // Project a tapered fan toward the real architectural surface. Each
        // sample aims at the support-wall plane, but the world's collision
        // geometry gets final say: walls, ceilings and pillars can receive the
        // patch; empty space receives nothing.
        ProjectedHit[][] samples =
                new ProjectedHit[RADIAL_RINGS][ANGULAR_SAMPLES];
        double tanHalfAngle = Math.tan(SPLASH_HALF_ANGLE);
        Vec3 rayStart = wallOrigin
                .add(tangent.scale(MIN_SPLASH_RADIUS))
                .add(outward.scale(PROJECTOR_OUTSET));

        for (int ring = 0; ring < RADIAL_RINGS; ring++) {
            double ringT = ring / (RADIAL_RINGS - 1.0D);
            double radius = MIN_SPLASH_RADIUS
                    + (MAX_SPLASH_RADIUS - MIN_SPLASH_RADIUS) * ringT;
            double halfWidth = 0.055D + radius * tanHalfAngle;
            Vec3 ringCenter = wallOrigin.add(tangent.scale(radius));

            for (int slice = 0; slice < ANGULAR_SAMPLES; slice++) {
                double u = -1.0D
                        + 2.0D * slice / (ANGULAR_SAMPLES - 1.0D);
                Vec3 intendedSurface = ringCenter.add(
                        fanSide.scale(halfWidth * u));
                Vec3 end = intendedSurface.add(
                        inward.scale(RAY_OVERSHOOT));
                samples[ring][slice] = cast(level, camera, pos,
                        rayStart, end);
            }
        }

        ProjectionCache fresh = new ProjectionCache(tick, samples);
        byPos.put(pos.immutable(), fresh);
        return fresh;
    }

    private static ProjectedHit cast(ClientLevel level, Entity context,
            BlockPos alarmPos, Vec3 start, Vec3 end) {
        BlockHitResult hit = level.clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context));
        if (hit.getType() != HitResult.Type.BLOCK
                || hit.getBlockPos().equals(alarmPos)) {
            return null;
        }

        Direction face = hit.getDirection();
        Vec3 normal = direction(face);
        Vec3 position = hit.getLocation().add(
                normal.scale(SURFACE_EPSILON));
        return new ProjectedHit(position, face);
    }

    private static boolean compatible(ProjectedHit a, ProjectedHit b,
            ProjectedHit c, ProjectedHit d) {
        if (a == null || b == null || c == null || d == null) return false;
        if (a.face != b.face || a.face != c.face || a.face != d.face) {
            return false;
        }
        return a.position.distanceToSqr(b.position) <= MAX_PATCH_EDGE_SQR
                && b.position.distanceToSqr(c.position) <= MAX_PATCH_EDGE_SQR
                && c.position.distanceToSqr(d.position) <= MAX_PATCH_EDGE_SQR
                && d.position.distanceToSqr(a.position)
                        <= MAX_PATCH_EDGE_SQR;
    }

    private static void emitProjectionQuad(VertexConsumer consumer,
            PoseStack poseStack, BlockPos blockOrigin,
            ProjectedHit a, ProjectedHit b, ProjectedHit c, ProjectedHit d,
            float u0, float v0, float u1, float v1) {
        Vec3 normal = direction(a.face);
        vertex(consumer, poseStack, local(a.position, blockOrigin),
                normal, u0, v0);
        vertex(consumer, poseStack, local(b.position, blockOrigin),
                normal, u1, v0);
        vertex(consumer, poseStack, local(c.position, blockOrigin),
                normal, u1, v1);
        vertex(consumer, poseStack, local(d.position, blockOrigin),
                normal, u0, v1);
    }

    private static void vertex(VertexConsumer consumer, PoseStack poseStack,
            Vec3 point, Vec3 normal, float u, float v) {
        consumer.vertex(poseStack.last().pose(),
                        (float) point.x, (float) point.y, (float) point.z)
                .color(255, 182, 78, 108)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(poseStack.last().normal(),
                        (float) normal.x, (float) normal.y,
                        (float) normal.z)
                .endVertex();
    }

    private static Vec3 modelPointToWorld(BlockPos pos, Direction facing,
            double modelX, double modelY, double modelZ) {
        Vec3 center = Vec3.atCenterOf(pos);
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

    private record ProjectedHit(Vec3 position, Direction face) {
    }

    private record ProjectionCache(long tick, ProjectedHit[][] samples) {
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
