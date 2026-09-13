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
    private static final ResourceLocation SPLASH_BLOOM = id(
            "textures/effect/alarm_light_bloom.png");
    private static final ResourceLocation SOURCE_GLOW = id(
            "textures/effect/alarm_source_glow.png");

    private static final double PROJECTOR_DISTANCE = 24.0D;
    private static final double PROJECTOR_DISTANCE_SQR =
            PROJECTOR_DISTANCE * PROJECTOR_DISTANCE;
    private static final double MIN_SPLASH_RADIUS = 0.08D;
    private static final double MAX_SPLASH_RADIUS = 2.85D;
    private static final double SPLASH_HALF_ANGLE =
            Math.toRadians(24.5D);
    private static final double PROJECTOR_OUTSET = 0.20D;
    private static final double WALL_PLANE_INSET = 0.0625D;
    private static final double RAY_OVERSHOOT = 0.05D;
    private static final double SURFACE_EPSILON = 0.0035D;
    private static final double MAX_TRIANGLE_EDGE_SQR = 0.34D;
    private static final int RADIAL_RINGS = 11;
    private static final int ANGULAR_SAMPLES = 21;
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
                float rotorAngle = body.rotorAngle();
                renderLampEmissive(alarm, poseStack, bufferSource,
                        rotorAngle, mountYOffset);
                renderSourceGlow(alarm, poseStack, bufferSource,
                        mountYOffset);
                renderProjection(alarm, poseStack, bufferSource,
                        rotorAngle, mountYOffset);
            }
        }

        @Override
        public boolean shouldRenderOffScreen(
                AlarmModule.AlarmBlockEntity blockEntity) {
            return true;
        }
    }

    /**
     * Render the authored lit lens as its own emissive surface instead of
     * re-rendering the complete GeckoLib model with a coplanar glowmask pass.
     * Shader packs can move the two vertex paths by tiny amounts, causing the
     * latter to lose the depth test entirely. This quad samples the exact
     * north-face UV of the authored 'lit' cube from alarm_glowmask.png and is
     * nudged a fraction outward, so the bulb is unambiguously HDR/emissive.
     */
    private static void renderLampEmissive(
            AlarmModule.AlarmBlockEntity alarm, PoseStack poseStack,
            MultiBufferSource buffers, float rotorAngle,
            double mountYOffset) {
        Direction facing = alarm.getBlockState().getValue(AlarmModule.FACING);
        Vec3 normal = direction(facing);
        BlockPos origin = alarm.getBlockPos();

        Vec3 a = rotatedModelPointToWorld(origin, facing,
                -0.70D, 8.70D, 6.25D, rotorAngle, mountYOffset)
                .add(normal.scale(SURFACE_EPSILON * 2.0D));
        Vec3 b = rotatedModelPointToWorld(origin, facing,
                0.70D, 8.70D, 6.25D, rotorAngle, mountYOffset)
                .add(normal.scale(SURFACE_EPSILON * 2.0D));
        Vec3 d = rotatedModelPointToWorld(origin, facing,
                -0.70D, 7.30D, 6.25D, rotorAngle, mountYOffset)
                .add(normal.scale(SURFACE_EPSILON * 2.0D));
        Vec3 cPoint = rotatedModelPointToWorld(origin, facing,
                0.70D, 7.30D, 6.25D, rotorAngle, mountYOffset)
                .add(normal.scale(SURFACE_EPSILON * 2.0D));

        // Authored lit north face: UV [0,10] size [1.5,1.5] on a 32x32
        // Blockbench texture grid. The actual PNG is 64x64, but GeckoLib's
        // normalized UVs are still based on the declared 32x32 grid.
        float u0 = 0.0F;
        float v0 = 10.0F / 32.0F;
        float u1 = 1.5F / 32.0F;
        float v1 = 11.5F / 32.0F;

        RenderType emissive = RenderType.eyes(GLOWMASK);
        VertexConsumer consumer = buffers.getBuffer(emissive);
        lampVertex(consumer, poseStack, local(a, origin), normal, u0, v0);
        lampVertex(consumer, poseStack, local(b, origin), normal, u1, v0);
        lampVertex(consumer, poseStack, local(cPoint, origin), normal, u1, v1);
        lampVertex(consumer, poseStack, local(d, origin), normal, u0, v1);
        flush(buffers, emissive);
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
        // Only a tiny bloom around the lens itself. The rotating projected
        // splash is the visual focus; this merely sells the emitting source.
        double half = 0.115D;

        Vec3 topLeft = center.add(right.scale(-half)).add(up.scale(half));
        Vec3 topRight = center.add(right.scale(half)).add(up.scale(half));
        Vec3 bottomRight = center.add(right.scale(half))
                .add(up.scale(-half));
        Vec3 bottomLeft = center.add(right.scale(-half))
                .add(up.scale(-half));

        RenderType glowType =
                RenderType.entityTranslucentEmissive(SOURCE_GLOW);
        VertexConsumer consumer = buffers.getBuffer(glowType);
        BlockPos origin = alarm.getBlockPos();
        sourceGlowVertex(consumer, poseStack,
                local(topLeft, origin), normal, 0.0F, 0.0F);
        sourceGlowVertex(consumer, poseStack,
                local(topRight, origin), normal, 1.0F, 0.0F);
        sourceGlowVertex(consumer, poseStack,
                local(bottomRight, origin), normal, 1.0F, 1.0F);
        sourceGlowVertex(consumer, poseStack,
                local(bottomLeft, origin), normal, 0.0F, 1.0F);
        flush(buffers, glowType);
    }

    private static void sourceGlowVertex(VertexConsumer consumer,
            PoseStack poseStack, Vec3 point, Vec3 normal, float u, float v) {
        consumer.vertex(poseStack.last().pose(),
                        (float) point.x, (float) point.y, (float) point.z)
                .color(255, 179, 76, 58)
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

        // The main pass must itself be emissive. A normal translucent entity
        // pass becomes almost invisible in dark shader scenes, which is why the
        // previous revision appeared to delete the cone entirely.
        RenderType softType =
                RenderType.entityTranslucentEmissive(SPLASH);
        RenderType bloomType = RenderType.eyes(SPLASH_BLOOM);
        VertexConsumer soft = buffers.getBuffer(softType);
        VertexConsumer bloom = buffers.getBuffer(bloomType);
        BlockPos originBlock = alarm.getBlockPos();

        for (int ring = 0; ring < RADIAL_RINGS - 1; ring++) {
            for (int slice = 0; slice < ANGULAR_SAMPLES - 1; slice++) {
                ProjectedHit a = cache.samples[ring][slice];
                ProjectedHit b = cache.samples[ring][slice + 1];
                ProjectedHit cHit = cache.samples[ring + 1][slice + 1];
                ProjectedHit d = cache.samples[ring + 1][slice];

                float u0 = slice / (float) (ANGULAR_SAMPLES - 1);
                float u1 = (slice + 1)
                        / (float) (ANGULAR_SAMPLES - 1);
                float v0 = ring / (float) (RADIAL_RINGS - 1);
                float v1 = (ring + 1)
                        / (float) (RADIAL_RINGS - 1);

                // Triangles let the projection fold naturally from wall onto a
                // ceiling. Only the tiny triangle crossing the geometric seam
                // is omitted instead of chopping out an entire square patch.
                if (compatibleTriangle(a, b, cHit)) {
                    emitProjectionTriangle(soft, poseStack, originBlock,
                            a, b, cHit,
                            u0, v0, u1, v0, u1, v1,
                            255, 196, 112, 104);
                    emitProjectionTriangle(bloom, poseStack, originBlock,
                            a, b, cHit,
                            u0, v0, u1, v0, u1, v1,
                            92, 58, 20, 28);
                }
                if (compatibleTriangle(a, cHit, d)) {
                    emitProjectionTriangle(soft, poseStack, originBlock,
                            a, cHit, d,
                            u0, v0, u1, v1, u0, v1,
                            255, 196, 112, 104);
                    emitProjectionTriangle(bloom, poseStack, originBlock,
                            a, cHit, d,
                            u0, v0, u1, v1, u0, v1,
                            92, 58, 20, 28);
                }
            }
        }
        flush(buffers, softType);
        flush(buffers, bloomType);
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

        // The reflector and projector share the exact evaluated rotor angle.
        // At rest the opposite face of the reflector points down.
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
        double tanHalfAngle = Math.tan(SPLASH_HALF_ANGLE);

        for (int ring = 0; ring < RADIAL_RINGS; ring++) {
            double ringT = ring / (RADIAL_RINGS - 1.0D);
            double radius = MIN_SPLASH_RADIUS
                    + (MAX_SPLASH_RADIUS - MIN_SPLASH_RADIUS) * ringT;
            // Reference shape: already visible at the lamp, then widens
            // gradually rather than exploding into a floodlight.
            double halfWidth = 0.085D + radius * tanHalfAngle;
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

    private static boolean compatibleTriangle(ProjectedHit a,
            ProjectedHit b, ProjectedHit c) {
        if (a == null || b == null || c == null) return false;
        if (a.face != b.face || a.face != c.face) return false;
        return a.position.distanceToSqr(b.position)
                        <= MAX_TRIANGLE_EDGE_SQR
                && b.position.distanceToSqr(c.position)
                        <= MAX_TRIANGLE_EDGE_SQR
                && c.position.distanceToSqr(a.position)
                        <= MAX_TRIANGLE_EDGE_SQR;
    }

    private static void emitProjectionTriangle(VertexConsumer consumer,
            PoseStack poseStack, BlockPos blockOrigin,
            ProjectedHit a, ProjectedHit b, ProjectedHit c,
            float ua, float va, float ub, float vb, float uc, float vc,
            int red, int green, int blue, int alpha) {
        Vec3 normal = direction(a.face);
        projectionVertex(consumer, poseStack,
                local(a.position, blockOrigin), normal,
                ua, va, red, green, blue, alpha);
        projectionVertex(consumer, poseStack,
                local(b.position, blockOrigin), normal,
                ub, vb, red, green, blue, alpha);
        projectionVertex(consumer, poseStack,
                local(c.position, blockOrigin), normal,
                uc, vc, red, green, blue, alpha);
        // Entity render types consume quads. Degenerate the final corner to
        // keep triangle topology without inventing geometry across a fold.
        projectionVertex(consumer, poseStack,
                local(c.position, blockOrigin), normal,
                uc, vc, red, green, blue, alpha);
    }

    private static void projectionVertex(VertexConsumer consumer,
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

    private static Vec3 rotatedModelPointToWorld(BlockPos pos,
            Direction facing, double modelX, double modelY, double modelZ,
            float rotorAngle, double mountYOffset) {
        double dx = modelX;
        double dy = modelY - 8.0D;
        double cos = Math.cos(rotorAngle);
        double sin = Math.sin(rotorAngle);
        double rotatedX = dx * cos - dy * sin;
        double rotatedY = 8.0D + dx * sin + dy * cos;
        return modelPointToWorld(pos, facing, rotatedX, rotatedY,
                modelZ, mountYOffset);
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
