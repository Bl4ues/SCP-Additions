package com.bl4ues.scpclassifieddirective.client.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Makes the authored zero-thickness auxiliary-generator panels visible from
 * both sides without thickening or moving their geometry.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AuxiliaryGeneratorDoubleSidedModels {
    private static final String MODEL_PATH = "scp_079_auxiliary_power";

    private AuxiliaryGeneratorDoubleSidedModels() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void modifyBakedModels(ModelEvent.ModifyBakingResult event) {
        event.getModels().replaceAll((location, model) ->
                shouldWrap(location, model)
                        ? new DoubleSidedModel(model) : model);
    }

    private static boolean shouldWrap(ResourceLocation location,
            BakedModel model) {
        return ScpClassifiedDirectiveMod.MODID.equals(location.getNamespace())
                && location.getPath().contains(MODEL_PATH)
                && !(model instanceof DoubleSidedModel);
    }

    private static final class DoubleSidedModel
            extends BakedModelWrapper<BakedModel> {
        private final Map<BakedQuad, BakedQuad> reversed =
                Collections.synchronizedMap(new IdentityHashMap<>());

        private DoubleSidedModel(BakedModel originalModel) {
            super(originalModel);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state,
                @Nullable Direction side, RandomSource random) {
            return doubleSided(originalModel.getQuads(state, side, random));
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state,
                @Nullable Direction side, RandomSource random,
                ModelData modelData, @Nullable RenderType renderType) {
            return doubleSided(originalModel.getQuads(state, side, random,
                    modelData, renderType));
        }

        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack,
                boolean fabulous) {
            return List.of(this);
        }

        private List<BakedQuad> doubleSided(List<BakedQuad> source) {
            if (source.isEmpty()) return source;
            List<BakedQuad> result = new ArrayList<>(source.size() * 2);
            for (BakedQuad quad : source) {
                result.add(quad);
                result.add(reversed.computeIfAbsent(quad,
                        DoubleSidedModel::reverse));
            }
            return List.copyOf(result);
        }

        private static BakedQuad reverse(BakedQuad source) {
            int[] original = source.getVertices();
            int stride = original.length / 4;
            int[] vertices = new int[original.length];
            int[] order = {0, 3, 2, 1};
            for (int vertex = 0; vertex < 4; vertex++) {
                System.arraycopy(original, order[vertex] * stride,
                        vertices, vertex * stride, stride);
            }
            return new BakedQuad(vertices, source.getTintIndex(),
                    source.getDirection().getOpposite(),
                    source.getSprite(), source.isShade(),
                    source.hasAmbientOcclusion());
        }
    }
}
