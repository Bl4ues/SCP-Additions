package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.blastdoor.BlastDoorModule;
import com.bl4ues.scpclassifieddirective.facility.blastdoor.BlastDoorStructure;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** GeckoLib body plus world-space adaptive wall mimics for the Blast Door. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BlastDoorClient {
    private static final ResourceLocation GEO = id(
            "geo/block/blast_door.geo.json");
    private static final ResourceLocation ITEM_GEO = id(
            "geo/item/blast_door.geo.json");
    private static final ResourceLocation TEXTURE = id(
            "textures/block/blast_door.png");
    private static final ResourceLocation ANIMATION = id(
            "animations/block/blast_door.animation.json");
    private static final double FLOOR_EPSILON = 0.1D / 16.0D;
    private static final double MIMIC_SURFACE = 7.95D;

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

    }

    private static final class BodyRenderer
            extends GeoBlockRenderer<BlastDoorModule.BlastDoorBlockEntity> {
        private BodyRenderer() {
            super(new BlockModel());
        }

        @Override
        public void preRender(PoseStack poseStack,
                BlastDoorModule.BlastDoorBlockEntity animatable,
                BakedGeoModel bakedModel, MultiBufferSource bufferSource,
                VertexConsumer buffer, boolean isReRender, float partialTick,
                int packedLight, int packedOverlay, float red, float green,
                float blue, float alpha) {
            // The same tiny isolation used by SCP-914 prevents the large
            // animated model from fighting the floor/outline depth pass.
            if (!isReRender) {
                poseStack.translate(0.0D, FLOOR_EPSILON, 0.0D);
            }
            super.preRender(poseStack, animatable, bakedModel, bufferSource,
                    buffer, isReRender, partialTick, packedLight,
                    packedOverlay, red, green, blue, alpha);
        }

        @Override
        public RenderType getRenderType(
                BlastDoorModule.BlastDoorBlockEntity animatable,
                ResourceLocation texture, MultiBufferSource bufferSource,
                float partialTick) {
            // Large animated GeckoLib blocks in this project use the
            // translucent entity path to avoid the selection/HUD depth
            // flicker seen with cutout rendering.
            return RenderType.entityTranslucent(texture, true);
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
        renderMimicLayer(door, poseStack, buffers, rightSide, false);
        renderMimicLayer(door, poseStack, buffers, rightSide, true);
    }

    private static void renderMimicLayer(
            BlastDoorModule.BlastDoorBlockEntity door,
            PoseStack poseStack, MultiBufferSource buffers,
            boolean rightSide, boolean upperLayer) {
        Level level = door.getLevel();
        if (level == null) return;
        BlockState doorState = door.getBlockState();
        if (!BlastDoorModule.isController(doorState)) return;

        Direction facing = doorState.getValue(BlastDoorModule.FACING);
        BlockPos sourcePos = BlastDoorStructure.mimicSource(
                door.getBlockPos(), facing, rightSide, upperLayer);
        if (!level.hasChunkAt(sourcePos)) return;

        BlockState sourceState = level.getBlockState(sourcePos);
        // Facility wall blocks are often authored/non-full models and therefore
        // legitimately report isSolidRender=false. The mimic cares about a
        // visible neighbouring wall surface, not vanilla's full-cube occlusion
        // flag, so do not discard those blocks.
        if (sourceState.isAir()
                || sourceState.getRenderShape() == RenderShape.INVISIBLE
                || BlastDoorModule.isStructureState(sourceState)) {
            return;
        }

        BakedModel model = Minecraft.getInstance().getBlockRenderer()
                .getBlockModel(sourceState);
        BlockPos targetPos = BlastDoorStructure.partPosition(
                door.getBlockPos(), facing, rightSide ? 2 : -2,
                upperLayer ? 3 : 2);
        int light = net.minecraft.client.renderer.LevelRenderer.getLightColor(
                level, targetPos);
        VertexConsumer consumer = buffers.getBuffer(Sheets.cutoutBlockSheet());
        PoseStack.Pose pose = poseStack.last();

        // The original authored corner mimic is only 8x8. The row above is
        // different: it must behave like a complete copycat wall block so the
        // facility wall remains sealed behind the sloped frame.
        double x1 = upperLayer
                ? (rightSide ? 24.0D : -40.0D)
                : (rightSide ? 32.0D : -40.0D);
        double x2 = upperLayer
                ? (rightSide ? 40.0D : -24.0D)
                : (rightSide ? 40.0D : -32.0D);
        double y1 = upperLayer ? 48.0D : 40.0D;
        double y2 = upperLayer ? 64.0D : 48.0D;

        Direction right = facing.getClockWise();
        Direction front = facing.getOpposite();

        // Preserve the exact orientation of the source block's baked face.
        // The outer horizontal half is copied on each side; the lower mimic
        // takes only the upper half of its source block. The new upper mimic
        // takes the complete block above and the metal frame masks the hidden
        // portion naturally in front of it.
        boolean rightAxisPositive =
                right == Direction.EAST || right == Direction.SOUTH;
        boolean takeHighHalf = rightSide == rightAxisPositive;
        float sourceH0 = upperLayer
                ? 0.0F : (takeHighHalf ? 0.5F : 0.0F);
        float sourceH1 = upperLayer
                ? 1.0F : (takeHighHalf ? 1.0F : 0.5F);
        float sourceV0 = upperLayer ? 0.0F : 0.5F;
        float sourceV1 = 1.0F;

        float hAtX1 = rightAxisPositive ? sourceH0 : sourceH1;
        float hAtX2 = rightAxisPositive ? sourceH1 : sourceH0;

        renderFrontBackFace(consumer, pose, model, sourceState, level,
                sourcePos, front, facing, x1, x2, y1, y2,
                hAtX1, hAtX2, sourceV0, sourceV1, light);

        Direction innerFace = rightSide
                ? right.getOpposite() : right;
        boolean forwardAxisPositive =
                front == Direction.EAST || front == Direction.SOUTH;
        float hAtBack = forwardAxisPositive ? 0.0F : 1.0F;
        float hAtFront = forwardAxisPositive ? 1.0F : 0.0F;
        double innerX = rightSide ? x1 : x2;
        renderInnerFace(consumer, pose, model, sourceState, level,
                sourcePos, facing, innerFace, innerX, y1, y2,
                hAtBack, hAtFront, sourceV0, sourceV1, light, rightSide);
    }

    private static void renderFrontBackFace(VertexConsumer consumer,
            PoseStack.Pose pose, BakedModel model, BlockState sourceState,
            Level level, BlockPos sourcePos, Direction front, Direction back,
            double x1, double x2, double y1, double y2,
            float hAtX1, float hAtX2, float v0, float v1, int light) {
        FaceUv frontUv = faceUv(model, sourceState, level, sourcePos,
                front, Math.min(hAtX1, hAtX2), Math.max(hAtX1, hAtX2),
                v0, v1);
        FaceUv backUv = faceUv(model, sourceState, level, sourcePos,
                back, Math.min(hAtX1, hAtX2), Math.max(hAtX1, hAtX2),
                v0, v1);
        if (frontUv != null) {
            Uv a = frontUv.sample(hAtX1, v0);
            Uv b = frontUv.sample(hAtX2, v0);
            Uv d = frontUv.sample(hAtX1, v1);
            Uv cc = frontUv.sample(hAtX2, v1);
            emitFace(consumer, pose,
                    point(back, x1, y1, MIMIC_SURFACE),
                    point(back, x2, y1, MIMIC_SURFACE),
                    point(back, x2, y2, MIMIC_SURFACE),
                    point(back, x1, y2, MIMIC_SURFACE),
                    front, a, b, cc, d, light);
        }
        if (backUv != null) {
            Uv a = backUv.sample(hAtX2, v0);
            Uv b = backUv.sample(hAtX1, v0);
            Uv d = backUv.sample(hAtX2, v1);
            Uv cc = backUv.sample(hAtX1, v1);
            emitFace(consumer, pose,
                    point(back, x2, y1, -MIMIC_SURFACE),
                    point(back, x1, y1, -MIMIC_SURFACE),
                    point(back, x1, y2, -MIMIC_SURFACE),
                    point(back, x2, y2, -MIMIC_SURFACE),
                    back, a, b, cc, d, light);
        }
    }

    private static void renderInnerFace(VertexConsumer consumer,
            PoseStack.Pose pose, BakedModel model, BlockState sourceState,
            Level level, BlockPos sourcePos, Direction facing,
            Direction innerFace, double x, double y1, double y2,
            float hAtBack, float hAtFront, float v0, float v1, int light,
            boolean rightSide) {
        FaceUv uv = faceUv(model, sourceState, level, sourcePos,
                innerFace, 0.0F, 1.0F, v0, v1);
        if (uv == null) return;

        if (rightSide) {
            emitFace(consumer, pose,
                    point(facing, x, y1, -MIMIC_SURFACE),
                    point(facing, x, y1, MIMIC_SURFACE),
                    point(facing, x, y2, MIMIC_SURFACE),
                    point(facing, x, y2, -MIMIC_SURFACE),
                    innerFace,
                    uv.sample(hAtBack, v0),
                    uv.sample(hAtFront, v0),
                    uv.sample(hAtFront, v1),
                    uv.sample(hAtBack, v1), light);
        } else {
            emitFace(consumer, pose,
                    point(facing, x, y1, MIMIC_SURFACE),
                    point(facing, x, y1, -MIMIC_SURFACE),
                    point(facing, x, y2, -MIMIC_SURFACE),
                    point(facing, x, y2, MIMIC_SURFACE),
                    innerFace,
                    uv.sample(hAtFront, v0),
                    uv.sample(hAtBack, v0),
                    uv.sample(hAtBack, v1),
                    uv.sample(hAtFront, v1), light);
        }
    }

    private static FaceUv faceUv(BakedModel model, BlockState state,
            Level level, BlockPos pos, Direction face,
            float requiredH0, float requiredH1,
            float requiredV0, float requiredV1) {
        ModelData data = model.getModelData(level, pos, state,
                ModelData.EMPTY);
        long seed = state.getSeed(pos);
        FaceUv best = null;
        float bestArea = -1.0F;

        for (int pass = 0; pass < 2; pass++) {
            RandomSource random = RandomSource.create(seed);
            Direction side = pass == 0 ? face : null;
            for (BakedQuad quad : model.getQuads(state, side, random,
                    data, null)) {
                if (quad.getDirection() != face) continue;
                FaceUv candidate = FaceUv.from(quad, face);
                if (candidate == null) continue;
                if (candidate.covers(requiredH0, requiredH1,
                        requiredV0, requiredV1)) {
                    return candidate;
                }
                float area = candidate.area();
                if (area > bestArea) {
                    bestArea = area;
                    best = candidate;
                }
            }
        }

        if (best != null) return best;
        TextureAtlasSprite sprite = model.getParticleIcon();
        return FaceUv.fullSprite(sprite);
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

    private static void emitFace(VertexConsumer consumer, PoseStack.Pose pose,
            Vec3 a, Vec3 b, Vec3 cc, Vec3 d, Direction normal,
            Uv uvA, Uv uvB, Uv uvC, Uv uvD, int light) {
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        vertex(consumer, matrix, normalMatrix, a, normal, uvA, light);
        vertex(consumer, matrix, normalMatrix, b, normal, uvB, light);
        vertex(consumer, matrix, normalMatrix, cc, normal, uvC, light);
        vertex(consumer, matrix, normalMatrix, d, normal, uvD, light);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose,
            Matrix3f normalMatrix, Vec3 point, Direction normal,
            Uv uv, int light) {
        consumer.vertex(pose, (float) point.x, (float) point.y,
                        (float) point.z)
                .color(255, 255, 255, 255)
                .uv(uv.u(), uv.v())
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normalMatrix, normal.getStepX(),
                        normal.getStepY(), normal.getStepZ())
                .endVertex();
    }

    private record Uv(float u, float v) {
    }

    private record FaceUv(float minH, float maxH, float minV, float maxV,
            Uv lowLow, Uv highLow, Uv highHigh, Uv lowHigh) {
        private static FaceUv from(BakedQuad quad, Direction face) {
            if (face.getAxis().isVertical()) return null;

            int[] vertices = quad.getVertices();
            int stride = vertices.length / 4;
            float[] hs = new float[4];
            float[] vs = new float[4];
            Uv[] uvs = new Uv[4];

            float minH = Float.POSITIVE_INFINITY;
            float maxH = Float.NEGATIVE_INFINITY;
            float minV = Float.POSITIVE_INFINITY;
            float maxV = Float.NEGATIVE_INFINITY;
            for (int index = 0; index < 4; index++) {
                int offset = index * stride;
                float x = Float.intBitsToFloat(vertices[offset]);
                float y = Float.intBitsToFloat(vertices[offset + 1]);
                float z = Float.intBitsToFloat(vertices[offset + 2]);
                float h = face.getAxis() == Direction.Axis.Z ? x : z;
                float v = y;
                int uvOffset = index * IQuadTransformer.STRIDE
                        + IQuadTransformer.UV0;
                hs[index] = h;
                vs[index] = v;
                uvs[index] = new Uv(
                        Float.intBitsToFloat(vertices[uvOffset]),
                        Float.intBitsToFloat(vertices[uvOffset + 1]));
                minH = Math.min(minH, h);
                maxH = Math.max(maxH, h);
                minV = Math.min(minV, v);
                maxV = Math.max(maxV, v);
            }

            if (maxH - minH < 1.0E-5F || maxV - minV < 1.0E-5F) {
                return null;
            }

            float midH = (minH + maxH) * 0.5F;
            float midV = (minV + maxV) * 0.5F;
            Uv lowLow = null;
            Uv highLow = null;
            Uv highHigh = null;
            Uv lowHigh = null;
            for (int index = 0; index < 4; index++) {
                boolean highH = hs[index] > midH;
                boolean highV = vs[index] > midV;
                if (!highH && !highV) lowLow = uvs[index];
                else if (highH && !highV) highLow = uvs[index];
                else if (highH) highHigh = uvs[index];
                else lowHigh = uvs[index];
            }
            if (lowLow == null || highLow == null
                    || highHigh == null || lowHigh == null) {
                return null;
            }
            return new FaceUv(minH, maxH, minV, maxV,
                    lowLow, highLow, highHigh, lowHigh);
        }

        private static FaceUv fullSprite(TextureAtlasSprite sprite) {
            return new FaceUv(0.0F, 1.0F, 0.0F, 1.0F,
                    new Uv(sprite.getU(0.0F), sprite.getV(16.0F)),
                    new Uv(sprite.getU(16.0F), sprite.getV(16.0F)),
                    new Uv(sprite.getU(16.0F), sprite.getV(0.0F)),
                    new Uv(sprite.getU(0.0F), sprite.getV(0.0F)));
        }

        private boolean covers(float h0, float h1, float v0, float v1) {
            final float epsilon = 1.0E-4F;
            return minH <= h0 + epsilon && maxH >= h1 - epsilon
                    && minV <= v0 + epsilon && maxV >= v1 - epsilon;
        }

        private float area() {
            return (maxH - minH) * (maxV - minV);
        }

        private Uv sample(float h, float v) {
            float tx = clamp01((h - minH) / (maxH - minH));
            float ty = clamp01((v - minV) / (maxV - minV));
            float bottomU = lerp(tx, lowLow.u(), highLow.u());
            float bottomV = lerp(tx, lowLow.v(), highLow.v());
            float topU = lerp(tx, lowHigh.u(), highHigh.u());
            float topV = lerp(tx, lowHigh.v(), highHigh.v());
            return new Uv(lerp(ty, bottomU, topU),
                    lerp(ty, bottomV, topV));
        }

        private static float lerp(float t, float a, float b) {
            return a + (b - a) * t;
        }

        private static float clamp01(float value) {
            return Math.max(0.0F, Math.min(1.0F, value));
        }
    }

    private static final class ItemModel
            extends GeoModel<BlastDoorModule.BlastDoorItem> {
        @Override
        public ResourceLocation getModelResource(
                BlastDoorModule.BlastDoorItem animatable) {
            return ITEM_GEO;
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
