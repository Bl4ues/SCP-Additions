package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.alarm.AlarmModule;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
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

import org.joml.Matrix4f;

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
    private static final ResourceLocation ANIMATION = id(
            "animations/block/alarm.animation.json");

    private static final double PROJECTOR_DISTANCE = 24.0D;
    private static final double PROJECTOR_DISTANCE_SQR =
            PROJECTOR_DISTANCE * PROJECTOR_DISTANCE;
    private static final double MIN_SPLASH_RADIUS = 0.045D;
    private static final double MAX_SPLASH_RADIUS = 3.05D;
    private static final double PROJECTOR_OUTSET = 0.30D;
    private static final double WALL_PLANE_INSET = 0.0625D;
    private static final double RAY_OVERSHOOT = 0.05D;
    private static final double SURFACE_EPSILON = 0.0040D;
    private static final double MAX_TRIANGLE_EDGE_SQR = 0.30D;
    private static final int RADIAL_RINGS = 13;
    private static final int ANGULAR_SAMPLES = 11;
    private static final double PER_FRAME_PROJECTOR_DISTANCE_SQR = 144.0D;

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
        private final LampRenderer lamp = new LampRenderer(false);
        private final LampRenderer lampGlow = new LampRenderer(true);
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

            poseStack.pushPose();
            poseStack.translate(0.0D, mountYOffset, 0.0D);

            base.render(alarm, partialTick, poseStack, bufferSource,
                    packedLight, packedOverlay);
            flush(bufferSource, RenderType.entityCutoutNoCull(TEXTURE));

            if (active) {
                lamp.render(alarm, partialTick, poseStack, bufferSource,
                        FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                flush(bufferSource,
                        RenderType.entityTranslucentEmissive(TEXTURE));

                lampGlow.render(alarm, partialTick, poseStack, bufferSource,
                        FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                flush(bufferSource, RenderType.eyes(GLOWMASK));
            }

            // Glass is deliberately last so the real emissive cube is seen
            // through, and tinted by, the authored translucent orange shell.
            cover.render(alarm, partialTick, poseStack, bufferSource,
                    packedLight, packedOverlay);
            flush(bufferSource, RenderType.entityTranslucent(TEXTURE, true));
            poseStack.popPose();

            if (active) {
                renderProjection(alarm, poseStack, bufferSource,
                        rotorAngle(alarm, partialTick), mountYOffset);
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
        if (cache == null) return;

        // Flush queued entity buffers before temporarily switching to an
        // immediate POSITION_COLOR pass. The projected light is deliberately
        // independent of Minecraft's lightmap, so it stays luminous in darkness
        // even without a shader pack.
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch();
        }

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().getBuilder();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        builder.begin(VertexFormat.Mode.TRIANGLES,
                DefaultVertexFormat.POSITION_COLOR);

        BlockPos origin = alarm.getBlockPos();
        for (int ring = 0; ring < RADIAL_RINGS; ring++) {
            float v = ring / (float) (RADIAL_RINGS - 1);
            for (int slice = 0; slice < ANGULAR_SAMPLES; slice++) {
                float u = slice / (float) (ANGULAR_SAMPLES - 1);
                ProjectedHit hit = cache.samples[ring][slice];
                if (hit != null) {
                    projectionSplat(builder, matrix, origin, hit, u, v);
                }
            }
        }

        BufferUploader.drawWithShader(builder.end());

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
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
        Vec3 wallOrigin = rotorCenter.add(inward.scale(WALL_PLANE_INSET));
        Vec3 rayStart = wallOrigin
                .add(tangent.scale(MIN_SPLASH_RADIUS))
                .add(outward.scale(PROJECTOR_OUTSET));

        ProjectedHit[][] samples =
                new ProjectedHit[RADIAL_RINGS][ANGULAR_SAMPLES];

        for (int ring = 0; ring < RADIAL_RINGS; ring++) {
            double t = ring / (RADIAL_RINGS - 1.0D);
            double radius = MIN_SPLASH_RADIUS
                    + (MAX_SPLASH_RADIUS - MIN_SPLASH_RADIUS)
                    * Math.pow(t, 1.03D);

            // Rounded beacon footprint: narrow at the lamp, fattest around
            // two-thirds of the throw, then gently narrows into the soft cap.
            double lobe = Math.sin(Math.PI * 0.78D * t);
            lobe = Math.pow(Math.max(0.0D, lobe), 0.72D);
            double halfWidth = 0.030D + 0.93D * lobe;
            Vec3 ringCenter = wallOrigin.add(tangent.scale(radius));

            for (int slice = 0; slice < ANGULAR_SAMPLES; slice++) {
                double u = -1.0D
                        + 2.0D * slice / (ANGULAR_SAMPLES - 1.0D);
                Vec3 intendedSurface = ringCenter.add(
                        fanSide.scale(halfWidth * u));
                Vec3 end = intendedSurface.add(inward.scale(RAY_OVERSHOOT));
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

    /**
     * Draw one tiny radial light footprint directly on the face hit by that
     * ray. Neighbouring footprints overlap additively. Unlike the old connected
     * triangle sheet there is no topology to tear when one ray moves from a
     * wall to a ceiling, so rotation cannot expose diagonal cuts or missing
     * checkerboard cells at folds.
     */
    private static void projectionSplat(BufferBuilder builder,
            Matrix4f matrix, BlockPos blockOrigin, ProjectedHit hit,
            float u, float v) {
        int centerAlpha = projectionAlpha(u, v);
        if (centerAlpha <= 0) return;

        Vec3 normal = direction(hit.face);
        Vec3 axisA;
        Vec3 axisB;
        if (hit.face.getAxis() == Direction.Axis.Y) {
            axisA = new Vec3(1.0D, 0.0D, 0.0D);
            axisB = new Vec3(0.0D, 0.0D, 1.0D);
        } else if (hit.face.getAxis() == Direction.Axis.X) {
            axisA = new Vec3(0.0D, 1.0D, 0.0D);
            axisB = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            axisA = new Vec3(1.0D, 0.0D, 0.0D);
            axisB = new Vec3(0.0D, 1.0D, 0.0D);
        }

        // Slightly larger farther from the beacon, matching the increasingly
        // diffuse footprint visible in the reference while keeping every splat
        // small enough to respect nearby architectural edges.
        double radius = 0.145D + 0.075D * v;
        Vec3 center = local(hit.position, blockOrigin);

        for (int segment = 0; segment < 8; segment++) {
            double angle0 = Math.PI * 2.0D * segment / 8.0D;
            double angle1 = Math.PI * 2.0D * (segment + 1) / 8.0D;
            Vec3 p0 = center.add(axisA.scale(Math.cos(angle0) * radius))
                    .add(axisB.scale(Math.sin(angle0) * radius));
            Vec3 p1 = center.add(axisA.scale(Math.cos(angle1) * radius))
                    .add(axisB.scale(Math.sin(angle1) * radius));

            projectionColorVertex(builder, matrix, center, centerAlpha);
            projectionColorVertex(builder, matrix, p0, 0);
            projectionColorVertex(builder, matrix, p1, 0);
        }
    }

    private static int projectionAlpha(float u, float v) {
        float lateral = Math.abs(u * 2.0F - 1.0F);
        float edgeFade = 1.0F - smoothStep(0.70F, 1.0F, lateral);
        float endFade = 1.0F - smoothStep(0.74F, 1.0F, v);

        // Reference profile: a compact hot root, a deliberately weak middle,
        // and brighter shoulders just inside the feathered boundary.
        float nearHot = 0.070F * (float) Math.exp(-v / 0.105F);
        float body = 0.012F + 0.008F * (1.0F - v);
        float shoulder = 0.032F * (float) Math.exp(
                -Math.pow((lateral - 0.66F) / 0.18F, 2.0D))
                * (0.72F + 0.28F * (1.0F - v));
        float middleDip = 1.0F - 0.44F
                * (float) Math.exp(-Math.pow((v - 0.43F) / 0.24F, 2.0D))
                * (float) Math.exp(-Math.pow(lateral / 0.47F, 2.0D));

        float opacity = (nearHot + body + shoulder)
                * edgeFade * endFade * middleDip;
        return Math.max(0, Math.min(30,
                Math.round(opacity * 255.0F)));
    }

    private static void projectionColorVertex(BufferBuilder builder,
            Matrix4f matrix, Vec3 point, int alpha) {
        builder.vertex(matrix, (float) point.x,
                        (float) point.y, (float) point.z)
                .color(255, 187, 92, alpha)
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
