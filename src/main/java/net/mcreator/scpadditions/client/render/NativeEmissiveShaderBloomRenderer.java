package net.mcreator.scpadditions.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Draws native block emissive masks through Minecraft's eyes render type while
 * a shader pack is active.
 *
 * <p>A maximum lightmap value is sufficient for vanilla full-bright rendering,
 * but shader packs usually reserve HDR emission and bloom for a separate
 * emissive render program. GeckoLib glow masks use that program through
 * {@link RenderType#eyes(ResourceLocation)}. This renderer reuses the same path
 * for ordinary baked blocks without converting them into block entities.</p>
 *
 * <p>Emissive positions are cached per client chunk. Loaded chunks are scanned
 * once, then one chunk in a rotating 3x3 area around the player is refreshed per
 * client tick so placed, removed and state-swapped blocks update promptly without
 * scanning the world every frame.</p>
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class NativeEmissiveShaderBloomRenderer {
    private static final String RUNTIME_SUFFIX = "_native_emissive";
    private static final String TEXTURE_MANIFEST =
            "/assets/scp_additions/native_emissive_textures.txt";
    private static final long INSPECTION_SEED = 42L;
    private static final Direction[] ALL_FACES_AND_NULL =
            Arrays.copyOf(Direction.values(), Direction.values().length + 1);
    private static final int[][] RESCAN_OFFSETS = {
            {0, 0}, {1, 0}, {0, 1}, {-1, 0}, {0, -1},
            {1, 1}, {-1, 1}, {-1, -1}, {1, -1}
    };

    private static final RenderType SHADER_EMISSIVE_TYPE =
            RenderType.eyes(TextureAtlas.LOCATION_BLOCKS);
    private static final IQuadTransformer FULL_BRIGHT =
            QuadTransformers.applyingColor(0xFFFFFFFF)
                    .andThen(QuadTransformers.settingMaxEmissivity());
    private static final Set<ResourceLocation> EMISSIVE_BASE_TEXTURES =
            loadEmissiveTextureManifest();

    private static final Map<BlockState, Boolean> EMISSIVE_STATE_CACHE =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<BakedQuad, Optional<BakedQuad>> OVERLAY_CACHE =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<Long, List<BlockPos>> CHUNK_EMISSIVES =
            new HashMap<>();

    @Nullable
    private static ClientLevel cachedLevel;
    private static int rescanCursor;
    private static boolean previousShaderPackState;
    private static boolean shaderStateInitialized;

    private NativeEmissiveShaderBloomRenderer() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ClientLevel level
                && event.getChunk() instanceof LevelChunk chunk) {
            ensureLevel(level);
            scanChunk(level, chunk);
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel level) {
            ensureLevel(level);
            synchronized (CHUNK_EMISSIVES) {
                CHUNK_EMISSIVES.remove(event.getChunk().getPos().toLong());
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean shaderPackState = isShaderPackInUse();
        if (!shaderStateInitialized) {
            previousShaderPackState = shaderPackState;
            shaderStateInitialized = true;
        } else if (previousShaderPackState != shaderPackState) {
            previousShaderPackState = shaderPackState;
            // Chunk meshes contain the native full-bright overlay only while
            // shaders are disabled. Rebuild once when the active shader state
            // changes so stale coplanar geometry cannot survive a mode switch.
            if (minecraft.level != null) {
                minecraft.levelRenderer.allChanged();
            }
        }

        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            clearLevelCache();
            return;
        }

        ensureLevel(level);
        int[] offset = RESCAN_OFFSETS[
                Math.floorMod(rescanCursor++, RESCAN_OFFSETS.length)];
        int chunkX = minecraft.player.chunkPosition().x + offset[0];
        int chunkZ = minecraft.player.chunkPosition().z + offset[1];
        if (level.hasChunk(chunkX, chunkZ)) {
            scanChunk(level, level.getChunk(chunkX, chunkZ));
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage()
                != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES
                || EMISSIVE_BASE_TEXTURES.isEmpty()
                || !ShaderBridge.isShaderPackInUse()
                || ShaderBridge.isRenderingShadowPass()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        ensureLevel(level);

        List<BlockPos> positions = snapshotPositions();
        if (positions.isEmpty()) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        int renderDistance = minecraft.options.renderDistance().get() * 16 + 16;
        double renderDistanceSqr = (double) renderDistance * renderDistance;
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers =
                minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(SHADER_EMISSIVE_TYPE);
        RandomSource random = RandomSource.create();
        int renderedQuads = 0;

        for (BlockPos pos : positions) {
            double centerX = pos.getX() + 0.5 - camera.x;
            double centerY = pos.getY() + 0.5 - camera.y;
            double centerZ = pos.getZ() + 0.5 - camera.z;
            if (centerX * centerX + centerY * centerY + centerZ * centerZ
                    > renderDistanceSqr) {
                continue;
            }
            if (!event.getFrustum().isVisible(new AABB(pos).inflate(0.05))) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (!stateHasEmission(state)) {
                continue;
            }

            BakedModel model = minecraft.getBlockRenderer()
                    .getBlockModel(state);
            ModelData modelData = model.getModelData(level, pos, state,
                    ModelData.EMPTY);
            long seed = state.getSeed(pos);

            poseStack.pushPose();
            poseStack.translate(pos.getX() - camera.x,
                    pos.getY() - camera.y,
                    pos.getZ() - camera.z);

            for (Direction side : ALL_FACES_AND_NULL) {
                if (side != null && !Block.shouldRenderFace(state, level, pos,
                        side, pos.relative(side))) {
                    continue;
                }

                random.setSeed(seed);
                List<BakedQuad> source = model.getQuads(state, side, random,
                        modelData, null);
                for (BakedQuad quad : source) {
                    BakedQuad overlay = overlayFor(quad);
                    if (overlay == null) {
                        continue;
                    }
                    consumer.putBulkData(poseStack.last(), overlay,
                            1.0F, 1.0F, 1.0F, 1.0F,
                            LightTexture.FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY, true);
                    renderedQuads++;
                }
            }

            poseStack.popPose();
        }

        if (renderedQuads > 0) {
            buffers.endBatch(SHADER_EMISSIVE_TYPE);
        }
    }

    private static void scanChunk(ClientLevel level, LevelChunk chunk) {
        List<BlockPos> found = new ArrayList<>();
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        LevelChunkSection[] sections = chunk.getSections();

        for (int sectionIndex = 0;
                sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section == null || section.hasOnlyAir()
                    || !section.maybeHas(
                            NativeEmissiveShaderBloomRenderer::stateHasEmission)) {
                continue;
            }

            int sectionY = level.getSectionYFromSectionIndex(sectionIndex);
            int minY = SectionPos.sectionToBlockCoord(sectionY);
            for (int localY = 0; localY < 16; localY++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    for (int localX = 0; localX < 16; localX++) {
                        BlockState state = section.getBlockState(
                                localX, localY, localZ);
                        if (stateHasEmission(state)) {
                            found.add(new BlockPos(minX + localX,
                                    minY + localY, minZ + localZ));
                        }
                    }
                }
            }
        }

        synchronized (CHUNK_EMISSIVES) {
            if (found.isEmpty()) {
                CHUNK_EMISSIVES.remove(chunk.getPos().toLong());
            } else {
                CHUNK_EMISSIVES.put(chunk.getPos().toLong(),
                        List.copyOf(found));
            }
        }
    }

    private static boolean stateHasEmission(BlockState state) {
        if (state.isAir()) {
            return false;
        }

        synchronized (EMISSIVE_STATE_CACHE) {
            Boolean cached = EMISSIVE_STATE_CACHE.get(state);
            if (cached != null) {
                return cached;
            }

            BakedModel model = Minecraft.getInstance().getBlockRenderer()
                    .getBlockModel(state);
            boolean emissive = modelHasEmissiveQuads(model, state);
            EMISSIVE_STATE_CACHE.put(state, emissive);
            return emissive;
        }
    }

    private static boolean modelHasEmissiveQuads(BakedModel model,
            BlockState state) {
        RandomSource random = RandomSource.create(INSPECTION_SEED);
        for (Direction side : ALL_FACES_AND_NULL) {
            random.setSeed(INSPECTION_SEED);
            for (BakedQuad quad : model.getQuads(state, side, random,
                    ModelData.EMPTY, null)) {
                if (EMISSIVE_BASE_TEXTURES.contains(
                        quad.getSprite().contents().name())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    private static BakedQuad overlayFor(BakedQuad source) {
        synchronized (OVERLAY_CACHE) {
            Optional<BakedQuad> cached = OVERLAY_CACHE.get(source);
            if (cached == null) {
                cached = Optional.ofNullable(createOverlay(source));
                OVERLAY_CACHE.put(source, cached);
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

    private static Set<ResourceLocation> loadEmissiveTextureManifest() {
        InputStream stream = NativeEmissiveShaderBloomRenderer.class
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

    private static List<BlockPos> snapshotPositions() {
        synchronized (CHUNK_EMISSIVES) {
            int size = CHUNK_EMISSIVES.values().stream()
                    .mapToInt(List::size).sum();
            List<BlockPos> result = new ArrayList<>(size);
            CHUNK_EMISSIVES.values().forEach(result::addAll);
            return result;
        }
    }

    private static void ensureLevel(ClientLevel level) {
        if (cachedLevel != level) {
            clearLevelCache();
            cachedLevel = level;
        }
    }

    private static void clearLevelCache() {
        synchronized (CHUNK_EMISSIVES) {
            CHUNK_EMISSIVES.clear();
        }
        cachedLevel = null;
        rescanCursor = 0;
    }

    private static void clearModelCaches() {
        EMISSIVE_STATE_CACHE.clear();
        OVERLAY_CACHE.clear();
        clearLevelCache();
    }

    /** Clears baked-quad identities after every model reload. */
    @Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
            bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModelLifecycle {
        private ModelLifecycle() {
        }

        @SubscribeEvent
        public static void onModelsModified(
                ModelEvent.ModifyBakingResult event) {
            clearModelCaches();
        }
    }

    /** Shared shader-state query used by the baked-model fallback pass. */
    static boolean isShaderPackInUse() {
        return ShaderBridge.isShaderPackInUse();
    }

    /** Optional Iris/Oculus bridge kept reflection-only for a clean runtime. */
    private static final class ShaderBridge {
        @Nullable
        private static final Object API;
        @Nullable
        private static final Method IS_SHADER_PACK_IN_USE;
        @Nullable
        private static final Method IS_RENDERING_SHADOW_PASS;

        static {
            Object api = null;
            Method inUse = null;
            Method shadowPass = null;
            String[] apiClasses = {
                    "net.irisshaders.iris.api.v0.IrisApi",
                    "net.coderbot.iris.api.v0.IrisApi"
            };

            for (String className : apiClasses) {
                try {
                    Class<?> apiClass = Class.forName(className);
                    api = apiClass.getMethod("getInstance").invoke(null);
                    inUse = apiClass.getMethod("isShaderPackInUse");
                    try {
                        shadowPass = apiClass.getMethod(
                                "isRenderingShadowPass");
                    } catch (ReflectiveOperationException ignored) {
                        shadowPass = null;
                    }
                    break;
                } catch (ReflectiveOperationException
                        | LinkageError ignored) {
                    api = null;
                    inUse = null;
                    shadowPass = null;
                }
            }

            API = api;
            IS_SHADER_PACK_IN_USE = inUse;
            IS_RENDERING_SHADOW_PASS = shadowPass;
        }

        private ShaderBridge() {
        }

        private static boolean isShaderPackInUse() {
            return invokeBoolean(IS_SHADER_PACK_IN_USE, false);
        }

        private static boolean isRenderingShadowPass() {
            return invokeBoolean(IS_RENDERING_SHADOW_PASS, false);
        }

        private static boolean invokeBoolean(@Nullable Method method,
                boolean fallback) {
            if (API == null || method == null) {
                return fallback;
            }
            try {
                Object result = method.invoke(API);
                return result instanceof Boolean value ? value : fallback;
            } catch (ReflectiveOperationException | LinkageError ignored) {
                return fallback;
            }
        }
    }
}
