package com.bl4ues.scpclassifieddirective.client.render;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.CopycatPanelMaterial;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CopycatPanelBakedModels {
    private CopycatPanelBakedModels() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void modifyBakedModels(ModelEvent.ModifyBakingResult event) {
        event.getModels().replaceAll((location, model) -> {
            if (!ScpClassifiedDirectiveMod.MODID.equals(
                    location.getNamespace())
                    || model.isCustomRenderer()
                    || model instanceof CopycatPanelModel) {
                return model;
            }
            return switch (location.getPath()) {
                case "wall_panel" -> new CopycatPanelModel(model, Kind.THIN);
                case "double_wall_panel" ->
                        new CopycatPanelModel(model, Kind.DOUBLE);
                default -> model;
            };
        });
    }

    private enum Kind {
        THIN,
        DOUBLE
    }

    private static final class CopycatPanelModel
            extends BakedModelWrapper<BakedModel> {
        private final Kind kind;
        private final Map<BakedQuad, BakedQuad> darkRearFallback =
                Collections.synchronizedMap(new IdentityHashMap<>());

        private CopycatPanelModel(BakedModel originalModel, Kind kind) {
            super(originalModel);
            this.kind = kind;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state,
                @Nullable Direction side, RandomSource random) {
            return originalModel.getQuads(state, side, random);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state,
                @Nullable Direction side, RandomSource random,
                ModelData modelData, @Nullable RenderType renderType) {
            if (state == null
                    || !modelData.has(CopycatPanelMaterial.MODEL_PROPERTY)) {
                return originalModel.getQuads(state, side, random,
                        modelData, renderType);
            }

            CopycatPanelMaterial.ModelSnapshot snapshot =
                    modelData.get(CopycatPanelMaterial.MODEL_PROPERTY);
            if (snapshot == null) {
                return originalModel.getQuads(state, side, random,
                        modelData, renderType);
            }

            Direction facing = state.hasProperty(
                    HorizontalDirectionalBlock.FACING)
                    ? state.getValue(HorizontalDirectionalBlock.FACING)
                    : Direction.NORTH;

            List<BakedQuad> result = new ArrayList<>();
            for (BakedQuad quad : originalModel.getQuads(state, side, random,
                    modelData, renderType)) {
                if (!replaceFallbackQuad(
                        quad, side, facing, snapshot)) {
                    result.add(kind == Kind.DOUBLE
                            && snapshot.back().isAir()
                            && localDepthCenter(quad, facing) > 0.5001F
                            ? darkRearFallback.computeIfAbsent(quad,
                                    CopycatPanelBakedModels::darkenFallback)
                            : quad);
                }
            }

            if (kind == Kind.THIN) {
                appendThinMaterial(result, snapshot.front(), true,
                        side, renderType, facing, snapshot.pos());
                appendThinMaterial(result, snapshot.back(), false,
                        side, renderType, facing, snapshot.pos());
            } else {
                appendDoubleMaterial(result, snapshot.front(), true,
                        side, renderType, facing, snapshot.pos());
                appendDoubleMaterial(result, snapshot.back(), false,
                        side, renderType, facing, snapshot.pos());
            }
            return result.isEmpty() ? List.of() : List.copyOf(result);
        }

        @Override
        public ChunkRenderTypeSet getRenderTypes(BlockState state,
                RandomSource random, ModelData modelData) {
            ChunkRenderTypeSet types = originalModel.getRenderTypes(
                    state, random, modelData);
            if (!modelData.has(CopycatPanelMaterial.MODEL_PROPERTY)) {
                return types;
            }
            CopycatPanelMaterial.ModelSnapshot snapshot =
                    modelData.get(CopycatPanelMaterial.MODEL_PROPERTY);
            if (snapshot == null) return types;
            types = unionMaterialTypes(types, snapshot.front(),
                    snapshot.pos());
            return unionMaterialTypes(types, snapshot.back(),
                    snapshot.pos());
        }

        private boolean replaceFallbackQuad(BakedQuad quad,
                @Nullable Direction requestedSide, Direction facing,
                CopycatPanelMaterial.ModelSnapshot snapshot) {
            Direction frontFace = facing;
            Direction backFace = facing.getOpposite();

            // The outer copied face must completely replace the authored
            // wall_panel face. Filtering by the quad's baked direction alone
            // was not reliable for every rotated blockstate, leaving the
            // fallback X coplanar with the copied material and making an
            // opaque block look translucent.
            if (!snapshot.front().isAir()
                    && requestedSide == frontFace) {
                return true;
            }
            if (!snapshot.back().isAir()
                    && requestedSide == backFace) {
                return true;
            }

            if (kind == Kind.THIN) {
                if (requestedSide != null) return false;
                Direction direction = quad.getDirection();
                return (!snapshot.front().isAir()
                        && direction == frontFace)
                        || (!snapshot.back().isAir()
                        && direction == backFace);
            }

            // Lateral faces of the Double Wall Panel are split into physical
            // front/back halves. Keep the untouched half's fallback texture,
            // but remove the occupied half so its cropped copied quad is the
            // only surface rendered there.
            float localDepth = localDepthCenter(quad, facing);
            return (!snapshot.front().isAir() && localDepth < 0.4999F)
                    || (!snapshot.back().isAir() && localDepth > 0.5001F);
        }

        private ChunkRenderTypeSet unionMaterialTypes(
                ChunkRenderTypeSet current, BlockState material,
                BlockPos pos) {
            if (material == null || material.isAir()) return current;
            BakedModel model = Minecraft.getInstance().getBlockRenderer()
                    .getBlockModel(material);
            ChunkRenderTypeSet source = model.getRenderTypes(material,
                    RandomSource.create(material.getSeed(pos)),
                    ModelData.EMPTY);
            return ChunkRenderTypeSet.union(current, source);
        }
    }

    private static void appendThinMaterial(List<BakedQuad> output,
            BlockState material, boolean front,
            @Nullable Direction requestedSide,
            @Nullable RenderType renderType, Direction facing, BlockPos pos) {
        if (material == null || material.isAir()) return;
        Direction localFace = front ? Direction.NORTH : Direction.SOUTH;
        Direction worldFace = rotateLocalDirection(localFace, facing);

        if (front) {
            if (requestedSide != worldFace) return;
        } else if (requestedSide != null) {
            return;
        }

        BakedModel sourceModel = Minecraft.getInstance().getBlockRenderer()
                .getBlockModel(material);
        if (!supportsRenderType(sourceModel, material, pos, renderType)) return;
        for (BakedQuad quad : sourceFaceQuads(sourceModel, material,
                localFace, pos, renderType)) {
            output.add(transformThinFace(quad, material, pos, facing,
                    front ? 0.0F : 1.0F / 16.0F));
        }
    }

    private static void appendDoubleMaterial(List<BakedQuad> output,
            BlockState material, boolean front,
            @Nullable Direction requestedSide,
            @Nullable RenderType renderType, Direction facing, BlockPos pos) {
        if (material == null || material.isAir()) return;
        BakedModel sourceModel = Minecraft.getInstance().getBlockRenderer()
                .getBlockModel(material);
        if (!supportsRenderType(sourceModel, material, pos, renderType)) return;

        float minZ = front ? 0.0F : 0.5F;
        float maxZ = front ? 0.5F : 1.0F;
        for (Direction localFace : Direction.values()) {
            if ((front && localFace == Direction.SOUTH)
                    || (!front && localFace == Direction.NORTH)) {
                continue;
            }
            Direction worldFace = rotateLocalDirection(localFace, facing);
            if (requestedSide != worldFace) continue;
            for (BakedQuad quad : sourceFaceQuads(sourceModel, material,
                    localFace, pos, renderType)) {
                output.add(transformDoubleHalf(quad, material, pos, facing,
                        minZ, maxZ));
            }
        }
    }

    private static boolean supportsRenderType(BakedModel model,
            BlockState material, BlockPos pos,
            @Nullable RenderType renderType) {
        if (renderType == null) return true;
        return model.getRenderTypes(material,
                RandomSource.create(material.getSeed(pos)), ModelData.EMPTY)
                .contains(renderType);
    }

    private static List<BakedQuad> sourceFaceQuads(BakedModel sourceModel,
            BlockState material, Direction face, BlockPos pos,
            @Nullable RenderType renderType) {
        long seed = material.getSeed(pos);
        List<BakedQuad> result = new ArrayList<>();
        RandomSource random = RandomSource.create(seed);
        for (BakedQuad quad : sourceModel.getQuads(material, face, random,
                ModelData.EMPTY, renderType)) {
            if (quad.getDirection() == face) result.add(quad);
        }
        random = RandomSource.create(seed);
        for (BakedQuad quad : sourceModel.getQuads(material, null, random,
                ModelData.EMPTY, renderType)) {
            if (quad.getDirection() == face) result.add(quad);
        }
        return result;
    }

    private static BakedQuad transformThinFace(BakedQuad source,
            BlockState material, BlockPos pos, Direction facing,
            float localZ) {
        int[] vertices = source.getVertices().clone();
        int stride = vertices.length / 4;
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            float localX = Float.intBitsToFloat(vertices[offset]);
            float[] rotated = rotateLocalPosition(localX, localZ, facing);
            vertices[offset] = Float.floatToRawIntBits(rotated[0]);
            vertices[offset + 2] = Float.floatToRawIntBits(rotated[1]);
            rotatePackedNormal(vertices, offset, facing);
        }
        int tintIndex = applySourceTint(vertices, stride, source,
                material, pos);
        return new BakedQuad(vertices, tintIndex,
                rotateLocalDirection(source.getDirection(), facing),
                source.getSprite(), source.isShade(),
                source.hasAmbientOcclusion());
    }

    private static BakedQuad transformDoubleHalf(BakedQuad source,
            BlockState material, BlockPos pos, Direction facing,
            float minZ, float maxZ) {
        int[] vertices = source.getVertices().clone();
        int stride = vertices.length / 4;
        Direction face = source.getDirection();

        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            float localX = Float.intBitsToFloat(vertices[offset]);
            float localZ = Float.intBitsToFloat(vertices[offset + 2]);
            float mappedZ = minZ + localZ * (maxZ - minZ);
            float[] uv = croppedUv(vertices, stride, vertex, face,
                    minZ, maxZ);
            float[] rotated = rotateLocalPosition(localX, mappedZ, facing);
            vertices[offset] = Float.floatToRawIntBits(rotated[0]);
            vertices[offset + 2] = Float.floatToRawIntBits(rotated[1]);
            vertices[offset + 4] = Float.floatToRawIntBits(uv[0]);
            vertices[offset + 5] = Float.floatToRawIntBits(uv[1]);
            rotatePackedNormal(vertices, offset, facing);
        }

        int tintIndex = applySourceTint(vertices, stride, source,
                material, pos);
        return new BakedQuad(vertices, tintIndex,
                rotateLocalDirection(face, facing),
                source.getSprite(), source.isShade(),
                source.hasAmbientOcclusion());
    }

    private static int applySourceTint(int[] vertices, int stride,
            BakedQuad source, BlockState material, BlockPos pos) {
        if (!source.isTinted()) return source.getTintIndex();
        Minecraft minecraft = Minecraft.getInstance();
        int rgb = minecraft.getBlockColors().getColor(material,
                minecraft.level, pos, source.getTintIndex());
        if (rgb < 0) rgb = 0xFFFFFF;
        int abgr = QuadTransformers.toABGR(0xFF000000 | rgb);
        for (int vertex = 0; vertex < 4; vertex++) {
            int colorOffset = vertex * stride + IQuadTransformer.COLOR;
            vertices[colorOffset] = multiplyAbgr(
                    vertices[colorOffset], abgr);
        }
        return -1;
    }

    private static int multiplyAbgr(int base, int tint) {
        int a = (base >>> 24) & 0xFF;
        int b = ((base >>> 16) & 0xFF)
                * ((tint >>> 16) & 0xFF) / 255;
        int g = ((base >>> 8) & 0xFF)
                * ((tint >>> 8) & 0xFF) / 255;
        int r = (base & 0xFF) * (tint & 0xFF) / 255;
        return a << 24 | b << 16 | g << 8 | r;
    }

    private static BakedQuad darkenFallback(BakedQuad source) {
        int[] vertices = source.getVertices().clone();
        BakedQuad result = new BakedQuad(vertices, source.getTintIndex(),
                source.getDirection(), source.getSprite(), source.isShade(),
                source.hasAmbientOcclusion());
        QuadTransformers.applyingColor(0xFFD1D1D1).processInPlace(result);
        return result;
    }

    private static float localDepthCenter(BakedQuad quad, Direction facing) {
        int[] vertices = quad.getVertices();
        int stride = vertices.length / 4;
        float depth = 0.0F;
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            float worldX = Float.intBitsToFloat(vertices[offset]);
            float worldZ = Float.intBitsToFloat(vertices[offset + 2]);
            depth += inverseLocalZ(worldX, worldZ, facing);
        }
        return depth * 0.25F;
    }

    private static float inverseLocalZ(float worldX, float worldZ,
            Direction facing) {
        return switch (facing) {
            case EAST -> 1.0F - worldX;
            case SOUTH -> 1.0F - worldZ;
            case WEST -> worldX;
            default -> worldZ;
        };
    }

    private static float[] rotateLocalPosition(float x, float z,
            Direction facing) {
        return switch (facing) {
            case EAST -> new float[]{1.0F - z, x};
            case SOUTH -> new float[]{1.0F - x, 1.0F - z};
            case WEST -> new float[]{z, 1.0F - x};
            default -> new float[]{x, z};
        };
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

    private static void rotatePackedNormal(int[] vertices, int vertexOffset,
            Direction facing) {
        int normalOffset = vertexOffset + IQuadTransformer.NORMAL;
        int packed = vertices[normalOffset];
        if ((packed & 0x00FFFFFF) == 0 || facing == Direction.NORTH) return;

        float x = ((byte) (packed & 0xFF)) / 127.0F;
        float y = ((byte) ((packed >>> 8) & 0xFF)) / 127.0F;
        float z = ((byte) ((packed >>> 16) & 0xFF)) / 127.0F;
        float worldX;
        float worldZ;
        switch (facing) {
            case EAST -> {
                worldX = -z;
                worldZ = x;
            }
            case SOUTH -> {
                worldX = -x;
                worldZ = -z;
            }
            case WEST -> {
                worldX = z;
                worldZ = -x;
            }
            default -> {
                worldX = x;
                worldZ = z;
            }
        }
        int nx = ((byte) Math.round(worldX * 127.0F)) & 0xFF;
        int ny = ((byte) Math.round(y * 127.0F)) & 0xFF;
        int nz = ((byte) Math.round(worldZ * 127.0F)) & 0xFF;
        vertices[normalOffset] = nx | ny << 8 | nz << 16
                | (packed & 0xFF000000);
    }

    private static float[] croppedUv(int[] vertices, int stride, int vertex,
            Direction face, float minZ, float maxZ) {
        int offset = vertex * stride;
        float originalU = Float.intBitsToFloat(vertices[offset + 4]);
        float originalV = Float.intBitsToFloat(vertices[offset + 5]);
        if (face.getAxis() == Direction.Axis.Z) {
            return new float[]{originalU, originalV};
        }

        float minSourceZ = Float.POSITIVE_INFINITY;
        float maxSourceZ = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < 4; i++) {
            float candidateZ = Float.intBitsToFloat(
                    vertices[i * stride + 2]);
            minSourceZ = Math.min(minSourceZ, candidateZ);
            maxSourceZ = Math.max(maxSourceZ, candidateZ);
        }
        float sourceSpan = maxSourceZ - minSourceZ;
        if (sourceSpan < 1.0E-6F) {
            return new float[]{originalU, originalV};
        }

        float lowU = 0.0F;
        float lowV = 0.0F;
        float highU = 0.0F;
        float highV = 0.0F;
        int lowCount = 0;
        int highCount = 0;
        for (int i = 0; i < 4; i++) {
            int candidateOffset = i * stride;
            float candidateZ = Float.intBitsToFloat(
                    vertices[candidateOffset + 2]);
            float candidateU = Float.intBitsToFloat(
                    vertices[candidateOffset + 4]);
            float candidateV = Float.intBitsToFloat(
                    vertices[candidateOffset + 5]);
            if (Math.abs(candidateZ - minSourceZ) < 1.0E-4F) {
                lowU += candidateU;
                lowV += candidateV;
                lowCount++;
            }
            if (Math.abs(candidateZ - maxSourceZ) < 1.0E-4F) {
                highU += candidateU;
                highV += candidateV;
                highCount++;
            }
        }
        if (lowCount == 0 || highCount == 0) {
            return new float[]{originalU, originalV};
        }

        lowU /= lowCount;
        lowV /= lowCount;
        highU /= highCount;
        highV /= highCount;

        float sourceZ = Float.intBitsToFloat(vertices[offset + 2]);
        float sourceT = (sourceZ - minSourceZ) / sourceSpan;
        sourceT = Math.max(0.0F, Math.min(1.0F, sourceT));
        float croppedT = minZ + sourceT * (maxZ - minZ);

        float deltaU = highU - lowU;
        float deltaV = highV - lowV;
        if (Math.abs(deltaU) >= Math.abs(deltaV)) {
            return new float[]{
                    lowU + deltaU * croppedT,
                    originalV
            };
        }
        return new float[]{
                originalU,
                lowV + deltaV * croppedT
        };
    }}
