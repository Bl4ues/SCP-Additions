from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


# ScpItemType: CODEX remains internal but is no longer a generic config token.
path = Path("src/main/java/com/bl4ues/scpinventory/item/ScpItemType.java")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
'            case "CODEX", "DOCUMENT", "DOC" -> Optional.of(CODEX);\n',
'', "remove generic CODEX token")
path.write_text(text, encoding="utf-8")

# K item editor: exclude CODEX and cycle through the filtered list safely.
path = Path("src/main/java/com/bl4ues/scpinventory/client/gui/ItemConfigScreen.java")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
'import net.minecraft.world.item.ItemStack;\n',
'import net.minecraft.world.item.ItemStack;\n\nimport java.util.Arrays;\n', "ItemConfigScreen Arrays import")
text = replace_once(text,
'    private static final ScpItemType[] TYPES = ScpItemType.values();\n',
'''    private static final ScpItemType[] TYPES = Arrays.stream(ScpItemType.values())
            .filter(type -> type != ScpItemType.CODEX)
            .toArray(ScpItemType[]::new);
''', "ItemConfigScreen filtered types")
text = replace_once(text,
'''        typeButton = addRenderableWidget(Button.builder(ScpFonts.roboto(typeText()), b -> {
            type = TYPES[(type.ordinal() + 1) % TYPES.length];
            b.setMessage(ScpFonts.roboto(typeText()));
        }).bounds(x, y, width, 22).build());
''',
'''        typeButton = addRenderableWidget(Button.builder(ScpFonts.roboto(typeText()), b -> {
            int index = Arrays.asList(TYPES).indexOf(type);
            type = TYPES[(Math.max(0, index) + 1) % TYPES.length];
            b.setMessage(ScpFonts.roboto(typeText()));
        }).bounds(x, y, width, 22).build());
''', "ItemConfigScreen safe cycle")
text = replace_once(text,
'''        try {
            return ScpItemType.valueOf(value == null ? "MISCELLANEOUS" : value.trim().toUpperCase());
        } catch (Exception ignored) {
''',
'''        try {
            ScpItemType parsed = ScpItemType.valueOf(value == null ? "MISCELLANEOUS" : value.trim().toUpperCase());
            return parsed == ScpItemType.CODEX ? ScpItemType.MISCELLANEOUS : parsed;
        } catch (Exception ignored) {
''', "ItemConfigScreen legacy CODEX normalization")
path.write_text(text, encoding="utf-8")

# Legacy item editor persistence: never write generic CODEX rules.
path = Path("src/main/java/com/bl4ues/scpinventory/config/ItemConfigManager.java")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
'            case "CODEX", "DOCUMENT", "DOC" -> "CODEX";\n',
'            case "CODEX", "DOCUMENT", "DOC" -> "MISCELLANEOUS";\n',
"ItemConfigManager CODEX normalization")
path.write_text(text, encoding="utf-8")

# Native config center: remove CODEX, retain PLACEABLE, and discard stale generic CODEX rows.
path = Path("src/main/java/net/mcreator/scpadditions/config/ui/ConfigCenterClient.java")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
'        private static final List<String> TYPES = List.of("MISCELLANEOUS", "CONSUMABLE", "USABLE", "HARMFUL", "KEY", "COIN", "AMMO", "HEAD", "CHEST", "LEGS", "FEET", "ACCESSORY", "ACCESSORYHAND", "WEAPON", "CODEX");\n',
'        private static final List<String> TYPES = List.of("MISCELLANEOUS", "CONSUMABLE", "USABLE", "PLACEABLE", "HARMFUL", "KEY", "COIN", "AMMO", "HEAD", "CHEST", "LEGS", "FEET", "ACCESSORY", "ACCESSORYHAND", "WEAPON");\n',
"ConfigCenterClient item types")
text = replace_once(text,
'''        private ItemRulesScreen(Screen parent, JsonObject root) {
            super(parent, "Item Categories & Equipment Effects");
            this.root = root;
            this.search = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 20, Component.literal("Search"));
        }
''',
'''        private ItemRulesScreen(Screen parent, JsonObject root) {
            super(parent, "Item Categories & Equipment Effects");
            this.root = root;
            JsonArray rules = array(root, "item_rules");
            for (int i = rules.size() - 1; i >= 0; i--) {
                JsonElement element = rules.get(i);
                if (!element.isJsonObject()) continue;
                String configured = string(element.getAsJsonObject(), "type", "");
                if ("CODEX".equalsIgnoreCase(configured)
                        || "DOCUMENT".equalsIgnoreCase(configured)
                        || "DOC".equalsIgnoreCase(configured)) {
                    rules.remove(i);
                }
            }
            this.search = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 20, Component.literal("Search"));
        }
''', "ConfigCenterClient stale CODEX cleanup")
text = text.replace('Component.literal(edit.has("world_image") ? "Replace PNG" : "Import PNG")',
                    'Component.literal(edit.has("world_image") ? "Replace Image" : "Import Image")')
text = text.replace('Component.literal("Remove World PNG")',
                    'Component.literal("Remove World Image")')
text = text.replace('"World PNG attached" : "No world PNG"',
                    '"World image attached" : "No world image"')
path.write_text(text, encoding="utf-8")

# Unity overlay category list and tooltips.
path = Path("src/main/java/net/mcreator/scpadditions/client/UnityConfigurationUiEvents.java")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
'''            "KEY", "COIN", "AMMO", "HEAD", "CHEST", "LEGS", "FEET",
            "ACCESSORY", "ACCESSORYHAND", "WEAPON", "CODEX");
''',
'''            "KEY", "COIN", "AMMO", "HEAD", "CHEST", "LEGS", "FEET",
            "ACCESSORY", "ACCESSORYHAND", "WEAPON");
''', "Unity category list")
text = text.replace('values.put("+ paper document", "Create a Codex definition using minecraft:paper as its temporary item.");',
'''values.put("+ paper document", "Create a configured Codex definition. Documents cannot be assigned through generic item categories.");''')
path.write_text(text, encoding="utf-8")

# World asset storage with PNG and JPEG support.
path = Path("src/main/java/net/mcreator/scpadditions/config/ui/CodexAssetStorage.java")
path.write_text('''package net.mcreator.scpadditions.config.ui;

import com.bl4ues.scpinventory.capability.ScpInventoryProvider;
import com.bl4ues.scpinventory.item.CodexDocumentDefinition;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.registries.ForgeRegistries;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

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
    public static final int MAX_IMAGE_BYTES = 2_500_000;
    public static final int MAX_TEXT_BYTES = 262_144;
    public static final int MAX_TRANSFER_BYTES = Math.max(MAX_IMAGE_BYTES, MAX_TEXT_BYTES);
    private static final Pattern SAFE_KEY = Pattern.compile(
            "(?:images/[0-9a-fA-F-]{36}\\\\.(?:png|jpe?g)|texts/[0-9a-fA-F-]{36}\\\\.txt)");
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
        int limit = "image".equals(normalized) ? MAX_IMAGE_BYTES : MAX_TEXT_BYTES;
        if (data.length == 0 || data.length > limit) {
            return UploadResult.failure("Codex asset exceeds the allowed size.");
        }

        String extension;
        if ("image".equals(normalized)) {
            extension = detectImageExtension(data);
            if (extension.isEmpty()) {
                return UploadResult.failure("Only PNG and JPEG Codex images are supported.");
            }
        } else {
            if (!isUtf8(data)) return UploadResult.failure("The uploaded text is not valid UTF-8.");
            extension = ".txt";
        }

        try {
            MinecraftServer server = player.getServer();
            if (server == null) return UploadResult.failure("No server world is available.");
            String folder = "image".equals(normalized) ? "images" : "texts";
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
        if (ScpAdditionsModulesConfig.get().inventory.enabled
                && ScpItemClassifier.getCodexDocument(stack).isPresent()) {
            boolean[] stored = {false};
            player.getCapability(ScpInventoryProvider.INSTANCE).ifPresent(inventory -> {
                if (inventory.addDocumentItem(stack.copy())) {
                    stored[0] = true;
                    ModNetwork.syncTo(player, inventory);
                }
            });
            if (stored[0]) return true;
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
        if ("image".equals(value) || "png".equals(value)
                || "jpg".equals(value) || "jpeg".equals(value)) return "image";
        return "text".equals(value) ? "text" : "";
    }

    private static String detectImageExtension(byte[] bytes) {
        if (hasPngSignature(bytes)) return ".png";
        if (hasJpegSignature(bytes)) return ".jpg";
        return "";
    }

    private static boolean hasPngSignature(byte[] bytes) {
        if (bytes.length < PNG_SIGNATURE.length) return false;
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (bytes[i] != PNG_SIGNATURE[i]) return false;
        }
        return true;
    }

    private static boolean hasJpegSignature(byte[] bytes) {
        return bytes.length >= 4
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF
                && (bytes[bytes.length - 2] & 0xFF) == 0xFF
                && (bytes[bytes.length - 1] & 0xFF) == 0xD9;
    }

    private static boolean isUtf8(byte[] bytes) {
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException exception) {
            return false;
        }
    }

    private static String safeName(String name) {
        if (name == null || name.isBlank()) return "asset";
        String cleaned = name.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        return cleaned.isBlank() ? "asset" : cleaned;
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
''', encoding="utf-8")

# Client cache uses a generic image channel while preserving old aliases.
path = Path("src/main/java/net/mcreator/scpadditions/client/CodexAssetClient.java")
text = path.read_text(encoding="utf-8")
text = text.replace('String cache = cacheKey("png", key);', 'String cache = cacheKey("image", key);')
text = text.replace('request("png", key);', 'request("image", key);')
text = replace_once(text,
'''    private static String cacheKey(String kind, String key) {
        return (kind == null ? "" : kind) + ":" + (key == null ? "" : key);
    }
''',
'''    private static String cacheKey(String kind, String key) {
        String normalized = kind == null ? "" : kind.trim().toLowerCase(java.util.Locale.ROOT);
        if ("png".equals(normalized) || "jpg".equals(normalized)
                || "jpeg".equals(normalized)) normalized = "image";
        return normalized + ":" + (key == null ? "" : key);
    }
''', "CodexAssetClient image cache normalization")
path.write_text(text, encoding="utf-8")

# Drag-and-drop image screen: PNG/JPG/JPEG and a 2.5 MB limit.
path = Path("src/main/java/net/mcreator/scpadditions/client/CodexImageDropScreen.java")
text = path.read_text(encoding="utf-8")
text = text.replace('private static final int MAX_PNG_BYTES = 900_000;',
                    'private static final int MAX_IMAGE_BYTES = 2_500_000;')
text = text.replace('"Drop one PNG file anywhere on this screen."',
                    '"Drop one PNG, JPG, or JPEG file anywhere on this screen."')
text = text.replace('ScpFonts.roboto("Import Codex PNG")',
                    'ScpFonts.roboto("Import Codex Image")')
text = text.replace('ScpFonts.roboto("Remove World PNG")',
                    'ScpFonts.roboto("Remove World Image")')
text = text.replace('fail("Drop exactly one PNG file.");',
                    'fail("Drop exactly one image file.");')
text = text.replace('String name = path.getFileName() == null ? "image.png"',
                    'String name = path.getFileName() == null ? "image.png"')
text = replace_once(text,
'''        if (!name.toLowerCase(Locale.ROOT).endsWith(".png")) {
            fail("Only PNG files are accepted.");
            return;
        }
''',
'''        String lowerName = name.toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".png") && !lowerName.endsWith(".jpg")
                && !lowerName.endsWith(".jpeg")) {
            fail("Only PNG, JPG, and JPEG files are accepted.");
            return;
        }
''', "CodexImageDropScreen extensions")
text = text.replace('bytes.length > MAX_PNG_BYTES', 'bytes.length > MAX_IMAGE_BYTES')
text = text.replace('fail("PNG must be between 1 byte and 900 KB.");',
                    'fail("Image must be between 1 byte and 2.5 MB.");')
text = text.replace('fail("PNG dimensions must be between 1 and 4096 pixels.");',
                    'fail("Image dimensions must be between 1 and 4096 pixels.");')
text = text.replace('CodexAssetClient.upload("png", name, bytes, key -> {',
                    'CodexAssetClient.upload("image", name, bytes, key -> {')
text = text.replace('fail("Could not read that PNG: " + readable(exception));',
                    'fail("Could not read that image: " + readable(exception));')
text = text.replace('ScpFonts.roboto("DROP PNG HERE")',
                    'ScpFonts.roboto("DROP PNG / JPG / JPEG HERE")')
text = text.replace('"Saved in this world · Maximum 900 KB · 4096 × 4096"',
                    '"Saved in this world · Maximum 2.5 MB · 4096 × 4096"')
path.write_text(text, encoding="utf-8")

# Documentation.
path = Path("docs/CONFIGURATION_CENTER.md")
text = path.read_text(encoding="utf-8")
text = replace_once(text,
'''Codex entries may use a packaged image resource, a UTF-8 text resource, or both. The editor also provides **Import PNG**, which opens a drag-and-drop area, and **Write Text**, which accepts direct multiline UTF-8 input. Both are saved as normal files under the active world's `scp_additions/codex_assets` folder; the server-authoritative JSON keeps only safe relative references, and clients request the files on demand. Packaged resources remain available for modpacks that prefer resource packs. **Match Mode** may target every copy of the selected item or a uniquely generated NBT-tagged document; **Give Test Item** creates the latter with its configured display name. Advanced NBT conditions and image dimensions are preserved when an entry is edited.
''',
'''Codex entries may use a packaged image resource, a UTF-8 text resource, or both. The editor also provides **Import Image**, which accepts PNG, JPG, and JPEG files up to 2.5 MB through drag-and-drop, and **Write Text**, which accepts direct multiline UTF-8 input. Both are saved as normal files under the active world's `scp_additions/codex_assets` folder; the server-authoritative JSON keeps only safe relative references, and clients request the files on demand. Packaged resources remain available for modpacks that prefer resource packs. **Match Mode** may target every copy of the selected item or a uniquely generated NBT-tagged document; **Save & Give Test Item** creates the latter with its configured display name. `CODEX` is an internal runtime category and is not available in generic item rules: documents must be defined through `codex_documents`, preventing ordinary items from becoming empty document entries. Advanced NBT conditions and image dimensions are preserved when an entry is edited.
''', "Codex documentation")
path.write_text(text, encoding="utf-8")

path = Path("CHANGELOG.md")
text = path.read_text(encoding="utf-8")
anchor = "## Configuration center\n"
if anchor not in text:
    raise RuntimeError("CHANGELOG Configuration center heading not found")
addition = (
    "- Removed `CODEX` from generic item-category editors and config-token parsing; documents are now created exclusively through `codex_documents`, avoiding empty document entries;\n"
    "- Expanded world-scoped Codex image import from PNG-only to PNG/JPG/JPEG and raised the per-image limit from 900 KB to 2.5 MB;\n"
)
text = text.replace(anchor, anchor + addition, 1)
text = text.replace("Added direct Codex text editing and PNG drag-and-drop import.",
                    "Added direct Codex text editing and PNG/JPG/JPEG drag-and-drop import.")
path.write_text(text, encoding="utf-8")
