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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adds a full-bright overlay to ordinary baked block and block-item models.
 *
 * <p>Source resources keep the convenient {@code *_e.png} authoring suffix.
 * Resource processing copies block masks to {@code *_native_emissive.png},
 * explicitly stitches them into the block atlas and writes a compact manifest
 * of base textures. Keeping the runtime name private prevents external
 * emissive-texture loaders from drawing the same overlay a second time.</p>
 *
 * <p>The overlay uses Forge's native maximum-lightmap quad transformer. It is
 * therefore visible without an external emissive-texture mod or shader pack. Compatible shader
 * packs can additionally use the LabPBR emission written into the matching
 * specular map during resource processing.</p>
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NativeEmissiveModelEvents {
    private static final String RUNTIME_SUFFIX = "_native_emissive";
    private static final String TEXTURE_MANIFEST =
            "/assets/scp_additions/native_emissive_textures.txt";
    private static final RenderType BLOCK_OVERLAY_TYPE = RenderType.cutout();
    private static final RenderType ITEM_OVERLAY_TYPE = Sheets.cutoutBlockSheet();
    private static final ChunkRenderTypeSet BLOCK_OVERLAY_TYPES =
            ChunkRenderTypeSet.of(BLOCK_OVERLAY_TYPE);
    private static final IQuadTransformer FULL_BRIGHT =
            QuadTransformers.applyingColor(0xFFFFFFFF)
                    .andThen(QuadTransformers.settingMaxEmissivity());

    private static final Set<String> OWNED_MODEL_NAMESPACES = Set.of(
            ScpAdditionsMod.MODID,
            "scp_keycards",
            "scp_ublocks",
            "scp_unity_extra_blocks");
    private static final Set<ResourceLocation> EMISSIVE_BASE_TEXTURES =
            loadEmissiveTextureManifest();

    private NativeEmissiveModelEvents() {
    }

    @SubscribeEvent
    public static void modifyBakedModels(ModelEvent.ModifyBakingResult event) {
        if (EMISSIVE_BASE_TEXTURES.isEmpty()) {
            return;
        }
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

    private static Set<ResourceLocation> loadEmissiveTextureManifest() {
        InputStream stream = NativeEmissiveModelEvents.class
                .getResourceAsStream(TEXTURE_MANIFEST);
        if (stream == null) {
            return Set.of();
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                stream, StandardCharsets.UTF_8))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(ResourceLocation::tryParse)
                    .filter(location -> location != null)
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to read native emissive texture manifest",
                    exception);
        }
    }

    private static final class NativeEmissiveBakedModel
            extends BakedModelWrapper<BakedModel> {
        private final QuadOverlayCache overlays = new QuadOverlayCache();
        private final Map<BlockState, Boolean> blockEmission =
                Collections.synchronizedMap(new IdentityHashMap<>());
        private final Map<BakedModel, Boolean> itemEmission =
                Collections.synchronizedMap(new IdentityHashMap<>());
        private final Map<BakedModel, BakedModel> itemOverlayPasses =
                Collections.synchronizedMap(new IdentityHashMap<>());

        private NativeEmissiveBakedModel(BakedModel originalModel) {
            super(originalModel);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state,
                @Nullable Direction side, RandomSource random) {
            // Native overlays are supplied through a dedicated block layer or
            // item render pass. Returning them here would draw them twice in
            // fallback render paths.
            return originalModel.getQuads(state, side, random);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state,
                @Nullable Direction side, RandomSource random,
                ModelData modelData, @Nullable RenderType renderType) {
            if (renderType == null) {
                return originalModel.getQuads(state, side, random, modelData,
                        null);
            }

            if (renderType == BLOCK_OVERLAY_TYPE) {
                // Shader bloom uses a separate eyes/spidereyes pass. Keeping
                // this coplanar full-bright overlay at the same time causes
                // depth flicker on bright masks, so the chunk pass supplies
                // only the model's original cutout geometry while shaders run.
                if (NativeEmissiveShaderBloomRenderer.isShaderPackInUse()) {
                    return originalModel.getQuads(state, side, random,
                            modelData, renderType);
                }

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
            ChunkRenderTypeSet original = originalModel.getRenderTypes(state,
                    random, modelData);
            if (NativeEmissiveShaderBloomRenderer.isShaderPackInUse()
                    || !hasBlockEmission(state, modelData)) {
                return original;
            }
            return ChunkRenderTypeSet.union(original, BLOCK_OVERLAY_TYPES);
        }

        private boolean hasBlockEmission(BlockState state,
                ModelData modelData) {
            Boolean cached;
            synchronized (blockEmission) {
                cached = blockEmission.get(state);
                if (cached == null) {
                    cached = overlays.modelHasEmissiveQuads(originalModel,
                            state, modelData);
                    blockEmission.put(state, cached);
                }
            }
            return cached;
        }

        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack,
                boolean fabulous) {
            List<BakedModel> result = new ArrayList<>();
            for (BakedModel pass : originalModel.getRenderPasses(stack,
                    fabulous)) {
                result.add(pass);
                if (hasItemEmission(pass)) {
                    result.add(itemOverlayPass(pass));
                }
            }
            return List.copyOf(result);
        }

        private boolean hasItemEmission(BakedModel pass) {
            Boolean cached;
            synchronized (itemEmission) {
                cached = itemEmission.get(pass);
                if (cached == null) {
                    cached = overlays.modelHasEmissiveQuads(pass, null,
                            ModelData.EMPTY);
                    itemEmission.put(pass, cached);
                }
            }
            return cached;
        }

        private BakedModel itemOverlayPass(BakedModel pass) {
            synchronized (itemOverlayPasses) {
                return itemOverlayPasses.computeIfAbsent(pass,
                        model -> new EmissiveItemPassModel(model, overlays));
            }
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
        private static final long INSPECTION_SEED = 42L;
        private final Map<BakedQuad, Optional<BakedQuad>> bySource =
                Collections.synchronizedMap(new IdentityHashMap<>());

        private boolean modelHasEmissiveQuads(BakedModel model,
                @Nullable BlockState state, ModelData modelData) {
            RandomSource random = RandomSource.create(INSPECTION_SEED);
            if (containsEmissiveSprite(model.getQuads(state, null, random,
                    modelData, null))) {
                return true;
            }
            for (Direction side : Direction.values()) {
                random.setSeed(INSPECTION_SEED);
                if (containsEmissiveSprite(model.getQuads(state, side, random,
                        modelData, null))) {
                    return true;
                }
            }
            return false;
        }

        private boolean containsEmissiveSprite(List<BakedQuad> quads) {
            for (BakedQuad quad : quads) {
                if (EMISSIVE_BASE_TEXTURES.contains(
                        quad.getSprite().contents().name())) {
                    return true;
                }
            }
            return false;
        }

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
        if (!EMISSIVE_BASE_TEXTURES.contains(baseTexture)
                || baseTexture.getPath().endsWith(RUNTIME_SUFFIX)) {
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
