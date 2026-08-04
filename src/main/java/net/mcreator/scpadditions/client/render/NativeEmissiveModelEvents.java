package net.mcreator.scpadditions.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Adds a full-bright overlay to ordinary baked block and block-item models.
 *
 * <p>Source resources keep the convenient {@code *_e.png} authoring suffix.
 * Resource processing copies those masks to {@code *_native_emissive.png} and
 * explicitly stitches them into the block atlas. Keeping the runtime name
 * private prevents MoreMcmeta from drawing the same overlay a second time when
 * it happens to be installed.</p>
 *
 * <p>The overlay uses Forge's native maximum-lightmap quad transformer. It is
 * therefore visible without MoreMcmeta or a shader pack. Compatible shader
 * packs can additionally use the LabPBR emission written into the matching
 * specular map during resource processing.</p>
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NativeEmissiveModelEvents {
    private static final String RUNTIME_SUFFIX = "_native_emissive";
    private static final RenderType BLOCK_OVERLAY_TYPE = RenderType.cutout();
    private static final RenderType ITEM_OVERLAY_TYPE = Sheets.cutoutBlockSheet();
    private static final ChunkRenderTypeSet BLOCK_OVERLAY_TYPES =
            ChunkRenderTypeSet.of(BLOCK_OVERLAY_TYPE);
    private static final IQuadTransformer FULL_BRIGHT =
            QuadTransformers.applyingColor(0xFFFFFFFF)
                    .andThen(QuadTransformers.settingMaxEmissivity());

    private static final Set<String> OWNED_MODEL_NAMESPACES = Set.of(
            ScpAdditionsMod.MODID,
            "scp_ublocks",
            "scp_unity_extra_blocks");

    private NativeEmissiveModelEvents() {
    }

    @SubscribeEvent
    public static void modifyBakedModels(ModelEvent.ModifyBakingResult event) {
        event.getModels().replaceAll((location, model) ->
                shouldWrap(location, model)
                        ? new NativeEmissiveBakedModel(model)
                        : model);
    }

    private static boolean shouldWrap(ResourceLocation location,
            BakedModel model) {
        return OWNED_MODEL_NAMESPACES.contains(location.getNamespace())
                && !model.isCustomRenderer()
                && !(model instanceof NativeEmissiveBakedModel)
                && !(model instanceof EmissiveItemPassModel);
    }

    private static final class NativeEmissiveBakedModel
            extends BakedModelWrapper<BakedModel> {
        private final QuadOverlayCache overlays = new QuadOverlayCache();

        private NativeEmissiveBakedModel(BakedModel originalModel) {
            super(originalModel);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state,
                @Nullable Direction side, RandomSource random) {
            List<BakedQuad> base = originalModel.getQuads(state, side, random);
            return appendOverlays(base, overlays.forQuads(base));
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state,
                @Nullable Direction side, RandomSource random,
                ModelData modelData, @Nullable RenderType renderType) {
            if (renderType == null) {
                List<BakedQuad> base = originalModel.getQuads(state, side,
                        random, modelData, null);
                return appendOverlays(base, overlays.forQuads(base));
            }

            if (renderType == BLOCK_OVERLAY_TYPE) {
                List<BakedQuad> source = originalModel.getQuads(state, side,
                        random, modelData, null);
                List<BakedQuad> emissive = overlays.forQuads(source);

                // Preserve an existing cutout pass instead of replacing it.
                if (state != null && originalModel.getRenderTypes(state,
                        RandomSource.create(0L), modelData)
                        .contains(BLOCK_OVERLAY_TYPE)) {
                    List<BakedQuad> base = originalModel.getQuads(state, side,
                            random, modelData, renderType);
                    return appendOverlays(base, emissive);
                }
                return emissive;
            }

            return originalModel.getQuads(state, side, random, modelData,
                    renderType);
        }

        @Override
        public ChunkRenderTypeSet getRenderTypes(BlockState state,
                RandomSource random, ModelData modelData) {
            return ChunkRenderTypeSet.union(
                    originalModel.getRenderTypes(state, random, modelData),
                    BLOCK_OVERLAY_TYPES);
        }

        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack,
                boolean fabulous) {
            List<BakedModel> result = new ArrayList<>();
            for (BakedModel pass : originalModel.getRenderPasses(stack,
                    fabulous)) {
                result.add(pass);
                result.add(new EmissiveItemPassModel(pass, overlays));
            }
            return List.copyOf(result);
        }
    }

    /** Item rendering uses model passes rather than chunk render layers. */
    private static final class EmissiveItemPassModel
            extends BakedModelWrapper<BakedModel> {
        private final QuadOverlayCache overlays;

        private EmissiveItemPassModel(BakedModel originalModel,
                QuadOverlayCache overlays) {
            super(originalModel);
            this.overlays = overlays;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state,
                @Nullable Direction side, RandomSource random) {
            return overlays.forQuads(originalModel.getQuads(state, side,
                    random));
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state,
                @Nullable Direction side, RandomSource random,
                ModelData modelData, @Nullable RenderType renderType) {
            return overlays.forQuads(originalModel.getQuads(state, side,
                    random, modelData, null));
        }

        @Override
        public List<RenderType> getRenderTypes(ItemStack stack,
                boolean fabulous) {
            return List.of(ITEM_OVERLAY_TYPE);
        }

        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack,
                boolean fabulous) {
            return List.of(this);
        }
    }

    private static final class QuadOverlayCache {
        private final Map<BakedQuad, Optional<BakedQuad>> bySource =
                Collections.synchronizedMap(new IdentityHashMap<>());

        private List<BakedQuad> forQuads(List<BakedQuad> source) {
            if (source.isEmpty()) {
                return List.of();
            }

            List<BakedQuad> result = new ArrayList<>();
            for (BakedQuad quad : source) {
                BakedQuad overlay = forQuad(quad);
                if (overlay != null) {
                    result.add(overlay);
                }
            }
            return result.isEmpty() ? List.of() : List.copyOf(result);
        }

        @Nullable
        private BakedQuad forQuad(BakedQuad source) {
            Optional<BakedQuad> cached;
            synchronized (bySource) {
                cached = bySource.get(source);
                if (cached == null) {
                    cached = Optional.ofNullable(createOverlay(source));
                    bySource.put(source, cached);
                }
            }
            return cached.orElse(null);
        }
    }

    @Nullable
    private static BakedQuad createOverlay(BakedQuad source) {
        TextureAtlasSprite baseSprite = source.getSprite();
        ResourceLocation baseTexture = baseSprite.contents().name();
        if (baseTexture.getPath().endsWith(RUNTIME_SUFFIX)) {
            return null;
        }

        ResourceLocation emissiveTexture = new ResourceLocation(
                baseTexture.getNamespace(),
                baseTexture.getPath() + RUNTIME_SUFFIX);
        TextureAtlas atlas = Minecraft.getInstance().getModelManager()
                .getAtlas(TextureAtlas.LOCATION_BLOCKS);
        TextureAtlasSprite emissiveSprite = atlas.getSprite(emissiveTexture);

        // TextureAtlas#getSprite returns the missing sprite for unknown names.
        if (!emissiveTexture.equals(emissiveSprite.contents().name())) {
            return null;
        }

        int[] vertices = Arrays.copyOf(source.getVertices(),
                source.getVertices().length);
        for (int vertex = 0; vertex < 4; vertex++) {
            int uvOffset = vertex * IQuadTransformer.STRIDE
                    + IQuadTransformer.UV0;
            float sourceU = Float.intBitsToFloat(vertices[uvOffset]);
            float sourceV = Float.intBitsToFloat(vertices[uvOffset + 1]);
            float localU = baseSprite.getUOffset(sourceU);
            float localV = baseSprite.getVOffset(sourceV);
            vertices[uvOffset] = Float.floatToRawIntBits(
                    emissiveSprite.getU(localU));
            vertices[uvOffset + 1] = Float.floatToRawIntBits(
                    emissiveSprite.getV(localV));
        }

        BakedQuad overlay = new BakedQuad(vertices, -1,
                source.getDirection(), emissiveSprite, false, false);
        FULL_BRIGHT.processInPlace(overlay);
        return overlay;
    }

    private static List<BakedQuad> appendOverlays(List<BakedQuad> base,
            List<BakedQuad> overlays) {
        if (overlays.isEmpty()) {
            return base;
        }
        List<BakedQuad> result = new ArrayList<>(
                base.size() + overlays.size());
        result.addAll(base);
        result.addAll(overlays);
        return List.copyOf(result);
    }
}
