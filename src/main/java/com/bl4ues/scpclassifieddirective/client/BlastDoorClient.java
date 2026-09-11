package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.blastdoor.BlastDoorModule;
import com.bl4ues.scpclassifieddirective.facility.blastdoor.BlastDoorStructure;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** GeckoLib body plus world-space adaptive wall mimics for the Blast Door. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BlastDoorClient {
    private static final ResourceLocation GEO = id(
            "geo/block/blast_door.geo.json");
    private static final ResourceLocation TEXTURE = id(
            "textures/block/blast_door.png");
    private static final ResourceLocation ANIMATION = id(
            "animations/block/blast_door.animation.json");

    private BlastDoorClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlastDoorModule.BLOCK_ENTITY.get(),
                BlockRenderer::new);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path);
    }

    private static void hideAuthoredMimics(GeoModel<?> model) {
        model.getAnimationProcessor().getBone("mimics").ifPresent(bone -> {
            bone.setHidden(true);
            bone.setScaleX(0.0F);
            bone.setScaleY(0.0F);
            bone.setScaleZ(0.0F);
        });
    }

    private static final class BlockModel
            extends GeoModel<BlastDoorModule.BlastDoorBlockEntity> {
        @Override
        public ResourceLocation getModelResource(
                BlastDoorModule.BlastDoorBlockEntity animatable) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(
                BlastDoorModule.BlastDoorBlockEntity animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(
                BlastDoorModule.BlastDoorBlockEntity animatable) {
            return ANIMATION;
        }

        @Override
        public void setCustomAnimations(
                BlastDoorModule.BlastDoorBlockEntity animatable,
                long instanceId,
                AnimationState<BlastDoorModule.BlastDoorBlockEntity> state) {
            super.setCustomAnimations(animatable, instanceId, state);
            hideAuthoredMimics(this);
        }
    }

    private static final class BodyRenderer
            extends GeoBlockRenderer<BlastDoorModule.BlastDoorBlockEntity> {
        private BodyRenderer() {
            super(new BlockModel());
        }

        @Override
        public RenderType getRenderType(
                BlastDoorModule.BlastDoorBlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }

        @Override
        public boolean shouldRenderOffScreen(
                BlastDoorModule.BlastDoorBlockEntity blockEntity) {
            return true;
        }
    }

    public static final class BlockRenderer
            implements BlockEntityRenderer<BlastDoorModule.BlastDoorBlockEntity> {
        private final BodyRenderer body = new BodyRenderer();

        public BlockRenderer(BlockEntityRendererProvider.Context context) {
        }

        @Override
        public void render(BlastDoorModule.BlastDoorBlockEntity door,
                float partialTick, PoseStack poseStack,
                MultiBufferSource bufferSource, int packedLight,
                int packedOverlay) {
            body.render(door, partialTick, poseStack, bufferSource,
                    packedLight, packedOverlay);
            renderMimic(door, poseStack, bufferSource, false);
            renderMimic(door, poseStack, bufferSource, true);
        }

        @Override
        public boolean shouldRenderOffScreen(
                BlastDoorModule.BlastDoorBlockEntity blockEntity) {
            return true;
        }
    }

    private static void renderMimic(
            BlastDoorModule.BlastDoorBlockEntity door,
            PoseStack poseStack, MultiBufferSource buffers,
            boolean rightSide) {
        Level level = door.getLevel();
        if (level == null) return;
        BlockState doorState = door.getBlockState();
        if (!BlastDoorModule.isController(doorState)) return;

        Direction facing = doorState.getValue(BlastDoorModule.FACING);
        BlockPos sourcePos = BlastDoorStructure.mimicSource(
                door.getBlockPos(), facing, rightSide);
        if (!level.hasChunkAt(sourcePos)) return;
        BlockState sourceState = level.getBlockState(sourcePos);
        if (sourceState.isAir()
                || !sourceState.isSolidRender(level, sourcePos)) {
            return;
        }

        BakedModel model = Minecraft.getInstance().getBlockRenderer()
                .getBlockModel(sourceState);
        int light = net.minecraft.client.renderer.LevelRenderer.getLightColor(
                level, sourcePos);
        VertexConsumer consumer = buffers.getBuffer(
                RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        double x1 = rightSide ? 32.0D : -40.0D;
        double x2 = rightSide ? 40.0D : -32.0D;
        double y1 = 40.0D;
        double y2 = 48.0D;
        double z1 = -8.0D;
        double z2 = 8.0D;

        Direction right = facing.getClockWise();
        Direction forward = facing.getOpposite();

        TextureAtlasSprite front = sprite(model, sourceState, forward,
                sourcePos);
        TextureAtlasSprite back = sprite(model, sourceState,
                forward.getOpposite(), sourcePos);
        TextureAtlasSprite outer = sprite(model, sourceState,
                rightSide ? right : right.getOpposite(), sourcePos);
        TextureAtlasSprite inner = sprite(model, sourceState,
                rightSide ? right.getOpposite() : right, sourcePos);
        TextureAtlasSprite up = sprite(model, sourceState, Direction.UP,
                sourcePos);
        TextureAtlasSprite down = sprite(model, sourceState, Direction.DOWN,
                sourcePos);

        float halfU0 = rightSide ? 8.0F : 0.0F;
        float halfU1 = rightSide ? 16.0F : 8.0F;

        emitFace(consumer, matrix, normalMatrix,
                point(facing, x1, y1, z2), point(facing, x2, y1, z2),
                point(facing, x2, y2, z2), point(facing, x1, y2, z2),
                forward, front, halfU0, 0.0F, halfU1, 8.0F, light);
        emitFace(consumer, matrix, normalMatrix,
                point(facing, x2, y1, z1), point(facing, x1, y1, z1),
                point(facing, x1, y2, z1), point(facing, x2, y2, z1),
                forward.getOpposite(), back, halfU0, 0.0F,
                halfU1, 8.0F, light);

        if (rightSide) {
            emitFace(consumer, matrix, normalMatrix,
                    point(facing, x2, y1, z2), point(facing, x2, y1, z1),
                    point(facing, x2, y2, z1), point(facing, x2, y2, z2),
                    right, outer, 0.0F, 0.0F, 16.0F, 8.0F, light);
            emitFace(consumer, matrix, normalMatrix,
                    point(facing, x1, y1, z1), point(facing, x1, y1, z2),
                    point(facing, x1, y2, z2), point(facing, x1, y2, z1),
                    right.getOpposite(), inner, 0.0F, 0.0F,
                    16.0F, 8.0F, light);
        } else {
            emitFace(consumer, matrix, normalMatrix,
                    point(facing, x1, y1, z1), point(facing, x1, y1, z2),
                    point(facing, x1, y2, z2), point(facing, x1, y2, z1),
                    right.getOpposite(), outer, 0.0F, 0.0F,
                    16.0F, 8.0F, light);
            emitFace(consumer, matrix, normalMatrix,
                    point(facing, x2, y1, z2), point(facing, x2, y1, z1),
                    point(facing, x2, y2, z1), point(facing, x2, y2, z2),
                    right, inner, 0.0F, 0.0F, 16.0F, 8.0F, light);
        }

        emitFace(consumer, matrix, normalMatrix,
                point(facing, x1, y2, z2), point(facing, x2, y2, z2),
                point(facing, x2, y2, z1), point(facing, x1, y2, z1),
                Direction.UP, up, halfU0, 0.0F, halfU1, 16.0F, light);
        emitFace(consumer, matrix, normalMatrix,
                point(facing, x1, y1, z1), point(facing, x2, y1, z1),
                point(facing, x2, y1, z2), point(facing, x1, y1, z2),
                Direction.DOWN, down, halfU0, 0.0F,
                halfU1, 16.0F, light);
    }

    private static TextureAtlasSprite sprite(BakedModel model,
            BlockState state, Direction face, BlockPos sourcePos) {
        RandomSource random = RandomSource.create(
                sourcePos.asLong() ^ face.ordinal() * 31L);
        for (BakedQuad quad : model.getQuads(state, face, random)) {
            return quad.getSprite();
        }
        random.setSeed(sourcePos.asLong() ^ face.ordinal() * 31L);
        for (BakedQuad quad : model.getQuads(state, null, random)) {
            if (quad.getDirection() == face) return quad.getSprite();
        }
        return model.getParticleIcon();
    }

    private static Vec3 point(Direction facing, double modelX,
            double modelY, double modelZ) {
        Direction right = facing.getClockWise();
        Direction forward = facing.getOpposite();
        return new Vec3(
                0.5D + right.getStepX() * modelX / 16.0D
                        + forward.getStepX() * modelZ / 16.0D,
                modelY / 16.0D,
                0.5D + right.getStepZ() * modelX / 16.0D
                        + forward.getStepZ() * modelZ / 16.0D);
    }

    private static void emitFace(VertexConsumer consumer, Matrix4f pose,
            Matrix3f normalMatrix, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
            Direction normal, TextureAtlasSprite sprite,
            float u0Pixels, float v0Pixels, float u1Pixels, float v1Pixels,
            int light) {
        float u0 = sprite.getU(u0Pixels);
        float u1 = sprite.getU(u1Pixels);
        float v0 = sprite.getV(v0Pixels);
        float v1 = sprite.getV(v1Pixels);
        vertex(consumer, pose, normalMatrix, a, normal, u0, v1, light);
        vertex(consumer, pose, normalMatrix, b, normal, u1, v1, light);
        vertex(consumer, pose, normalMatrix, c, normal, u1, v0, light);
        vertex(consumer, pose, normalMatrix, d, normal, u0, v0, light);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose,
            Matrix3f normalMatrix, Vec3 point, Direction normal,
            float u, float v, int light) {
        consumer.vertex(pose, (float) point.x, (float) point.y,
                        (float) point.z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normalMatrix, normal.getStepX(),
                        normal.getStepY(), normal.getStepZ())
                .endVertex();
    }

    private static final class ItemModel
            extends GeoModel<BlastDoorModule.BlastDoorItem> {
        @Override
        public ResourceLocation getModelResource(
                BlastDoorModule.BlastDoorItem animatable) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(
                BlastDoorModule.BlastDoorItem animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(
                BlastDoorModule.BlastDoorItem animatable) {
            return ANIMATION;
        }

        @Override
        public void setCustomAnimations(BlastDoorModule.BlastDoorItem animatable,
                long instanceId,
                AnimationState<BlastDoorModule.BlastDoorItem> state) {
            super.setCustomAnimations(animatable, instanceId, state);
            hideAuthoredMimics(this);
        }
    }

    public static final class ItemRenderer
            extends GeoItemRenderer<BlastDoorModule.BlastDoorItem> {
        public ItemRenderer() {
            super(new ItemModel());
        }

        @Override
        public RenderType getRenderType(
                BlastDoorModule.BlastDoorItem animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            return RenderType.entityCutoutNoCull(texture);
        }
    }
}
