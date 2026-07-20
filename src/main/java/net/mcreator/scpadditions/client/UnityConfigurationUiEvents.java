package net.mcreator.scpadditions.client;

import net.neoforged.fml.common.EventBusSubscriber;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.bl4ues.scpinventory.client.gui.ContextConfigScreen;
import com.bl4ues.scpinventory.client.gui.ItemConfigScreen;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/** Native SCP Unity presentation shared by every SCP Additions configuration screen. */
@EventBusSubscriber(modid = "scp_additions", value = Dist.CLIENT)
public final class UnityConfigurationUiEvents {
    private static final int PANEL = 0xFF111317;
    private static final int HEADER = 0xFF24282E;
    private static final int NAVY = 0xFF081022;
    private static final int NAVY_HOVER = 0xFF131E36;
    private static final int NAVY_DISABLED = 0xFF1B1E26;
    private static final int BORDER = 0xFF46536C;
    private static final int BORDER_HOVER = 0xFF73809A;
    private static final int ACCENT = 0xFFC59A2A;
    private static final int ACCENT_SOFT = 0xFF8D711F;
    private static final int PALE_GOLD = 0xFFE5D49A;
    private static final int WHITE = 0xFFF7F8FC;
    private static final int MUTED = 0xFF9CA3AF;
    private static final int DANGER = 0xFFD46060;
    private static final int BLOCK_BADGE = 0xFF22384A;
    private static final int ENTITY_BADGE = 0xFF3D2E4B;
    private static final ResourceLocation CONFIG_LOGO =
            ResourceLocation.fromNamespaceAndPath("scp_additions", "textures/screens/logo.png");
    private static final List<String> ITEM_TYPES = List.of(
            "MISCELLANEOUS", "CONSUMABLE", "USABLE", "PLACEABLE", "HARMFUL",
            "KEY", "COIN", "AMMO", "HEAD", "CHEST", "LEGS", "FEET",
            "ACCESSORY", "ACCESSORYHAND", "WEAPON");
    private static final List<String> MODULE_DESCRIPTIONS = List.of(
            "Enables the custom survival-horror inventory.",
            "Remembers the selected panel, document and scroll positions until leaving the world.",
            "Enables SCP Unity-style interaction prompts.",
            "Disables custom prompts for Creative players.",
            "Shows the SCP Additions health, stamina and blink presentation.",
            "Enables custom health behavior.",
            "Enables stamina drain and regeneration.",
            "Uses slower walking and committed sprinting.",
            "Enables automatic and manual blinking.",
            "Enables SCP-173 behavior.",
            "Allows the configurable natural spawn system.");
    private static final Map<String, String> TOOLTIPS = buildTooltips();
    private static final Map<AbstractButton, Component> BUTTON_LABELS = new WeakHashMap<>();
    private static final Map<ResourceLocation, LivingEntity> ENTITY_PREVIEWS = new HashMap<>();
    private static ClientLevel entityPreviewLevel;

    private UnityConfigurationUiEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!isConfigurationScreen(screen)) return;

        for (GuiEventListener listener : event.getListenersList()) prepareWidget(listener);

        String name = screen.getClass().getSimpleName();
        if ("DrinkDetailScreen".equals(name)) installDrinkColorPicker(event, screen);
        if ("ItemRuleDetailScreen".equals(name)) installPlaceableCategory(event, screen);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        Screen screen = event.getScreen();
        if (!isConfigurationScreen(screen)) return;
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof EditBox editBox) styleEditBox(editBox);
            if (listener instanceof AbstractButton button) {
                Component current = button.getMessage();
                if (current != null && !current.getString().isBlank()) {
                    BUTTON_LABELS.put(button, ScpFonts.roboto(current));
                }
                button.setMessage(Component.empty());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!isConfigurationScreen(screen)) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();
        Font font = Minecraft.getInstance().font;
        Component hoveredTooltip = null;

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 420.0F);
        renderKnownHeader(graphics, screen);
        renderKnownBodyText(graphics, screen);
        graphics.pose().popPose();

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 440.0F);
        for (GuiEventListener listener : screen.children()) {
            if (!(listener instanceof AbstractButton button) || !button.visible) continue;
            Component label = labelFor(button);
            drawButton(graphics, font, button, label, mouseX, mouseY);
            if (contains(button, mouseX, mouseY)) {
                String tooltip = tooltipFor(label.getString());
                if (!tooltip.isBlank()) hoveredTooltip = ScpFonts.roboto(tooltip);
            }
        }
        graphics.pose().popPose();

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 460.0F);
        String name = screen.getClass().getSimpleName();
        if ("DrinkListScreen".equals(name)) renderDrinkRows(graphics, screen, mouseX, mouseY);
        if ("ItemRulesScreen".equals(name)) renderItemRuleRows(graphics, screen, mouseX, mouseY);
        if ("RecipeListScreen".equals(name)) renderRecipeRows(graphics, screen, mouseX, mouseY);
        if ("ContextListScreen".equals(name)) renderContextRows(graphics, screen, mouseX, mouseY);
        if ("IdListScreen".equals(name)) renderIdRows(graphics, screen, mouseX, mouseY);
        if ("DrinkEffectsScreen".equals(name)) renderDrinkEffectRows(graphics, screen, mouseX, mouseY);
        graphics.pose().popPose();

        if (hoveredTooltip != null) {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 1000.0F);
            graphics.renderTooltip(font, hoveredTooltip, mouseX, mouseY);
            graphics.pose().popPose();
        }

        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof AbstractButton button) {
                Component label = BUTTON_LABELS.get(button);
                if (label != null) button.setMessage(label);
            }
        }
    }

    private static void prepareWidget(GuiEventListener listener) {
        if (listener instanceof AbstractSliderButton slider) {
            slider.setMessage(ScpFonts.roboto(slider.getMessage()));
        } else if (listener instanceof AbstractButton button) {
            Component label = ScpFonts.roboto(button.getMessage());
            BUTTON_LABELS.put(button, label);
            button.setMessage(Component.empty());
        } else if (listener instanceof EditBox editBox) {
            styleEditBox(editBox);
        }
    }

    private static void styleEditBox(EditBox editBox) {
        editBox.setFormatter((value, cursor) -> ScpFonts.roboto(value).getVisualOrderText());
        editBox.setTextColor(WHITE);
        editBox.setTextColorUneditable(MUTED);
        Component hint = readField(editBox, "hint", Component.class);
        if (hint != null) writeField(editBox, "hint", ScpFonts.roboto(hint));
    }

    private static void drawButton(GuiGraphics graphics, Font font, AbstractButton button,
                                   Component label, int mouseX, int mouseY) {
        boolean hovered = contains(button, mouseX, mouseY);
        String plain = label.getString();
        boolean danger = isDanger(plain);
        boolean primary = isPrimary(plain);

        int background = !button.active ? NAVY_DISABLED : hovered ? NAVY_HOVER : NAVY;
        int border = danger ? DANGER : hovered ? BORDER_HOVER : BORDER;
        int stripe = danger ? DANGER : primary ? ACCENT : hovered ? ACCENT_SOFT : BORDER;
        int text = !button.active ? MUTED : primary && !danger ? PALE_GOLD : WHITE;

        int x = button.getX();
        int y = button.getY();
        int right = x + button.getWidth();
        int bottom = y + button.getHeight();
        graphics.fill(x, y, right, bottom, background);
        graphics.fill(x, y, right, y + 1, border);
        graphics.fill(x, bottom - 1, right, bottom, border);
        graphics.fill(x, y, x + 1, bottom, border);
        graphics.fill(right - 1, y, right, bottom, border);
        graphics.fill(x + 1, y + 1, x + (primary || danger || hovered ? 4 : 2), bottom - 1, stripe);

        int textX = x + Math.max(5, (button.getWidth() - font.width(label)) / 2);
        int textY = y + Math.max(1, (button.getHeight() - 8) / 2);
        graphics.drawString(font, label, textX, textY, text, false);
    }

    private static Component labelFor(AbstractButton button) {
        Component saved = BUTTON_LABELS.get(button);
        if (saved != null) return saved;
        Component current = button.getMessage();
        return current == null ? Component.empty() : ScpFonts.roboto(current);
    }

    private static void installDrinkColorPicker(ScreenEvent.Init.Post event, Screen screen) {
        EditBox colorBox = readField(screen, "colorBox", EditBox.class);
        if (colorBox == null || colorBox.getWidth() < 120) return;
        int originalWidth = colorBox.getWidth();
        colorBox.setWidth(Math.max(76, originalWidth - 96));
        Button picker = Button.builder(ScpFonts.roboto("Pick Color"), button -> {
            invokeNoArgs(screen, "sync");
            JsonObject edit = readField(screen, "edit", JsonObject.class);
            String initial = edit == null ? colorBox.getValue()
                    : string(edit, "cup_color", colorBox.getValue());
            Minecraft.getInstance().setScreen(new UnityColorPickerScreen(screen, initial, selected -> {
                if (edit != null) edit.addProperty("cup_color", selected);
                colorBox.setValue(selected);
            }));
        }).bounds(colorBox.getX() + colorBox.getWidth() + 6, colorBox.getY(), 90, 20).build();
        prepareWidget(picker);
        event.addListener(picker);
    }

    private static void installPlaceableCategory(ScreenEvent.Init.Post event, Screen screen) {
        Button old = readField(screen, "typeButton", Button.class);
        if (old == null) return;
        Component oldLabel = BUTTON_LABELS.getOrDefault(old,
                ScpFonts.roboto("Category: MISCELLANEOUS"));
        event.removeListener(old);
        BUTTON_LABELS.remove(old);
        Button replacement = Button.builder(oldLabel, button -> {
            String current = readField(screen, "type", String.class);
            int index = ITEM_TYPES.indexOf(current == null ? "MISCELLANEOUS"
                    : current.toUpperCase(Locale.ROOT));
            String next = ITEM_TYPES.get((Math.max(0, index) + 1) % ITEM_TYPES.size());
            writeField(screen, "type", next);
            Component label = ScpFonts.roboto("Category: " + formatCategory(next));
            BUTTON_LABELS.put(button, label);
            button.setMessage(label);
        }).bounds(old.getX(), old.getY(), old.getWidth(), old.getHeight()).build();
        writeField(screen, "typeButton", replacement);
        prepareWidget(replacement);
        event.addListener(replacement);
    }

    private static void installPaperDocumentDefault(ScreenEvent.Init.Post event, Screen screen) {
        List<GuiEventListener> snapshot = new ArrayList<>(event.getListenersList());
        for (GuiEventListener listener : snapshot) {
            if (!(listener instanceof Button button)) continue;
            String label = BUTTON_LABELS.getOrDefault(button, button.getMessage()).getString();
            if (!"+ Add Document".equals(label)) continue;
            event.removeListener(button);
            BUTTON_LABELS.remove(button);
            Button replacement = Button.builder(ScpFonts.roboto("+ Paper Document"),
                    ignored -> addPaperDocument(screen))
                    .bounds(button.getX(), button.getY(), button.getWidth(), button.getHeight())
                    .build();
            prepareWidget(replacement);
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

    private static void renderKnownHeader(GuiGraphics graphics, Screen screen) {
        PanelSpec spec = panelSpec(screen);
        if (spec == null) return;
        Font font = Minecraft.getInstance().font;
        String name = screen.getClass().getSimpleName();
        if (screen instanceof ContextConfigScreen) {
            graphics.fill(spec.x(), spec.y(), spec.x() + spec.width(), spec.y() + 34, HEADER);
            graphics.drawString(font, ScpFonts.roboto(screen.getTitle()),
                    spec.x() + 12, spec.y() + 7, WHITE, false);
            return;
        }
        if ("HomeScreen".equals(name)) {
            graphics.fill(spec.x(), spec.y(), spec.x() + spec.width(), spec.y() + 44, HEADER);
            int titleX = spec.x() + 14;
            if (Minecraft.getInstance().getResourceManager().getResource(CONFIG_LOGO).isPresent()) {
                graphics.blit(CONFIG_LOGO, spec.x() + 10, spec.y() + 7, 30, 26,
                        0.0F, 0.0F, 960, 832, 960, 832);
                titleX = spec.x() + 48;
            }
            graphics.pose().pushPose();
            graphics.pose().translate(titleX, spec.y() + 8, 0.0F);
            graphics.pose().scale(1.08F, 1.08F, 1.0F);
            graphics.drawString(font, ScpFonts.montserrat("SCP Additions Configuration"),
                    0, 0, WHITE, false);
            graphics.pose().popPose();
            graphics.drawString(font, ScpFonts.roboto(
                            "Server-owned editors with validation and automatic .bak backups."),
                    titleX, spec.y() + 27, MUTED, false);
            return;
        }

        graphics.fill(spec.x(), spec.y(), spec.x() + spec.width(), spec.y() + 26, HEADER);
        graphics.drawString(font, ScpFonts.roboto(screen.getTitle()),
                spec.x() + 10, spec.y() + 9, WHITE, false);
    }

    private static void renderKnownBodyText(GuiGraphics graphics, Screen screen) {
        PanelSpec spec = panelSpec(screen);
        if (spec == null) return;
        Font font = Minecraft.getInstance().font;
        String name = screen.getClass().getSimpleName();

        if ("MessageScreen".equals(name)) {
            String message = readField(screen, "message", String.class);
            Boolean passive = readField(screen, "passive", Boolean.class);
            graphics.fill(spec.x() + 8, spec.y() + 27, spec.x() + spec.width() - 8,
                    spec.y() + spec.height() - 8, PANEL);
            int lineY = spec.y() + 42;
            for (net.minecraft.util.FormattedCharSequence line : font.split(
                    ScpFonts.roboto(message == null ? "" : message), spec.width() - 24)) {
                graphics.drawString(font, line, spec.x() + 12, lineY,
                        Boolean.TRUE.equals(passive) ? MUTED : DANGER, false);
                lineY += 11;
            }
            return;
        }

        if ("HomeScreen".equals(name)) {
            String notice = readStaticField(screen.getClass().getDeclaringClass(),
                    "homeNotice", String.class);
            if (notice != null && !notice.isBlank()) {
                coverTextLine(graphics, spec.x() + 10, spec.y() + spec.height() - 44,
                        spec.width() - 20);
                graphics.drawString(font, ScpFonts.roboto(compact(notice, 66)),
                        spec.x() + 14, spec.y() + spec.height() - 40, 0xFF79D58B, false);
            }
            return;
        }

        if ("ModulesScreen".equals(name)) {
            int scroll = integerField(screen, "scroll", 0);
            int visible = Math.max(4, Math.min(8, (screen.height - 118) / 34));
            coverTextLine(graphics, spec.x() + spec.width() - 180, spec.y() + 27, 170);
            graphics.drawString(font, ScpFonts.roboto("Mouse wheel: scroll options"),
                    spec.x() + spec.width() - 160, spec.y() + 31, MUTED, false);
            int startY = spec.y() + 68;
            for (int i = scroll; i < Math.min(MODULE_DESCRIPTIONS.size(), scroll + visible); i++) {
                int lineY = startY + (i - scroll) * 34;
                coverTextLine(graphics, spec.x() + 14, lineY - 3, spec.width() - 28);
                graphics.drawString(font, ScpFonts.roboto(compact(MODULE_DESCRIPTIONS.get(i), 80)),
                        spec.x() + 18, lineY, MUTED, false);
            }
            return;
        }

        if ("InventoryHubScreen".equals(name)) {
            coverTextLine(graphics, spec.x() + 8, spec.y() + 27, spec.width() - 16);
            graphics.drawString(font, ScpFonts.roboto(
                            "Edits remain local until Save All is pressed."),
                    spec.x() + 14, spec.y() + 31, MUTED, false);
            return;
        }

        if ("ItemRulesScreen".equals(name)) {
            int count = readList(screen, "filtered").size();
            drawFooter(graphics, font, spec, count + " matching item rule(s)", MUTED);
            return;
        }

        if ("ItemRuleDetailScreen".equals(name)) {
            String id = readField(screen, "id", String.class);
            coverTextLine(graphics, spec.x() + 10, spec.y() + 29, spec.width() - 20);
            graphics.drawString(font, ScpFonts.roboto(displayNameForItem(id) + "  (" + id + ")"),
                    spec.x() + 14, spec.y() + 34, PALE_GOLD, false);
            coverTextLine(graphics, spec.x() + 10, spec.y() + 45, spec.width() - 20);
            graphics.drawString(font, ScpFonts.roboto(
                            "Category controls where the item is stored."),
                    spec.x() + 14, spec.y() + 50, MUTED, false);
            coverTextLine(graphics, spec.x() + 10, spec.y() + 161, spec.width() - 20);
            graphics.drawString(font, ScpFonts.roboto(
                            "Equipment effects are evaluated while held, worn, or equipped."),
                    spec.x() + 14, spec.y() + 166, MUTED, false);
            return;
        }

        if ("IdListScreen".equals(name)) {
            JsonObject root = readField(screen, "root", JsonObject.class);
            String key = readField(screen, "key", String.class);
            int count = root == null || key == null ? 0 : array(root, key).size();
            drawFooter(graphics, font, spec, count + " configured value(s)", MUTED);
            return;
        }

        if ("CodexListScreen".equals(name)) {
            drawFooter(graphics, font, spec,
                    "Image and text resource paths are preserved exactly as entered.", MUTED);
            return;
        }

        if ("CodexDetailScreen".equals(name)) {
            coverTextLine(graphics, spec.x() + 10, spec.y() + 27, spec.width() - 20);
            graphics.drawString(font, ScpFonts.roboto(
                            "Use either an image, written text, or both for the document."),
                    spec.x() + 14, spec.y() + 31, MUTED, false);
            return;
        }

        if ("ItemPickerScreen".equals(name)) {
            drawFooter(graphics, font, spec,
                    readList(screen, "filtered").size() + " matching item(s)", MUTED);
            return;
        }

        if ("ContextListScreen".equals(name)) {
            drawFooter(graphics, font, spec,
                    "Use K while looking at an object for live visual anchor placement.", MUTED);
            return;
        }

        if ("ContextDetailScreen".equals(name)) {
            coverTextLine(graphics, spec.x() + 10, spec.y() + 27, spec.width() - 20);
            graphics.drawString(font, ScpFonts.roboto(
                            "Use K while looking at the object for live visual anchor positioning."),
                    spec.x() + 14, spec.y() + 30, MUTED, false);
            return;
        }

        if ("DrinkListScreen".equals(name)) {
            drawFooter(graphics, font, spec,
                    "Unknown custom action fields remain preserved when a drink is edited.", MUTED);
            return;
        }

        if ("DrinkDetailScreen".equals(name)) {
            coverTextLine(graphics, spec.x() + 10, spec.y() + 26, spec.width() - 20);
            graphics.drawString(font, ScpFonts.roboto(
                            "Custom dispense and drink action arrays remain preserved."),
                    spec.x() + 14, spec.y() + 29, MUTED, false);
            return;
        }

        if ("RecipeListScreen".equals(name)) {
            int count = readList(screen, "filtered").size();
            Map<?, ?> roots = readField(screen, "roots", Map.class);
            java.util.Set<?> dirty = readField(screen, "dirty", java.util.Set.class);
            int fileCount = roots == null ? 0 : roots.size();
            boolean changed = dirty != null && !dirty.isEmpty();
            drawFooter(graphics, font, spec,
                    count + " matching recipe(s) across " + fileCount + " file(s)"
                            + (changed ? " — unsaved changes" : ""),
                    changed ? 0xFFFFC56D : MUTED);
            return;
        }

        if ("MachineSettingsScreen".equals(name)) {
            coverTextLine(graphics, spec.x() + 10, spec.y() + 28, spec.width() - 20);
            graphics.drawString(font, ScpFonts.roboto(
                            "Offsets are relative to the placed SCP-914 controller orientation."),
                    spec.x() + 14, spec.y() + 33, MUTED, false);
            coverTextLine(graphics, spec.x() + 10, spec.y() + 41, spec.width() - 20);
            graphics.drawString(font, ScpFonts.roboto(
                            "These values affect every recipe and should match the built structure."),
                    spec.x() + 14, spec.y() + 46, 0xFFFFC56D, false);
            return;
        }

        if ("RecipeDetailScreen".equals(name)) {
            coverTextLine(graphics, spec.x() + 8, spec.y() + 25, spec.width() - 16);
            graphics.drawString(font, ScpFonts.roboto(
                            "Choose items by search; use + for any number of intake or output entries."),
                    spec.x() + 12, spec.y() + 28, MUTED, false);
            JsonObject edit = readField(screen, "edit", JsonObject.class);
            Boolean advanced = readField(screen, "advanced", Boolean.class);
            if (edit != null && Boolean.TRUE.equals(advanced)) {
                int entityInputs = jsonArraySize(edit, "entity_inputs");
                int entityOutputs = jsonArraySize(edit, "entity_outputs");
                if (entityInputs + entityOutputs > 0) {
                    coverTextLine(graphics, spec.x() + 10,
                            spec.y() + spec.height() - 47, spec.width() - 20);
                    graphics.drawString(font, ScpFonts.roboto(
                                    "Preserving " + entityInputs + " entity intake and "
                                            + entityOutputs + " entity output rule(s) from JSON."),
                            spec.x() + 14, spec.y() + spec.height() - 42,
                            0xFFFFC56D, false);
                }
            }
            return;
        }

        if (screen instanceof ContextConfigScreen) renderKEditorText(graphics, screen, spec, font);
    }

    private static void renderKEditorText(GuiGraphics graphics, Screen screen,
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

    private static PanelSpec panelSpec(Screen screen) {
        String name = screen.getClass().getSimpleName();
        int w;
        int h;
        int y;
        switch (name) {
            case "MessageScreen" -> { w = Math.min(430, screen.width - 20); h = 130; y = Math.max(12, screen.height / 2 - 65); }
            case "HomeScreen" -> { w = Math.min(420, screen.width - 20); h = Math.min(310, screen.height - 20); y = Math.max(10, (screen.height - h) / 2); }
            case "ModulesScreen" -> { w = Math.min(560, screen.width - 20); h = Math.min(380, screen.height - 16); y = Math.max(8, (screen.height - h) / 2); }
            case "InventoryHubScreen" -> { w = Math.min(430, screen.width - 20); h = Math.min(300, screen.height - 20); y = Math.max(10, (screen.height - h) / 2); }
            case "ItemRulesScreen" -> { w = Math.min(650, screen.width - 16); h = Math.min(400, screen.height - 16); y = Math.max(8, (screen.height - h) / 2); }
            case "ItemRuleDetailScreen" -> { w = Math.min(430, screen.width - 20); h = 280; y = Math.max(10, (screen.height - h) / 2); }
            case "IdListScreen" -> { w = Math.min(600, screen.width - 18); h = Math.min(380, screen.height - 16); y = Math.max(8, (screen.height - h) / 2); }
            case "CodexListScreen" -> { w = Math.min(650, screen.width - 18); h = Math.min(390, screen.height - 16); y = Math.max(8, (screen.height - h) / 2); }
            case "CodexDetailScreen" -> { w = Math.min(700, screen.width - 16); h = Math.min(470, screen.height - 16); y = Math.max(8, (screen.height - h) / 2); }
            case "ItemPickerScreen" -> { w = Math.min(680, screen.width - 16); h = Math.min(410, screen.height - 16); y = Math.max(8, (screen.height - h) / 2); }
            case "ContextListScreen" -> { w = Math.min(700, screen.width - 16); h = Math.min(410, screen.height - 16); y = Math.max(8, (screen.height - h) / 2); }
            case "ContextDetailScreen" -> { w = Math.min(700, screen.width - 16); h = Math.min(450, screen.height - 16); y = Math.max(8, (screen.height - h) / 2); }
            case "DrinkListScreen" -> { w = Math.min(700, screen.width - 16); h = Math.min(410, screen.height - 16); y = Math.max(8, (screen.height - h) / 2); }
            case "DrinkDetailScreen" -> { w = Math.min(730, screen.width - 14); h = Math.min(470, screen.height - 14); y = Math.max(7, (screen.height - h) / 2); }
            case "DrinkEffectsScreen" -> { w = Math.min(680, screen.width - 16); h = Math.min(410, screen.height - 16); y = Math.max(8, (screen.height - h) / 2); }
            case "DrinkEffectDetailScreen" -> { w = Math.min(500, screen.width - 20); h = 290; y = Math.max(10, (screen.height - h) / 2); }
            case "RecipeListScreen" -> { w = Math.min(760, screen.width - 12); h = Math.min(440, screen.height - 12); y = Math.max(6, (screen.height - h) / 2); }
            case "MachineSettingsScreen" -> { w = Math.min(620, screen.width - 18); h = 360; y = Math.max(9, (screen.height - h) / 2); }
            case "RecipeDetailScreen" -> { w = Math.min(790, screen.width - 10); h = Math.min(480, screen.height - 10); y = Math.max(5, (screen.height - h) / 2); }
            case "ContextConfigScreen" -> {
                w = 270; h = 366; y = Math.max(10, (screen.height - h) / 2);
                return new PanelSpec(Math.max(10, screen.width - w - 10), y, w, h);
            }
            default -> { return null; }
        }
        return new PanelSpec(Math.max(8, (screen.width - w) / 2), y, w, h);
    }

    private static void renderDrinkRows(GuiGraphics graphics, Screen screen,
                                        int mouseX, int mouseY) {
        List<JsonObject> filtered = readJsonObjectList(screen, "filtered");
        int scroll = integerField(screen, "scroll", 0);
        int w = Math.min(700, screen.width - 16);
        int x = (screen.width - w) / 2 + 12;
        int top = Math.max(8, (screen.height - Math.min(410, screen.height - 16)) / 2) + 38;
        int listY = top + 30;
        int visible = Math.max(5, Math.min(11, (screen.height - 132) / 24));
        int rowRight = x + w - 150;
        Font font = Minecraft.getInstance().font;

        for (int i = scroll; i < Math.min(filtered.size(), scroll + visible); i++) {
            JsonObject drink = filtered.get(i);
            int rowY = listY + (i - scroll) * 24;
            drawSummaryCard(graphics, x, rowY, rowRight, rowY + 20,
                    mouseX, mouseY);
            graphics.renderItem(coloredCup(string(drink, "cup_color", "#FFFFFF")),
                    x + 7, rowY + 2);

            String alias = firstAlias(drink);
            String title = alias.isBlank() ? humanizeId(string(drink, "id", "Drink")) : alias;
            JsonObject result = childObject(drink, "result");
            String resultName = result == null ? "No item result"
                    : displayNameForItem(string(result, "item", ""));
            int count = result == null ? 1 : integer(result, "count", 1);
            String detail = "→ " + resultName + (count > 1 ? " ×" + count : "");
            if (!bool(drink, "enabled", true)) detail += "  [Disabled]";

            graphics.enableScissor(x + 26, rowY, rowRight - 5, rowY + 20);
            graphics.drawString(font, ScpFonts.roboto(compact(title, 34)),
                    x + 29, rowY + 4, bool(drink, "enabled", true) ? WHITE : MUTED,
                    false);
            int detailX = x + 34 + Math.min(150, font.width(ScpFonts.roboto(compact(title, 34))));
            graphics.drawString(font, ScpFonts.roboto(compact(detail, 44)),
                    detailX, rowY + 4, MUTED, false);
            graphics.disableScissor();
        }
    }

    private static void renderItemRuleRows(GuiGraphics graphics, Screen screen,
                                           int mouseX, int mouseY) {
        List<JsonObject> filtered = readJsonObjectList(screen, "filtered");
        int scroll = integerField(screen, "scroll", 0);
        int w = Math.min(650, screen.width - 16);
        int x = (screen.width - w) / 2 + 12;
        int top = Math.max(8, (screen.height - Math.min(400, screen.height - 16)) / 2) + 38;
        int listY = top + 30;
        int visible = Math.max(4, Math.min(10, (screen.height - 128) / 24));
        int rowRight = x + w - 78;
        Font font = Minecraft.getInstance().font;

        for (int i = scroll; i < Math.min(filtered.size(), scroll + visible); i++) {
            JsonObject rule = filtered.get(i);
            int rowY = listY + (i - scroll) * 24;
            drawSummaryCard(graphics, x, rowY, rowRight, rowY + 20,
                    mouseX, mouseY);
            String id = string(rule, "id", "unknown");
            ItemStack stack = itemStack(id);
            if (!stack.isEmpty()) graphics.renderItem(stack, x + 7, rowY + 2);

            String category = formatCategory(string(rule, "type", "MISCELLANEOUS"));
            int badgeWidth = Math.min(118,
                    Math.max(54, font.width(ScpFonts.roboto(category)) + 14));
            int badgeX = rowRight - badgeWidth - 4;
            drawBadge(graphics, badgeX, rowY + 3, rowRight - 4, rowY + 17,
                    category, NAVY_HOVER, PALE_GOLD);
            graphics.enableScissor(x + 26, rowY, badgeX - 5, rowY + 20);
            graphics.drawString(font, ScpFonts.roboto(displayNameForItem(id)),
                    x + 29, rowY + 6, WHITE, false);
            graphics.disableScissor();
        }
    }

    private static void renderRecipeRows(GuiGraphics graphics, Screen screen,
                                         int mouseX, int mouseY) {
        List<?> filtered = readList(screen, "filtered");
        int scroll = integerField(screen, "scroll", 0);
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
            if (recipe == null) continue;
            int rowY = listY + (i - scroll) * 24;
            drawSummaryCard(graphics, x, rowY, rowRight, rowY + 20,
                    mouseX, mouseY);

            RecipePreview input = firstRecipePreview(screen, recipe, true);
            RecipePreview output = firstRecipePreview(screen, recipe, false);
            renderRecipePreview(graphics, input, x + 7, rowY + 2);
            graphics.drawString(font, ScpFonts.roboto("→"), x + 26, rowY + 6,
                    PALE_GOLD, false);
            renderRecipePreview(graphics, output, x + 38, rowY + 2);

            String setting = settingLabel(string(recipe, "setting", "?"));
            int badgeWidth = Math.min(112,
                    Math.max(62, font.width(ScpFonts.roboto(setting)) + 14));
            int badgeX = rowRight - badgeWidth - 4;
            drawBadge(graphics, badgeX, rowY + 3, rowRight - 4, rowY + 17,
                    setting, NAVY_HOVER, PALE_GOLD);

            String transformation = summarizeRecipeSide(recipe, true) + "  →  "
                    + summarizeRecipeSide(recipe, false);
            if (!bool(recipe, "enabled", true)) transformation += "  [Disabled]";
            graphics.enableScissor(x + 58, rowY, badgeX - 5, rowY + 20);
            graphics.drawString(font, ScpFonts.roboto(transformation),
                    x + 61, rowY + 6, bool(recipe, "enabled", true) ? WHITE : MUTED,
                    false);
            graphics.disableScissor();
        }
    }

    private static void renderContextRows(GuiGraphics graphics, Screen screen,
                                          int mouseX, int mouseY) {
        List<JsonObject> filtered = readJsonObjectList(screen, "filtered");
        int scroll = integerField(screen, "scroll", 0);
        int w = Math.min(700, screen.width - 16);
        int x = (screen.width - w) / 2 + 12;
        int top = Math.max(8, (screen.height - Math.min(410, screen.height - 16)) / 2) + 38;
        int listY = top + 30;
        int visible = Math.max(5, Math.min(11, (screen.height - 132) / 24));
        int rowRight = x + w - 82;
        Font font = Minecraft.getInstance().font;

        for (int i = scroll; i < Math.min(filtered.size(), scroll + visible); i++) {
            JsonObject rule = filtered.get(i);
            int rowY = listY + (i - scroll) * 24;
            drawSummaryCard(graphics, x, rowY, rowRight, rowY + 20,
                    mouseX, mouseY);
            String type = string(rule, "type", "block").toLowerCase(Locale.ROOT);
            String badge = "entity".equals(type) ? "ENTITY" : "BLOCK";
            int badgeRight = x + 58;
            drawBadge(graphics, x + 4, rowY + 3, badgeRight, rowY + 17,
                    badge, "entity".equals(type) ? ENTITY_BADGE : BLOCK_BADGE, WHITE);

            String id = string(rule, "id", "unknown");
            ItemStack icon = contextIcon(type, id);
            if (!icon.isEmpty()) graphics.renderItem(icon, x + 63, rowY + 2);
            String target = "entity".equals(type)
                    ? displayNameForEntity(id) : displayNameForBlock(id);
            String action = string(childObject(rule, "text"), "action", "Use");
            graphics.enableScissor(x + 82, rowY, rowRight - 5, rowY + 20);
            graphics.drawString(font, ScpFonts.roboto(target), x + 85, rowY + 6,
                    WHITE, false);
            int actionX = x + 91 + Math.min(210, font.width(ScpFonts.roboto(target)));
            graphics.drawString(font, ScpFonts.roboto("— " + action), actionX,
                    rowY + 6, MUTED, false);
            graphics.disableScissor();
        }
    }

    private static void renderIdRows(GuiGraphics graphics, Screen screen,
                                     int mouseX, int mouseY) {
        List<?> raw = readList(screen, "filtered");
        List<String> filtered = new ArrayList<>();
        for (Object value : raw) if (value instanceof String text) filtered.add(text);
        int scroll = integerField(screen, "scroll", 0);
        String key = readField(screen, "key", String.class);
        int w = Math.min(600, screen.width - 18);
        int x = (screen.width - w) / 2 + 12;
        int top = Math.max(8, (screen.height - Math.min(380, screen.height - 16)) / 2) + 38;
        int listY = top + 56;
        int visible = Math.max(4, Math.min(9, (screen.height - 146) / 24));
        int rowRight = x + w - 88;
        Font font = Minecraft.getInstance().font;
        boolean effects = "hidden_status_effects".equals(key);

        for (int i = scroll; i < Math.min(filtered.size(), scroll + visible); i++) {
            String id = filtered.get(i);
            int rowY = listY + (i - scroll) * 24;
            drawSummaryCard(graphics, x, rowY, rowRight, rowY + 20,
                    mouseX, mouseY);
            String badge = effects ? "EFFECT" : "TARGET";
            drawBadge(graphics, x + 4, rowY + 3, x + 62, rowY + 17,
                    badge, effects ? ENTITY_BADGE : BLOCK_BADGE, WHITE);
            String display = effects ? displayNameForEffect(id)
                    : id.startsWith("#") ? "Tag: " + humanizeId(id.substring(1))
                    : displayNameForEntity(id);
            graphics.enableScissor(x + 66, rowY, rowRight - 5, rowY + 20);
            graphics.drawString(font, ScpFonts.roboto(display), x + 69, rowY + 6,
                    WHITE, false);
            graphics.disableScissor();
        }
    }

    private static void renderDrinkEffectRows(GuiGraphics graphics, Screen screen,
                                              int mouseX, int mouseY) {
        JsonObject drink = readField(screen, "drink", JsonObject.class);
        if (drink == null) return;
        JsonArray effects = array(drink, "effects");
        int scroll = integerField(screen, "scroll", 0);
        int w = Math.min(680, screen.width - 16);
        int x = (screen.width - w) / 2 + 12;
        int top = Math.max(8, (screen.height - Math.min(410, screen.height - 16)) / 2) + 42;
        int listY = top + 30;
        int visible = Math.max(4, Math.min(10, (screen.height - 130) / 24));
        int rowRight = x + w - 82;
        Font font = Minecraft.getInstance().font;

        for (int i = scroll; i < Math.min(effects.size(), scroll + visible); i++) {
            JsonElement element = effects.get(i);
            if (!element.isJsonObject()) continue;
            JsonObject effect = element.getAsJsonObject();
            int rowY = listY + (i - scroll) * 24;
            drawSummaryCard(graphics, x, rowY, rowRight, rowY + 20,
                    mouseX, mouseY);
            String id = string(effect, "id", "unknown");
            int duration = integer(effect, "duration", 200);
            int amplifier = integer(effect, "amplifier", 0) + 1;
            String label = displayNameForEffect(id) + "  —  "
                    + formatDuration(duration) + "  ·  Level " + amplifier;
            graphics.enableScissor(x + 5, rowY, rowRight - 5, rowY + 20);
            graphics.drawString(font, ScpFonts.roboto(label), x + 9, rowY + 6,
                    WHITE, false);
            graphics.disableScissor();
        }
    }

    private static void drawSummaryCard(GuiGraphics graphics, int left, int top,
                                        int right, int bottom, int mouseX, int mouseY) {
        boolean hovered = mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
        graphics.fill(left, top, right, bottom, hovered ? NAVY_HOVER : NAVY);
        graphics.fill(left, top, left + (hovered ? 4 : 2), bottom,
                hovered ? ACCENT_SOFT : BORDER);
        graphics.fill(left, top, right, top + 1, hovered ? BORDER_HOVER : BORDER);
        graphics.fill(left, bottom - 1, right, bottom,
                hovered ? BORDER_HOVER : BORDER);
    }

    private static void drawBadge(GuiGraphics graphics, int left, int top,
                                  int right, int bottom, String text,
                                  int background, int color) {
        Font font = Minecraft.getInstance().font;
        graphics.fill(left, top, right, bottom, background);
        graphics.fill(left, top, left + 2, bottom, ACCENT_SOFT);
        Component component = ScpFonts.roboto(text);
        int x = left + Math.max(4, (right - left - font.width(component)) / 2);
        graphics.drawString(font, component, x, top + 3, color, false);
    }

    private static void coverTextLine(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 14, PANEL);
    }

    private static void drawFooter(GuiGraphics graphics, Font font, PanelSpec spec,
                                   String text, int color) {
        coverTextLine(graphics, spec.x() + 8, spec.y() + spec.height() - 23,
                spec.width() - 16);
        graphics.drawString(font, ScpFonts.roboto(text), spec.x() + 12,
                spec.y() + spec.height() - 17, color, false);
    }

    private static RecipePreview firstRecipePreview(Screen screen, JsonObject recipe,
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
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(key);
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

    private static String summarizeRecipeSide(JsonObject recipe, boolean intake) {
        List<String> values = new ArrayList<>();
        List<String> keys = intake
                ? List.of("item_inputs", "entity_inputs")
                : List.of("item_outputs", "weighted_item_outputs", "entity_outputs");
        for (String key : keys) {
            if (!recipe.has(key) || !recipe.get(key).isJsonArray()) continue;
            for (JsonElement element : recipe.getAsJsonArray(key)) {
                if (!element.isJsonObject()) continue;
                JsonObject value = element.getAsJsonObject();
                String item = string(value, "item", "");
                String entity = string(value, "entity", "");
                String name = !item.isBlank() ? displayNameForItem(item)
                        : !entity.isBlank() ? displayNameForEntity(entity) : "Unknown";
                int count = integer(value, "count", 1);
                values.add(name + (count > 1 ? " ×" + count : ""));
            }
        }
        if (values.isEmpty()) return intake ? "No intake" : "No output";
        String first = values.get(0);
        return values.size() == 1 ? first : first + " +" + (values.size() - 1);
    }

    private static ItemStack contextIcon(String type, String idText) {
        ResourceLocation id = ResourceLocation.tryParse(idText == null ? "" : idText);
        if (id == null) return ItemStack.EMPTY;
        if ("entity".equals(type)) {
            ResourceLocation eggId = new ResourceLocation(id.getNamespace(),
                    id.getPath() + "_spawn_egg");
            Item egg = BuiltInRegistries.ITEM.getValue(eggId);
            return egg == null || egg == Items.AIR
                    ? new ItemStack(Items.SPAWNER) : new ItemStack(egg);
        }
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        if (block != null && block.asItem() != Items.AIR) return new ItemStack(block.asItem());
        return itemStack(idText);
    }

    private static ItemStack itemStack(String id) {
        ResourceLocation resource = ResourceLocation.tryParse(id == null ? "" : id);
        if (resource == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.getValue(resource);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static String displayNameForItem(String id) {
        ItemStack stack = itemStack(id);
        return stack.isEmpty() ? humanizeId(id) : stack.getHoverName().getString();
    }

    private static String displayNameForBlock(String idText) {
        ResourceLocation id = ResourceLocation.tryParse(idText == null ? "" : idText);
        Block block = id == null ? null : BuiltInRegistries.BLOCK.getValue(id);
        return block == null ? humanizeId(idText) : block.getName().getString();
    }

    private static String displayNameForEntity(String idText) {
        ResourceLocation id = ResourceLocation.tryParse(idText == null ? "" : idText);
        net.minecraft.world.entity.EntityType<?> type = id == null
                ? null : BuiltInRegistries.ENTITY_TYPE.getValue(id);
        return type == null ? humanizeId(idText) : type.getDescription().getString();
    }

    private static String displayNameForEffect(String idText) {
        ResourceLocation id = ResourceLocation.tryParse(idText == null ? "" : idText);
        net.minecraft.world.effect.MobEffect effect = id == null
                ? null : BuiltInRegistries.MOB_EFFECT.getValue(id);
        return effect == null ? humanizeId(idText)
                : Component.translatable(effect.getDescriptionId()).getString();
    }

    private static String humanizeId(String id) {
        if (id == null || id.isBlank()) return "Unknown";
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        StringBuilder result = new StringBuilder();
        for (String part : path.split("[_/.-]+")) {
            if (part.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) result.append(part.substring(1));
        }
        return result.isEmpty() ? id : result.toString();
    }

    private static String formatCategory(String value) {
        if (value == null || value.isBlank()) return "Miscellaneous";
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "ACCESSORYHAND" -> "Hand Accessory";
            case "NO_STAMINA" -> "No Stamina";
            default -> humanizeId(value.toLowerCase(Locale.ROOT));
        };
    }

    private static String settingLabel(String setting) {
        return switch (setting == null ? "" : setting.toLowerCase(Locale.ROOT)) {
            case "1_to_1" -> "1:1";
            case "very_fine" -> "Very Fine";
            case "rough" -> "Rough";
            case "coarse" -> "Coarse";
            case "fine" -> "Fine";
            default -> humanizeId(setting);
        };
    }

    private static String formatDuration(int ticks) {
        double seconds = Math.max(0, ticks) / 20.0D;
        return seconds == Math.rint(seconds)
                ? String.format(Locale.ROOT, "%.0fs", seconds)
                : String.format(Locale.ROOT, "%.1fs", seconds);
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
        if (screen instanceof ItemConfigScreen || screen instanceof ContextConfigScreen
                || screen instanceof UnityColorPickerScreen
                || screen instanceof CodexImageDropScreen
                || screen instanceof CodexTextEditorScreen) return true;
        return screen.getClass().getName()
                .startsWith("net.mcreator.scpadditions.config.ui.ConfigCenterClient$");
    }

    private static boolean contains(AbstractButton button, int mouseX, int mouseY) {
        return mouseX >= button.getX() && mouseX < button.getX() + button.getWidth()
                && mouseY >= button.getY() && mouseY < button.getY() + button.getHeight();
    }

    private static boolean isDanger(String label) {
        String value = normalize(label);
        return value.equals("x") || value.startsWith("delete")
                || value.startsWith("remove") || value.startsWith("forget")
                || value.startsWith("confirm");
    }

    private static boolean isPrimary(String label) {
        String value = normalize(label);
        return value.startsWith("save") || value.startsWith("done")
                || value.startsWith("create") || value.startsWith("+ add")
                || value.startsWith("+ new") || value.startsWith("+ paper")
                || value.startsWith("apply");
    }

    private static String tooltipFor(String label) {
        String normalized = normalize(label);
        for (Map.Entry<String, String> entry : TOOLTIPS.entrySet()) {
            if (normalized.equals(entry.getKey())
                    || normalized.startsWith(entry.getKey() + ":")
                    || normalized.startsWith(entry.getKey() + " ")) {
                return entry.getValue();
            }
        }
        if (normalized.startsWith("category")) {
            return "Choose how this item is stored and used by SCP Inventory.";
        }
        if (normalized.startsWith("type") || normalized.startsWith("target type")) {
            return "Choose the registry kind represented by this entry.";
        }
        if (normalized.contains("on") || normalized.contains("off")) {
            return "Toggle this setting. Changes are applied only after saving.";
        }
        if (normalized.startsWith("x")) {
            return "Remove this entry from the working configuration.";
        }
        return "Open or modify this configuration entry.";
    }

    private static String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, String> buildTooltips() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("general & modules", "Enable or disable gameplay, HUD, blink, interaction and SCP systems.");
        values.put("inventory, equipment & codex", "Edit item categories, equipment effects, Status filters and Codex documents.");
        values.put("contextual interactions", "Edit prompts and right-click actions attached to blocks and entities.");
        values.put("scp-294 drinks", "Create, search and edit drinks, aliases, colors, effects and machine actions.");
        values.put("scp-914 recipes", "Create and edit intake, setting, output and advanced transformations.");
        values.put("item categories & equipment effects", "Classify items and assign supported equipment modifiers.");
        values.put("hidden status effects", "Choose effects hidden from the custom Conditions panel.");
        values.put("scp-173 entity targets", "Choose entities participating in SCP-173 observation and targeting rules.");
        values.put("codex documents", "Create document definitions shown in the SCP Inventory Codex.");
        values.put("reload snapshot", "Discard unsaved local edits and request the server configuration again.");
        values.put("save & reload", "Validate, save with a .bak backup and reload the affected system.");
        values.put("save all", "Validate and save every edited section in this group.");
        values.put("save rule", "Save this item category and its equipment effects.");
        values.put("save", "Validate and save the current changes.");
        values.put("defaults", "Restore the default values on this screen before saving.");
        values.put("back", "Return to the previous configuration screen.");
        values.put("cancel", "Discard unsaved changes on this screen.");
        values.put("done", "Close the configuration center.");
        values.put("forget", "Remove the explicit rule and return the item to automatic classification.");
        values.put("confirm", "Confirm the destructive removal action.");
        values.put("+ add item", "Search the loaded item registry and create a new item rule.");
        values.put("+ new drink", "Create a new SCP-294 drink definition.");
        values.put("+ recipe", "Create a new SCP-914 recipe in the in-game editor fragment.");
        values.put("+ block", "Create a contextual interaction for a block.");
        values.put("+ entity", "Create a contextual interaction for an entity.");
        values.put("+ paper document", "Create a configured Codex definition. Documents cannot be assigned through generic item categories.");
        values.put("pick color", "Choose the cup color with synchronized RGB sliders and hexadecimal input.");
        values.put("import png", "Upload a PNG into this world's Codex asset folder.");
        values.put("replace png", "Replace the PNG reference with another world asset.");
        values.put("write text", "Write UTF-8 text and save it in this world's Codex asset folder.");
        values.put("edit text", "Edit the text file referenced by this Codex definition.");
        values.put("match mode", "Choose whether any matching item or only a generated NBT-tagged item opens this document.");
        values.put("give test item", "Give yourself a uniquely tagged and named test document item.");
        values.put("copy", "Create an independent copy of this entry for faster editing.");
        values.put("additional recipe settings", "Show less common chance, NBT, action-bar and weighted-output options.");
        values.put("machine", "Edit the intake, output, radius and timing shared by every SCP-914 recipe.");
        return Map.copyOf(values);
    }

    private static Screen constructNestedScreen(String className, Screen parent,
                                                JsonObject object) {
        try {
            Class<?> type = Class.forName(className);
            Constructor<?> constructor = type.getDeclaredConstructor(Screen.class,
                    JsonObject.class);
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

    private static <T> T readStaticField(Class<?> type, String name, Class<T> expected) {
        if (type == null) return null;
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            if (!Modifier.isStatic(field.getModifiers())) return null;
            Object value = field.get(null);
            return expected.isInstance(value) ? expected.cast(value) : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
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

    private static int integerField(Object target, String name, int fallback) {
        Object value = readField(target, name);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static double numberField(Object target, String name, double fallback) {
        Object value = readField(target, name);
        return value instanceof Number number ? number.doubleValue() : fallback;
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
        if (root == null) return new JsonArray();
        if (!root.has(key) || !root.get(key).isJsonArray()) root.add(key, new JsonArray());
        return root.getAsJsonArray(key);
    }

    private static JsonObject childObject(JsonObject root, String key) {
        return root != null && root.has(key) && root.get(key).isJsonObject()
                ? root.getAsJsonObject(key) : null;
    }

    private static String string(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int integer(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int jsonArraySize(JsonObject root, String key) {
        return root != null && root.has(key) && root.get(key).isJsonArray()
                ? root.getAsJsonArray(key).size() : 0;
    }

    private static String firstAlias(JsonObject drink) {
        if (drink == null || !drink.has("aliases") || !drink.get("aliases").isJsonArray()) {
            return "";
        }
        JsonArray aliases = drink.getAsJsonArray("aliases");
        if (aliases.isEmpty()) return "";
        JsonElement first = aliases.get(0);
        return first.isJsonPrimitive() ? first.getAsString() : "";
    }

    private static String compact(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text
                : text.substring(0, Math.max(0, max - 3)) + "...";
    }

    private static int parseColor(String value, int fallback) {
        if (value == null) return fallback;
        String clean = value.trim();
        if (clean.startsWith("#")) clean = clean.substring(1);
        try {
            return clean.matches("[0-9A-Fa-f]{6}")
                    ? Integer.parseInt(clean, 16) : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private record RecipePreview(ItemStack item, ResourceLocation entityId) {
    }

    private record PanelSpec(int x, int y, int width, int height) {
    }
}
