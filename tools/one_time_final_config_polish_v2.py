from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, content: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")


def replace_once(path: str, old: str, new: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"Expected one occurrence in {path}, found {count}: {old[:140]!r}")
    write(path, text.replace(old, new, 1))


def replace_between(path: str, start: str, end: str, replacement: str) -> None:
    text = read(path)
    a = text.find(start)
    if a < 0:
        raise RuntimeError(f"Start marker not found in {path}: {start!r}")
    b = text.find(end, a)
    if b < 0:
        raise RuntimeError(f"End marker not found in {path}: {end!r}")
    write(path, text[:a] + replacement + text[b:])


# ---------------------------------------------------------------------------
# Mod metadata and logo
# ---------------------------------------------------------------------------
mods = "src/main/resources/META-INF/mods.toml"
replace_once(
    mods,
    '''authors="Bl4ues"
description='''
SCP Additions 3.0.3 — Quality of Life Update.
Combines the existing SCP Additions content with the SCP Inventory gameplay systems and SCP Unity-inspired facility content.
''' ''',
    '''authors="Bl4ues"
logoFile="logo.png"
description='''
SCP Additions is an SCP survival horror and facility-building mod for Minecraft 1.20.1. Inspired by SCP: Containment Breach and SCP Unity, it combines functional SCPs and containment machinery with a custom inventory, survival systems, keycard security, animated doors, and a large collection of facility-building content.
''' ''')
shutil.copyfile(
    ROOT / "src/main/resources/assets/scp_additions/textures/screens/logo.png",
    ROOT / "src/main/resources/logo.png")


# ---------------------------------------------------------------------------
# Runtime Codex definition: world-backed assets and optional unique matching
# ---------------------------------------------------------------------------
write("src/main/java/com/bl4ues/scpinventory/item/CodexDocumentDefinition.java", r'''package com.bl4ues.scpinventory.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class CodexDocumentDefinition {

    public static final String UNIQUE_TAG = "ScpCodexId";
    private static final int DEFAULT_IMAGE_WIDTH = 1279;
    private static final int DEFAULT_IMAGE_HEIGHT = 1920;

    private final ResourceLocation itemId;
    private final String category;
    private final String displayName;
    private final ResourceLocation imageLocation;
    private final ResourceLocation textLocation;
    private final String worldImageKey;
    private final String worldTextKey;
    private final String matchMode;
    private final String codexId;
    private final int imageWidth;
    private final int imageHeight;
    private final String creator;
    private final String timestamp;
    private final String uuid;
    private final String nbtKey;
    private final String nbtValue;

    private CodexDocumentDefinition(ResourceLocation itemId, String category,
                                    String displayName, ResourceLocation imageLocation,
                                    ResourceLocation textLocation, String worldImageKey,
                                    String worldTextKey, String matchMode, String codexId,
                                    int imageWidth, int imageHeight, String creator,
                                    String timestamp, String uuid, String nbtKey,
                                    String nbtValue) {
        this.itemId = itemId;
        this.category = cleanOrDefault(category, "Documents");
        this.displayName = displayName == null ? "" : displayName.trim();
        this.imageLocation = imageLocation;
        this.textLocation = textLocation;
        this.worldImageKey = clean(worldImageKey);
        this.worldTextKey = clean(worldTextKey);
        this.matchMode = "unique".equalsIgnoreCase(matchMode) ? "unique" : "item";
        this.codexId = clean(codexId);
        this.imageWidth = Math.max(1, imageWidth);
        this.imageHeight = Math.max(1, imageHeight);
        this.creator = clean(creator);
        this.timestamp = clean(timestamp);
        this.uuid = clean(uuid);
        this.nbtKey = clean(nbtKey);
        this.nbtValue = clean(nbtValue);
    }

    public static Optional<CodexDocumentDefinition> parse(String rawRule) {
        if (rawRule == null || rawRule.isBlank()) return Optional.empty();
        String raw = rawRule.trim();
        if (raw.contains("=")) return parseKeyValueFormat(raw);
        if (raw.contains("|")) return parsePipeFormat(raw);
        ResourceLocation itemId = ResourceLocation.tryParse(raw);
        if (itemId == null) return Optional.empty();
        return Optional.of(new CodexDocumentDefinition(itemId, "Documents", "",
                null, null, "", "", "item", "",
                DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT,
                "", "", "", "", ""));
    }

    public static CodexDocumentDefinition fallback(ItemStack stack) {
        ResourceLocation itemId = stack == null || stack.isEmpty()
                ? new ResourceLocation("minecraft", "air")
                : BuiltInRegistries.ITEM.getKey(stack.getItem());
        String name = stack == null || stack.isEmpty()
                ? "Unknown Document" : stack.getHoverName().getString();
        return new CodexDocumentDefinition(itemId, "Documents", name,
                null, null, "", "", "item", "",
                DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT,
                "", "", "", "", "");
    }

    private static Optional<CodexDocumentDefinition> parsePipeFormat(String raw) {
        String[] parts = raw.split("\\|", -1);
        if (parts.length == 0 || parts[0].isBlank()) return Optional.empty();
        ResourceLocation itemId = ResourceLocation.tryParse(parts[0].trim());
        if (itemId == null) return Optional.empty();
        if (parts.length >= 6) {
            return Optional.of(new CodexDocumentDefinition(itemId, getPart(parts, 1),
                    getPart(parts, 2), parseLocation(getPart(parts, 3)),
                    parseLocation(getPart(parts, 4)), "", "", "item", "",
                    parseInt(getPart(parts, 8), DEFAULT_IMAGE_WIDTH),
                    parseInt(getPart(parts, 9), DEFAULT_IMAGE_HEIGHT),
                    getPart(parts, 5), getPart(parts, 6), getPart(parts, 7), "", ""));
        }
        return Optional.of(new CodexDocumentDefinition(itemId, "Documents",
                getPart(parts, 1), null, null, "", "", "item", "",
                DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT,
                getPart(parts, 2), getPart(parts, 3), getPart(parts, 4), "", ""));
    }

    private static Optional<CodexDocumentDefinition> parseKeyValueFormat(String raw) {
        Map<String, String> values = new HashMap<>();
        for (String part : raw.split(";|\\r?\\n")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && !pair[0].isBlank()) {
                values.put(pair[0].trim().toLowerCase(Locale.ROOT), pair[1].trim());
            }
        }
        String id = values.get("id");
        if (id == null || id.isBlank()) return Optional.empty();
        ResourceLocation itemId = ResourceLocation.tryParse(id);
        if (itemId == null) return Optional.empty();
        return Optional.of(new CodexDocumentDefinition(itemId,
                firstPresent(values, "category", "type", "section"),
                firstPresent(values, "name", "display_name", "title"),
                parseLocation(firstPresent(values, "image", "texture", "photo")),
                parseLocation(firstPresent(values, "text", "transcript", "transcription")),
                values.getOrDefault("world_image", ""),
                values.getOrDefault("world_text", ""),
                values.getOrDefault("match_mode", "item"),
                values.getOrDefault("codex_id", ""),
                parseInt(firstPresent(values, "image_width", "width"), DEFAULT_IMAGE_WIDTH),
                parseInt(firstPresent(values, "image_height", "height"), DEFAULT_IMAGE_HEIGHT),
                values.getOrDefault("creator", ""),
                values.getOrDefault("timestamp", ""),
                values.getOrDefault("uuid", ""),
                firstPresent(values, "nbt_key", "tag_key"),
                firstPresent(values, "nbt_value", "tag_value")));
    }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!itemId.equals(stackId)) return false;
        CompoundTag tag = stack.getTag();
        if (isUnique()) {
            if (codexId.isBlank() || tag == null || !tag.contains(UNIQUE_TAG, Tag.TAG_STRING)
                    || !codexId.equals(tag.getString(UNIQUE_TAG))) return false;
        }
        return matchesTagValue(tag, "creator", creator)
                && matchesTagValue(tag, "timestamp", timestamp)
                && matchesTagValue(tag, "uuid", uuid)
                && matchesTagValue(tag, nbtKey, nbtValue);
    }

    public String getCategory() { return category; }
    public boolean isUnique() { return "unique".equals(matchMode); }
    public String getCodexId() { return codexId; }
    public String getWorldImageKey() { return worldImageKey; }
    public String getWorldTextKey() { return worldTextKey; }

    public String getDisplayName(ItemStack fallbackStack) {
        if (!displayName.isBlank()) return displayName;
        if (fallbackStack != null && !fallbackStack.isEmpty()) {
            return fallbackStack.getHoverName().getString();
        }
        return itemId.toString();
    }

    public Optional<ResourceLocation> getImageLocation() {
        return Optional.ofNullable(imageLocation);
    }

    public Optional<ResourceLocation> getTextLocation() {
        return Optional.ofNullable(textLocation);
    }

    public int getImageWidth() { return imageWidth; }
    public int getImageHeight() { return imageHeight; }

    public String getStableId(ItemStack fallbackStack) {
        return category + "|" + getDisplayName(fallbackStack) + "|" + itemId
                + "|" + creator + "|" + timestamp + "|" + uuid
                + "|" + matchMode + "|" + codexId
                + "|" + worldImageKey + "|" + worldTextKey;
    }

    private static boolean matchesTagValue(CompoundTag tag, String key, String expected) {
        if (key == null || key.isBlank() || expected == null || expected.isBlank()) return true;
        if (tag == null || !tag.contains(key)) return false;
        Tag actual = tag.get(key);
        if (actual == null) return false;
        return normalize(actual.getAsString()).equals(normalize(expected))
                || normalize(actual.toString()).equals(normalize(expected));
    }

    private static ResourceLocation parseLocation(String value) {
        if (value == null || value.isBlank()) return null;
        return ResourceLocation.tryParse(value.trim());
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) return fallback;
        try { return Integer.parseInt(value.trim()); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static String firstPresent(Map<String, String> values, String... keys) {
        for (String key : keys) {
            String value = values.get(key);
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }

    private static String getPart(String[] parts, int index) {
        return index >= 0 && index < parts.length ? parts[index].trim() : "";
    }

    private static String cleanOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replace(" ", "").replace("[", "")
                .replace("]", "").replace("\"", "").trim().toLowerCase(Locale.ROOT);
    }
}
''')


# Preserve new fields when inventory JSON is flattened into runtime rules.
replace_once(
    "src/main/java/com/bl4ues/scpinventory/config/ScpInventoryConfig.java",
    '''            putIfPresent(fields, obj, "inline_image_png");
            putIfPresent(fields, obj, "inline_text");''',
    '''            putIfPresent(fields, obj, "world_image");
            putIfPresent(fields, obj, "world_text");
            putIfPresent(fields, obj, "match_mode");
            putIfPresent(fields, obj, "codex_id");''')


# ---------------------------------------------------------------------------
# Server-side world storage
# ---------------------------------------------------------------------------
write("src/main/java/net/mcreator/scpadditions/config/ui/CodexAssetStorage.java", r'''package net.mcreator.scpadditions.config.ui;

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
''')


# ---------------------------------------------------------------------------
# Network packets for uploads, on-demand downloads and test-item creation
# ---------------------------------------------------------------------------
write("src/main/java/net/mcreator/scpadditions/config/ui/ConfigCenterNetwork.java", r'''package net.mcreator.scpadditions.config.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.mcreator.scpadditions.client.CodexAssetClient;

import java.util.List;
import java.util.function.Supplier;

public final class ConfigCenterNetwork {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private ConfigCenterNetwork() {
    }

    public static int register(SimpleChannel channel, int id) {
        channel.registerMessage(id++, OpenRequest.class, OpenRequest::encode, OpenRequest::decode, OpenRequest::handle);
        channel.registerMessage(id++, Snapshot.class, Snapshot::encode, Snapshot::decode, Snapshot::handle);
        channel.registerMessage(id++, SaveRequest.class, SaveRequest::encode, SaveRequest::decode, SaveRequest::handle);
        channel.registerMessage(id++, SaveResult.class, SaveResult::encode, SaveResult::decode, SaveResult::handle);
        channel.registerMessage(id++, AssetUploadRequest.class, AssetUploadRequest::encode, AssetUploadRequest::decode, AssetUploadRequest::handle);
        channel.registerMessage(id++, AssetUploadResult.class, AssetUploadResult::encode, AssetUploadResult::decode, AssetUploadResult::handle);
        channel.registerMessage(id++, AssetRequest.class, AssetRequest::encode, AssetRequest::decode, AssetRequest::handle);
        channel.registerMessage(id++, AssetData.class, AssetData::encode, AssetData::decode, AssetData::handle);
        channel.registerMessage(id++, GiveCodexItemRequest.class, GiveCodexItemRequest::encode, GiveCodexItemRequest::decode, GiveCodexItemRequest::handle);
        return id;
    }

    public static void openFor(ServerPlayer player, SimpleChannel channel) {
        if (!ConfigCenterService.canEdit(player)) return;
        try {
            channel.send(PacketDistributor.PLAYER.with(() -> player),
                    new Snapshot(GSON.toJson(ConfigCenterService.snapshot())));
        } catch (Exception exception) {
            channel.send(PacketDistributor.PLAYER.with(() -> player),
                    new SaveResult(false, "Could not read configuration files: "
                            + readable(exception), "", List.of()));
        }
    }

    public static final class OpenRequest {
        public static void encode(OpenRequest ignored, FriendlyByteBuf buffer) { }
        public static OpenRequest decode(FriendlyByteBuf buffer) { return new OpenRequest(); }
        public static void handle(OpenRequest ignored, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                if (!ConfigCenterService.canEdit(player)) {
                    com.bl4ues.scpinventory.network.ModNetwork.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new SaveResult(false, "Operator permission level 2 is required to edit server configuration.", "", List.of()));
                    return;
                }
                openFor(player, com.bl4ues.scpinventory.network.ModNetwork.CHANNEL);
            });
            context.setPacketHandled(true);
        }
    }

    public record Snapshot(String payload) {
        public Snapshot { payload = payload == null ? "{}" : payload; }
        public static void encode(Snapshot message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.payload, ConfigCenterService.MAX_PAYLOAD_LENGTH);
        }
        public static Snapshot decode(FriendlyByteBuf buffer) {
            return new Snapshot(buffer.readUtf(ConfigCenterService.MAX_PAYLOAD_LENGTH));
        }
        public static void handle(Snapshot message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ConfigCenterClient.openSnapshot(message.payload)));
            context.setPacketHandled(true);
        }
    }

    public record SaveRequest(String changes) {
        public SaveRequest { changes = changes == null ? "{}" : changes; }
        public static void encode(SaveRequest message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.changes, ConfigCenterService.MAX_PAYLOAD_LENGTH);
        }
        public static SaveRequest decode(FriendlyByteBuf buffer) {
            return new SaveRequest(buffer.readUtf(ConfigCenterService.MAX_PAYLOAD_LENGTH));
        }
        public static void handle(SaveRequest message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                ConfigCenterService.SaveResult result = ConfigCenterService.saveBatch(player, message.changes);
                String snapshot = result.success() ? GSON.toJson(result.snapshot()) : "";
                com.bl4ues.scpinventory.network.ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new SaveResult(result.success(), result.message(), snapshot, result.warnings()));
            });
            context.setPacketHandled(true);
        }
    }

    public record SaveResult(boolean success, String message, String snapshot,
                             List<String> warnings) {
        public SaveResult {
            message = message == null ? "" : message;
            snapshot = snapshot == null ? "" : snapshot;
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }
        public static void encode(SaveResult result, FriendlyByteBuf buffer) {
            buffer.writeBoolean(result.success);
            buffer.writeUtf(result.message, 8192);
            buffer.writeUtf(result.snapshot, ConfigCenterService.MAX_PAYLOAD_LENGTH);
            buffer.writeVarInt(Math.min(result.warnings.size(), 128));
            for (int i = 0; i < Math.min(result.warnings.size(), 128); i++) {
                buffer.writeUtf(result.warnings.get(i), 2048);
            }
        }
        public static SaveResult decode(FriendlyByteBuf buffer) {
            boolean success = buffer.readBoolean();
            String message = buffer.readUtf(8192);
            String snapshot = buffer.readUtf(ConfigCenterService.MAX_PAYLOAD_LENGTH);
            int count = Math.min(buffer.readVarInt(), 128);
            java.util.ArrayList<String> warnings = new java.util.ArrayList<>(count);
            for (int i = 0; i < count; i++) warnings.add(buffer.readUtf(2048));
            return new SaveResult(success, message, snapshot, warnings);
        }
        public static void handle(SaveResult result, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ConfigCenterClient.onSaveResult(result)));
            context.setPacketHandled(true);
        }
    }

    public record AssetUploadRequest(String requestId, String kind,
                                     String fileName, byte[] data) {
        public AssetUploadRequest {
            requestId = requestId == null ? "" : requestId;
            kind = kind == null ? "" : kind;
            fileName = fileName == null ? "asset" : fileName;
            data = data == null ? new byte[0] : data;
        }
        public static void encode(AssetUploadRequest message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.requestId, 64);
            buffer.writeUtf(message.kind, 16);
            buffer.writeUtf(message.fileName, 256);
            buffer.writeByteArray(message.data);
        }
        public static AssetUploadRequest decode(FriendlyByteBuf buffer) {
            return new AssetUploadRequest(buffer.readUtf(64), buffer.readUtf(16),
                    buffer.readUtf(256),
                    buffer.readByteArray(CodexAssetStorage.MAX_TRANSFER_BYTES));
        }
        public static void handle(AssetUploadRequest message,
                                  Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                CodexAssetStorage.UploadResult result = CodexAssetStorage.save(
                        player, message.kind, message.fileName, message.data);
                com.bl4ues.scpinventory.network.ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new AssetUploadResult(message.requestId, result.success(),
                                message.kind, result.key(), result.message()));
            });
            context.setPacketHandled(true);
        }
    }

    public record AssetUploadResult(String requestId, boolean success, String kind,
                                    String key, String message) {
        public AssetUploadResult {
            requestId = requestId == null ? "" : requestId;
            kind = kind == null ? "" : kind;
            key = key == null ? "" : key;
            message = message == null ? "" : message;
        }
        public static void encode(AssetUploadResult message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.requestId, 64);
            buffer.writeBoolean(message.success);
            buffer.writeUtf(message.kind, 16);
            buffer.writeUtf(message.key, 160);
            buffer.writeUtf(message.message, 2048);
        }
        public static AssetUploadResult decode(FriendlyByteBuf buffer) {
            return new AssetUploadResult(buffer.readUtf(64), buffer.readBoolean(),
                    buffer.readUtf(16), buffer.readUtf(160), buffer.readUtf(2048));
        }
        public static void handle(AssetUploadResult message,
                                  Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> CodexAssetClient.onUploadResult(message)));
            context.setPacketHandled(true);
        }
    }

    public record AssetRequest(String kind, String key) {
        public AssetRequest {
            kind = kind == null ? "" : kind;
            key = key == null ? "" : key;
        }
        public static void encode(AssetRequest message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.kind, 16);
            buffer.writeUtf(message.key, 160);
        }
        public static AssetRequest decode(FriendlyByteBuf buffer) {
            return new AssetRequest(buffer.readUtf(16), buffer.readUtf(160));
        }
        public static void handle(AssetRequest message,
                                  Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                byte[] bytes;
                try { bytes = CodexAssetStorage.read(player, message.key); }
                catch (Exception ignored) { bytes = new byte[0]; }
                com.bl4ues.scpinventory.network.ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new AssetData(message.kind, message.key, bytes));
            });
            context.setPacketHandled(true);
        }
    }

    public record AssetData(String kind, String key, byte[] data) {
        public AssetData {
            kind = kind == null ? "" : kind;
            key = key == null ? "" : key;
            data = data == null ? new byte[0] : data;
        }
        public static void encode(AssetData message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.kind, 16);
            buffer.writeUtf(message.key, 160);
            buffer.writeByteArray(message.data);
        }
        public static AssetData decode(FriendlyByteBuf buffer) {
            return new AssetData(buffer.readUtf(16), buffer.readUtf(160),
                    buffer.readByteArray(CodexAssetStorage.MAX_TRANSFER_BYTES));
        }
        public static void handle(AssetData message,
                                  Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> CodexAssetClient.onAssetData(message)));
            context.setPacketHandled(true);
        }
    }

    public record GiveCodexItemRequest(String itemId, String codexId,
                                       String displayName) {
        public GiveCodexItemRequest {
            itemId = itemId == null ? "" : itemId;
            codexId = codexId == null ? "" : codexId;
            displayName = displayName == null ? "" : displayName;
        }
        public static void encode(GiveCodexItemRequest message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.itemId, 256);
            buffer.writeUtf(message.codexId, 128);
            buffer.writeUtf(message.displayName, 256);
        }
        public static GiveCodexItemRequest decode(FriendlyByteBuf buffer) {
            return new GiveCodexItemRequest(buffer.readUtf(256), buffer.readUtf(128),
                    buffer.readUtf(256));
        }
        public static void handle(GiveCodexItemRequest message,
                                  Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) CodexAssetStorage.giveDocument(player,
                        message.itemId, message.codexId, message.displayName);
            });
            context.setPacketHandled(true);
        }
    }

    private static String readable(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getSimpleName() : message;
    }
}
''')


# ---------------------------------------------------------------------------
# Client cache and upload controller
# ---------------------------------------------------------------------------
write("src/main/java/net/mcreator/scpadditions/client/CodexAssetClient.java", r'''package net.mcreator.scpadditions.client;

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
        BYTES.put(cacheKey(result.kind(), result.key()), pending.data());
        REQUESTED.remove(cacheKey(result.kind(), result.key()));
        if (pending.success() != null) pending.success().accept(result.key());
    }

    public static Optional<ResourceLocation> getTexture(String key) {
        if (key == null || key.isBlank()) return Optional.empty();
        String cache = cacheKey("png", key);
        ResourceLocation existing = TEXTURES.get(cache);
        if (existing != null) return Optional.of(existing);
        byte[] data = BYTES.get(cache);
        if (data == null) {
            request("png", key);
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
            request("text", key);
            return Optional.empty();
        }
        return Optional.of(new String(data, StandardCharsets.UTF_8));
    }

    public static void onAssetData(ConfigCenterNetwork.AssetData data) {
        String cache = cacheKey(data.kind(), data.key());
        REQUESTED.remove(cache);
        if (data.data().length > 0) BYTES.put(cache, data.data());
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
        return (kind == null ? "" : kind) + ":" + (key == null ? "" : key);
    }

    private record PendingUpload(String kind, byte[] data,
                                 Consumer<String> success,
                                 Consumer<String> failure) {
    }
}
''')


# ---------------------------------------------------------------------------
# Image and text editors upload to the server instead of embedding Base64
# ---------------------------------------------------------------------------
write("src/main/java/net/mcreator/scpadditions/client/CodexImageDropScreen.java", r'''package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class CodexImageDropScreen extends Screen {
    private static final int MAX_PNG_BYTES = 900_000;
    private final Screen parent;
    private final boolean hasWorldImage;
    private final Consumer<ImportedImage> callback;
    private final Runnable clearCallback;
    private String status = "Drop one PNG file anywhere on this screen.";
    private int statusColor = 0xFFA9AFBA;
    private boolean uploading;

    public CodexImageDropScreen(Screen parent, boolean hasWorldImage,
                                Consumer<ImportedImage> callback,
                                Runnable clearCallback) {
        super(ScpFonts.roboto("Import Codex PNG"));
        this.parent = parent;
        this.hasWorldImage = hasWorldImage;
        this.callback = callback;
        this.clearCallback = clearCallback;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(500, width - 24);
        int left = (width - panelWidth) / 2;
        int top = Math.max(12, (height - 240) / 2);
        int buttonWidth = (panelWidth - 56) / 2;
        Button remove = addRenderableWidget(Button.builder(
                ScpFonts.roboto("Remove World PNG"), b -> {
                    if (clearCallback != null) clearCallback.run();
                }).bounds(left + 20, top + 190, buttonWidth, 22).build());
        remove.active = hasWorldImage;
        addRenderableWidget(Button.builder(ScpFonts.roboto("Cancel"),
                b -> Minecraft.getInstance().setScreen(parent))
                .bounds(left + 36 + buttonWidth, top + 190,
                        buttonWidth, 22).build());
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        if (uploading) return;
        if (paths == null || paths.size() != 1) {
            fail("Drop exactly one PNG file.");
            return;
        }
        Path path = paths.get(0);
        String name = path.getFileName() == null ? "image.png"
                : path.getFileName().toString();
        if (!name.toLowerCase(Locale.ROOT).endsWith(".png")) {
            fail("Only PNG files are accepted.");
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length == 0 || bytes.length > MAX_PNG_BYTES) {
                fail("PNG must be between 1 byte and 900 KB.");
                return;
            }
            int imageWidth;
            int imageHeight;
            try (NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes))) {
                imageWidth = image.getWidth();
                imageHeight = image.getHeight();
            }
            if (imageWidth < 1 || imageHeight < 1
                    || imageWidth > 4096 || imageHeight > 4096) {
                fail("PNG dimensions must be between 1 and 4096 pixels.");
                return;
            }
            uploading = true;
            status = "Uploading to this world...";
            statusColor = 0xFFE5D49A;
            CodexAssetClient.upload("png", name, bytes, key -> {
                uploading = false;
                if (callback != null) callback.accept(
                        new ImportedImage(key, imageWidth, imageHeight, name));
            }, message -> {
                uploading = false;
                fail(message);
            });
        } catch (Exception exception) {
            fail("Could not read that PNG: " + readable(exception));
        }
    }

    private void fail(String message) {
        status = message;
        statusColor = 0xFFD46060;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                       float partialTick) {
        renderBackground(graphics);
        int panelWidth = Math.min(500, width - 24);
        int left = (width - panelWidth) / 2;
        int top = Math.max(12, (height - 240) / 2);
        graphics.fill(left, top, left + panelWidth, top + 225, 0xFF111317);
        graphics.fill(left, top, left + panelWidth, top + 34, 0xFF24282E);
        graphics.fill(left, top + 33, left + panelWidth, top + 34, 0xFFC59A2A);
        graphics.drawString(font, ScpFonts.montserrat("IMPORT CODEX IMAGE"),
                left + 18, top + 12, 0xFFF7F8FC, false);
        graphics.fill(left + 20, top + 52, left + panelWidth - 20,
                top + 164, 0xFF081022);
        Component drop = ScpFonts.roboto("DROP PNG HERE");
        graphics.drawString(font, drop,
                left + (panelWidth - font.width(drop)) / 2,
                top + 88, 0xFFE5D49A, false);
        Component limit = ScpFonts.roboto(
                "Saved in this world · Maximum 900 KB · 4096 × 4096");
        graphics.drawString(font, limit,
                left + (panelWidth - font.width(limit)) / 2,
                top + 108, 0xFFA9AFBA, false);
        Component statusText = ScpFonts.roboto(status);
        graphics.drawString(font, statusText,
                left + (panelWidth - font.width(statusText)) / 2,
                top + 142, statusColor, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() { Minecraft.getInstance().setScreen(parent); }
    @Override
    public boolean isPauseScreen() { return false; }

    private static String readable(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getSimpleName() : message;
    }

    public record ImportedImage(String key, int width, int height,
                                String fileName) { }
}
''')

write("src/main/java/net/mcreator/scpadditions/client/CodexTextEditorScreen.java", r'''package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class CodexTextEditorScreen extends Screen {
    private static final int MAX_TEXT_LENGTH = 65_536;
    private final Screen parent;
    private final String existingKey;
    private final Consumer<String> callback;
    private final List<String> lines = new ArrayList<>();
    private final Map<EditBox, Integer> lineBoxes = new LinkedHashMap<>();
    private int scroll;
    private int focusLine = -1;
    private boolean loadedExisting;
    private boolean uploading;
    private String notice;
    private int noticeColor = 0xFFA9AFBA;

    public CodexTextEditorScreen(Screen parent, String existingKey,
                                 Consumer<String> callback) {
        super(ScpFonts.roboto("Write Codex Text"));
        this.parent = parent;
        this.existingKey = existingKey == null ? "" : existingKey;
        this.callback = callback;
        setText("");
        this.loadedExisting = this.existingKey.isBlank();
        this.notice = this.loadedExisting
                ? "Enter creates a line. You may also drop one UTF-8 text file."
                : "Loading the text saved in this world...";
    }

    private void setText(String text) {
        lines.clear();
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        for (String line : normalized.split("\n", -1)) lines.add(line);
        if (lines.isEmpty()) lines.add("");
        scroll = 0;
        focusLine = -1;
    }

    @Override
    protected void init() { buildWidgets(); }

    @Override
    public void tick() {
        super.tick();
        if (!loadedExisting && !existingKey.isBlank()) {
            java.util.Optional<String> loaded = CodexAssetClient.getText(existingKey);
            if (loaded.isPresent()) {
                loadedExisting = true;
                setText(loaded.get());
                notice = "Loaded the text saved in this world.";
                noticeColor = 0xFF79D58B;
                rebuild();
            }
        }
    }

    private void buildWidgets() {
        lineBoxes.clear();
        int panelWidth = Math.min(720, width - 20);
        int panelHeight = Math.min(460, height - 16);
        int left = (width - panelWidth) / 2;
        int top = Math.max(8, (height - panelHeight) / 2);
        int editorTop = top + 58;
        int bottom = top + panelHeight - 54;
        int visible = Math.max(5, (bottom - editorTop) / 22);
        scroll = Math.max(0, Math.min(Math.max(0, lines.size() - visible), scroll));
        for (int row = 0; row < visible; row++) {
            int logical = scroll + row;
            if (logical >= lines.size()) break;
            EditBox box = new EditBox(font, left + 50, editorTop + row * 22,
                    panelWidth - 70, 20,
                    Component.literal("Line " + (logical + 1)));
            box.setMaxLength(4096);
            box.setValue(lines.get(logical));
            box.setFormatter((value, cursor) ->
                    ScpFonts.roboto(value).getVisualOrderText());
            box.setResponder(value -> lines.set(logical, value));
            addRenderableWidget(box);
            lineBoxes.put(box, logical);
            if (logical == focusLine) box.setFocused(true);
        }
        int third = (panelWidth - 52) / 3;
        addRenderableWidget(Button.builder(ScpFonts.roboto("Clear Text"), b -> {
            syncVisible();
            setText("");
            rebuild();
        }).bounds(left + 16, top + panelHeight - 30, third, 22).build());
        addRenderableWidget(Button.builder(ScpFonts.roboto("Save Text"),
                b -> save()).bounds(left + 26 + third,
                top + panelHeight - 30, third, 22).build());
        addRenderableWidget(Button.builder(ScpFonts.roboto("Cancel"),
                b -> Minecraft.getInstance().setScreen(parent))
                .bounds(left + 36 + third * 2,
                        top + panelHeight - 30, third, 22).build());
    }

    private void rebuild() { clearWidgets(); buildWidgets(); }

    private void syncVisible() {
        for (Map.Entry<EditBox, Integer> entry : lineBoxes.entrySet()) {
            lines.set(entry.getValue(), entry.getKey().getValue());
        }
    }

    private void save() {
        if (uploading) return;
        syncVisible();
        String text = String.join("\n", lines);
        if (text.length() > MAX_TEXT_LENGTH) {
            notice = "Text is too long. Maximum: 65,536 characters.";
            noticeColor = 0xFFD46060;
            return;
        }
        if (text.isBlank()) {
            if (callback != null) callback.accept("");
            return;
        }
        uploading = true;
        notice = "Saving text in this world...";
        noticeColor = 0xFFE5D49A;
        CodexAssetClient.upload("text", "codex-document.txt",
                text.getBytes(StandardCharsets.UTF_8), key -> {
                    uploading = false;
                    if (callback != null) callback.accept(key);
                }, message -> {
                    uploading = false;
                    notice = message;
                    noticeColor = 0xFFD46060;
                });
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (Map.Entry<EditBox, Integer> entry : lineBoxes.entrySet()) {
            if (!entry.getKey().isFocused()) continue;
            int index = entry.getValue();
            if (keyCode == GLFW.GLFW_KEY_ENTER
                    || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                syncVisible();
                lines.add(index + 1, "");
                focusLine = index + 1;
                ensureVisible(focusLine);
                rebuild();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP && index > 0) {
                syncVisible(); focusLine = index - 1;
                ensureVisible(focusLine); rebuild(); return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN && index + 1 < lines.size()) {
                syncVisible(); focusLine = index + 1;
                ensureVisible(focusLine); rebuild(); return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void ensureVisible(int line) {
        int panelHeight = Math.min(460, height - 16);
        int visible = Math.max(5, (panelHeight - 112) / 22);
        if (line < scroll) scroll = line;
        if (line >= scroll + visible) scroll = line - visible + 1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        syncVisible();
        int visible = Math.max(5, (Math.min(460, height - 16) - 112) / 22);
        int next = Math.max(0, Math.min(Math.max(0, lines.size() - visible),
                scroll + (delta < 0 ? 1 : -1)));
        if (next != scroll) {
            scroll = next; focusLine = -1; rebuild(); return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        if (paths == null || paths.size() != 1) {
            notice = "Drop exactly one UTF-8 text file.";
            noticeColor = 0xFFD46060;
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(paths.get(0));
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (text.length() > MAX_TEXT_LENGTH) {
                notice = "Text exceeds 65,536 characters.";
                noticeColor = 0xFFD46060;
                return;
            }
            loadedExisting = true;
            setText(text);
            notice = "Imported " + paths.get(0).getFileName();
            noticeColor = 0xFF79D58B;
            rebuild();
        } catch (Exception exception) {
            notice = "Could not read that text file.";
            noticeColor = 0xFFD46060;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                       float partialTick) {
        renderBackground(graphics);
        int panelWidth = Math.min(720, width - 20);
        int panelHeight = Math.min(460, height - 16);
        int left = (width - panelWidth) / 2;
        int top = Math.max(8, (height - panelHeight) / 2);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xFF111317);
        graphics.fill(left, top, left + panelWidth, top + 34, 0xFF24282E);
        graphics.fill(left, top + 33, left + panelWidth, top + 34, 0xFFC59A2A);
        graphics.drawString(font, ScpFonts.montserrat("WRITE CODEX TEXT"),
                left + 16, top + 12, 0xFFF7F8FC, false);
        graphics.drawString(font, ScpFonts.roboto(notice),
                left + 16, top + 42, noticeColor, false);
        for (Map.Entry<EditBox, Integer> entry : lineBoxes.entrySet()) {
            graphics.drawString(font,
                    ScpFonts.roboto(Integer.toString(entry.getValue() + 1)),
                    left + 18, entry.getKey().getY() + 6,
                    0xFF6F7888, false);
        }
        graphics.drawString(font, ScpFonts.roboto(lines.size() + " line(s) · "
                        + String.join("\n", lines).length() + "/"
                        + MAX_TEXT_LENGTH + " characters"),
                left + 16, top + panelHeight - 43, 0xFFA9AFBA, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() { Minecraft.getInstance().setScreen(parent); }
    @Override
    public boolean isPauseScreen() { return false; }
}
''')

# Remove the reflection-based Base64 enhancer; the native Codex screen is replaced below.
enhancer = ROOT / "src/main/java/net/mcreator/scpadditions/client/CodexEditorEnhancements.java"
if enhancer.exists(): enhancer.unlink()


# ---------------------------------------------------------------------------
# Native Codex editor with import, direct text, and unique-item mode
# ---------------------------------------------------------------------------
client_path = "src/main/java/net/mcreator/scpadditions/config/ui/ConfigCenterClient.java"
replace_once(
    client_path,
    '''import net.mcreator.scpadditions.ScpAdditionsMod;''',
    '''import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.client.CodexAssetClient;
import net.mcreator.scpadditions.client.CodexImageDropScreen;
import net.mcreator.scpadditions.client.CodexTextEditorScreen;''')
replace_between(
    client_path,
    '''    private static final class CodexDetailScreen extends ConfigScreen {
''',
    '''    private static void setOrRemove(JsonObject object, String key, String value) {
''',
    r'''    private static final class CodexDetailScreen extends ConfigScreen {
        private final JsonObject document;
        private final JsonObject edit;
        private EditBox idBox;
        private EditBox categoryBox;
        private EditBox nameBox;
        private EditBox imageBox;
        private EditBox textBox;
        private EditBox imageWidthBox;
        private EditBox imageHeightBox;
        private EditBox nbtKeyBox;
        private EditBox nbtValueBox;
        private boolean advanced;
        private boolean uniqueMode;

        private CodexDetailScreen(Screen parent, JsonObject document) {
            super(parent, "Codex Document");
            this.document = document;
            this.edit = document.deepCopy();
            this.advanced = edit.has("image_width") || edit.has("image_height")
                    || edit.has("nbt_key") || edit.has("nbt_value");
            this.uniqueMode = "unique".equalsIgnoreCase(
                    string(edit, "match_mode", "item"));
        }

        @Override
        protected void init() {
            int w = Math.min(700, width - 16);
            int x = left(width, w) + 14;
            int y = Math.max(8, (height - Math.min(470, height - 16)) / 2) + 47;
            int fieldW = w - 28;
            idBox = field(x, y, fieldW, "Item ID", string(edit, "id", "")); y += 31;
            categoryBox = field(x, y, fieldW, "Category",
                    string(edit, "category", "Documents")); y += 31;
            nameBox = field(x, y, fieldW, "Display name",
                    string(edit, "name", "")); y += 31;

            addRenderableWidget(Button.builder(Component.literal(uniqueMode
                            ? "Match Mode: Unique Generated Item"
                            : "Match Mode: Any Matching Item"), b -> {
                        sync();
                        uniqueMode = !uniqueMode;
                        if (uniqueMode && string(edit, "codex_id", "").isBlank()) {
                            edit.addProperty("codex_id", java.util.UUID.randomUUID().toString());
                        }
                        rebuild();
                    }).bounds(x, y, fieldW, 20).build());
            y += 27;

            int resourceW = fieldW - 124;
            imageBox = field(x, y, resourceW, "Packaged image resource",
                    string(edit, "image", ""));
            addRenderableWidget(Button.builder(Component.literal(
                            edit.has("world_image") ? "Replace PNG" : "Import PNG"),
                    b -> openImageImporter())
                    .bounds(x + resourceW + 6, y, 118, 20).build());
            y += 31;
            textBox = field(x, y, resourceW, "Packaged UTF-8 text resource",
                    string(edit, "text", ""));
            addRenderableWidget(Button.builder(Component.literal(
                            edit.has("world_text") ? "Edit Text" : "Write Text"),
                    b -> openTextEditor())
                    .bounds(x + resourceW + 6, y, 118, 20).build());
            y += 32;

            addRenderableWidget(Button.builder(Component.literal(
                            (advanced ? "▼" : "▶")
                                    + " Additional conditions and image sizing"), b -> {
                        sync(); advanced = !advanced; rebuild();
                    }).bounds(x, y, fieldW, 20).build());
            y += 26;
            if (advanced) {
                int half = (fieldW - 6) / 2;
                imageWidthBox = field(x, y, half, "Image width",
                        string(edit, "image_width", ""));
                imageHeightBox = field(x + half + 6, y, half, "Image height",
                        string(edit, "image_height", "")); y += 31;
                nbtKeyBox = field(x, y, half, "NBT key",
                        string(edit, "nbt_key", ""));
                nbtValueBox = field(x + half + 6, y, half, "NBT value",
                        string(edit, "nbt_value", "")); y += 33;
            }

            int buttonY = Math.min(height - 30, y + 4);
            int third = (fieldW - 12) / 3;
            addRenderableWidget(Button.builder(Component.literal("Save Document"),
                    b -> save()).bounds(x, buttonY, third, 20).build());
            Button give = addRenderableWidget(Button.builder(
                    Component.literal("Give Test Item"), b -> giveTestItem())
                    .bounds(x + third + 6, buttonY, third, 20).build());
            give.active = uniqueMode;
            addRenderableWidget(Button.builder(Component.literal("Cancel"),
                    b -> goBack()).bounds(x + (third + 6) * 2,
                    buttonY, third, 20).build());
        }

        private EditBox field(int x, int y, int width, String hint, String value) {
            EditBox box = new EditBox(font, x, y, width, 20, Component.literal(hint));
            box.setHint(Component.literal(hint));
            box.setMaxLength(2048);
            box.setValue(value == null ? "" : value);
            addRenderableWidget(box);
            return box;
        }

        private void sync() {
            if (idBox == null) return;
            setOrRemove(edit, "id", idBox.getValue());
            setOrRemove(edit, "category", categoryBox.getValue());
            setOrRemove(edit, "name", nameBox.getValue());
            setOrRemove(edit, "image", imageBox.getValue());
            setOrRemove(edit, "text", textBox.getValue());
            edit.addProperty("match_mode", uniqueMode ? "unique" : "item");
            if (uniqueMode && string(edit, "codex_id", "").isBlank()) {
                edit.addProperty("codex_id", java.util.UUID.randomUUID().toString());
            }
            if (!uniqueMode) edit.remove("codex_id");
            if (advanced && imageWidthBox != null) {
                setNumberOrRemove(edit, "image_width", imageWidthBox.getValue());
                setNumberOrRemove(edit, "image_height", imageHeightBox.getValue());
                setOrRemove(edit, "nbt_key", nbtKeyBox.getValue());
                setOrRemove(edit, "nbt_value", nbtValueBox.getValue());
            }
        }

        private void openImageImporter() {
            sync();
            Minecraft.getInstance().setScreen(new CodexImageDropScreen(this,
                    edit.has("world_image"), imported -> {
                        edit.addProperty("world_image", imported.key());
                        edit.addProperty("image_width", imported.width());
                        edit.addProperty("image_height", imported.height());
                        Minecraft.getInstance().setScreen(this);
                        rebuild();
                    }, () -> {
                        edit.remove("world_image");
                        Minecraft.getInstance().setScreen(this);
                        rebuild();
                    }));
        }

        private void openTextEditor() {
            sync();
            Minecraft.getInstance().setScreen(new CodexTextEditorScreen(this,
                    string(edit, "world_text", ""), key -> {
                        if (key == null || key.isBlank()) edit.remove("world_text");
                        else edit.addProperty("world_text", key);
                        Minecraft.getInstance().setScreen(this);
                        rebuild();
                    }));
        }

        private void giveTestItem() {
            sync();
            if (!uniqueMode) return;
            String id = string(edit, "id", "");
            try { new ResourceLocation(id); }
            catch (Exception ignored) { idBox.setTextColor(BAD); return; }
            CodexAssetClient.giveDocument(id, string(edit, "codex_id", ""),
                    string(edit, "name", "Document"));
        }

        private void save() {
            sync();
            String id = string(edit, "id", "");
            try { new ResourceLocation(id); }
            catch (Exception ignored) { idBox.setTextColor(BAD); return; }
            for (String key : new ArrayList<>(document.keySet())) document.remove(key);
            for (Map.Entry<String, JsonElement> entry : edit.entrySet()) {
                document.add(entry.getKey(), entry.getValue().deepCopy());
            }
            goBack();
        }

        private void rebuild() { clearWidgets(); init(); }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY,
                           float partialTick) {
            renderBackground(graphics);
            int w = Math.min(700, width - 16);
            int h = Math.min(470, height - 16);
            int x = left(width, w);
            int y = Math.max(8, (height - h) / 2);
            panel(graphics, x, y, w, h, screenTitle, font);
            String imageState = edit.has("world_image")
                    ? "World PNG attached" : "No world PNG";
            String textState = edit.has("world_text")
                    ? "world text attached" : "no world text";
            graphics.drawString(font, imageState + " · " + textState
                            + " · packaged resources remain optional fallbacks.",
                    x + 14, y + 31, MUTED, false);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

''')


# ---------------------------------------------------------------------------
# Codex panel requests world assets on demand
# ---------------------------------------------------------------------------
panel_path = "src/main/java/com/bl4ues/scpinventory/client/gui/components/CodexPanel.java"
replace_once(panel_path, 'import com.mojang.blaze3d.platform.NativeImage;\n', '')
replace_once(panel_path, 'import net.minecraft.client.renderer.texture.DynamicTexture;\n', '')
replace_once(panel_path, 'import java.io.ByteArrayInputStream;\n', '')
replace_once(panel_path, 'import java.util.Base64;\n', '')
replace_once(panel_path, 'import java.util.HashMap;\n', '')
replace_once(
    panel_path,
    '''import com.bl4ues.scpinventory.network.DocumentActionPacket;''',
    '''import com.bl4ues.scpinventory.network.DocumentActionPacket;
import net.mcreator.scpadditions.client.CodexAssetClient;''')
replace_once(
    panel_path,
    '''    private static final Map<String, ResourceLocation> INLINE_IMAGE_TEXTURES = new HashMap<>();

''',
    '')
replace_once(
    panel_path,
    '''        ResourceLocation image = inlineImageTexture(definition)
                .orElseGet(() -> definition.getImageLocation().orElse(null));''',
    '''        ResourceLocation image = CodexAssetClient.getTexture(
                        definition.getWorldImageKey())
                .orElseGet(() -> definition.getImageLocation().orElse(null));''')
replace_between(
    panel_path,
    '''    private Optional<String> readText(CodexDocumentDefinition definition) {
''',
    '''    private String buildFallbackText(ItemStack document, CodexDocumentDefinition definition)''',
    '''    private Optional<String> readText(CodexDocumentDefinition definition) {
        Optional<String> worldText = CodexAssetClient.getText(
                definition.getWorldTextKey());
        if (worldText.isPresent()) return worldText;
        ResourceLocation textLocation = definition.getTextLocation().orElse(null);
        if (textLocation == null || mc == null) return Optional.empty();
        return mc.getResourceManager().getResource(textLocation).flatMap(resource -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    resource.open(), StandardCharsets.UTF_8))) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (builder.length() > 0) builder.append('\n');
                    builder.append(line);
                }
                return Optional.of(builder.toString());
            } catch (IOException ignored) {
                return Optional.empty();
            }
        });
    }

''')


# ---------------------------------------------------------------------------
# Validation for world asset keys and unique definitions
# ---------------------------------------------------------------------------
service = "src/main/java/net/mcreator/scpadditions/config/ui/ConfigCenterService.java"
replace_once(
    service,
    '''        validateObjectIds(root, "codex_documents", "id", errors, warnings, true);
        validateSimpleIds(root, "hidden_status_effects", errors, warnings, false);''',
    '''        validateObjectIds(root, "codex_documents", "id", errors, warnings, true);
        if (root.has("codex_documents") && root.get("codex_documents").isJsonArray()) {
            int index = 0;
            for (JsonElement element : root.getAsJsonArray("codex_documents")) {
                if (element.isJsonObject()) {
                    JsonObject document = element.getAsJsonObject();
                    String mode = string(document, "match_mode");
                    if (!mode.isBlank() && !"item".equals(mode) && !"unique".equals(mode)) {
                        errors.add("codex_documents[" + index + "].match_mode must be item or unique");
                    }
                    if ("unique".equals(mode)) {
                        String codexId = string(document, "codex_id");
                        if (codexId.isBlank() || codexId.length() > 128
                                || !codexId.matches("[A-Za-z0-9._:-]+")) {
                            errors.add("codex_documents[" + index + "].codex_id is invalid");
                        }
                    }
                    for (String assetKey : List.of("world_image", "world_text")) {
                        String value = string(document, assetKey);
                        if (!value.isBlank() && !CodexAssetStorage.isSafeKey(value)) {
                            errors.add("codex_documents[" + index + "]." + assetKey
                                    + " is not a safe world asset key");
                        }
                    }
                }
                index++;
            }
        }
        validateSimpleIds(root, "hidden_status_effects", errors, warnings, false);''')


# The visual layer needs the larger native Codex editor panel.
ui_path = "src/main/java/net/mcreator/scpadditions/client/UnityConfigurationUiEvents.java"
replace_once(
    ui_path,
    '''            case "CodexDetailScreen" -> { w = Math.min(670, screen.width - 16); h = Math.min(430, screen.height - 16); y = Math.max(8, (screen.height - h) / 2); }''',
    '''            case "CodexDetailScreen" -> { w = Math.min(700, screen.width - 16); h = Math.min(470, screen.height - 16); y = Math.max(8, (screen.height - h) / 2); }''')
replace_once(
    ui_path,
    '''        values.put("import png", "Open a drop zone and embed a PNG directly in this Codex definition.");
        values.put("replace png", "Replace the PNG embedded in this Codex definition.");
        values.put("write text", "Write Codex text directly without creating a resource file.");
        values.put("edit text", "Edit the text embedded directly in this Codex definition.");''',
    '''        values.put("import png", "Upload a PNG into this world's Codex asset folder.");
        values.put("replace png", "Replace the PNG reference with another world asset.");
        values.put("write text", "Write UTF-8 text and save it in this world's Codex asset folder.");
        values.put("edit text", "Edit the text file referenced by this Codex definition.");
        values.put("match mode", "Choose whether any matching item or only a generated NBT-tagged item opens this document.");
        values.put("give test item", "Give yourself a uniquely tagged and named test document item.");''')


# ---------------------------------------------------------------------------
# Documentation: no persistent image data in JSON
# ---------------------------------------------------------------------------
replace_once(
    "README.md",
    '''The editor can also embed a dropped PNG and directly written UTF-8 text in the JSON definition, so ordinary users do not need to create resource-pack paths for individual documents.''',
    '''The editor can also import a dropped PNG and directly written UTF-8 text without manual resource paths. These assets are stored as normal files under the current world's `scp_additions/codex_assets` folder and transferred to clients on demand; the JSON stores only compact references. A document can match every copy of its configured item or use the optional unique-item mode, which requires a generated NBT-tagged item with the configured display name.''')
replace_once(
    "CHANGELOG.md",
    '''- Added direct Codex text editing and PNG drag-and-drop import. Embedded content is stored in the JSON definition, while packaged resource locations remain supported.''',
    '''- Added direct Codex text editing and PNG drag-and-drop import. Imported assets are stored as real files in the current world's `scp_additions/codex_assets` folder and sent to clients on demand; JSON definitions contain only compact references, while packaged resources remain supported;
- Added optional unique-item matching for Codex definitions. Unique documents use an NBT identifier, retain the configured display name and can be generated with the editor's **Give Test Item** action, avoiding every ordinary copy of the base item becoming the same document.''')
replace_once(
    "docs/CONFIGURATION_CENTER.md",
    '''The editor also provides **Import PNG**, which opens a drag-and-drop area and embeds the image directly in the JSON, and **Write Text**, which stores directly written UTF-8 text in the same definition. Embedded content is size-limited and server-authoritative; packaged resources remain available for modpacks that prefer resource packs.''',
    '''The editor also provides **Import PNG**, which opens a drag-and-drop area, and **Write Text**, which accepts direct multiline UTF-8 input. Both are saved as normal files under the active world's `scp_additions/codex_assets` folder; the server-authoritative JSON keeps only safe relative references, and clients request the files on demand. Packaged resources remain available for modpacks that prefer resource packs. **Match Mode** may target every copy of the selected item or a uniquely generated NBT-tagged document; **Give Test Item** creates the latter with its configured display name.''')

# Ensure mod metadata change is also documented.
replace_once(
    "CHANGELOG.md",
    '''## Configuration center''',
    '''## Mod presentation

- Added the SCP Additions logo to Forge's Mods list and replaced the update-specific metadata text with the full mod description used on the project pages.

## Configuration center''')

# Remove one-time tooling from the final tree.
for temporary in [
    ROOT / "tools/one_time_final_config_polish.py",
    ROOT / "tools/one_time_final_config_polish_v2.py",
    ROOT / ".github/workflows/one-time-final-config-polish.yml",
]:
    if temporary.exists(): temporary.unlink()

print("World-backed Codex assets and final presentation applied.")
