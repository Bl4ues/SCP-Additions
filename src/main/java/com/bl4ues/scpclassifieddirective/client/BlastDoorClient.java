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
            renderLowerMimic(door, poseStack, bufferSource, false);
            renderLowerMimic(door, poseStack, bufferSource, true);
            renderUpperWallMimics(door, poseStack, bufferSource);
        }

        @Override
        public boolean shouldRenderOffScreen(
                BlastDoorModule.BlastDoorBlockEntity blockEntity) {
            return true;
        }
    }

    private static void renderLowerMimic(
            BlastDoorModule.BlastDoorBlockEntity door,
            PoseStack poseStack, MultiBufferSource buffers,
            boolean rightSide) {
        Level level = door.getLevel();
        if (level == null) return;
        BlockState doorState = door.getBlockState();
        if (!BlastDoorModule.isController(doorState)) return;

        Direction facing = doorState.getValue(BlastDoorModule.FACING);
        BlockPos sourcePos = BlastDoorStructure.mimicSource(
                door.getBlockPos(), facing, rightSide, false);
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

        BlockPos targetPos = BlastDoorStructure.partPosition(
                door.getBlockPos(), facing, rightSide ? 2 : -2, 2);
        BakedModel model = Minecraft.getInstance().getBlockRenderer()
                .getBlockModel(sourceState);
        VertexConsumer consumer = buffers.getBuffer(Sheets.cutoutBlockSheet());
        PoseStack.Pose pose = poseStack.last();

        double x1 = rightSide ? 32.0D : -40.0D;
        double x2 = rightSide ? 40.0D : -32.0D;
        double y1 = 40.0D;
        double y2 = 48.0D;

        Direction right = facing.getClockWise();
        Direction front = facing.getOpposite();

        // Preserve the exact orientation of the source block's baked face.
        // This lower mimic is the original exposed 8x8 outer/top quarter.
        boolean rightAxisPositive =
                right == Direction.EAST || right == Direction.SOUTH;
        boolean takeHighHalf = rightSide == rightAxisPositive;
        float sourceH0 = takeHighHalf ? 0.5F : 0.0F;
        float sourceH1 = takeHighHalf ? 1.0F : 0.5F;
        float sourceV0 = 0.5F;
        float sourceV1 = 1.0F;

        float hAtX1 = rightAxisPositive ? sourceH0 : sourceH1;
        float hAtX2 = rightAxisPositive ? sourceH1 : sourceH0;

        renderFrontBackFace(consumer, pose, model, sourceState, level,
                sourcePos, targetPos, front, facing, x1, x2, y1, y2,
                hAtX1, hAtX2, sourceV0, sourceV1);

        Direction innerFace = rightSide
                ? right.getOpposite() : right;
        boolean forwardAxisPositive =
                front == Direction.EAST || front == Direction.SOUTH;
        float hAtBack = forwardAxisPositive ? 0.0F : 1.0F;
        float hAtFront = forwardAxisPositive ? 1.0F : 0.0F;
        double innerX = rightSide ? x1 : x2;
        renderInnerFace(consumer, pose, model, sourceState, level,
                sourcePos, targetPos, facing, innerFace, innerX, y1, y2,
                hAtBack, hAtFront, sourceV0, sourceV1, rightSide);
    }

    private static void renderUpperWallMimics(
            BlastDoorModule.BlastDoorBlockEntity door,
            PoseStack poseStack, MultiBufferSource buffers) {
        Level level = door.getLevel();
        if (level == null) return;
        BlockState doorState = door.getBlockState();
        if (!BlastDoorModule.isController(doorState)) return;

        Direction facing = doorState.getValue(BlastDoorModule.FACING);
        Minecraft minecraft = Minecraft.getInstance();

        // The entire Y+3 row is reserved by the multiblock but visually belongs
        // to the facility wall. Each cell copies the real block immediately
        // above itself (Y+4), which closes the opening across the full five-block
        // width instead of only patching the two outer corners.
        for (int side = BlastDoorStructure.MIN_SIDE;
                side <= BlastDoorStructure.MAX_SIDE; side++) {
            BlockPos sourcePos = BlastDoorStructure.topMimicSource(
                    door.getBlockPos(), facing, side);
            BlockPos targetPos = BlastDoorStructure.partPosition(
                    door.getBlockPos(), facing, side, 3);
            if (!level.hasChunkAt(sourcePos)) continue;

            BlockState sourceState = level.getBlockState(sourcePos);
            if (sourceState.isAir()
                    || sourceState.getRenderShape() == RenderShape.INVISIBLE
                    || BlastDoorModule.isStructureState(sourceState)) {
                continue;
            }

            BakedModel model = minecraft.getBlockRenderer()
                    .getBlockModel(sourceState);
            ModelData modelData = model.getModelData(level, sourcePos,
                    sourceState, ModelData.EMPTY);
            long seed = sourceState.getSeed(sourcePos);

            poseStack.pushPose();
            poseStack.translate(
                    targetPos.getX() - door.getBlockPos().getX(),
                    targetPos.getY() - door.getBlockPos().getY(),
                    targetPos.getZ() - door.getBlockPos().getZ());

            // Use the normal world block tessellator, not renderSingleBlock.
            // That restores per-face/ambient-occlusion lighting and keeps Forge
            // render layers/PBR hooks consistent with the surrounding wall.
            for (RenderType renderType : model.getRenderTypes(sourceState,
                    RandomSource.create(seed), modelData)) {
                minecraft.getBlockRenderer().renderBatched(
                        sourceState, targetPos, level, poseStack,
                        buffers.getBuffer(renderType), false,
                        RandomSource.create(seed));
            }
            poseStack.popPose();
        }
    }

    private static void renderFrontBackFace(VertexConsumer consumer,
            PoseStack.Pose pose, BakedModel model, BlockState sourceState,
            Level level, BlockPos sourcePos, BlockPos targetPos,
            Direction front, Direction back,
            double x1, double x2, double y1, double y2,
            float hAtX1, float hAtX2, float v0, float v1) {
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
                    front, a, b, cc, d,
                    faceLight(level, targetPos, front),
                    faceColor(level, sourceState, sourcePos, frontUv, front));
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
                    back, a, b, cc, d,
                    faceLight(level, targetPos, back),
                    faceColor(level, sourceState, sourcePos, backUv, back));
        }
    }

    private static void renderInnerFace(VertexConsumer consumer,
            PoseStack.Pose pose, BakedModel model, BlockState sourceState,
            Level level, BlockPos sourcePos, BlockPos targetPos,
            Direction facing, Direction innerFace,
            double x, double y1, double y2,
            float hAtBack, float hAtFront, float v0, float v1,
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
                    uv.sample(hAtBack, v1),
                    faceLight(level, targetPos, innerFace),
                    faceColor(level, sourceState, sourcePos, uv, innerFace));
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
                    uv.sample(hAtFront, v1),
                    faceLight(level, targetPos, innerFace),
                    faceColor(level, sourceState, sourcePos, uv, innerFace));
        }
    }

    private static int faceLight(Level level, BlockPos targetPos,
            Direction face) {
        // Match vanilla block-face lighting: a visible face samples the light
        // in the neighbouring cell on that side, not the invisible placeholder
        // cell occupied by the multiblock itself.
        return net.minecraft.client.renderer.LevelRenderer.getLightColor(
                level, targetPos.relative(face));
    }

    private static int faceColor(Level level, BlockState state,
            BlockPos sourcePos, FaceUv uv, Direction face) {
        int tint = 0xFFFFFF;
        if (uv.tintIndex() >= 0) {
            tint = Minecraft.getInstance().getBlockColors().getColor(
                    state, level, sourcePos, uv.tintIndex());
        }
        float shade = uv.shade() ? level.getShade(face, true) : 1.0F;
        int red = Math.round(((tint >> 16) & 0xFF) * shade);
        int green = Math.round(((tint >> 8) & 0xFF) * shade);
        int blue = Math.round((tint & 0xFF) * shade);
        return (red << 16) | (green << 8) | blue;
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
            Uv uvA, Uv uvB, Uv uvC, Uv uvD, int light, int color) {
        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        vertex(consumer, matrix, normalMatrix, a, normal, uvA, light, color);
        vertex(consumer, matrix, normalMatrix, b, normal, uvB, light, color);
        vertex(consumer, matrix, normalMatrix, cc, normal, uvC, light, color);
        vertex(consumer, matrix, normalMatrix, d, normal, uvD, light, color);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose,
            Matrix3f normalMatrix, Vec3 point, Direction normal,
            Uv uv, int light, int color) {
        consumer.vertex(pose, (float) point.x, (float) point.y,
                        (float) point.z)
                .color((color >> 16) & 0xFF, (color >> 8) & 0xFF,
                        color & 0xFF, 255)
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
            Uv lowLow, Uv highLow, Uv highHigh, Uv lowHigh,
            int tintIndex, boolean shade) {
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
                    lowLow, highLow, highHigh, lowHigh,
                    quad.getTintIndex(), quad.isShade());
        }

        private static FaceUv fullSprite(TextureAtlasSprite sprite) {
            return new FaceUv(0.0F, 1.0F, 0.0F, 1.0F,
                    new Uv(sprite.getU(0.0F), sprite.getV(16.0F)),
                    new Uv(sprite.getU(16.0F), sprite.getV(16.0F)),
                    new Uv(sprite.getU(16.0F), sprite.getV(0.0F)),
                    new Uv(sprite.getU(0.0F), sprite.getV(0.0F)),
                    -1, true);
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
