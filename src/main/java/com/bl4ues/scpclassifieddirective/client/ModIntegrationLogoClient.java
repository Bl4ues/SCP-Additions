package com.bl4ues.scpclassifieddirective.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Best-effort loader for logos declared by optional integrated mods. */
public final class ModIntegrationLogoClient {
    private static final Map<String, Optional<ResourceLocation>> CACHE =
            new HashMap<>();

    private ModIntegrationLogoClient() {
    }

    public static ResourceLocation logo(String modId) {
        if (modId == null || modId.isBlank()) return null;
        return CACHE.computeIfAbsent(modId,
                ModIntegrationLogoClient::load).orElse(null);
    }

    private static Optional<ResourceLocation> load(String modId) {
        if (!ModList.get().isLoaded(modId)) return Optional.empty();

        // Prefer normal namespace resources when a mod already ships a compact
        // icon there. This keeps the integration UI generic for future mods.
        Minecraft minecraft = Minecraft.getInstance();
        for (String candidate : new String[]{"icon.png", "logo.png",
                "textures/icon.png", "textures/logo.png"}) {
            ResourceLocation location = new ResourceLocation(modId, candidate);
            if (minecraft.getResourceManager().getResource(location).isPresent()) {
                return Optional.of(location);
            }
        }

        // Forge's Mod List accepts a root-level logoFile in mods.toml. MineZero
        // 1.1.0 currently does not declare one, but integrations that do can be
        // rendered without SCP: Classified Directive bundling or copying their artwork.
        try {
            Object container = ModList.get().getModContainerById(modId).orElse(null);
            if (container == null) return Optional.empty();
            Object modInfo = container.getClass().getMethod("getModInfo").invoke(container);
            Object logoValue = modInfo.getClass().getMethod("getLogoFile").invoke(modInfo);
            if (!(logoValue instanceof Optional<?> optional) || optional.isEmpty()) {
                return Optional.empty();
            }
            String logoPath = String.valueOf(optional.get());
            Object owningFile = modInfo.getClass().getMethod("getOwningFile").invoke(modInfo);
            Object modFile = owningFile.getClass().getMethod("getFile").invoke(owningFile);

            Method findResource = null;
            for (Method method : modFile.getClass().getMethods()) {
                if ("findResource".equals(method.getName())
                        && method.getParameterCount() == 1
                        && method.getParameterTypes()[0].isArray()) {
                    findResource = method;
                    break;
                }
            }
            if (findResource == null) return Optional.empty();
            Object found = findResource.invoke(modFile,
                    (Object) new String[]{logoPath});
            if (!(found instanceof Path path) || !Files.isRegularFile(path)) {
                return Optional.empty();
            }

            try (InputStream stream = Files.newInputStream(path)) {
                NativeImage image = NativeImage.read(stream);
                DynamicTexture texture = new DynamicTexture(image);
                ResourceLocation registered = minecraft.getTextureManager().register(
                        "scp_additions_integration_" + modId, texture);
                return Optional.of(registered);
            }
        } catch (ReflectiveOperationException | java.io.IOException exception) {
            ScpClassifiedDirectiveMod.LOGGER.debug(
                    "Could not load optional integration logo for {}", modId,
                    exception);
            return Optional.empty();
        }
    }
}
