package com.bl4ues.scpclassifieddirective.config.ui;

import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;
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
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Native editor for entity-specific rules used by the stealth framework. */
public final class StealthConfigCenterEnhancements {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting().disableHtmlEscaping().create();

    private StealthConfigCenterEnhancements() {
    }

    /** Opens only the authored per-mob perception rules. Core stealth values are integrated. */
    public static void openPerceptionEditor(Screen parent, JsonObject modules) {
        Minecraft.getInstance().setScreen(new PerceptionRulesScreen(parent,
                modules == null ? new JsonObject() : modules));
    }

    private static void submitModules(JsonObject modules) {
        JsonObject changes = new JsonObject();
        changes.add(ConfigCenterService.MODULES, modules);
        ModNetwork.CHANNEL.sendToServer(new ConfigCenterNetwork.SaveRequest(
                GSON.toJson(changes)));
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (!parent.has(key) || !parent.get(key).isJsonObject()) {
            parent.add(key, new JsonObject());
        }
        return parent.getAsJsonObject(key);
    }

    private static boolean bool(JsonObject object, String key,
            boolean fallback) {
        if (object == null || !object.has(key)) return fallback;
        try {
            return object.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double number(JsonObject object, String key,
            double fallback) {
        if (object == null || !object.has(key)) return fallback;
        try {
            return object.get(key).getAsDouble();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String ruleId(JsonObject rule) {
        if (rule == null || !rule.has("entity")) return "";
        try {
            return rule.get("entity").getAsString().trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static boolean integratedRule(JsonObject rule) {
        return ScpClassifiedDirectiveModulesConfig.isIntegratedPerceptionEntity(
                ruleId(rule));
    }

    private abstract static class StealthScreen extends Screen {
        protected static final int ROW = 0xFF081022;
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
            CenteredEditBox box = new CenteredEditBox(font, x, y, width, 20,
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

        protected static final class CenteredEditBox extends EditBox {
            private CenteredEditBox(Font font, int x, int y, int width,
                    int height, Component narration) {
                super(font, x, y, width, height, narration);
            }

            @Override
            public void renderWidget(GuiGraphics graphics, int mouseX,
                    int mouseY, float partialTick) {
                graphics.pose().pushPose();
                graphics.pose().translate(0.0F, 6.0F, 0.0F);
                super.renderWidget(graphics, mouseX, mouseY - 6, partialTick);
                graphics.pose().popPose();
            }
        }
    }

    private static final class PerceptionRulesScreen extends StealthScreen {
        private static final int ROW_HEIGHT = 58;

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
            if (!stealth.has("enabled")) stealth.addProperty("enabled", true);
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
            int y = panelY() + 61;
            int inner = panelWidth() - 32;
            addId = edit(x, y, inner - 112, "", "namespace:entity_id");
            button(x + inner - 104, y - 2, 104, 24,
                    "+ Add Mob", true, false, this::addRule);
            addRuleButtons(x, y + 36, inner);

            int footer = panelY() + panelHeight() - 30;
            button(x, footer, 116, 20, "Save & Reload", true, false,
                    this::save);
            button(x + inner - 100, footer, 100, 20, "Back", false, false,
                    () -> Minecraft.getInstance().setScreen(parent));
        }

        private void addRuleButtons(int x, int y, int width) {
            JsonArray rules = rules();
            int visible = visibleRows();
            scroll = Math.min(scroll, Math.max(0, rules.size() - visible));
            int end = Math.min(rules.size(), scroll + visible);
            for (int index = scroll; index < end; index++) {
                JsonObject rule = rules.get(index).isJsonObject()
                        ? rules.get(index).getAsJsonObject() : new JsonObject();
                if (integratedRule(rule)) continue;
                int rowY = y + (index - scroll) * ROW_HEIGHT + 12;
                final int ruleIndex = index;
                button(x + width - 126, rowY, 76, 24,
                        "Edit", true, false,
                        () -> Minecraft.getInstance().setScreen(
                                new PerceptionRuleScreen(this, rule)));
                button(x + width - 44, rowY, 44, 24,
                        "X", false, true, () -> removeRule(ruleIndex));
            }
        }

        private void save() {
            notice = "Saving perception rules...";
            noticeGood = true;
            submitModules(modules);
        }

        private void addRule() {
            String raw = addId.getValue().trim().toLowerCase(Locale.ROOT);
            ResourceLocation id = ResourceLocation.tryParse(raw);
            if (id == null || !ForgeRegistries.ENTITY_TYPES.containsKey(id)) {
                notice = "Unknown entity id: " + raw;
                noticeGood = false;
                return;
            }
            if (ScpClassifiedDirectiveModulesConfig
                    .isIntegratedPerceptionEntity(raw)) {
                notice = "That SCP uses an integrated perception profile.";
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
            notice = "Added " + entityDisplayName(id.toString());
            noticeGood = true;
            scroll = Math.max(0, rules().size() - visibleRows());
            rebuild();
        }

        private void removeRule(int index) {
            if (index < 0 || index >= rules().size()) return;
            JsonElement element = rules().get(index);
            if (element.isJsonObject()
                    && integratedRule(element.getAsJsonObject())) {
                notice = "Integrated SCP perception profiles cannot be removed.";
                noticeGood = false;
                return;
            }
            rules().remove(index);
            scroll = Math.max(0, Math.min(scroll, Math.max(0, rules().size() - 1)));
            notice = "Removed perception rule.";
            noticeGood = true;
            rebuild();
        }

        private int visibleRows() {
            return Math.max(3, (panelHeight() - 150) / ROW_HEIGHT);
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
                    "Integrated SCP profiles are shown for reference; other mob rules remain editable.");
            drawEditBox(graphics, addId);
            int x = panelX() + 16;
            int y = panelY() + 97;
            int width = panelWidth() - 32;
            JsonArray rules = rules();
            int end = Math.min(rules.size(), scroll + visibleRows());
            for (int index = scroll; index < end; index++) {
                JsonObject rule = rules.get(index).isJsonObject()
                        ? rules.get(index).getAsJsonObject() : new JsonObject();
                int row = index - scroll;
                int rowY = y + row * ROW_HEIGHT;
                graphics.fill(x, rowY, x + width, rowY + 52,
                        row % 2 == 0 ? 0xD20B0E12 : 0xD20D1628);
                graphics.fill(x, rowY, x + 4, rowY + 52, ACCENT);
                String id = rule.has("entity")
                        ? rule.get("entity").getAsString() : "<invalid>";
                String display = entityDisplayName(id);
                graphics.drawString(font, ScpFonts.roboto(compact(display, 48)),
                        x + 12, rowY + 6, TEXT, false);
                graphics.drawString(font, ScpFonts.roboto(compact(id, 52)),
                        x + 12, rowY + 21, MUTED, false);
                graphics.drawString(font, ScpFonts.roboto(traits(rule)),
                        x + 12, rowY + 36, ACCENT, false);
                if (integratedRule(rule)) {
                    Component label = ScpFonts.roboto("INTEGRATED");
                    graphics.drawString(font, label,
                            x + width - 12 - font.width(label), rowY + 21,
                            ACCENT, false);
                }
            }
            if (rules.size() == 0) {
                graphics.drawCenteredString(font,
                        ScpFonts.roboto("No entity-specific rules. All mobs use the integrated defaults."),
                        panelX() + panelWidth() / 2, y + 32, MUTED);
            }
            if (!notice.isBlank()) {
                graphics.drawString(font, ScpFonts.roboto(notice), x,
                        panelY() + panelHeight() - 48,
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
            String id = rule.has("entity")
                    ? rule.get("entity").getAsString() : "Unknown entity";
            drawPanel(graphics, "Perception Rule",
                    entityDisplayName(id) + "  ·  " + id);
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
                    ScpFonts.roboto("Omniscient ignores hiding. Blind uses acoustic acquisition. Night Vision ignores light penalties."),
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

    private static String entityDisplayName(String rawId) {
        ResourceLocation id = ResourceLocation.tryParse(rawId == null ? "" : rawId);
        if (id == null) return rawId == null || rawId.isBlank() ? "Unknown Entity" : rawId;
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (type == null) return id.toString();
        String translated = type.getDescription().getString();
        return translated == null || translated.isBlank() ? id.toString() : translated;
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

    private static String compact(String value, int maximum) {
        if (value == null) return "";
        return value.length() <= maximum ? value
                : value.substring(0, Math.max(0, maximum - 3)) + "...";
    }
}
