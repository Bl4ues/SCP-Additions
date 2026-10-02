package com.bl4ues.scpclassifieddirective.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.List;

final class CopycatPanelRenderUtil {
    private static final Direction[] SIDES = {
            Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH,
            Direction.WEST, Direction.EAST, null
    };

    private CopycatPanelRenderUtil() {
    }

    static void render(BlockState state, Level level, BlockPos pos,
            Direction facing, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedOverlay,
            double minZ, double maxZ, Direction hiddenLocalFace) {
        if (state == null || state.isAir() || level == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);
        ModelData data = model.getModelData(level, pos, state, ModelData.EMPTY);
        long seed = state.getSeed(pos);
        RandomSource typeRandom = RandomSource.create(seed);
        List<RenderType> renderTypes = new ArrayList<>(
                model.getRenderTypes(state, typeRandom, data).asList());
        if (renderTypes.isEmpty()) {
            renderTypes.add(ItemBlockRenderTypes.getChunkRenderType(state));
        }

        poseStack.pushPose();
        try {
            poseStack.translate(-0.5D, 0.0D, -0.5D);
            for (RenderType renderType : renderTypes) {
                VertexConsumer consumer = bufferSource.getBuffer(renderType);
                for (Direction side : SIDES) {
                    if (side != null && side == hiddenLocalFace) continue;
                    RandomSource random = RandomSource.create(seed);
                    for (BakedQuad quad : model.getQuads(state, side,
                            random, data, renderType)) {
                        if (quad.getDirection() == hiddenLocalFace) continue;
                        emitQuad(minecraft, state, level, pos, facing,
                                poseStack.last(), consumer, quad,
                                packedOverlay, minZ, maxZ);
                    }
                }
            }
        } finally {
            poseStack.popPose();
        }
    }

    private static void emitQuad(Minecraft minecraft, BlockState state,
            Level level, BlockPos pos, Direction facing, PoseStack.Pose pose,
            VertexConsumer consumer, BakedQuad quad, int packedOverlay,
            double minZ, double maxZ) {
        int[] vertices = quad.getVertices();
        int stride = vertices.length / 4;
        int tint = quad.isTinted()
                ? minecraft.getBlockColors().getColor(
                        state, level, pos, quad.getTintIndex())
                : 0xFFFFFF;
        if (tint < 0) tint = 0xFFFFFF;

        Direction localFace = quad.getDirection();
        Direction worldFace = rotateLocalDirection(localFace, facing);
        int light = faceLight(level, state, pos, worldFace);

        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            float x = Float.intBitsToFloat(vertices[offset]);
            float y = Float.intBitsToFloat(vertices[offset + 1]);
            float z = Float.intBitsToFloat(vertices[offset + 2]);
            float u = stride > 4
                    ? Float.intBitsToFloat(vertices[offset + 4]) : 0.0F;
            float v = stride > 5
                    ? Float.intBitsToFloat(vertices[offset + 5]) : 0.0F;
            float compressedZ = (float) (minZ
                    + z * (maxZ - minZ));

            consumer.vertex(pose.pose(), x, y, compressedZ)
                    .color((tint >> 16) & 0xFF,
                            (tint >> 8) & 0xFF,
                            tint & 0xFF, 255)
                    .uv(u, v)
                    .overlayCoords(packedOverlay)
                    .uv2(light)
                    .normal(pose.normal(),
                            localFace.getStepX(),
                            localFace.getStepY(),
                            localFace.getStepZ())
                    .endVertex();
        }
    }

    private static int faceLight(Level level, BlockState state,
            BlockPos pos, Direction worldFace) {
        int source = LevelRenderer.getLightColor(level, state, pos);
        BlockPos exposedPos = pos.relative(worldFace);
        int exposed = level.hasChunkAt(exposedPos)
                ? LevelRenderer.getLightColor(level, exposedPos)
                : source;
        return LightTexture.pack(
                Math.max(LightTexture.block(source),
                        LightTexture.block(exposed)),
                Math.max(LightTexture.sky(source),
                        LightTexture.sky(exposed)));
    }

    private static Direction rotateLocalDirection(Direction local,
            Direction facing) {
        if (local.getAxis().isVertical() || facing == Direction.NORTH) {
            return local;
        }
        return switch (facing) {
            case SOUTH -> local.getOpposite();
            case WEST -> local.getCounterClockWise();
            case EAST -> local.getClockWise();
            default -> local;
        };
    }
}
