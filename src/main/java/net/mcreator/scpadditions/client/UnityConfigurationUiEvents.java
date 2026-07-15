package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.bl4ues.scpinventory.client.gui.ItemConfigScreen;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared SCP Unity presentation for every native configuration screen.
 * Existing controls retain their server-authoritative behavior while this
 * layer standardizes typography, visual hierarchy, previews and explanations.
 */
@Mod.EventBusSubscriber(modid = "scp_additions", value = Dist.CLIENT)
public final class UnityConfigurationUiEvents {
    private static final int NAVY = 0xF000071F;
    private static final int NAVY_HOVER = 0xF0141E42;
    private static final int NAVY_DISABLED = 0xE01B1E26;
    private static final int GOLD = 0xFFE1A704;
    private static final int GOLD_SOFT = 0xFFB78B13;
    private static final int WHITE = 0xFFF7F8FC;
    private static final int MUTED = 0xFF9CA3AF;
    private static final int DANGER = 0xFFD46060;
    private static final List<String> ITEM_TYPES = List.of(
            "MISCELLANEOUS", "CONSUMABLE", "USABLE", "PLACEABLE", "HARMFUL",
            "KEY", "COIN", "AMMO", "HEAD", "CHEST", "LEGS", "FEET",
            "ACCESSORY", "ACCESSORYHAND", "WEAPON", "CODEX");
    private static final Map<String, String> TOOLTIPS = buildTooltips();

    private UnityConfigurationUiEvents() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!isConfigurationScreen(screen)) return;

        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof AbstractButton button) {
                button.setMessage(ScpFonts.roboto(button.getMessage()));
            } else if (listener instanceof EditBox editBox) {
                editBox.setFormatter((value, cursor) -> ScpFonts.roboto(value).getVisualOrderText());
                editBox.setTextColor(WHITE);
                editBox.setTextColorUneditable(MUTED);
            }
        }

        String name = screen.getClass().getSimpleName();
        if ("DrinkDetailScreen".equals(name)) installDrinkColorPicker(event, screen);
        if ("ItemRuleDetailScreen".equals(name)) installPlaceableCategory(event, screen);
        if ("CodexListScreen".equals(name)) installPaperDocumentDefault(event, screen);
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!isConfigurationScreen(screen)) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();
        Font font = Minecraft.getInstance().font;
        Component hoveredTooltip = null;

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 420.0F);
        for (GuiEventListener listener : screen.children()) {
            if (!(listener instanceof AbstractButton button) || !button.visible) continue;

            boolean hovered = contains(button, mouseX, mouseY);
            String plain = button.getMessage().getString();
            boolean danger = isDanger(plain);
            boolean primary = isPrimary(plain);

            int background = !button.active ? NAVY_DISABLED : hovered ? NAVY_HOVER : NAVY;
            int border = danger ? DANGER : hovered || primary ? GOLD : GOLD_SOFT;
            int text = !button.active ? MUTED : primary && !danger ? GOLD : WHITE;

            int x = button.getX();
            int y = button.getY();
            int right = x + button.getWidth();
            int bottom = y + button.getHeight();

            graphics.fill(x, y, right, bottom, background);
            graphics.fill(x, y, right, y + 1, border);
            graphics.fill(x, bottom - 1, right, bottom, border);
            graphics.fill(x, y, x + 1, bottom, border);
            graphics.fill(right - 1, y, right, bottom, border);
            graphics.fill(x + 1, y + 1, x + 4, bottom - 1, border);

            Component message = ScpFonts.roboto(button.getMessage());
            int textX = x + Math.max(5, (button.getWidth() - font.width(message)) / 2);
            int textY = y + Math.max(1, (button.getHeight() - 8) / 2);
            graphics.drawString(font, message, textX, textY, text, false);

            if (hovered) {
                String tooltip = tooltipFor(plain);
                if (!tooltip.isBlank()) hoveredTooltip = ScpFonts.roboto(tooltip);
            }
        }
        graphics.pose().popPose();

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 430.0F);
        String name = screen.getClass().getSimpleName();
        if ("DrinkListScreen".equals(name)) renderDrinkRows(graphics, screen);
        if ("ItemRulesScreen".equals(name)) renderItemRuleRows(graphics, screen);
        if ("RecipeListScreen".equals(name)) renderRecipeRows(graphics, screen);
        graphics.pose().popPose();

        if (hoveredTooltip != null) {
            graphics.renderTooltip(font, hoveredTooltip, mouseX, mouseY);
        }
    }

    private static void installDrinkColorPicker(ScreenEvent.Init.Post event, Screen screen) {
        EditBox colorBox = readField(screen, "colorBox", EditBox.class);
        if (colorBox == null || colorBox.getWidth() < 120) return;
        int originalWidth = colorBox.getWidth();
        colorBox.setWidth(Math.max(76, originalWidth - 96));
        Button picker = Button.builder(ScpFonts.roboto("Pick Color"), button -> {
            invokeNoArgs(screen, "sync");
            JsonObject edit = readField(screen, "edit", JsonObject.class);
            String initial = edit == null ? colorBox.getValue() : string(edit, "cup_color", colorBox.getValue());
            Minecraft.getInstance().setScreen(new UnityColorPickerScreen(screen, initial, selected -> {
                if (edit != null) edit.addProperty("cup_color", selected);
                colorBox.setValue(selected);
            }));
        }).bounds(colorBox.getX() + colorBox.getWidth() + 6, colorBox.getY(), 90, 20).build();
        event.addListener(picker);
    }

    private static void installPlaceableCategory(ScreenEvent.Init.Post event, Screen screen) {
        Button old = readField(screen, "typeButton", Button.class);
        if (old == null) return;
        event.removeListener(old);
        Button replacement = Button.builder(ScpFonts.roboto(old.getMessage()), button -> {
            String current = readField(screen, "type", String.class);
            int index = ITEM_TYPES.indexOf(current == null ? "MISCELLANEOUS" : current.toUpperCase(Locale.ROOT));
            String next = ITEM_TYPES.get((Math.max(0, index) + 1) % ITEM_TYPES.size());
            writeField(screen, "type", next);
            button.setMessage(ScpFonts.roboto("Category: " + next));
        }).bounds(old.getX(), old.getY(), old.getWidth(), old.getHeight()).build();
        writeField(screen, "typeButton", replacement);
        event.addListener(replacement);
    }

    private static void installPaperDocumentDefault(ScreenEvent.Init.Post event, Screen screen) {
        List<GuiEventListener> snapshot = new ArrayList<>(event.getListenersList());
        for (GuiEventListener listener : snapshot) {
            if (!(listener instanceof Button button)
                    || !"+ Add Document".equals(button.getMessage().getString())) continue;
            event.removeListener(button);
            Button replacement = Button.builder(ScpFonts.roboto("+ Paper Document"), ignored -> addPaperDocument(screen))
                    .bounds(button.getX(), button.getY(), button.getWidth(), button.getHeight()).build();
            event.addListener(replacement);
        }
    }

    private static void addPaperDocument(Screen parent) {
        JsonObject root = readField(parent, "root", JsonObject.class);
        if (root == null) return;
        JsonArray documents = array(root, "codex_documents");
        JsonObject document = new JsonObject();
        document.addProperty("id", "minecraft:paper");
        document.addProperty("category", "Documents");
        document.addProperty("name", "New Document");
        document.addProperty("image", "");
        document.addProperty("text", "");
        documents.add(document);
        Screen detail = constructNestedScreen(
                "net.mcreator.scpadditions.config.ui.ConfigCenterClient$CodexDetailScreen",
                parent, document);
        if (detail != null) Minecraft.getInstance().setScreen(detail);
    }

    private static void renderDrinkRows(GuiGraphics graphics, Screen screen) {
        List<JsonObject> filtered = readJsonObjectList(screen, "filtered");
        Integer scrollValue = readField(screen, "scroll", Integer.class);
        int scroll = scrollValue == null ? 0 : scrollValue;
        int w = Math.min(700, screen.width - 16);
        int x = (screen.width - w) / 2 + 12;
        int top = Math.max(8, (screen.height - Math.min(410, screen.height - 16)) / 2) + 38;
        int listY = top + 30;
        int visible = Math.max(5, Math.min(11, (screen.height - 132) / 24));
        Font font = Minecraft.getInstance().font;

        for (int i = scroll; i < Math.min(filtered.size(), scroll + visible); i++) {
            JsonObject drink = filtered.get(i);
            int rowY = listY + (i - scroll) * 24;
            int rowRight = x + w - 150;
            drawSummaryCard(graphics, x, rowY, rowRight, rowY + 20);

            ItemStack cup = coloredCup(string(drink, "cup_color", "#FFFFFF"));
            graphics.renderItem(cup, x + 7, rowY + 2);
            String id = string(drink, "id", "unknown");
            String alias = firstAlias(drink);
            String label = id + (alias.isBlank() ? "" : " — “" + alias + "”")
                    + (bool(drink, "enabled", true) ? "" : " [disabled]");
            graphics.enableScissor(x + 26, rowY, rowRight - 5, rowY + 20);
            graphics.drawString(font, ScpFonts.roboto(label), x + 29, rowY + 6,
                    bool(drink, "enabled", true) ? WHITE : MUTED, false);
            graphics.disableScissor();
        }
    }

    private static void renderItemRuleRows(GuiGraphics graphics, Screen screen) {
        List<JsonObject> filtered = readJsonObjectList(screen, "filtered");
        Integer scrollValue = readField(screen, "scroll", Integer.class);
        int scroll = scrollValue == null ? 0 : scrollValue;
        int w = Math.min(650, screen.width - 16);
        int x = (screen.width - w) / 2 + 12;
        int top = Math.max(8, (screen.height - Math.min(400, screen.height - 16)) / 2) + 38;
        int listY = top + 30;
        int visible = Math.max(4, Math.min(10, (screen.height - 128) / 24));
        Font font = Minecraft.getInstance().font;

        for (int i = scroll; i < Math.min(filtered.size(), scroll + visible); i++) {
            JsonObject rule = filtered.get(i);
            int rowY = listY + (i - scroll) * 24;
            int rowRight = x + w - 78;
            drawSummaryCard(graphics, x, rowY, rowRight, rowY + 20);
            String id = string(rule, "id", "unknown");
            ItemStack stack = itemStack(id);
            if (!stack.isEmpty()) graphics.renderItem(stack, x + 7, rowY + 2);
            String label = id + "  [" + string(rule, "type", "MISCELLANEOUS") + "]";
            graphics.enableScissor(x + 26, rowY, rowRight - 5, rowY + 20);
            graphics.drawString(font, ScpFonts.roboto(label), x + 29, rowY + 6, WHITE, false);
            graphics.disableScissor();
        }
    }

    private static void renderRecipeRows(GuiGraphics graphics, Screen screen) {
        List<?> filtered = readList(screen, "filtered");
        Integer scrollValue = readField(screen, "scroll", Integer.class);
        int scroll = scrollValue == null ? 0 : scrollValue;
        int w = Math.min(760, screen.width - 12);
        int x = (screen.width - w) / 2 + 12;
        int top = Math.max(6, (screen.height - Math.min(440, screen.height - 12)) / 2) + 38;
        int listY = top + 30;
        int visible = Math.max(5, Math.min(12, (screen.height - 130) / 24));
        int rowRight = x + w - 154;
        Font font = Minecraft.getInstance().font;

        for (int i = scroll; i < Math.min(filtered.size(), scroll + visible); i++) {
            Object ref = filtered.get(i);
            JsonObject recipe = readField(ref, "recipe", JsonObject.class);
            String source = readField(ref, "source", String.class);
            if (recipe == null) continue;
            int rowY = listY + (i - scroll) * 24;
            drawSummaryCard(graphics, x, rowY, rowRight, rowY + 20);

            ItemStack input = firstRecipeItem(recipe, "item_inputs");
            ItemStack output = firstRecipeItem(recipe, "item_outputs");
            if (output.isEmpty()) output = firstRecipeItem(recipe, "weighted_item_outputs");
            if (!input.isEmpty()) graphics.renderItem(input, x + 7, rowY + 2);
            graphics.drawString(font, ScpFonts.roboto("→"), x + 26, rowY + 6, GOLD, false);
            if (!output.isEmpty()) graphics.renderItem(output, x + 38, rowY + 2);

            String setting = string(recipe, "setting", "?");
            String sourceName = source == null ? "?" : source.startsWith("914/") ? source.substring(4) : source;
            String id = string(recipe, "id", "unknown");
            int badgeWidth = Math.min(112, Math.max(62, font.width(ScpFonts.roboto(setting)) + 14));
            int badgeX = rowRight - badgeWidth - 4;
            graphics.fill(badgeX, rowY + 3, rowRight - 4, rowY + 17, 0xD0141E42);
            graphics.fill(badgeX, rowY + 3, badgeX + 2, rowY + 17, GOLD);
            graphics.drawString(font, ScpFonts.roboto(setting), badgeX + 7, rowY + 6, GOLD, false);

            graphics.enableScissor(x + 58, rowY, badgeX - 5, rowY + 20);
            graphics.drawString(font, ScpFonts.roboto(id + "  ‹" + sourceName + "›"), x + 61, rowY + 6, WHITE, false);
            graphics.disableScissor();
        }
    }

    private static void drawSummaryCard(GuiGraphics graphics, int left, int top, int right, int bottom) {
        graphics.fill(left, top, right, bottom, NAVY);
        graphics.fill(left, top, left + 4, bottom, GOLD_SOFT);
        graphics.fill(left, top, right, top + 1, GOLD_SOFT);
        graphics.fill(left, bottom - 1, right, bottom, 0xAA6A5210);
    }

    private static ItemStack firstRecipeItem(JsonObject recipe, String key) {
        if (recipe == null || !recipe.has(key) || !recipe.get(key).isJsonArray()) return ItemStack.EMPTY;
        JsonArray values = recipe.getAsJsonArray(key);
        for (JsonElement element : values) {
            if (!element.isJsonObject()) continue;
            String id = string(element.getAsJsonObject(), "item", "");
            ItemStack stack = itemStack(id);
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack itemStack(String id) {
        ResourceLocation resource = ResourceLocation.tryParse(id == null ? "" : id);
        return resource == null ? ItemStack.EMPTY
                : BuiltInRegistries.ITEM.getOptional(resource).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    private static ItemStack coloredCup(String hex) {
        ItemStack stack = new ItemStack(ScpAdditionsModItems.CUP_OF_COFFEE.get());
        CompoundTag drink = new CompoundTag();
        drink.putInt("cup_color", parseColor(hex, 0xFFFFFF));
        stack.getOrCreateTag().put("Scp294Drink", drink);
        return stack;
    }

    private static boolean isConfigurationScreen(Screen screen) {
        if (screen == null) return false;
        if (screen instanceof ItemConfigScreen || screen instanceof UnityColorPickerScreen) return true;
        return screen.getClass().getName().startsWith(
                "net.mcreator.scpadditions.config.ui.ConfigCenterClient$");
    }

    private static boolean contains(AbstractButton button, int mouseX, int mouseY) {
        return mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                && mouseY >= button.getY() && mouseY < button.getY() + button.getHeight();
    }

    private static boolean isDanger(String label) {
        String value = normalize(label);
        return value.equals("x") || value.startsWith("delete") || value.startsWith("remove")
                || value.startsWith("forget") || value.startsWith("confirm");
    }

    private static boolean isPrimary(String label) {
        String value = normalize(label);
        return value.startsWith("save") || value.startsWith("done") || value.startsWith("create")
                || value.startsWith("+ add") || value.startsWith("+ new")
                || value.startsWith("+ paper") || value.startsWith("apply");
    }

    private static String tooltipFor(String label) {
        String normalized = normalize(label);
        for (Map.Entry<String, String> entry : TOOLTIPS.entrySet()) {
            if (normalized.equals(entry.getKey()) || normalized.startsWith(entry.getKey() + ":")
                    || normalized.startsWith(entry.getKey() + " ")) {
                return entry.getValue();
            }
        }
        if (normalized.startsWith("category")) return "Choose how this item is stored and used by SCP Inventory.";
        if (normalized.startsWith("type")) return "Choose the SCP Inventory category assigned to this item.";
        if (normalized.contains("on") || normalized.contains("off")) return "Toggle this setting. Changes are applied only after saving.";
        if (normalized.startsWith("x")) return "Remove this entry from the working configuration.";
        return "Open or modify this configuration entry.";
    }

    private static String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, String> buildTooltips() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("general & modules", "Enable or disable gameplay, HUD, blink, interaction and SCP systems.");
        values.put("inventory, equipment & codex", "Edit item categories, equipment effects, Status filters and Codex documents.");
        values.put("contextual interactions", "Edit the prompts and right-click actions attached to blocks and entities.");
        values.put("scp-294 drinks", "Create, search and edit drinks, aliases, colors, effects and machine actions.");
        values.put("scp-914 recipes", "Create and edit intake, setting, output and advanced SCP-914 transformations.");
        values.put("item categories & equipment effects", "Classify items and assign supported equipment modifiers.");
        values.put("hidden status effects", "Choose effects that should not appear in the custom Conditions panel.");
        values.put("scp-173 entity targets", "Choose entities that participate in SCP-173 observation and targeting rules.");
        values.put("codex documents", "Create document definitions shown in the SCP Inventory Codex.");
        values.put("reload snapshot", "Discard unsaved local edits and request the current server configuration again.");
        values.put("save & reload", "Validate, save with a .bak backup and reload the affected system.");
        values.put("save all", "Validate and save every edited section in this configuration group.");
        values.put("save rule", "Save this item category and its equipment effects.");
        values.put("save", "Validate and save the current changes.");
        values.put("defaults", "Restore the default values in this screen before saving.");
        values.put("back", "Return to the previous configuration screen.");
        values.put("cancel", "Discard the unsaved changes on this screen.");
        values.put("done", "Close the configuration center.");
        values.put("forget", "Remove this item's explicit rule and return it to automatic classification.");
        values.put("confirm", "Confirm the destructive removal action.");
        values.put("+ add item", "Search the loaded item registry and create a new item rule.");
        values.put("+ new drink", "Create a new SCP-294 drink definition.");
        values.put("+ recipe", "Create a new SCP-914 recipe in the in-game editor fragment.");
        values.put("+ paper document", "Create a Codex definition using minecraft:paper as its temporary item.");
        values.put("pick color", "Choose the cup color with synchronized RGB sliders and hexadecimal input.");
        values.put("duplicate", "Create an independent copy of this entry for faster editing.");
        values.put("additional recipe settings", "Show less common chance, NBT, action-bar and weighted-output options.");
        return Map.copyOf(values);
    }

    private static Screen constructNestedScreen(String className, Screen parent, JsonObject object) {
        try {
            Class<?> type = Class.forName(className);
            Constructor<?> constructor = type.getDeclaredConstructor(Screen.class, JsonObject.class);
            constructor.setAccessible(true);
            return (Screen) constructor.newInstance(parent, object);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void invokeNoArgs(Object target, String name) {
        if (target == null) return;
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
        Object value = readField(target, name);
        return expected.isInstance(value) ? expected.cast(value) : null;
    }

    private static Object readField(Object target, String name) {
        if (target == null) return null;
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    private static void writeField(Object target, String name, Object value) {
        if (target == null) return;
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
    }

    private static List<JsonObject> readJsonObjectList(Object target, String name) {
        List<?> list = readList(target, name);
        List<JsonObject> values = new ArrayList<>();
        for (Object value : list) if (value instanceof JsonObject object) values.add(object);
        return values;
    }

    private static List<?> readList(Object target, String name) {
        Object raw = readField(target, name);
        return raw instanceof List<?> list ? list : List.of();
    }

    private static JsonArray array(JsonObject root, String key) {
        if (!root.has(key) || !root.get(key).isJsonArray()) root.add(key, new JsonArray());
        return root.getAsJsonArray(key);
    }

    private static String string(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return fallback;
        try { return object.get(key).getAsString(); }
        catch (Exception ignored) { return fallback; }
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return fallback;
        try { return object.get(key).getAsBoolean(); }
        catch (Exception ignored) { return fallback; }
    }

    private static String firstAlias(JsonObject drink) {
        if (drink == null || !drink.has("aliases") || !drink.get("aliases").isJsonArray()) return "";
        JsonArray aliases = drink.getAsJsonArray("aliases");
        if (aliases.isEmpty()) return "";
        JsonElement first = aliases.get(0);
        return first.isJsonPrimitive() ? first.getAsString() : "";
    }

    private static int parseColor(String value, int fallback) {
        if (value == null) return fallback;
        String clean = value.trim();
        if (clean.startsWith("#")) clean = clean.substring(1);
        try { return clean.matches("[0-9A-Fa-f]{6}") ? Integer.parseInt(clean, 16) : fallback; }
        catch (NumberFormatException ignored) { return fallback; }
    }
}
