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
    private static final ResourceLocation SOURCE_GLOW = id(
            "textures/effect/alarm_source_glow.png");

    private static final double PROJECTOR_DISTANCE = 24.0D;
    private static final double PROJECTOR_DISTANCE_SQR =
            PROJECTOR_DISTANCE * PROJECTOR_DISTANCE;
    private static final double MIN_SPLASH_RADIUS = 0.12D;
    private static final double MAX_SPLASH_RADIUS = 3.75D;
    private static final double SPLASH_HALF_ANGLE =
            Math.toRadians(35.0D);
    private static final double PROJECTOR_OUTSET = 0.24D;
    private static final double WALL_PLANE_INSET = 0.0625D;
    private static final double RAY_OVERSHOOT = 0.06D;
    private static final double SURFACE_EPSILON = 0.0035D;
    private static final int RADIAL_RINGS = 9;
    private static final int ANGULAR_SAMPLES = 15;
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

            // Do not trust GeckoLib's controller clock for the rotor after a
            // client pause/resume. The visible rotor and projected light share
            // this deterministic world-time phase instead, so both resume
            // cleanly and remain locked to the authored one-second CCW turn.
            CoreGeoBone rotor = getAnimationProcessor().getBone("rotor");
            if (rotor != null) {
                float phase = active
                        ? animatable.projectionPhase(
                                animationState.getPartialTick())
                        : 0.0F;
                rotor.setRotZ((float) (-phase * Math.PI * 2.0D));
            }
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
            double mountYOffset = alarm.getLevel() == null ? 0.0D
                    : AlarmModule.visualYOffset(alarm.getLevel(),
                            alarm.getBlockPos(), alarm.getBlockState());

            poseStack.pushPose();
            poseStack.translate(0.0D, mountYOffset, 0.0D);
            body.render(alarm, partialTick, poseStack, bufferSource,
                    packedLight, packedOverlay);
            poseStack.popPose();

            if (alarm.getBlockState().getValue(AlarmModule.ACTIVE)) {
                renderSourceGlow(alarm, poseStack, bufferSource,
                        mountYOffset);
                renderProjection(alarm, poseStack, bufferSource,
                        body.rotorAngle(), mountYOffset);
            }
        }

        @Override
        public boolean shouldRenderOffScreen(
                AlarmModule.AlarmBlockEntity blockEntity) {
            return true;
        }
    }

    private static void renderSourceGlow(
            AlarmModule.AlarmBlockEntity alarm, PoseStack poseStack,
            MultiBufferSource buffers, double mountYOffset) {
        if (!(alarm.getLevel() instanceof ClientLevel)) return;
        Direction facing = alarm.getBlockState().getValue(AlarmModule.FACING);
        Vec3 normal = direction(facing);
        Vec3 right = direction(facing.getClockWise());
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 center = modelPointToWorld(alarm.getBlockPos(), facing,
                0.0D, 8.0D, 8.0D, mountYOffset)
                .add(normal.scale(SURFACE_EPSILON * 1.5D));
        double half = 0.42D;

        Vec3 topLeft = center.add(right.scale(-half)).add(up.scale(half));
        Vec3 topRight = center.add(right.scale(half)).add(up.scale(half));
        Vec3 bottomRight = center.add(right.scale(half))
                .add(up.scale(-half));
        Vec3 bottomLeft = center.add(right.scale(-half))
                .add(up.scale(-half));

        VertexConsumer consumer = buffers.getBuffer(
                RenderType.eyes(SOURCE_GLOW));
        BlockPos origin = alarm.getBlockPos();
        sourceGlowVertex(consumer, poseStack,
                local(topLeft, origin), normal, 0.0F, 0.0F);
        sourceGlowVertex(consumer, poseStack,
                local(topRight, origin), normal, 1.0F, 0.0F);
        sourceGlowVertex(consumer, poseStack,
                local(bottomRight, origin), normal, 1.0F, 1.0F);
        sourceGlowVertex(consumer, poseStack,
                local(bottomLeft, origin), normal, 0.0F, 1.0F);
    }

    private static void sourceGlowVertex(VertexConsumer consumer,
            PoseStack poseStack, Vec3 point, Vec3 normal, float u, float v) {
        consumer.vertex(poseStack.last().pose(),
                        (float) point.x, (float) point.y, (float) point.z)
                .color(255, 184, 76, 72)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(poseStack.last().normal(),
                        (float) normal.x, (float) normal.y,
                        (float) normal.z)
                .endVertex();
    }

    private static void renderProjection(AlarmModule.AlarmBlockEntity alarm,
            PoseStack poseStack, MultiBufferSource buffers,
            float rotorAngle, double mountYOffset) {
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
                rotorAngle, mountYOffset,
                cameraDistanceSqr <= PER_FRAME_PROJECTOR_DISTANCE_SQR);
        if (cache == null) return;

        VertexConsumer soft = buffers.getBuffer(
                RenderType.entityTranslucentEmissive(SPLASH));
        VertexConsumer bloom = buffers.getBuffer(RenderType.eyes(SPLASH));
        BlockPos originBlock = alarm.getBlockPos();

        for (int ring = 0; ring < RADIAL_RINGS; ring++) {
            for (int slice = 0; slice < ANGULAR_SAMPLES; slice++) {
                ProjectedHit hit = cache.samples[ring][slice];
                if (hit == null || hit.intensity <= 0.001F) continue;

                // Overlapping radial splats avoid the hard polygon boundary of
                // the old connected fan. At corners each ray simply paints the
                // surface it actually hit, so wall-to-ceiling folds stay soft.
                renderSplat(soft, poseStack, originBlock, hit,
                        Math.min(1.0F, hit.intensity * 0.42F), false);
                renderSplat(bloom, poseStack, originBlock, hit,
                        Math.min(1.0F, hit.intensity * 0.24F), true);
            }
        }
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

        // The visible reflector and projector use the same evaluated rotor bone.
        // In the dormant pose the reflector points up, so its opposite-facing
        // light exits down. Negative Z rotation is the authored CCW one-second
        // revolution.
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

        ProjectedHit[][] samples =
                new ProjectedHit[RADIAL_RINGS][ANGULAR_SAMPLES];
        double tanHalfAngle = Math.tan(SPLASH_HALF_ANGLE);
        double radialStep = (MAX_SPLASH_RADIUS - MIN_SPLASH_RADIUS)
                / (RADIAL_RINGS - 1.0D);

        for (int ring = 0; ring < RADIAL_RINGS; ring++) {
            double ringT = ring / (RADIAL_RINGS - 1.0D);
            double radius = MIN_SPLASH_RADIUS
                    + (MAX_SPLASH_RADIUS - MIN_SPLASH_RADIUS) * ringT;
            double halfWidth = 0.045D + radius * tanHalfAngle;
            Vec3 ringCenter = wallOrigin.add(tangent.scale(radius));

            for (int slice = 0; slice < ANGULAR_SAMPLES; slice++) {
                double u = -1.0D
                        + 2.0D * slice / (ANGULAR_SAMPLES - 1.0D);
                double side = Math.abs(u);

                // Match the reference profile: a hot root, weak body, a subtle
                // brighter ridge near both outer edges, then a very soft fade.
                double hotRoot = 0.16D + 0.84D
                        * Math.exp(-Math.pow(ringT / 0.15D, 2.0D));
                double edgeRidge = 0.68D + 0.78D
                        * Math.exp(-Math.pow((side - 0.70D) / 0.19D, 2.0D));
                double sideFade = 1.0D - smoothstep(0.78D, 1.0D, side);
                double farFade = 1.0D - smoothstep(0.70D, 1.0D, ringT);
                float intensity = (float) Math.max(0.0D, Math.min(1.0D,
                        hotRoot * edgeRidge * sideFade * farFade));

                Vec3 intendedSurface = ringCenter.add(
                        fanSide.scale(halfWidth * u));
                Vec3 end = intendedSurface.add(
                        inward.scale(RAY_OVERSHOOT));
                ProjectedHit hit = cast(level, camera, pos,
                        rayStart, end);
                if (hit == null) continue;

                double angularStep = (2.0D * halfWidth)
                        / (ANGULAR_SAMPLES - 1.0D);
                float halfSize = (float) Math.min(0.36D,
                        Math.max(0.16D,
                                Math.max(radialStep, angularStep) * 0.72D));
                samples[ring][slice] = new ProjectedHit(
                        hit.position, hit.face, intensity, halfSize);
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
        return new ProjectedHit(position, face, 1.0F, 0.2F);
    }

    private static double smoothstep(double edge0, double edge1,
            double value) {
        if (edge0 == edge1) return value < edge0 ? 0.0D : 1.0D;
        double t = Math.max(0.0D, Math.min(1.0D,
                (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0D - 2.0D * t);
    }

    private static void renderSplat(VertexConsumer consumer,
            PoseStack poseStack, BlockPos blockOrigin, ProjectedHit hit,
            float passIntensity, boolean bloomPass) {
        Vec3 normal = direction(hit.face);
        Vec3 axisU;
        Vec3 axisV;

        if (hit.face.getAxis() == Direction.Axis.Y) {
            axisU = new Vec3(1.0D, 0.0D, 0.0D);
            axisV = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            axisV = new Vec3(0.0D, 1.0D, 0.0D);
            axisU = axisV.cross(normal).normalize();
        }

        double half = hit.halfSize;
        Vec3 a = hit.position.add(axisU.scale(-half))
                .add(axisV.scale(half));
        Vec3 b = hit.position.add(axisU.scale(half))
                .add(axisV.scale(half));
        Vec3 d = hit.position.add(axisU.scale(-half))
                .add(axisV.scale(-half));
        Vec3 c = hit.position.add(axisU.scale(half))
                .add(axisV.scale(-half));

        int alpha = Math.max(0, Math.min(255, Math.round(
                passIntensity * (bloomPass ? 120.0F : 150.0F))));
        int red = bloomPass ? 255 : 255;
        int green = bloomPass ? 198 : 188;
        int blue = bloomPass ? 104 : 82;

        splatVertex(consumer, poseStack, local(a, blockOrigin),
                normal, 0.0F, 0.0F, red, green, blue, alpha);
        splatVertex(consumer, poseStack, local(b, blockOrigin),
                normal, 1.0F, 0.0F, red, green, blue, alpha);
        splatVertex(consumer, poseStack, local(c, blockOrigin),
                normal, 1.0F, 1.0F, red, green, blue, alpha);
        splatVertex(consumer, poseStack, local(d, blockOrigin),
                normal, 0.0F, 1.0F, red, green, blue, alpha);
    }

    private static void splatVertex(VertexConsumer consumer,
            PoseStack poseStack, Vec3 point, Vec3 normal,
            float u, float v, int red, int green, int blue, int alpha) {
        consumer.vertex(poseStack.last().pose(),
                        (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT)
                .normal(poseStack.last().normal(),
                        (float) normal.x, (float) normal.y,
                        (float) normal.z)
                .endVertex();
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

    private record ProjectedHit(Vec3 position, Direction face,
            float intensity, float halfSize) {
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
