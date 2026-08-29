package com.bl4ues.scpclassifieddirective.config.ui;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Adds a native stealth/perception editor to the Configuration Center. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class StealthConfigCenterEnhancements {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting().disableHtmlEscaping().create();

    private StealthConfigCenterEnhancements() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!event.getScreen().getClass().getName()
                .endsWith("ConfigCenterClient$HomeScreen")) return;
        AbstractWidget general = findWidget(event, "General & Modules");
        if (general == null) return;

        int insertY = general.getY() + 31;
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof AbstractWidget widget
                    && widget != general && widget.getY() >= insertY) {
                widget.setY(widget.getY() + 31);
            }
        }
        event.addListener(Button.builder(ScpFonts.roboto("Stealth & Perception"),
                        button -> openEditor(event.getScreen()))
                .bounds(general.getX(), insertY,
                        general.getWidth(), general.getHeight())
                .build());
    }

    private static void openEditor(Screen parent) {
        JsonObject modules = moduleSnapshot();
        Minecraft.getInstance().setScreen(
                new StealthSettingsScreen(parent, modules));
    }

    private static JsonObject moduleSnapshot() {
        try {
            Field field = ConfigCenterClient.class.getDeclaredField("files");
            field.setAccessible(true);
            Object raw = field.get(null);
            if (raw instanceof JsonObject files && files.has(ConfigCenterService.MODULES)
                    && files.get(ConfigCenterService.MODULES).isJsonObject()) {
                return files.getAsJsonObject(ConfigCenterService.MODULES).deepCopy();
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return new JsonObject();
    }

    private static AbstractWidget findWidget(ScreenEvent.Init.Post event,
            String label) {
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof AbstractWidget widget
                    && label.equals(widget.getMessage().getString())) {
                return widget;
            }
        }
        return null;
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (!parent.has(key) || !parent.get(key).isJsonObject()) {
            parent.add(key, new JsonObject());
        }
        return parent.getAsJsonObject(key);
    }

    private static boolean bool(JsonObject object, String key,
            boolean fallback) {
        if (!object.has(key)) return fallback;
        try {
            return object.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double number(JsonObject object, String key,
            double fallback) {
        if (!object.has(key)) return fallback;
        try {
            return object.get(key).getAsDouble();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private abstract static class StealthScreen extends Screen {
        protected static final int PANEL = 0xFF111317;
        protected static final int ROW = 0xFF081022;
        protected static final int ROW_ALT = 0xFF0D1628;
        protected static final int BORDER = 0xFF46536C;
        protected static final int TEXT = 0xFFF7F8FC;
        protected static final int MUTED = 0xFF9CA3AF;
        protected static final int ACCENT = 0xFFC59A2A;
        protected static final int GOOD = 0xFF79D58B;
        protected static final int BAD = 0xFFD46060;

        protected StealthScreen(String title) {
            super(ScpFonts.roboto(title));
        }

        protected int panelWidth() {
            return Math.min(720, width - 28);
        }

        protected int panelHeight() {
            return Math.min(440, height - 24);
        }

        protected int panelX() {
            return ConfigCenterVisuals.contentLeft(width, panelWidth());
        }

        protected int panelY() {
            return Math.max(12, (height - panelHeight()) / 2);
        }

        protected void drawPanel(GuiGraphics graphics, String title,
                String subtitle) {
            ConfigCenterVisuals.drawPanel(graphics, font, panelX(), panelY(),
                    panelWidth(), panelHeight(), title);
            graphics.drawString(font, ScpFonts.roboto(subtitle),
                    panelX() + ConfigCenterVisuals.contentOffsetX() + 16,
                    panelY() + 39, ConfigCenterVisuals.fadeColor(MUTED), false);
        }

        protected StyledButton button(int x, int y, int width, int height,
                String label, boolean primary, boolean danger, Runnable action) {
            StyledButton button = new StyledButton(x, y, width, height,
                    label, primary, danger, action);
            addRenderableWidget(button);
            return button;
        }

        protected EditBox edit(int x, int y, int width, String value,
                String hint) {
            EditBox box = new EditBox(font, x, y, width, 20,
                    ScpFonts.roboto(hint));
            box.setValue(value);
            box.setHint(ScpFonts.roboto(hint));
            box.setMaxLength(96);
            box.setBordered(false);
            box.setTextColor(TEXT);
            addRenderableWidget(box);
            return box;
        }

        protected void drawEditBox(GuiGraphics graphics, EditBox box) {
            int x = box.getX() - 3;
            int y = box.getY() - 2;
            int right = box.getX() + box.getWidth() + 3;
            int bottom = box.getY() + box.getHeight() + 2;
            graphics.fill(x, y, right, bottom, 0xE8080B10);
            int border = box.isFocused() ? ACCENT : BORDER;
            graphics.fill(x, y, right, y + 1, border);
            graphics.fill(x, bottom - 1, right, bottom, border);
            graphics.fill(x, y, x + 1, bottom, border);
            graphics.fill(right - 1, y, right, bottom, border);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        protected static final class StyledButton extends AbstractButton {
            private final Runnable action;
            private final boolean primary;
            private final boolean danger;

            private StyledButton(int x, int y, int width, int height,
                    String label, boolean primary, boolean danger,
                    Runnable action) {
                super(x, y, width, height, ScpFonts.roboto(label));
                this.action = action;
                this.primary = primary;
                this.danger = danger;
            }

            @Override
            public void onPress() {
                action.run();
            }

            @Override
            protected void renderWidget(GuiGraphics graphics, int mouseX,
                    int mouseY, float partialTick) {
                Font font = Minecraft.getInstance().font;
                boolean hovered = isHoveredOrFocused();
                int background = !active ? 0xFF1B1E26
                        : hovered ? 0xFF131E36 : ROW;
                int border = danger ? BAD : hovered ? 0xFF73809A : BORDER;
                int stripe = danger ? BAD : primary ? ACCENT
                        : hovered ? 0xFF8D711F : BORDER;
                int textColor = !active ? MUTED
                        : primary && !danger ? 0xFFE5D49A : TEXT;
                int right = getX() + getWidth();
                int bottom = getY() + getHeight();
                graphics.fill(getX(), getY(), right, bottom, background);
                graphics.fill(getX(), getY(), right, getY() + 1, border);
                graphics.fill(getX(), bottom - 1, right, bottom, border);
                graphics.fill(getX(), getY(), getX() + 1, bottom, border);
                graphics.fill(right - 1, getY(), right, bottom, border);
                graphics.fill(getX() + 1, getY() + 1,
                        getX() + (primary || danger || hovered ? 4 : 2),
                        bottom - 1, stripe);
                int textX = getX() + Math.max(5,
                        (getWidth() - font.width(getMessage())) / 2);
                int textY = getY() + Math.max(1, (getHeight() - 8) / 2);
                graphics.drawString(font, getMessage(), textX, textY,
                        textColor, false);
            }

            @Override
            protected void updateWidgetNarration(NarrationElementOutput output) {
                defaultButtonNarrationText(output);
            }
        }
    }

    private static final class StealthSettingsScreen extends StealthScreen {
        private final Screen parent;
        private final JsonObject modules;
        private final JsonObject stealth;
        private EditBox standing;
        private EditBox crouching;
        private EditBox crawling;
        private EditBox darkness;
        private EditBox closeRange;
        private EditBox delay;
        private String notice = "";
        private boolean noticeGood;

        private StealthSettingsScreen(Screen parent, JsonObject modules) {
            super("Stealth & Perception");
            this.parent = parent;
            this.modules = modules;
            this.stealth = object(modules, "stealth");
            installDefaultsIfMissing();
        }

        private void installDefaultsIfMissing() {
            if (!stealth.has("enabled")) stealth.addProperty("enabled", true);
            if (!stealth.has("standing_visibility")) stealth.addProperty("standing_visibility", 1.0D);
            if (!stealth.has("crouching_visibility")) stealth.addProperty("crouching_visibility", 0.60D);
            if (!stealth.has("crawling_visibility")) stealth.addProperty("crawling_visibility", 0.30D);
            if (!stealth.has("darkness_floor")) stealth.addProperty("darkness_floor", 0.18D);
            if (!stealth.has("minimum_close_range")) stealth.addProperty("minimum_close_range", 2.5D);
            if (!stealth.has("max_acquire_delay_ticks")) stealth.addProperty("max_acquire_delay_ticks", 50);
            if (!stealth.has("perception_rules") || !stealth.get("perception_rules").isJsonArray()) {
                stealth.add("perception_rules", new JsonArray());
            }
        }

        @Override
        protected void init() {
            int x = panelX() + 18;
            int y = panelY() + 68;
            int inner = panelWidth() - 36;
            int half = (inner - 18) / 2;

            button(x, y, inner, 22,
                    "Advanced Crouch & Stealth: "
                            + (bool(stealth, "enabled", true) ? "ON" : "OFF"),
                    bool(stealth, "enabled", true), false, () -> {
                        stealth.addProperty("enabled",
                                !bool(stealth, "enabled", true));
                        rebuild();
                    });
            y += 48;

            standing = numberField(x, y, half, "Standing visibility",
                    "standing_visibility", 1.0D);
            crouching = numberField(x + half + 18, y, half,
                    "Crouching visibility", "crouching_visibility", 0.60D);
            y += 48;
            crawling = numberField(x, y, half, "Crawling visibility",
                    "crawling_visibility", 0.30D);
            darkness = numberField(x + half + 18, y, half, "Darkness floor",
                    "darkness_floor", 0.18D);
            y += 48;
            closeRange = numberField(x, y, half, "Minimum close range",
                    "minimum_close_range", 2.5D);
            delay = numberField(x + half + 18, y, half,
                    "Maximum acquire delay (ticks)",
                    "max_acquire_delay_ticks", 50.0D);
            y += 52;

            button(x, y, inner, 24, "Mob Perception Rules",
                    true, false, () -> {
                        if (applyFields()) {
                            Minecraft.getInstance().setScreen(
                                    new PerceptionRulesScreen(this, modules));
                        }
                    });

            int footer = panelY() + panelHeight() - 32;
            button(x, footer, 100, 20, "Defaults", false, false,
                    this::resetDefaults);
            button(x + inner - 222, footer, 120, 20,
                    "Save & Reload", true, false, this::save);
            button(x + inner - 94, footer, 94, 20,
                    "Back", false, false,
                    () -> Minecraft.getInstance().setScreen(parent));
        }

        private EditBox numberField(int x, int y, int width, String label,
                String key, double fallback) {
            EditBox box = edit(x, y + 14, width,
                    compactNumber(number(stealth, key, fallback)), label);
            return box;
        }

        private void rebuild() {
            clearWidgets();
            init();
        }

        private boolean applyFields() {
            try {
                double standingValue = bounded(standing.getValue(), 0.0D, 1.0D);
                double crouchValue = bounded(crouching.getValue(), 0.0D, 1.0D);
                double crawlValue = bounded(crawling.getValue(), 0.0D, 1.0D);
                double darkValue = bounded(darkness.getValue(), 0.0D, 1.0D);
                double closeValue = bounded(closeRange.getValue(), 0.0D, 16.0D);
                int delayValue = (int) Math.round(bounded(delay.getValue(), 0.0D, 600.0D));
                stealth.addProperty("standing_visibility", standingValue);
                stealth.addProperty("crouching_visibility", crouchValue);
                stealth.addProperty("crawling_visibility", crawlValue);
                stealth.addProperty("darkness_floor", darkValue);
                stealth.addProperty("minimum_close_range", closeValue);
                stealth.addProperty("max_acquire_delay_ticks", delayValue);
                notice = "";
                return true;
            } catch (NumberFormatException exception) {
                notice = "One of the numeric fields is invalid.";
                noticeGood = false;
                return false;
            }
        }

        private void save() {
            if (!applyFields()) return;
            JsonObject changes = new JsonObject();
            changes.add(ConfigCenterService.MODULES, modules);
            notice = "Saving server configuration...";
            noticeGood = true;
            ModNetwork.CHANNEL.sendToServer(new ConfigCenterNetwork.SaveRequest(
                    GSON.toJson(changes)));
        }

        private void resetDefaults() {
            stealth.addProperty("enabled", true);
            stealth.addProperty("standing_visibility", 1.0D);
            stealth.addProperty("crouching_visibility", 0.60D);
            stealth.addProperty("crawling_visibility", 0.30D);
            stealth.addProperty("darkness_floor", 0.18D);
            stealth.addProperty("minimum_close_range", 2.5D);
            stealth.addProperty("max_acquire_delay_ticks", 50);
            rebuild();
            notice = "Restored stealth defaults; Save & Reload to apply.";
            noticeGood = true;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY,
                float partialTick) {
            ConfigCenterVisuals.renderBackdrop(this, graphics, mouseX, mouseY);
            drawPanel(graphics, "Stealth & Perception",
                    "Server-owned crouch, crawl and visual detection behavior.");
            int x = panelX() + 18;
            int y = panelY() + 111;
            int inner = panelWidth() - 36;
            int half = (inner - 18) / 2;
            graphics.drawString(font, ScpFonts.roboto("Standing visibility"),
                    x, y, MUTED, false);
            graphics.drawString(font, ScpFonts.roboto("Crouching visibility"),
                    x + half + 18, y, MUTED, false);
            graphics.drawString(font, ScpFonts.roboto("Crawling visibility"),
                    x, y + 48, MUTED, false);
            graphics.drawString(font, ScpFonts.roboto("Darkness floor"),
                    x + half + 18, y + 48, MUTED, false);
            graphics.drawString(font, ScpFonts.roboto("Minimum close range"),
                    x, y + 96, MUTED, false);
            graphics.drawString(font,
                    ScpFonts.roboto("Maximum acquire delay (ticks)"),
                    x + half + 18, y + 96, MUTED, false);
            for (EditBox box : List.of(standing, crouching, crawling,
                    darkness, closeRange, delay)) {
                if (box != null) drawEditBox(graphics, box);
            }
            if (!notice.isBlank()) {
                graphics.drawString(font, ScpFonts.roboto(notice), x,
                        panelY() + panelHeight() - 50,
                        noticeGood ? GOOD : BAD, false);
            }
            super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void onClose() {
            Minecraft.getInstance().setScreen(parent);
        }
    }

    private static final class PerceptionRulesScreen extends StealthScreen {
        private final Screen parent;
        private final JsonObject modules;
        private final JsonObject stealth;
        private EditBox addId;
        private int scroll;
        private String notice = "";
        private boolean noticeGood;

        private PerceptionRulesScreen(Screen parent, JsonObject modules) {
            super("Mob Perception Rules");
            this.parent = parent;
            this.modules = modules;
            this.stealth = object(modules, "stealth");
        }

        private JsonArray rules() {
            if (!stealth.has("perception_rules")
                    || !stealth.get("perception_rules").isJsonArray()) {
                stealth.add("perception_rules", new JsonArray());
            }
            return stealth.getAsJsonArray("perception_rules");
        }

        @Override
        protected void init() {
            int x = panelX() + 16;
            int y = panelY() + 58;
            int inner = panelWidth() - 32;
            addId = edit(x, y, inner - 112, "",
                    "namespace:entity_id");
            button(x + inner - 104, y - 2, 104, 24,
                    "+ Add Mob", true, false, this::addRule);
            addRuleButtons(x, y + 38, inner);
            button(x + inner - 100, panelY() + panelHeight() - 30,
                    100, 20, "Back", false, false,
                    () -> Minecraft.getInstance().setScreen(parent));
        }

        private void addRuleButtons(int x, int y, int width) {
            JsonArray rules = rules();
            int visible = Math.max(3, (panelHeight() - 140) / 46);
            scroll = Math.min(scroll, Math.max(0, rules.size() - visible));
            int end = Math.min(rules.size(), scroll + visible);
            for (int index = scroll; index < end; index++) {
                JsonObject rule = rules.get(index).isJsonObject()
                        ? rules.get(index).getAsJsonObject() : new JsonObject();
                int rowY = y + (index - scroll) * 46 + 10;
                final int ruleIndex = index;
                button(x + width - 126, rowY, 76, 24,
                        "Edit", true, false,
                        () -> Minecraft.getInstance().setScreen(
                                new PerceptionRuleScreen(this, rule)));
                button(x + width - 44, rowY, 44, 24,
                        "X", false, true, () -> removeRule(ruleIndex));
            }
        }

        private void addRule() {
            String raw = addId.getValue().trim().toLowerCase(Locale.ROOT);
            ResourceLocation id = ResourceLocation.tryParse(raw);
            if (id == null || !ForgeRegistries.ENTITY_TYPES.containsKey(id)) {
                notice = "Unknown entity id: " + raw;
                noticeGood = false;
                return;
            }
            for (JsonElement element : rules()) {
                if (element.isJsonObject()
                        && raw.equals(element.getAsJsonObject().has("entity")
                                ? element.getAsJsonObject().get("entity").getAsString()
                                : "")) {
                    notice = "That entity already has a perception rule.";
                    noticeGood = false;
                    return;
                }
            }
            JsonObject rule = new JsonObject();
            rule.addProperty("entity", id.toString());
            rule.addProperty("omniscient", false);
            rule.addProperty("blind", false);
            rule.addProperty("night_vision", false);
            rule.addProperty("visibility_multiplier", 1.0D);
            rule.addProperty("range_multiplier", 1.0D);
            rule.addProperty("acquire_delay_multiplier", 1.0D);
            rules().add(rule);
            notice = "Added " + id;
            noticeGood = true;
            scroll = Math.max(0, rules().size() - visibleRows());
            rebuild();
        }

        private void removeRule(int index) {
            if (index >= 0 && index < rules().size()) rules().remove(index);
            scroll = Math.max(0, Math.min(scroll, rules().size() - 1));
            notice = "Removed perception rule.";
            noticeGood = true;
            rebuild();
        }

        private int visibleRows() {
            return Math.max(3, (panelHeight() - 140) / 46);
        }

        private void rebuild() {
            clearWidgets();
            init();
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            int max = Math.max(0, rules().size() - visibleRows());
            int next = Math.max(0, Math.min(max,
                    scroll + (delta < 0 ? 1 : -1)));
            if (next == scroll) return super.mouseScrolled(mouseX, mouseY, delta);
            scroll = next;
            rebuild();
            return true;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY,
                float partialTick) {
            ConfigCenterVisuals.renderBackdrop(this, graphics, mouseX, mouseY);
            drawPanel(graphics, "Mob Perception Rules",
                    "Special sensory behavior by entity type. Changes save with the stealth page.");
            drawEditBox(graphics, addId);
            int x = panelX() + 16;
            int y = panelY() + 96;
            int width = panelWidth() - 32;
            JsonArray rules = rules();
            int end = Math.min(rules.size(), scroll + visibleRows());
            for (int index = scroll; index < end; index++) {
                JsonObject rule = rules.get(index).isJsonObject()
                        ? rules.get(index).getAsJsonObject() : new JsonObject();
                int row = index - scroll;
                int rowY = y + row * 46;
                graphics.fill(x, rowY, x + width, rowY + 40,
                        row % 2 == 0 ? 0xD20B0E12 : 0xD20D1628);
                graphics.fill(x, rowY, x + 4, rowY + 40, ACCENT);
                String id = rule.has("entity")
                        ? rule.get("entity").getAsString() : "<invalid>";
                graphics.drawString(font, ScpFonts.roboto(id),
                        x + 12, rowY + 7, TEXT, false);
                graphics.drawString(font, ScpFonts.roboto(traits(rule)),
                        x + 12, rowY + 22, MUTED, false);
            }
            if (rules.size() == 0) {
                graphics.drawCenteredString(font,
                        ScpFonts.roboto("No entity-specific rules. All mobs use the defaults."),
                        panelX() + panelWidth() / 2, y + 32, MUTED);
            }
            if (!notice.isBlank()) {
                graphics.drawString(font, ScpFonts.roboto(notice), x,
                        panelY() + panelHeight() - 25,
                        noticeGood ? GOOD : BAD, false);
            }
            super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void onClose() {
            Minecraft.getInstance().setScreen(parent);
        }
    }

    private static final class PerceptionRuleScreen extends StealthScreen {
        private final PerceptionRulesScreen parent;
        private final JsonObject rule;
        private EditBox visibility;
        private EditBox range;
        private EditBox delay;
        private String notice = "";

        private PerceptionRuleScreen(PerceptionRulesScreen parent,
                JsonObject rule) {
            super("Perception Rule");
            this.parent = parent;
            this.rule = rule;
        }

        @Override
        protected void init() {
            int x = panelX() + 18;
            int y = panelY() + 72;
            int inner = panelWidth() - 36;
            int third = (inner - 16) / 3;

            button(x, y, third, 24,
                    "Omniscient: " + onOff("omniscient"),
                    bool(rule, "omniscient", false), false,
                    () -> toggle("omniscient"));
            button(x + third + 8, y, third, 24,
                    "Blind: " + onOff("blind"),
                    bool(rule, "blind", false), false,
                    () -> toggle("blind"));
            button(x + (third + 8) * 2, y, third, 24,
                    "Night Vision: " + onOff("night_vision"),
                    bool(rule, "night_vision", false), false,
                    () -> toggle("night_vision"));

            y += 64;
            visibility = edit(x, y + 14, third,
                    compactNumber(number(rule, "visibility_multiplier", 1.0D)),
                    "Visibility multiplier");
            range = edit(x + third + 8, y + 14, third,
                    compactNumber(number(rule, "range_multiplier", 1.0D)),
                    "Range multiplier");
            delay = edit(x + (third + 8) * 2, y + 14, third,
                    compactNumber(number(rule, "acquire_delay_multiplier", 1.0D)),
                    "Delay multiplier");

            int footer = panelY() + panelHeight() - 32;
            button(x + inner - 198, footer, 96, 20,
                    "Apply", true, false, this::applyAndBack);
            button(x + inner - 94, footer, 94, 20,
                    "Back", false, false,
                    () -> Minecraft.getInstance().setScreen(parent));
        }

        private String onOff(String key) {
            return bool(rule, key, false) ? "ON" : "OFF";
        }

        private void toggle(String key) {
            boolean next = !bool(rule, key, false);
            rule.addProperty(key, next);
            // Omniscience and blindness are mutually exclusive sensory models.
            if (next && "omniscient".equals(key)) rule.addProperty("blind", false);
            if (next && "blind".equals(key)) rule.addProperty("omniscient", false);
            clearWidgets();
            init();
        }

        private void applyAndBack() {
            try {
                rule.addProperty("visibility_multiplier",
                        bounded(visibility.getValue(), 0.0D, 4.0D));
                rule.addProperty("range_multiplier",
                        bounded(range.getValue(), 0.0D, 4.0D));
                rule.addProperty("acquire_delay_multiplier",
                        bounded(delay.getValue(), 0.0D, 4.0D));
                Minecraft.getInstance().setScreen(parent);
            } catch (NumberFormatException exception) {
                notice = "Multipliers must be numbers from 0 to 4.";
            }
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY,
                float partialTick) {
            ConfigCenterVisuals.renderBackdrop(this, graphics, mouseX, mouseY);
            String id = rule.has("entity") ? rule.get("entity").getAsString() : "Unknown entity";
            drawPanel(graphics, "Perception Rule", id);
            int x = panelX() + 18;
            int y = panelY() + 131;
            int inner = panelWidth() - 36;
            int third = (inner - 16) / 3;
            graphics.drawString(font, ScpFonts.roboto("Visibility multiplier"),
                    x, y, MUTED, false);
            graphics.drawString(font, ScpFonts.roboto("Detection range multiplier"),
                    x + third + 8, y, MUTED, false);
            graphics.drawString(font, ScpFonts.roboto("Acquire delay multiplier"),
                    x + (third + 8) * 2, y, MUTED, false);
            for (EditBox box : List.of(visibility, range, delay)) {
                if (box != null) drawEditBox(graphics, box);
            }
            graphics.drawString(font,
                    ScpFonts.roboto("Omniscient ignores hiding. Blind disables visual acquisition. Night Vision ignores light penalties."),
                    x, y + 66, MUTED, false);
            if (!notice.isBlank()) {
                graphics.drawString(font, ScpFonts.roboto(notice), x,
                        panelY() + panelHeight() - 54, BAD, false);
            }
            super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void onClose() {
            Minecraft.getInstance().setScreen(parent);
        }
    }

    private static String traits(JsonObject rule) {
        List<String> traits = new ArrayList<>();
        if (bool(rule, "omniscient", false)) traits.add("OMNISCIENT");
        if (bool(rule, "blind", false)) traits.add("BLIND");
        if (bool(rule, "night_vision", false)) traits.add("NIGHT VISION");
        if (traits.isEmpty()) traits.add("STANDARD VISION");
        return String.join(" · ", traits);
    }

    private static double bounded(String raw, double minimum, double maximum) {
        double value = Double.parseDouble(raw.trim());
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            throw new NumberFormatException(raw);
        }
        return value;
    }

    private static String compactNumber(double value) {
        if (Math.rint(value) == value) return Long.toString(Math.round(value));
        return String.format(Locale.ROOT, "%.3f", value)
                .replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
