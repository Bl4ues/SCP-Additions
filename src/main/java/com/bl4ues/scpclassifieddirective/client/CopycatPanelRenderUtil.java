package com.bl4ues.scpclassifieddirective.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
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
        render(state, level, pos, facing, poseStack, bufferSource,
                packedOverlay, minZ, maxZ, hiddenLocalFace, 1.0F);
    }

    static void render(BlockState state, Level level, BlockPos pos,
            Direction facing, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedOverlay,
            double minZ, double maxZ, Direction hiddenLocalFace,
            float brightness) {
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
                                packedOverlay, minZ, maxZ, brightness);
                    }
                }
            }
        } finally {
            poseStack.popPose();
        }
    }

    static void renderFace(BlockState state, Level level, BlockPos pos,
            Direction facing, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedOverlay,
            Direction onlyLocalFace, double localZ) {
        if (state == null || state.isAir() || level == null
                || onlyLocalFace == null) return;

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
                for (Direction sourceSide : new Direction[]{onlyLocalFace, null}) {
                    RandomSource random = RandomSource.create(seed);
                    for (BakedQuad quad : model.getQuads(state, sourceSide,
                            random, data, renderType)) {
                        if (quad.getDirection() != onlyLocalFace) continue;
                        emitFaceQuad(minecraft, state, level, pos, facing,
                                poseStack.last(), consumer, quad,
                                packedOverlay, localZ);
                    }
                }
            }
        } finally {
            poseStack.popPose();
        }
    }

    private static void emitFaceQuad(Minecraft minecraft, BlockState state,
            Level level, BlockPos pos, Direction facing, PoseStack.Pose pose,
            VertexConsumer consumer, BakedQuad quad, int packedOverlay,
            double localZ) {
        int[] vertices = quad.getVertices();
        int stride = vertices.length / 4;
        int tint = quad.isTinted()
                ? minecraft.getBlockColors().getColor(
                        state, level, pos, quad.getTintIndex())
                : 0xFFFFFF;
        if (tint < 0) tint = 0xFFFFFF;

        Direction localFace = quad.getDirection();
        Direction worldFace = rotateLocalDirection(localFace, facing);
        float directionalShade = level.getShade(worldFace, quad.isShade());

        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            float x = Float.intBitsToFloat(vertices[offset]);
            float y = Float.intBitsToFloat(vertices[offset + 1]);
            float z = Float.intBitsToFloat(vertices[offset + 2]);
            float u = stride > 4
                    ? Float.intBitsToFloat(vertices[offset + 4]) : 0.0F;
            float v = stride > 5
                    ? Float.intBitsToFloat(vertices[offset + 5]) : 0.0F;
            VertexLighting lighting = vertexLighting(level, pos, facing,
                    localFace, worldFace, x, y, z);
            float shade = directionalShade * lighting.ambientOcclusion();
            int red = Math.max(0, Math.min(255,
                    Math.round(((tint >> 16) & 0xFF) * shade)));
            int green = Math.max(0, Math.min(255,
                    Math.round(((tint >> 8) & 0xFF) * shade)));
            int blue = Math.max(0, Math.min(255,
                    Math.round((tint & 0xFF) * shade)));

            consumer.vertex(pose.pose(), x, y, (float) localZ)
                    .color(red, green, blue, 255)
                    .uv(u, v)
                    .overlayCoords(packedOverlay)
                    .uv2(lighting.packedLight())
                    .normal(pose.normal(),
                            localFace.getStepX(),
                            localFace.getStepY(),
                            localFace.getStepZ())
                    .endVertex();
        }
    }

    private static void emitQuad(Minecraft minecraft, BlockState state,
            Level level, BlockPos pos, Direction facing, PoseStack.Pose pose,
            VertexConsumer consumer, BakedQuad quad, int packedOverlay,
            double minZ, double maxZ, float brightness) {
        int[] vertices = quad.getVertices();
        int stride = vertices.length / 4;
        int tint = quad.isTinted()
                ? minecraft.getBlockColors().getColor(
                        state, level, pos, quad.getTintIndex())
                : 0xFFFFFF;
        if (tint < 0) tint = 0xFFFFFF;

        Direction localFace = quad.getDirection();
        Direction worldFace = rotateLocalDirection(localFace, facing);
        float directionalShade = level.getShade(worldFace, quad.isShade());

        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            float x = Float.intBitsToFloat(vertices[offset]);
            float y = Float.intBitsToFloat(vertices[offset + 1]);
            float z = Float.intBitsToFloat(vertices[offset + 2]);
            float[] uv = croppedUv(vertices, stride, vertex, localFace,
                    minZ, maxZ);
            float compressedZ = (float) (minZ
                    + z * (maxZ - minZ));
            VertexLighting lighting = vertexLighting(level, pos, facing,
                    localFace, worldFace, x, y, z);
            float shade = directionalShade * lighting.ambientOcclusion()
                    * Math.max(0.0F, Math.min(1.0F, brightness));
            int red = Math.max(0, Math.min(255,
                    Math.round(((tint >> 16) & 0xFF) * shade)));
            int green = Math.max(0, Math.min(255,
                    Math.round(((tint >> 8) & 0xFF) * shade)));
            int blue = Math.max(0, Math.min(255,
                    Math.round((tint & 0xFF) * shade)));

            consumer.vertex(pose.pose(), x, y, compressedZ)
                    .color(red, green, blue, 255)
                    .uv(uv[0], uv[1])
                    .overlayCoords(packedOverlay)
                    .uv2(lighting.packedLight())
                    .normal(pose.normal(),
                            localFace.getStepX(),
                            localFace.getStepY(),
                            localFace.getStepZ())
                    .endVertex();
        }
    }

    /**
     * Compressing the geometry must crop the copied texture, not squeeze the
     * whole side texture into the new depth. For X-facing side quads the UV
     * varies with local Z at a fixed Y; for top/bottom quads it varies with Z
     * at a fixed X. Interpolate the authored UV endpoints at the requested
     * source-depth interval, preserving the exact half/strip that would be
     * visible if the original block were physically cut there.
     */
    private static float[] croppedUv(int[] vertices, int stride, int vertex,
            Direction face, double minZ, double maxZ) {
        int offset = vertex * stride;
        float originalU = stride > 4
                ? Float.intBitsToFloat(vertices[offset + 4]) : 0.0F;
        float originalV = stride > 5
                ? Float.intBitsToFloat(vertices[offset + 5]) : 0.0F;
        if (face == null || face.getAxis() == Direction.Axis.Z
                || Math.abs(maxZ - minZ - 1.0D) < 1.0E-6D) {
            return new float[]{originalU, originalV};
        }

        float x = Float.intBitsToFloat(vertices[offset]);
        float y = Float.intBitsToFloat(vertices[offset + 1]);
        float z = Float.intBitsToFloat(vertices[offset + 2]);
        float fixed = face.getAxis() == Direction.Axis.X ? y : x;
        int fixedIndex = face.getAxis() == Direction.Axis.X ? 1 : 0;

        int low = -1;
        int high = -1;
        float lowZ = Float.POSITIVE_INFINITY;
        float highZ = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < 4; i++) {
            int candidateOffset = i * stride;
            float candidateFixed = Float.intBitsToFloat(
                    vertices[candidateOffset + fixedIndex]);
            if (Math.abs(candidateFixed - fixed) > 1.0E-4F) continue;
            float candidateZ = Float.intBitsToFloat(
                    vertices[candidateOffset + 2]);
            if (candidateZ < lowZ) {
                lowZ = candidateZ;
                low = i;
            }
            if (candidateZ > highZ) {
                highZ = candidateZ;
                high = i;
            }
        }
        if (low < 0 || high < 0 || Math.abs(highZ - lowZ) < 1.0E-6F) {
            return new float[]{originalU, originalV};
        }

        float targetZ = (float) (minZ + z * (maxZ - minZ));
        float t = (targetZ - lowZ) / (highZ - lowZ);
        t = Math.max(0.0F, Math.min(1.0F, t));

        int lowOffset = low * stride;
        int highOffset = high * stride;
        float lowU = stride > 4
                ? Float.intBitsToFloat(vertices[lowOffset + 4]) : 0.0F;
        float lowV = stride > 5
                ? Float.intBitsToFloat(vertices[lowOffset + 5]) : 0.0F;
        float highU = stride > 4
                ? Float.intBitsToFloat(vertices[highOffset + 4]) : 0.0F;
        float highV = stride > 5
                ? Float.intBitsToFloat(vertices[highOffset + 5]) : 0.0F;
        return new float[]{
                lowU + (highU - lowU) * t,
                lowV + (highV - lowV) * t
        };
    }

    /**
     * Approximate the same per-vertex ambient occlusion neighborhood sampled by
     * vanilla's ModelBlockRenderer. One flat light/color for the whole quad is
     * visibly wrong beside floors and ceilings, and shader packs amplify that
     * error into a bright vertical sheen.
     */
    private static VertexLighting vertexLighting(Level level, BlockPos pos,
            Direction facing, Direction localFace, Direction worldFace,
            float x, float y, float z) {
        BlockPos facePos = pos.relative(worldFace);
        Direction tangentA;
        Direction tangentB;
        switch (localFace.getAxis()) {
            case X -> {
                tangentA = z < 0.5F ? Direction.NORTH : Direction.SOUTH;
                tangentB = y < 0.5F ? Direction.DOWN : Direction.UP;
            }
            case Y -> {
                tangentA = x < 0.5F ? Direction.WEST : Direction.EAST;
                tangentB = z < 0.5F ? Direction.NORTH : Direction.SOUTH;
            }
            case Z -> {
                tangentA = x < 0.5F ? Direction.WEST : Direction.EAST;
                tangentB = y < 0.5F ? Direction.DOWN : Direction.UP;
            }
            default -> {
                tangentA = Direction.WEST;
                tangentB = Direction.DOWN;
            }
        }
        tangentA = rotateLocalDirection(tangentA, facing);
        tangentB = rotateLocalDirection(tangentB, facing);

        BlockPos sideA = facePos.relative(tangentA);
        BlockPos sideB = facePos.relative(tangentB);
        BlockPos corner = sideA.relative(tangentB);

        float rawAo = (shadeBrightness(level, facePos)
                + shadeBrightness(level, sideA)
                + shadeBrightness(level, sideB)
                + shadeBrightness(level, corner)) * 0.25F;
        rawAo = Math.max(0.0F, Math.min(1.0F, rawAo));

        /*
         * Full-strength corner AO was substantially stronger than vanilla on
         * these BER surfaces. Shader packs then amplified the interpolated
         * lightmap into a pale vertical sheen. Keep only a restrained part of
         * the corner term and use one stable face light value.
         */
        float ao = 1.0F - (1.0F - rawAo) * 0.18F;
        int packed = level.hasChunkAt(facePos)
                ? LevelRenderer.getLightColor(level, facePos)
                : LevelRenderer.getLightColor(level, pos);
        return new VertexLighting(ao, packed);
    }

    private static float shadeBrightness(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return 1.0F;
        BlockState sample = level.getBlockState(pos);
        return sample.getShadeBrightness(level, pos);
    }

    private record VertexLighting(float ambientOcclusion, int packedLight) {
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
