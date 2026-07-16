from pathlib import Path
import re

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
        raise RuntimeError(f"Expected one occurrence in {path}, found {count}: {old[:120]!r}")
    write(path, text.replace(old, new, 1))


def replace_between(path: str, start: str, end: str, replacement: str) -> None:
    text = read(path)
    start_index = text.find(start)
    if start_index < 0:
        raise RuntimeError(f"Start marker not found in {path}: {start!r}")
    end_index = text.find(end, start_index)
    if end_index < 0:
        raise RuntimeError(f"End marker not found in {path}: {end!r}")
    write(path, text[:start_index] + replacement + text[end_index:])


# ---------------------------------------------------------------------------
# Context editor header spacing
# ---------------------------------------------------------------------------
replace_once(
    "src/main/java/com/bl4ues/scpinventory/client/gui/ContextConfigScreen.java",
    '''        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0xCC111317);
        g.fill(left, top, left + PANEL_W, top + 24, 0xE525282D);
        g.drawString(font, "Context Interaction Editor", left + 12, top + 8, 0xFFE8E8E8, false);
        g.drawString(font, (existing ? "Editing " : "New ") + compact(blockId, 30), left + 12, top + 28, 0xFFB5C7FF, false);

        g.drawString(font, "Action", left + 12, top + 35, 0xFFB7B7B7, false);''',
    '''        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0xCC111317);
        g.fill(left, top, left + PANEL_W, top + 34, 0xE525282D);
        g.drawString(font, "Context Interaction Editor", left + 12, top + 7, 0xFFE8E8E8, false);
        g.drawString(font, (existing ? "Editing " : "New ") + compact(blockId, 30), left + 12, top + 21, 0xFFB5C7FF, false);

        g.drawString(font, "Action", left + 12, top + 36, 0xFFB7B7B7, false);''')


# ---------------------------------------------------------------------------
# Configuration presentation: K header, SCP-294 cup previews, entity previews
# ---------------------------------------------------------------------------
ui_path = "src/main/java/net/mcreator/scpadditions/client/UnityConfigurationUiEvents.java"
replace_once(
    ui_path,
    '''import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;''',
    '''import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;''')
replace_once(
    ui_path,
    '''import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;''',
    '''import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;''')
replace_once(
    ui_path,
    '''import java.util.ArrayList;
import java.util.LinkedHashMap;''',
    '''import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;''')
replace_once(
    ui_path,
    '''    private static final Map<String, String> TOOLTIPS = buildTooltips();
    private static final Map<AbstractButton, Component> BUTTON_LABELS = new WeakHashMap<>();''',
    '''    private static final Map<String, String> TOOLTIPS = buildTooltips();
    private static final Map<AbstractButton, Component> BUTTON_LABELS = new WeakHashMap<>();
    private static final Map<ResourceLocation, LivingEntity> ENTITY_PREVIEWS = new HashMap<>();
    private static ClientLevel entityPreviewLevel;''')
replace_once(
    ui_path,
    '''        if ("HomeScreen".equals(name)) {
            graphics.fill(spec.x(), spec.y(), spec.x() + spec.width(), spec.y() + 44, HEADER);''',
    '''        if (screen instanceof ContextConfigScreen) {
            graphics.fill(spec.x(), spec.y(), spec.x() + spec.width(), spec.y() + 34, HEADER);
            graphics.drawString(font, ScpFonts.roboto(screen.getTitle()),
                    spec.x() + 12, spec.y() + 7, WHITE, false);
            return;
        }
        if ("HomeScreen".equals(name)) {
            graphics.fill(spec.x(), spec.y(), spec.x() + spec.width(), spec.y() + 44, HEADER);''')

replace_between(
    ui_path,
    '''    private static void renderKEditorText(GuiGraphics graphics, Screen screen,
''',
    '''    private static PanelSpec panelSpec(Screen screen) {
''',
    '''    private static void renderKEditorText(GuiGraphics graphics, Screen screen,
                                          PanelSpec spec, Font font) {
        String blockId = readField(screen, "blockId", String.class);
        Boolean existing = readField(screen, "existing", Boolean.class);
        double anchorX = numberField(screen, "anchorX", 0.5D);
        double anchorY = numberField(screen, "anchorY", 0.5D);
        double anchorZ = numberField(screen, "anchorZ", 0.5D);

        graphics.fill(spec.x() + 8, spec.y() + 18, spec.x() + spec.width() - 8,
                spec.y() + 34, HEADER);
        graphics.drawString(font, ScpFonts.roboto(
                        (Boolean.TRUE.equals(existing) ? "Editing " : "New ")
                                + compact(blockId, 34)),
                spec.x() + 12, spec.y() + 21, PALE_GOLD, false);

        String[] lines = {
                "Action",
                "Name / Display",
                "Range / Input / Item",
                "Anchor tools",
                "Anchor local",
                String.format(Locale.ROOT, "X %.3f  Y %.3f  Z %.3f", anchorX, anchorY, anchorZ),
                "Arrows X/Y, PgUp/PgDn Z, wheel Y",
                "Shift=0.10  Ctrl=0.01  normal=0.05",
                "Rotate mode previews the final in-world anchor.",
                "Forget asks twice, then removes this rule."
        };
        int[] ys = {36, 71, 107, 142, 242, 254, 270, 282, 296, 310};
        int[] colors = {MUTED, MUTED, MUTED, MUTED, PALE_GOLD,
                PALE_GOLD, MUTED, MUTED, 0xFF88DDEE, DANGER};
        for (int i = 0; i < lines.length; i++) {
            coverTextLine(graphics, spec.x() + 8, spec.y() + ys[i] - 3,
                    spec.width() - 16);
            graphics.drawString(font, ScpFonts.roboto(lines[i]), spec.x() + 12,
                    spec.y() + ys[i], colors[i], false);
        }
    }

''')

replace_once(
    ui_path,
    '''            ItemStack input = firstRecipeItem(recipe, "item_inputs");
            ItemStack output = firstRecipeItem(recipe, "item_outputs");
            if (output.isEmpty()) output = firstRecipeItem(recipe, "weighted_item_outputs");
            if (!input.isEmpty()) graphics.renderItem(input, x + 7, rowY + 2);
            graphics.drawString(font, ScpFonts.roboto("→"), x + 26, rowY + 6,
                    PALE_GOLD, false);
            if (!output.isEmpty()) graphics.renderItem(output, x + 38, rowY + 2);''',
    '''            RecipePreview input = firstRecipePreview(screen, recipe, true);
            RecipePreview output = firstRecipePreview(screen, recipe, false);
            renderRecipePreview(graphics, input, x + 7, rowY + 2);
            graphics.drawString(font, ScpFonts.roboto("→"), x + 26, rowY + 6,
                    PALE_GOLD, false);
            renderRecipePreview(graphics, output, x + 38, rowY + 2);''')

replace_between(
    ui_path,
    '''    private static ItemStack firstRecipeItem(JsonObject recipe, String key) {
''',
    '''    private static String summarizeRecipeSide(JsonObject recipe, boolean intake) {
''',
    '''    private static RecipePreview firstRecipePreview(Screen screen, JsonObject recipe,
                                                    boolean intake) {
        List<String> itemKeys = intake
                ? List.of("item_inputs")
                : List.of("item_outputs", "weighted_item_outputs");
        for (String key : itemKeys) {
            if (!recipe.has(key) || !recipe.get(key).isJsonArray()) continue;
            for (JsonElement element : recipe.getAsJsonArray(key)) {
                if (!element.isJsonObject()) continue;
                String itemId = string(element.getAsJsonObject(), "item", "");
                if (!itemId.isBlank()) {
                    return new RecipePreview(recipeItemPreview(screen, itemId), null);
                }
            }
        }

        String entityKey = intake ? "entity_inputs" : "entity_outputs";
        if (recipe.has(entityKey) && recipe.get(entityKey).isJsonArray()) {
            for (JsonElement element : recipe.getAsJsonArray(entityKey)) {
                if (!element.isJsonObject()) continue;
                ResourceLocation entityId = ResourceLocation.tryParse(
                        string(element.getAsJsonObject(), "entity", ""));
                if (entityId != null) return new RecipePreview(ItemStack.EMPTY, entityId);
            }
        }
        return new RecipePreview(ItemStack.EMPTY, null);
    }

    private static ItemStack recipeItemPreview(Screen screen, String itemId) {
        JsonObject files = readStaticField(screen.getClass().getDeclaringClass(),
                "files", JsonObject.class);
        JsonObject drinkRoot = childObject(files, "294");
        if (drinkRoot != null && drinkRoot.has("drinks")
                && drinkRoot.get("drinks").isJsonArray()) {
            for (JsonElement element : drinkRoot.getAsJsonArray("drinks")) {
                if (!element.isJsonObject()) continue;
                JsonObject drink = element.getAsJsonObject();
                JsonObject result = childObject(drink, "result");
                if (result != null && itemId.equals(string(result, "item", ""))) {
                    return coloredCup(string(drink, "cup_color", "#FFFFFF"));
                }
            }
        }
        return itemStack(itemId);
    }

    private static void renderRecipePreview(GuiGraphics graphics, RecipePreview preview,
                                            int x, int y) {
        if (preview == null) return;
        if (!preview.item().isEmpty()) {
            graphics.renderItem(preview.item(), x, y);
            return;
        }
        if (preview.entityId() == null) return;
        if (!renderEntityPreview(graphics, preview.entityId(), x, y)) {
            graphics.renderItem(new ItemStack(Items.SPAWNER), x, y);
        }
    }

    private static boolean renderEntityPreview(GuiGraphics graphics, ResourceLocation id,
                                               int x, int y) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return false;
        if (entityPreviewLevel != level) {
            ENTITY_PREVIEWS.clear();
            entityPreviewLevel = level;
        }

        LivingEntity living = ENTITY_PREVIEWS.computeIfAbsent(id, key -> {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(key);
            if (type == null) return null;
            Entity entity = type.create(level);
            if (!(entity instanceof LivingEntity created)) return null;
            if (created instanceof Mob mob) mob.setNoAi(true);
            created.setYRot(25.0F);
            created.setXRot(0.0F);
            return created;
        });
        if (living == null) return false;

        float size = Math.max(0.45F, Math.max(living.getBbWidth(), living.getBbHeight()));
        int scale = Mth.clamp(Math.round(12.0F / size), 5, 13);
        try {
            graphics.enableScissor(x - 1, y - 1, x + 18, y + 19);
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                    x + 8, y + 17, scale, 0.0F, 0.0F, living);
            graphics.disableScissor();
            return true;
        } catch (Throwable ignored) {
            graphics.disableScissor();
            return false;
        }
    }

''')

replace_once(
    ui_path,
    '''        if (screen instanceof ItemConfigScreen || screen instanceof ContextConfigScreen
                || screen instanceof UnityColorPickerScreen) return true;''',
    '''        if (screen instanceof ItemConfigScreen || screen instanceof ContextConfigScreen
                || screen instanceof UnityColorPickerScreen
                || screen instanceof CodexImageDropScreen
                || screen instanceof CodexTextEditorScreen) return true;''')
replace_once(
    ui_path,
    '''        values.put("pick color", "Choose the cup color with synchronized RGB sliders and hexadecimal input.");''',
    '''        values.put("pick color", "Choose the cup color with synchronized RGB sliders and hexadecimal input.");
        values.put("import png", "Open a drop zone and embed a PNG directly in this Codex definition.");
        values.put("replace png", "Replace the PNG embedded in this Codex definition.");
        values.put("write text", "Write Codex text directly without creating a resource file.");
        values.put("edit text", "Edit the text embedded directly in this Codex definition.");''')
replace_once(
    ui_path,
    '''    private record PanelSpec(int x, int y, int width, int height) {
    }
}''',
    '''    private record RecipePreview(ItemStack item, ResourceLocation entityId) {
    }

    private record PanelSpec(int x, int y, int width, int height) {
    }
}''')


# ---------------------------------------------------------------------------
# Preserve embedded Codex fields when flattening the runtime definitions
# ---------------------------------------------------------------------------
replace_once(
    "src/main/java/com/bl4ues/scpinventory/config/ScpInventoryConfig.java",
    '''            putIfPresent(fields, obj, "text");
            putIfPresent(fields, obj, "image_width");''',
    '''            putIfPresent(fields, obj, "text");
            putIfPresent(fields, obj, "inline_image_png");
            putIfPresent(fields, obj, "inline_text");
            putIfPresent(fields, obj, "image_width");''')


# ---------------------------------------------------------------------------
# Codex definition supports embedded PNG and UTF-8 text
# ---------------------------------------------------------------------------
write("src/main/java/com/bl4ues/scpinventory/item/CodexDocumentDefinition.java", r'''package com.bl4ues.scpinventory.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class CodexDocumentDefinition {

    private static final int DEFAULT_IMAGE_WIDTH = 1279;
    private static final int DEFAULT_IMAGE_HEIGHT = 1920;

    private final ResourceLocation itemId;
    private final String category;
    private final String displayName;
    private final ResourceLocation imageLocation;
    private final ResourceLocation textLocation;
    private final String inlineImageBase64;
    private final String inlineText;
    private final int imageWidth;
    private final int imageHeight;
    private final String creator;
    private final String timestamp;
    private final String uuid;
    private final String nbtKey;
    private final String nbtValue;

    private CodexDocumentDefinition(ResourceLocation itemId, String category,
                                    String displayName, ResourceLocation imageLocation,
                                    ResourceLocation textLocation, String inlineImageBase64,
                                    String inlineText, int imageWidth, int imageHeight,
                                    String creator, String timestamp, String uuid,
                                    String nbtKey, String nbtValue) {
        this.itemId = itemId;
        this.category = cleanOrDefault(category, "Documents");
        this.displayName = displayName == null ? "" : displayName.trim();
        this.imageLocation = imageLocation;
        this.textLocation = textLocation;
        this.inlineImageBase64 = inlineImageBase64 == null ? "" : inlineImageBase64.trim();
        this.inlineText = inlineText == null ? "" : inlineText;
        this.imageWidth = Math.max(1, imageWidth);
        this.imageHeight = Math.max(1, imageHeight);
        this.creator = creator == null ? "" : creator.trim();
        this.timestamp = timestamp == null ? "" : timestamp.trim();
        this.uuid = uuid == null ? "" : uuid.trim();
        this.nbtKey = nbtKey == null ? "" : nbtKey.trim();
        this.nbtValue = nbtValue == null ? "" : nbtValue.trim();
    }

    public static Optional<CodexDocumentDefinition> parse(String rawRule) {
        if (rawRule == null || rawRule.isBlank()) return Optional.empty();
        String raw = rawRule.trim();
        if (raw.contains("=")) return parseKeyValueFormat(raw);
        if (raw.contains("|")) return parsePipeFormat(raw);

        ResourceLocation itemId = ResourceLocation.tryParse(raw);
        if (itemId == null) return Optional.empty();
        return Optional.of(new CodexDocumentDefinition(itemId, "Documents", "",
                null, null, "", "", DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT,
                "", "", "", "", ""));
    }

    public static CodexDocumentDefinition fallback(ItemStack stack) {
        ResourceLocation itemId = stack == null || stack.isEmpty()
                ? new ResourceLocation("minecraft", "air")
                : BuiltInRegistries.ITEM.getKey(stack.getItem());
        String name = stack == null || stack.isEmpty()
                ? "Unknown Document" : stack.getHoverName().getString();
        return new CodexDocumentDefinition(itemId, "Documents", name,
                null, null, "", "", DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT,
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
                    parseLocation(getPart(parts, 4)), "", "",
                    parseInt(getPart(parts, 8), DEFAULT_IMAGE_WIDTH),
                    parseInt(getPart(parts, 9), DEFAULT_IMAGE_HEIGHT),
                    getPart(parts, 5), getPart(parts, 6), getPart(parts, 7), "", ""));
        }

        return Optional.of(new CodexDocumentDefinition(itemId, "Documents",
                getPart(parts, 1), null, null, "", "",
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
                values.getOrDefault("inline_image_png", ""),
                decodeBase64(values.get("inline_text")),
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
        return matchesTagValue(tag, "creator", creator)
                && matchesTagValue(tag, "timestamp", timestamp)
                && matchesTagValue(tag, "uuid", uuid)
                && matchesTagValue(tag, nbtKey, nbtValue);
    }

    public String getCategory() {
        return category;
    }

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

    public Optional<String> getInlineImageBase64() {
        return inlineImageBase64.isBlank() ? Optional.empty() : Optional.of(inlineImageBase64);
    }

    public Optional<String> getInlineText() {
        return inlineText.isBlank() ? Optional.empty() : Optional.of(inlineText);
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public String getStableId(ItemStack fallbackStack) {
        return category + "|" + getDisplayName(fallbackStack) + "|" + itemId
                + "|" + creator + "|" + timestamp + "|" + uuid
                + "|" + Integer.toHexString(inlineImageBase64.hashCode())
                + "|" + Integer.toHexString(inlineText.hashCode());
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
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String decodeBase64(String value) {
        if (value == null || value.isBlank()) return "";
        try {
            return new String(Base64.getDecoder().decode(value.trim()), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return "";
        }
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

    private static String normalize(String value) {
        return value == null ? "" : value.replace(" ", "").replace("[", "")
                .replace("]", "").replace("\"", "").trim().toLowerCase(Locale.ROOT);
    }
}
''')


# ---------------------------------------------------------------------------
# Codex panel loads embedded resources
# ---------------------------------------------------------------------------
codex_panel = "src/main/java/com/bl4ues/scpinventory/client/gui/components/CodexPanel.java"
replace_once(
    codex_panel,
    '''import com.bl4ues.scpinventory.client.ScpFonts;

import com.bl4ues.scpinventory.capability.IScpInventory;''',
    '''import com.bl4ues.scpinventory.client.ScpFonts;
import com.mojang.blaze3d.platform.NativeImage;

import com.bl4ues.scpinventory.capability.IScpInventory;''')
replace_once(
    codex_panel,
    '''import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;''',
    '''import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;''')
replace_once(
    codex_panel,
    '''import java.io.BufferedReader;
import java.io.IOException;''',
    '''import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;''')
replace_once(
    codex_panel,
    '''import java.util.ArrayList;
import java.util.HashSet;''',
    '''import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;''')
replace_once(
    codex_panel,
    '''    private static final double DRAG_THRESHOLD = 4.0D;

    private final Minecraft mc = Minecraft.getInstance();''',
    '''    private static final double DRAG_THRESHOLD = 4.0D;
    private static final Map<String, ResourceLocation> INLINE_IMAGE_TEXTURES = new HashMap<>();

    private final Minecraft mc = Minecraft.getInstance();''')
replace_once(
    codex_panel,
    '''        ResourceLocation image = definition.getImageLocation().orElse(null);''',
    '''        ResourceLocation image = inlineImageTexture(definition)
                .orElseGet(() -> definition.getImageLocation().orElse(null));''')
replace_between(
    codex_panel,
    '''    private Optional<String> readText(CodexDocumentDefinition definition) {
''',
    '''    private String buildFallbackText(ItemStack document, CodexDocumentDefinition definition)''',
    '''    private Optional<String> readText(CodexDocumentDefinition definition) {
        Optional<String> inline = definition.getInlineText();
        if (inline.isPresent()) return inline;
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

    private Optional<ResourceLocation> inlineImageTexture(CodexDocumentDefinition definition) {
        Optional<String> encodedValue = definition.getInlineImageBase64();
        if (encodedValue.isEmpty() || mc == null) return Optional.empty();
        String encoded = encodedValue.get();
        String cacheKey = encoded.length() + ":" + Integer.toHexString(encoded.hashCode());
        ResourceLocation cached = INLINE_IMAGE_TEXTURES.get(cacheKey);
        if (cached != null) return Optional.of(cached);
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));
            ResourceLocation location = new ResourceLocation("scp_additions",
                    "codex_inline/" + encoded.length() + "_"
                            + Integer.toHexString(encoded.hashCode()));
            mc.getTextureManager().register(location, new DynamicTexture(image));
            INLINE_IMAGE_TEXTURES.put(cacheKey, location);
            return Optional.of(location);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

''')


# ---------------------------------------------------------------------------
# Codex editor enhancement hook and dedicated editors
# ---------------------------------------------------------------------------
write("src/main/java/net/mcreator/scpadditions/client/CodexEditorEnhancements.java", r'''package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Mod.EventBusSubscriber(modid = "scp_additions", value = Dist.CLIENT)
public final class CodexEditorEnhancements {
    private static final String INLINE_IMAGE = "inline_image_png";
    private static final String INLINE_TEXT = "inline_text";

    private CodexEditorEnhancements() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!"CodexDetailScreen".equals(screen.getClass().getSimpleName())) return;
        JsonObject edit = readField(screen, "edit", JsonObject.class);
        EditBox imageBox = readField(screen, "imageBox", EditBox.class);
        EditBox textBox = readField(screen, "textBox", EditBox.class);
        if (edit == null || imageBox == null || textBox == null) return;

        int buttonWidth = 106;
        shrink(imageBox, buttonWidth + 6);
        Button imageButton = Button.builder(ScpFonts.roboto(
                        edit.has(INLINE_IMAGE) ? "Replace PNG" : "Import PNG"),
                ignored -> openImageEditor(screen, edit))
                .bounds(imageBox.getX() + imageBox.getWidth() + 6, imageBox.getY(),
                        buttonWidth, 20).build();
        event.addListener(imageButton);

        shrink(textBox, buttonWidth + 6);
        Button textButton = Button.builder(ScpFonts.roboto(
                        edit.has(INLINE_TEXT) ? "Edit Text" : "Write Text"),
                ignored -> openTextEditor(screen, edit))
                .bounds(textBox.getX() + textBox.getWidth() + 6, textBox.getY(),
                        buttonWidth, 20).build();
        event.addListener(textButton);
    }

    private static void shrink(EditBox box, int amount) {
        if (box.getWidth() > amount + 80) box.setWidth(box.getWidth() - amount);
    }

    private static void openImageEditor(Screen parent, JsonObject edit) {
        invokeNoArgs(parent, "sync");
        Minecraft.getInstance().setScreen(new CodexImageDropScreen(parent,
                edit.has(INLINE_IMAGE), imported -> {
                    edit.addProperty(INLINE_IMAGE, imported.base64());
                    edit.addProperty("image_width", imported.width());
                    edit.addProperty("image_height", imported.height());
                    Minecraft.getInstance().setScreen(parent);
                }, () -> {
                    edit.remove(INLINE_IMAGE);
                    Minecraft.getInstance().setScreen(parent);
                }));
    }

    private static void openTextEditor(Screen parent, JsonObject edit) {
        invokeNoArgs(parent, "sync");
        String initial = decode(edit.has(INLINE_TEXT) ? edit.get(INLINE_TEXT).getAsString() : "");
        Minecraft.getInstance().setScreen(new CodexTextEditorScreen(parent, initial, text -> {
            if (text == null || text.isBlank()) edit.remove(INLINE_TEXT);
            else edit.addProperty(INLINE_TEXT, Base64.getEncoder().encodeToString(
                    text.getBytes(StandardCharsets.UTF_8)));
            Minecraft.getInstance().setScreen(parent);
        }));
    }

    private static String decode(String encoded) {
        if (encoded == null || encoded.isBlank()) return "";
        try {
            return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    private static void invokeNoArgs(Object target, String name) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Method method = type.getDeclaredMethod(name);
                method.setAccessible(true);
                method.invoke(target);
                return;
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
    }

    private static <T> T readField(Object target, String name, Class<T> expected) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                Object value = field.get(target);
                return expected.isInstance(value) ? expected.cast(value) : null;
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }
}
''')

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
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class CodexImageDropScreen extends Screen {
    private static final int MAX_PNG_BYTES = 900_000;
    private final Screen parent;
    private final boolean hasImportedImage;
    private final Consumer<ImportedImage> callback;
    private final Runnable clearCallback;
    private String status = "Drop one PNG file anywhere on this screen.";
    private int statusColor = 0xFFA9AFBA;

    public CodexImageDropScreen(Screen parent, boolean hasImportedImage,
                                Consumer<ImportedImage> callback, Runnable clearCallback) {
        super(ScpFonts.roboto("Import Codex PNG"));
        this.parent = parent;
        this.hasImportedImage = hasImportedImage;
        this.callback = callback;
        this.clearCallback = clearCallback;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(500, width - 24);
        int left = (width - panelWidth) / 2;
        int top = Math.max(12, (height - 240) / 2);
        int buttonWidth = (panelWidth - 56) / 2;
        Button remove = addRenderableWidget(Button.builder(ScpFonts.roboto("Remove Imported PNG"), b -> {
            if (clearCallback != null) clearCallback.run();
        }).bounds(left + 20, top + 190, buttonWidth, 22).build());
        remove.active = hasImportedImage;
        addRenderableWidget(Button.builder(ScpFonts.roboto("Cancel"),
                b -> Minecraft.getInstance().setScreen(parent))
                .bounds(left + 36 + buttonWidth, top + 190, buttonWidth, 22).build());
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
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
            if (imageWidth < 1 || imageHeight < 1 || imageWidth > 4096 || imageHeight > 4096) {
                fail("PNG dimensions must be between 1 and 4096 pixels.");
                return;
            }
            if (callback != null) callback.accept(new ImportedImage(
                    Base64.getEncoder().encodeToString(bytes), imageWidth, imageHeight, name));
        } catch (Exception exception) {
            fail("Could not read that PNG: " + readable(exception));
        }
    }

    private void fail(String message) {
        status = message;
        statusColor = 0xFFD46060;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int panelWidth = Math.min(500, width - 24);
        int left = (width - panelWidth) / 2;
        int top = Math.max(12, (height - 240) / 2);
        graphics.fill(left, top, left + panelWidth, top + 225, 0xFF111317);
        graphics.fill(left, top, left + panelWidth, top + 34, 0xFF24282E);
        graphics.fill(left, top + 33, left + panelWidth, top + 34, 0xFFC59A2A);
        graphics.drawString(font, ScpFonts.montserrat("IMPORT CODEX IMAGE"),
                left + 18, top + 12, 0xFFF7F8FC, false);
        graphics.fill(left + 20, top + 52, left + panelWidth - 20, top + 164, 0xFF081022);
        graphics.fill(left + 20, top + 52, left + panelWidth - 20, top + 53, 0xFF46536C);
        graphics.fill(left + 20, top + 163, left + panelWidth - 20, top + 164, 0xFF46536C);
        Component drop = ScpFonts.roboto("DROP PNG HERE");
        graphics.drawString(font, drop,
                left + (panelWidth - font.width(drop)) / 2, top + 88, 0xFFE5D49A, false);
        Component limit = ScpFonts.roboto("Maximum 900 KB · Maximum 4096 × 4096");
        graphics.drawString(font, limit,
                left + (panelWidth - font.width(limit)) / 2, top + 108, 0xFFA9AFBA, false);
        Component statusText = ScpFonts.roboto(status);
        graphics.drawString(font, statusText,
                left + (panelWidth - font.width(statusText)) / 2, top + 142,
                statusColor, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String readable(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getSimpleName() : message;
    }

    public record ImportedImage(String base64, int width, int height, String fileName) {
    }
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
    private static final int MAX_LINE_LENGTH = 4_096;
    private final Screen parent;
    private final Consumer<String> callback;
    private final List<String> lines = new ArrayList<>();
    private final Map<EditBox, Integer> lineBoxes = new LinkedHashMap<>();
    private int scroll;
    private int focusLine = -1;
    private String notice = "Enter creates a new line. You may also drop one UTF-8 .txt file.";
    private int noticeColor = 0xFFA9AFBA;

    public CodexTextEditorScreen(Screen parent, String initial, Consumer<String> callback) {
        super(ScpFonts.roboto("Write Codex Text"));
        this.parent = parent;
        this.callback = callback;
        setText(initial == null ? "" : initial);
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
    protected void init() {
        buildWidgets();
    }

    private void buildWidgets() {
        lineBoxes.clear();
        int panelWidth = Math.min(720, width - 20);
        int panelHeight = Math.min(460, height - 16);
        int left = (width - panelWidth) / 2;
        int top = Math.max(8, (height - panelHeight) / 2);
        int editorTop = top + 58;
        int bottom = top + panelHeight - 44;
        int visible = Math.max(5, (bottom - editorTop) / 22);
        scroll = Math.max(0, Math.min(Math.max(0, lines.size() - visible), scroll));

        for (int row = 0; row < visible; row++) {
            int logical = scroll + row;
            if (logical >= lines.size()) break;
            EditBox box = new EditBox(font, left + 50, editorTop + row * 22,
                    panelWidth - 70, 20, Component.literal("Line " + (logical + 1)));
            box.setMaxLength(MAX_LINE_LENGTH);
            box.setValue(lines.get(logical));
            box.setFormatter((value, cursor) -> ScpFonts.roboto(value).getVisualOrderText());
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
        addRenderableWidget(Button.builder(ScpFonts.roboto("Save Text"), b -> save())
                .bounds(left + 26 + third, top + panelHeight - 30, third, 22).build());
        addRenderableWidget(Button.builder(ScpFonts.roboto("Cancel"),
                b -> Minecraft.getInstance().setScreen(parent))
                .bounds(left + 36 + third * 2, top + panelHeight - 30, third, 22).build());
    }

    private void rebuild() {
        clearWidgets();
        buildWidgets();
    }

    private void syncVisible() {
        for (Map.Entry<EditBox, Integer> entry : lineBoxes.entrySet()) {
            lines.set(entry.getValue(), entry.getKey().getValue());
        }
    }

    private void save() {
        syncVisible();
        String text = String.join("\n", lines);
        if (text.length() > MAX_TEXT_LENGTH) {
            notice = "Text is too long. Maximum: 65,536 characters.";
            noticeColor = 0xFFD46060;
            return;
        }
        if (callback != null) callback.accept(text);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (Map.Entry<EditBox, Integer> entry : lineBoxes.entrySet()) {
            if (!entry.getKey().isFocused()) continue;
            int index = entry.getValue();
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                syncVisible();
                lines.add(index + 1, "");
                focusLine = index + 1;
                ensureVisible(focusLine);
                rebuild();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP && index > 0) {
                syncVisible();
                focusLine = index - 1;
                ensureVisible(focusLine);
                rebuild();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN && index + 1 < lines.size()) {
                syncVisible();
                focusLine = index + 1;
                ensureVisible(focusLine);
                rebuild();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void ensureVisible(int line) {
        int panelHeight = Math.min(460, height - 16);
        int visible = Math.max(5, (panelHeight - 102) / 22);
        if (line < scroll) scroll = line;
        if (line >= scroll + visible) scroll = line - visible + 1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        syncVisible();
        int next = Math.max(0, Math.min(Math.max(0, lines.size() - 1),
                scroll + (delta < 0 ? 1 : -1)));
        if (next != scroll) {
            scroll = next;
            focusLine = -1;
            rebuild();
            return true;
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
            if (bytes.length > MAX_TEXT_LENGTH * 4) {
                notice = "Text file is too large.";
                noticeColor = 0xFFD46060;
                return;
            }
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (text.length() > MAX_TEXT_LENGTH) {
                notice = "Text exceeds 65,536 characters.";
                noticeColor = 0xFFD46060;
                return;
            }
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
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
            graphics.drawString(font, ScpFonts.roboto(Integer.toString(entry.getValue() + 1)),
                    left + 18, entry.getKey().getY() + 6, 0xFF6F7888, false);
        }
        graphics.drawString(font, ScpFonts.roboto(
                        lines.size() + " line(s) · " + String.join("\n", lines).length()
                                + "/" + MAX_TEXT_LENGTH + " characters"),
                left + 16, top + panelHeight - 43, 0xFFA9AFBA, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
''')


# ---------------------------------------------------------------------------
# Documentation
# ---------------------------------------------------------------------------
replace_once(
    "README.md",
    '''New Codex entries created through the configuration center currently start with `minecraft:paper` as a temporary default until dedicated document items are introduced in a future update; this is only an initial value and can be changed to any registered item.''',
    '''New Codex entries created through the configuration center currently start with `minecraft:paper` as a temporary default until dedicated document items are introduced in a future update; this is only an initial value and can be changed to any registered item. The editor can also embed a dropped PNG and directly written UTF-8 text in the JSON definition, so ordinary users do not need to create resource-pack paths for individual documents.''')
replace_once(
    "CHANGELOG.md",
    '''- New Codex entries currently use `minecraft:paper` as a temporary default item until dedicated document items are implemented in a future update. This default can be changed to any registered item before saving.''',
    '''- New Codex entries currently use `minecraft:paper` as a temporary default item until dedicated document items are implemented in a future update. This default can be changed to any registered item before saving;
- Added direct Codex text editing and PNG drag-and-drop import. Embedded content is stored in the JSON definition, while packaged resource locations remain supported.''')
replace_once(
    "CHANGELOG.md",
    '''- Reduced the bundled SCP-914 1:1 skin pool to five selected defaults and renumbered them consistently as `skin1.png` through `skin5.png`.''',
    '''- Reduced the bundled SCP-914 1:1 skin pool to five selected defaults and renumbered them consistently as `skin1.png` through `skin5.png`;
- SCP-914 recipe summaries now reuse SCP-294's configured cup colors and render cached, non-ticking miniature previews for visible living-entity inputs and outputs, with a safe fallback icon for unsupported entity types.''')
replace_once(
    "CHANGELOG.md",
    '''- Fixed block interaction anchors edited with `K` not being persisted or reloaded reliably;''',
    '''- Fixed block interaction anchors edited with `K` not being persisted or reloaded reliably;
- Fixed the target identifier in the `K` interaction editor overlapping the Action label and field;''')
replace_once(
    "docs/CONFIGURATION_CENTER.md",
    '''Codex entries may use a packaged image resource, a UTF-8 text resource, or both. Advanced NBT conditions and image dimensions are preserved when an entry is edited.''',
    '''Codex entries may use a packaged image resource, a UTF-8 text resource, or both. The editor also provides **Import PNG**, which opens a drag-and-drop area and embeds the image directly in the JSON, and **Write Text**, which stores directly written UTF-8 text in the same definition. Embedded content is size-limited and server-authoritative; packaged resources remain available for modpacks that prefer resource packs. Advanced NBT conditions and image dimensions are preserved when an entry is edited.''')

print("Final configuration polish applied.")
