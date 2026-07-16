package net.mcreator.scpadditions.config.ui;

import com.bl4ues.scpinventory.item.CodexDocumentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public final class CodexAssetStorage {
    public static final int MAX_PNG_BYTES = 900_000;
    public static final int MAX_TEXT_BYTES = 262_144;
    public static final int MAX_TRANSFER_BYTES = MAX_PNG_BYTES;
    private static final Pattern SAFE_KEY = Pattern.compile(
            "(?:images|texts)/[0-9a-fA-F-]{36}\\.(?:png|txt)");
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private CodexAssetStorage() {
    }

    public static UploadResult save(ServerPlayer player, String kind,
                                    String originalName, byte[] bytes) {
        if (!ConfigCenterService.canEdit(player)) {
            return UploadResult.failure("Operator permission level 2 is required.");
        }
        String normalized = normalizeKind(kind);
        if (normalized.isEmpty()) return UploadResult.failure("Unsupported Codex asset type.");
        byte[] data = bytes == null ? new byte[0] : bytes;
        int limit = "png".equals(normalized) ? MAX_PNG_BYTES : MAX_TEXT_BYTES;
        if (data.length == 0 || data.length > limit) {
            return UploadResult.failure("Codex asset exceeds the allowed size.");
        }
        if ("png".equals(normalized) && !hasPngSignature(data)) {
            return UploadResult.failure("The uploaded image is not a PNG file.");
        }
        if ("text".equals(normalized) && !isUtf8(data)) {
            return UploadResult.failure("The uploaded text is not valid UTF-8.");
        }
        try {
            MinecraftServer server = player.getServer();
            if (server == null) return UploadResult.failure("No server world is available.");
            String folder = "png".equals(normalized) ? "images" : "texts";
            String extension = "png".equals(normalized) ? ".png" : ".txt";
            String key = folder + "/" + UUID.randomUUID() + extension;
            Path target = resolve(server, key);
            Files.createDirectories(target.getParent());
            Path temp = Files.createTempFile(target.getParent(), "codex-", ".tmp");
            Files.write(temp, data);
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailure) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return UploadResult.success(key,
                    "Saved " + safeName(originalName) + " in this world's Codex assets.");
        } catch (Exception exception) {
            return UploadResult.failure("Could not save Codex asset: " + readable(exception));
        }
    }

    public static byte[] read(ServerPlayer player, String key) throws IOException {
        MinecraftServer server = player == null ? null : player.getServer();
        if (server == null || !isSafeKey(key)) return new byte[0];
        Path path = resolve(server, key);
        if (Files.notExists(path) || !Files.isRegularFile(path)) return new byte[0];
        long size = Files.size(path);
        if (size <= 0 || size > MAX_TRANSFER_BYTES) return new byte[0];
        return Files.readAllBytes(path);
    }

    public static boolean isSafeKey(String key) {
        return key != null && SAFE_KEY.matcher(key).matches();
    }

    public static boolean giveDocument(ServerPlayer player, String itemId,
                                       String codexId, String displayName) {
        if (!ConfigCenterService.canEdit(player) || codexId == null || codexId.isBlank()) {
            return false;
        }
        ResourceLocation id = ResourceLocation.tryParse(itemId == null ? "" : itemId);
        Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        if (item == null) return false;
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putString(CodexDocumentDefinition.UNIQUE_TAG, codexId.trim());
        if (displayName != null && !displayName.isBlank()) {
            stack.setHoverName(Component.literal(displayName.trim()));
        }
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        return true;
    }

    private static Path resolve(MinecraftServer server, String key) throws IOException {
        Path root = server.getWorldPath(LevelResource.ROOT)
                .resolve("scp_additions").resolve("codex_assets").normalize();
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) throw new IOException("Unsafe Codex asset path");
        return target;
    }

    private static String normalizeKind(String kind) {
        String value = kind == null ? "" : kind.trim().toLowerCase(Locale.ROOT);
        return "png".equals(value) || "text".equals(value) ? value : "";
    }

    private static boolean hasPngSignature(byte[] bytes) {
        if (bytes.length < PNG_SIGNATURE.length) return false;
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (bytes[i] != PNG_SIGNATURE[i]) return false;
        }
        return true;
    }

    private static boolean isUtf8(byte[] bytes) {
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException ignored) {
            return false;
        }
    }

    private static String safeName(String value) {
        if (value == null || value.isBlank()) return "asset";
        String cleaned = value.replaceAll("[^A-Za-z0-9._ -]", "_");
        return cleaned.length() <= 80 ? cleaned : cleaned.substring(0, 80);
    }

    private static String readable(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getSimpleName() : message;
    }

    public record UploadResult(boolean success, String key, String message) {
        public static UploadResult success(String key, String message) {
            return new UploadResult(true, key, message);
        }

        public static UploadResult failure(String message) {
            return new UploadResult(false, "", message);
        }
    }
}
