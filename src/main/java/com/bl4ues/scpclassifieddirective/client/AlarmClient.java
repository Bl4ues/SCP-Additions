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

    private static final double PROJECTION_RANGE = 4.0D;
    private static final double PROJECTOR_DISTANCE = 24.0D;
    private static final double PROJECTOR_DISTANCE_SQR =
            PROJECTOR_DISTANCE * PROJECTOR_DISTANCE;
    private static final double WALL_BIAS = 0.12D;
    private static final double CONE_SPREAD = Math.tan(Math.toRadians(22.0D));
    private static final double SURFACE_EPSILON = 0.0035D;
    private static final double MAX_PATCH_EDGE_SQR = 2.25D;
    private static final int GRID = 4;

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

    public static final class BlockRenderer
            extends GeoBlockRenderer<AlarmModule.AlarmBlockEntity> {
        public BlockRenderer(BlockEntityRendererProvider.Context context) {
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

        @Override
        public void render(AlarmModule.AlarmBlockEntity alarm,
                float partialTick, PoseStack poseStack,
                MultiBufferSource bufferSource, int packedLight,
                int packedOverlay) {
            super.render(alarm, partialTick, poseStack, bufferSource,
                    packedLight, packedOverlay);
            if (alarm.getBlockState().getValue(AlarmModule.ACTIVE)) {
                CoreGeoBone rotor = getGeoModel().getAnimationProcessor()
                        .getBone("rotor");
                float rotorAngle = rotor == null ? 0.0F : rotor.getRotZ();
                renderProjection(alarm, poseStack, bufferSource,
                        rotorAngle);
            }
        }

        @Override
        public RenderType getRenderType(AlarmModule.AlarmBlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            // Block and item use isolated GeoModels, matching the Intercom fix
            // that prevents inventory render state from corrupting placed blocks.
            return RenderType.entityCutoutNoCull(texture);
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

        for (int row = 0; row < GRID - 1; row++) {
            for (int col = 0; col < GRID - 1; col++) {
                ProjectedHit a = cache.samples[row][col];
                ProjectedHit b = cache.samples[row][col + 1];
                ProjectedHit c = cache.samples[row + 1][col + 1];
                ProjectedHit d = cache.samples[row + 1][col];
                if (!compatible(a, b, c, d)) continue;

                float u0 = col / (float) (GRID - 1);
                float u1 = (col + 1) / (float) (GRID - 1);
                float v0 = row / (float) (GRID - 1);
                float v1 = (row + 1) / (float) (GRID - 1);
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

        // Read the actual GeckoLib bone after animation evaluation. The authored
        // rotor pivot is [0, 8, 7], its reflector sits above the bulb at rest,
        // and the beam leaves the opposite side, so zero rotation points DOWN.
        // This makes projector and model share one source of truth even if the
        // authored animation is retimed later.
        double rotorRadians = rotorAngle;
        Vec3 tangent = right.scale(Math.sin(rotorRadians))
                .add(up.scale(-Math.cos(rotorRadians))).normalize();

        Vec3 rotorCenter = modelPointToWorld(pos, facing,
                0.0D, 8.0D, 7.0D);
        // Start just beyond the compact housing in the current beam direction.
        // That prevents the Alarm from clipping its own projection.
        Vec3 start = rotorCenter.add(tangent.scale(0.14D))
                .add(outward.scale(0.025D));
        Vec3 beam = tangent.add(inward.scale(WALL_BIAS)).normalize();

        // Circular cone basis. One axis remains mostly in the wall plane while
        // the other includes wall-normal spread, allowing a patch to naturally
        // split onto a ceiling or corner when those surfaces are actually hit.
        Vec3 coneX = outward.cross(beam);
        if (coneX.lengthSqr() < 1.0E-6D) coneX = right;
        coneX = coneX.normalize();
        Vec3 coneY = beam.cross(coneX).normalize();

        ProjectedHit[][] samples = new ProjectedHit[GRID][GRID];
        for (int row = 0; row < GRID; row++) {
            double v = -1.0D + 2.0D * row / (GRID - 1.0D);
            for (int col = 0; col < GRID; col++) {
                double u = -1.0D + 2.0D * col / (GRID - 1.0D);
                Vec3 direction = beam
                        .add(coneX.scale(u * CONE_SPREAD))
                        .add(coneY.scale(v * CONE_SPREAD))
                        .normalize();
                samples[row][col] = cast(level, camera, pos,
                        start, direction);
            }
        }

        ProjectionCache fresh = new ProjectionCache(tick, samples);
        byPos.put(pos.immutable(), fresh);
        return fresh;
    }

    private static ProjectedHit cast(ClientLevel level, Entity context,
            BlockPos alarmPos, Vec3 start, Vec3 direction) {
        Vec3 end = start.add(direction.scale(PROJECTION_RANGE));
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
                .color(255, 105, 20, 225)
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
            return RenderType.entityCutoutNoCull(texture);
        }
    }
}
