package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.config.ui.ConfigCenterNetwork;
import com.bl4ues.scpinventory.network.ModNetwork;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class CodexAssetClient {
    private static final Map<String, byte[]> BYTES = new HashMap<>();
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();
    private static final Set<String> REQUESTED = new HashSet<>();
    private static final Set<String> MISSING = new HashSet<>();
    private static final Map<String, PendingUpload> UPLOADS = new HashMap<>();

    private CodexAssetClient() {
    }

    public static void upload(String kind, String fileName, byte[] data,
                              Consumer<String> success, Consumer<String> failure) {
        String requestId = UUID.randomUUID().toString();
        UPLOADS.put(requestId, new PendingUpload(kind, data, success, failure));
        ModNetwork.CHANNEL.sendToServer(new ConfigCenterNetwork.AssetUploadRequest(
                requestId, kind, fileName, data));
    }

    public static void onUploadResult(ConfigCenterNetwork.AssetUploadResult result) {
        PendingUpload pending = UPLOADS.remove(result.requestId());
        if (pending == null) return;
        if (!result.success()) {
            if (pending.failure() != null) pending.failure().accept(result.message());
            return;
        }
        String cache = cacheKey(result.kind(), result.key());
        BYTES.put(cache, pending.data());
        REQUESTED.remove(cache);
        MISSING.remove(cache);
        if (pending.success() != null) pending.success().accept(result.key());
    }

    public static Optional<ResourceLocation> getTexture(String key) {
        if (key == null || key.isBlank()) return Optional.empty();
        String cache = cacheKey("image", key);
        ResourceLocation existing = TEXTURES.get(cache);
        if (existing != null) return Optional.of(existing);
        byte[] data = BYTES.get(cache);
        if (data == null) {
            if (MISSING.contains(cache)) return Optional.empty();
            request("image", key);
            return Optional.empty();
        }
        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(data));
            ResourceLocation location = new ResourceLocation("scp_additions",
                    "world_codex/" + Integer.toHexString(cache.hashCode()));
            Minecraft.getInstance().getTextureManager().register(location,
                    new DynamicTexture(image));
            TEXTURES.put(cache, location);
            return Optional.of(location);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public static Optional<String> getText(String key) {
        if (key == null || key.isBlank()) return Optional.empty();
        String cache = cacheKey("text", key);
        byte[] data = BYTES.get(cache);
        if (data == null) {
            if (MISSING.contains(cache)) return Optional.empty();
            request("text", key);
            return Optional.empty();
        }
        return Optional.of(new String(data, StandardCharsets.UTF_8));
    }

    public static void onAssetData(ConfigCenterNetwork.AssetData data) {
        String cache = cacheKey(data.kind(), data.key());
        REQUESTED.remove(cache);
        if (data.data().length > 0) {
            BYTES.put(cache, data.data());
            MISSING.remove(cache);
        } else {
            MISSING.add(cache);
        }
    }

    public static boolean isPending(String kind, String key) {
        return key != null && !key.isBlank()
                && REQUESTED.contains(cacheKey(kind, key));
    }

    public static boolean isMissing(String kind, String key) {
        return key != null && !key.isBlank()
                && MISSING.contains(cacheKey(kind, key));
    }

    public static void giveDocument(String itemId, String codexId,
                                    String displayName) {
        ModNetwork.CHANNEL.sendToServer(new ConfigCenterNetwork.GiveCodexItemRequest(
                itemId, codexId, displayName));
    }

    private static void request(String kind, String key) {
        String cache = cacheKey(kind, key);
        if (!REQUESTED.add(cache)) return;
        ModNetwork.CHANNEL.sendToServer(new ConfigCenterNetwork.AssetRequest(kind, key));
    }

    private static String cacheKey(String kind, String key) {
        String normalized = kind == null ? "" : kind.trim().toLowerCase(java.util.Locale.ROOT);
        if ("png".equals(normalized) || "jpg".equals(normalized)
                || "jpeg".equals(normalized)) normalized = "image";
        return normalized + ":" + (key == null ? "" : key);
    }

    private record PendingUpload(String kind, byte[] data,
                                 Consumer<String> success,
                                 Consumer<String> failure) {
    }
}
