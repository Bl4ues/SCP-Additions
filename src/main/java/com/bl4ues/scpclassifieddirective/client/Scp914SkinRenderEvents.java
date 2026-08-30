package com.bl4ues.scpclassifieddirective.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.data.Scp914SkinManager;
import com.bl4ues.scpclassifieddirective.equipment.HazmatSuitAccess;
import com.bl4ues.scpclassifieddirective.network.ScpClassifiedDirectiveModVariables;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Mod.EventBusSubscriber(
        modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp914SkinRenderEvents {
    private static final String KLEIDERS_MOD_ID = "kleiders_custom_renderer";
    private static final String RENDERER_CLASS =
            "com.kleiders.kleidersplayerrenderer.ClassicPlayerRenderer";
    private static final ThreadLocal<Boolean> RENDERING =
            ThreadLocal.withInitial(() -> false);
    private static final Map<String, CachedRenderer> CACHE = new HashMap<>();
    private static boolean missingRendererLogged;

    private Scp914SkinRenderEvents() {
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (RENDERING.get() || !ModList.get().isLoaded(KLEIDERS_MOD_ID)) {
            return;
        }

        if (!(event.getEntity() instanceof AbstractClientPlayer player)) {
            return;
        }
        ScpClassifiedDirectiveModVariables.PlayerVariables variables = player
                .getCapability(ScpClassifiedDirectiveModVariables.PLAYER_VARIABLES_CAPABILITY)
                .orElse(null);
        if (variables == null || variables.scp914Skin == null
                || variables.scp914Skin.isBlank()) {
            return;
        }

        CachedRenderer cached = rendererFor(variables.scp914Skin);
        if (cached == null) {
            return;
        }

        PlayerModel<?> customModel = cached.playerModel();
        HazmatSuitClientEvents.OuterLayerVisibility hiddenOuterLayers = null;
        if (customModel != null && HazmatSuitAccess.isFullyEquipped(player)) {
            hiddenOuterLayers = HazmatSuitClientEvents.hideOuterLayers(customModel);
        }

        event.setCanceled(true);
        RENDERING.set(true);
        try {
            cached.renderMethod().invoke(
                    cached.renderer(),
                    player,
                    player.getYRot(),
                    event.getPartialTick(),
                    event.getPoseStack(),
                    event.getMultiBufferSource(),
                    event.getPackedLight());
        } catch (Exception exception) {
            CACHE.remove(variables.scp914Skin);
            ScpClassifiedDirectiveMod.LOGGER.error(
                    "Failed to render SCP-914 skin {} for {}",
                    variables.scp914Skin, player.getScoreboardName(), exception);
        } finally {
            if (customModel != null && hiddenOuterLayers != null) {
                HazmatSuitClientEvents.restoreOuterLayers(
                        customModel, hiddenOuterLayers);
            }
            RENDERING.set(false);
        }
    }

    private static CachedRenderer rendererFor(String fileName) {
        Path path = Scp914SkinManager.resolveSkin(fileName);
        if (path == null) {
            return null;
        }

        try {
            long modified = Files.getLastModifiedTime(path).toMillis();
            CachedRenderer existing = CACHE.get(fileName);
            if (existing != null && existing.modified() == modified) {
                return existing;
            }

            CachedRenderer loaded = loadRenderer(fileName, path, modified);
            if (loaded != null) {
                CACHE.put(fileName, loaded);
            }
            return loaded;
        } catch (Exception exception) {
            ScpClassifiedDirectiveMod.LOGGER.error(
                    "Failed to load SCP-914 skin {}", path, exception);
            return null;
        }
    }

    private static CachedRenderer loadRenderer(
            String fileName, Path path, long modified) throws Exception {
        NativeImage image;
        try (InputStream input = Files.newInputStream(path)) {
            image = NativeImage.read(input);
        }

        int width = image.getWidth();
        int height = image.getHeight();
        if (width != 64 || (height != 64 && height != 32)) {
            image.close();
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Ignoring SCP-914 skin {}: expected 64x64 or 64x32, found {}x{}",
                    path, width, height);
            return null;
        }

        Minecraft minecraft = Minecraft.getInstance();
        String hash = Integer.toUnsignedString(
                (fileName.toLowerCase(Locale.ROOT) + ":" + modified).hashCode(), 16);
        ResourceLocation textureId = new ResourceLocation(
                ScpClassifiedDirectiveMod.MODID, "dynamic/scp914_skins/" + hash);
        minecraft.getTextureManager().register(
                textureId, new DynamicTexture(image));

        Class<?> rendererType;
        try {
            rendererType = Class.forName(RENDERER_CLASS);
        } catch (ClassNotFoundException exception) {
            if (!missingRendererLogged) {
                missingRendererLogged = true;
                ScpClassifiedDirectiveMod.LOGGER.warn(
                        "Kleiders Custom Renderer is loaded, but {} was not found; "
                                + "SCP-914 custom skins will not render",
                        RENDERER_CLASS);
            }
            return null;
        }

        EntityRenderDispatcher dispatcher =
                minecraft.getEntityRenderDispatcher();
        EntityRendererProvider.Context context =
                new EntityRendererProvider.Context(
                        dispatcher,
                        minecraft.getItemRenderer(),
                        minecraft.getBlockRenderer(),
                        dispatcher.getItemInHandRenderer(),
                        minecraft.getResourceManager(),
                        minecraft.getEntityModels(),
                        minecraft.font);

        Constructor<?> constructor = rendererType.getConstructor(
                EntityRendererProvider.Context.class, ResourceLocation.class);
        Object renderer = constructor.newInstance(context, textureId);
        Method renderMethod = rendererType.getMethod(
                "render",
                AbstractClientPlayer.class,
                float.class,
                float.class,
                com.mojang.blaze3d.vertex.PoseStack.class,
                MultiBufferSource.class,
                int.class);

        PlayerModel<?> playerModel = null;
        try {
            Object resolvedModel = rendererType.getMethod("getModel").invoke(renderer);
            if (resolvedModel instanceof PlayerModel<?> model) {
                playerModel = model;
            }
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.debug(
                    "Kleiders renderer {} does not expose a PlayerModel; "
                            + "Hazmat outer-layer suppression will use the vanilla path only",
                    RENDERER_CLASS);
        }

        return new CachedRenderer(
                modified, renderer, renderMethod, playerModel);
    }

    private record CachedRenderer(
            long modified,
            Object renderer,
            Method renderMethod,
            PlayerModel<?> playerModel) {
    }
}
