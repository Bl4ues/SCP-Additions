package com.bl4ues.scpclassifieddirective.client.render;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.IQuadTransformer;
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

/**
 * Keeps SCP-079's authored floor-cable texture visibly flush with the floor
 * without leaving its zero-thickness plane coplanar with the block below.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class Scp079CablePlaneModelEvents {
    // 1/32 of one authored model unit. Vanilla model coordinates use 16 units
    // per block, so this is only 1/512 block above the floor.
    private static final float CABLE_PLANE_OFFSET = 1.0F / 512.0F;
    private static final float EPSILON = 1.0F / 4096.0F;

    private Scp079CablePlaneModelEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void modifyBakedModels(ModelEvent.ModifyBakingResult event) {
        event.getModels().replaceAll((location, model) ->
                shouldWrap(location, model)
                        ? new RaisedCablePlaneModel(model) : model);
    }

    private static boolean shouldWrap(ResourceLocation location,
            BakedModel model) {
        if (!ScpClassifiedDirectiveMod.MODID.equals(location.getNamespace())
                || model instanceof RaisedCablePlaneModel) {
            return false;
        }
        String path = location.getPath();
        return path.equals("scp_079off") || path.equals("scp_079on");
    }

    private static final class RaisedCablePlaneModel
            extends BakedModelWrapper<BakedModel> {
        private final Map<BakedQuad, BakedQuad> cache =
                Collections.synchronizedMap(new IdentityHashMap<>());

        private RaisedCablePlaneModel(BakedModel originalModel) {
            super(originalModel);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state,
                @Nullable Direction side, RandomSource random) {
            return raiseCablePlane(originalModel.getQuads(state, side, random));
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state,
                @Nullable Direction side, RandomSource random,
                ModelData modelData, @Nullable RenderType renderType) {
            return raiseCablePlane(originalModel.getQuads(state, side, random,
                    modelData, renderType));
        }

        private List<BakedQuad> raiseCablePlane(List<BakedQuad> source) {
            if (source.isEmpty()) return source;
            List<BakedQuad> result = null;
            for (int index = 0; index < source.size(); index++) {
                BakedQuad quad = source.get(index);
                BakedQuad raised = transformed(quad);
                if (raised != quad) {
                    if (result == null) result = new ArrayList<>(source);
                    result.set(index, raised);
                }
            }
            return result == null ? source : List.copyOf(result);
        }

        private BakedQuad transformed(BakedQuad source) {
            synchronized (cache) {
                BakedQuad cached = cache.get(source);
                if (cached != null) return cached;
                BakedQuad transformed = isCablePlane(source)
                        ? raise(source) : source;
                cache.put(source, transformed);
                return transformed;
            }
        }
    }

    private static boolean isCablePlane(BakedQuad quad) {
        int[] vertices = quad.getVertices();
        int stride = vertices.length / 4;
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            float x = Float.intBitsToFloat(vertices[offset]);
            float y = Float.intBitsToFloat(vertices[offset + 1]);
            float z = Float.intBitsToFloat(vertices[offset + 2]);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        return Math.abs(minY) <= EPSILON && Math.abs(maxY) <= EPSILON
                && minX <= EPSILON && maxX >= 1.0F - EPSILON
                && minZ <= EPSILON && maxZ >= 1.0F - EPSILON;
    }

    private static BakedQuad raise(BakedQuad source) {
        int[] vertices = source.getVertices().clone();
        for (int vertex = 0; vertex < 4; vertex++) {
            int positionOffset = vertex * IQuadTransformer.STRIDE;
            float y = Float.intBitsToFloat(vertices[positionOffset + 1]);
            vertices[positionOffset + 1] = Float.floatToRawIntBits(
                    y + CABLE_PLANE_OFFSET);
        }
        return new BakedQuad(vertices, source.getTintIndex(),
                source.getDirection(), source.getSprite(), source.isShade(),
                source.hasAmbientOcclusion());
    }
}
