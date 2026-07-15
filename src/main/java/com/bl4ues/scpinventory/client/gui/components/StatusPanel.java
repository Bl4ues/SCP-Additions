package com.bl4ues.scpinventory.client.gui.components;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.bl4ues.scpinventory.client.StatusEffectTimelineClient;
import com.bl4ues.scpinventory.config.ScpInventoryConfig;
import net.mcreator.scpadditions.init.ScpAdditionsModMobEffects;
import net.mcreator.scpadditions.network.ScpAdditionsModVariables;
import net.mcreator.scpadditions.vitals.client.PlayerVitalsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class StatusPanel {

    private static final int TEXT_WHITE = 0xFFB2B3B3;
    private static final int TEXT_GRAY = 0xFF6A6C6C;
    private static final int TEXT_SELECTED = 0xFF202020;
    private static final int TAB_ACTIVE = 0x55B2B3B3;
    private static final int TAB_INACTIVE = 0x336A6C6C;
    private static final int ROW_BACKGROUND = 0x24303638;
    private static final int PREVIEW_BACKGROUND_INNER = 0x66181E20;
    private static final int LINE_GRAY = 0x446A6C6C;
    private static final int LINE_LIGHT = 0x776A6C6C;
    private static final int BAR_BACKGROUND = 0x55303638;
    private static final int BAR_GOOD = 0xAA6FA07A;
    private static final int BAR_WARN = 0xAAA09A6F;
    private static final int BAR_BAD = 0xAAA06F6F;
    private static final int SCROLL_TRACK = 0x55303638;
    private static final int SCROLL_THUMB = 0xAA6A6C6C;

    private static final int CONDITION_ROW_HEIGHT = 38;
    private static final int CONDITION_ICON_SIZE = 24;
    private static final int CONDITIONS_PAD_X = 18;
    private static final int CONDITIONS_PAD_TOP = 36;
    private static final int TAB_WIDTH = 104;
    private static final int TAB_HEIGHT = 17;
    private static final int TAB_GAP = 12;

    private final Minecraft mc = Minecraft.getInstance();
    private final int conditionsX;
    private final int conditionsY;
    private final int conditionsWidth;
    private final int conditionsHeight;
    private final int parametersX;
    private final int parametersY;
    private final int parametersWidth;
    private final int parametersHeight;
    private final int titleY;

    private int conditionsScroll = 0;
    private boolean conditionsScrollbarDragging = false;
    private ConditionTab conditionTab = ConditionTab.POSITIVE;

    public StatusPanel(int conditionsX, int conditionsY, int conditionsWidth, int conditionsHeight,
                       int parametersX, int parametersY, int parametersWidth, int parametersHeight,
                       int titleY, int conditionsTitleX, int parametersTitleX) {
        this.conditionsX = conditionsX;
        this.conditionsY = conditionsY;
        this.conditionsWidth = conditionsWidth;
        this.conditionsHeight = conditionsHeight;
        this.parametersX = parametersX;
        this.parametersY = parametersY;
        this.parametersWidth = parametersWidth;
        this.parametersHeight = parametersHeight;
        this.titleY = titleY;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        drawSectionTitleCentered(g, conditionsX, conditionsWidth, titleY, "CONDITIONS");
        drawSectionTitleCentered(g, parametersX, parametersWidth, titleY, "PARAMETERS");
        renderConditions(g);
        renderParameters(g, mouseX, mouseY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isMouseOverConditions(mouseX, mouseY) || mc.player == null) return false;
        int totalRows = getVisibleEffects().size();
        int visibleRows = getVisibleConditionRows();
        int maxScroll = Math.max(0, totalRows - visibleRows);
        if (maxScroll <= 0) return false;
        conditionsScroll = Math.max(0, Math.min(maxScroll, conditionsScroll - (int) Math.signum(delta)));
        return true;
    }

    public boolean mouseClickedScrollbar(double mouseX, double mouseY, int button) {
        if (button == 0 && clickConditionTab(mouseX, mouseY)) return true;
        if (button != 0 || !hasScrollableConditions() || !isMouseOverConditionScrollbar(mouseX, mouseY)) return false;
        conditionsScrollbarDragging = true;
        updateConditionsScrollFromMouse(mouseY);
        return true;
    }

    public boolean mouseDraggedScrollbar(double mouseY) {
        if (!conditionsScrollbarDragging) return false;
        updateConditionsScrollFromMouse(mouseY);
        return true;
    }

    public boolean mouseReleasedScrollbar(int button) {
        if (button != 0 || !conditionsScrollbarDragging) return false;
        conditionsScrollbarDragging = false;
        return true;
    }

    public int getConditionsScroll() { return conditionsScroll; }
    public boolean isShowingPositiveConditions() { return conditionTab == ConditionTab.POSITIVE; }

    public void restoreSessionState(int scroll, boolean positive) {
        conditionTab = positive ? ConditionTab.POSITIVE : ConditionTab.NEGATIVE;
        conditionsScroll = Math.max(0, scroll);
    }

    private void renderConditions(GuiGraphics g) {
        renderConditionTabs(g);
        if (mc.player == null) return;

        List<MobEffectInstance> effects = getVisibleEffects();
        int contentX = conditionsX + CONDITIONS_PAD_X;
        int contentY = conditionsY + CONDITIONS_PAD_TOP;
        int contentWidth = conditionsWidth - (CONDITIONS_PAD_X * 2) - 14;
        int visibleRows = getVisibleConditionRows();
        int maxScroll = Math.max(0, effects.size() - visibleRows);
        conditionsScroll = Math.max(0, Math.min(maxScroll, conditionsScroll));

        for (int i = 0; i < visibleRows; i++) {
            int effectIndex = i + conditionsScroll;
            if (effectIndex >= effects.size()) break;
            MobEffectInstance effect = effects.get(effectIndex);
            int rowY = contentY + (i * CONDITION_ROW_HEIGHT);
            renderConditionRow(g, effect, contentX, rowY, contentWidth);
        }

        if (effects.size() > visibleRows) {
            renderConditionScrollbar(g, effects.size(), visibleRows);
        }
    }

    private void renderConditionTabs(GuiGraphics g) {
        int tabX = getConditionTabX();
        int tabY = getConditionTabY();
        drawConditionTab(g, tabX, tabY, "POSITIVE", conditionTab == ConditionTab.POSITIVE);
        drawConditionTab(g, tabX + TAB_WIDTH + TAB_GAP, tabY, "NEGATIVE", conditionTab == ConditionTab.NEGATIVE);
    }

    private int getConditionTabX() {
        int total = (TAB_WIDTH * 2) + TAB_GAP;
        return conditionsX + Math.max(0, (conditionsWidth - total) / 2);
    }

    private int getConditionTabY() {
        return conditionsY + 8;
    }

    private void drawConditionTab(GuiGraphics g, int x, int y, String label, boolean active) {
        g.fill(x, y, x + TAB_WIDTH, y + TAB_HEIGHT, active ? TAB_ACTIVE : TAB_INACTIVE);
        g.drawString(mc.font, ScpFonts.roboto(label), x + (TAB_WIDTH - mc.font.width(ScpFonts.roboto(label))) / 2, y + 5, active ? TEXT_SELECTED : TEXT_WHITE, false);
    }

    private boolean clickConditionTab(double mouseX, double mouseY) {
        int tabX = getConditionTabX();
        int tabY = getConditionTabY();
        if (mouseY < tabY || mouseY > tabY + TAB_HEIGHT) return false;

        if (mouseX >= tabX && mouseX <= tabX + TAB_WIDTH) {
            conditionTab = ConditionTab.POSITIVE;
            conditionsScroll = 0;
            return true;
        }

        int negativeX = tabX + TAB_WIDTH + TAB_GAP;
        if (mouseX >= negativeX && mouseX <= negativeX + TAB_WIDTH) {
            conditionTab = ConditionTab.NEGATIVE;
            conditionsScroll = 0;
            return true;
        }

        return false;
    }

    private List<MobEffectInstance> getVisibleEffects() {
        List<MobEffectInstance> effects = new ArrayList<>();
        if (mc.player == null) return effects;

        for (MobEffectInstance effect : mc.player.getActiveEffects()) {
            if (isHiddenEffect(effect)) continue;
            if (conditionTab == ConditionTab.NEGATIVE && !isNegativeEffect(effect)) continue;
            if (conditionTab == ConditionTab.POSITIVE && isNegativeEffect(effect)) continue;
            effects.add(effect);
        }

        effects.sort(Comparator.comparing(effect -> effect.getEffect().getDisplayName().getString()));
        return effects;
    }

    private boolean isNegativeEffect(MobEffectInstance effect) {
        return effect.getEffect().getCategory() == MobEffectCategory.HARMFUL;
    }

    private boolean isHiddenEffect(MobEffectInstance effect) {
        if (effect.getEffect() == ScpAdditionsModMobEffects.SCP_1176_HONEYED.get()) return true;
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect());
        if (id == null) return false;
        String idString = id.toString();
        if ("minecraft:bad_omen".equalsIgnoreCase(idString)) return true;
        for (String raw : ScpInventoryConfig.HIDDEN_STATUS_EFFECTS.get()) {
            if (raw != null && raw.trim().equalsIgnoreCase(idString)) {
                return true;
            }
        }
        return false;
    }

    private int getVisibleConditionRows() {
        return Math.max(1, (conditionsHeight - CONDITIONS_PAD_TOP - 12) / CONDITION_ROW_HEIGHT);
    }

    private void renderConditionScrollbar(GuiGraphics g, int totalRows, int visibleRows) {
        int trackX = getConditionScrollbarX();
        int trackY = getConditionScrollbarY();
        int trackH = getConditionScrollbarHeight();
        g.fill(trackX, trackY, trackX + 4, trackY + trackH, SCROLL_TRACK);

        int thumbH = getConditionScrollbarThumbHeight(totalRows, visibleRows);
        int thumbY = getConditionScrollbarThumbY(totalRows, visibleRows, thumbH);
        g.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, SCROLL_THUMB);
    }

    private void renderConditionRow(GuiGraphics g, MobEffectInstance effect, int x, int y, int width) {
        g.fill(x, y, x + width, y + CONDITION_ROW_HEIGHT - 5, ROW_BACKGROUND);

        int iconX = x + 8;
        int iconY = y + 5;
        drawIconFrame(g, iconX, iconY);
        renderEffectIcon(g, effect, iconX + 3, iconY + 3);

        String name = effect.getEffect().getDisplayName().getString() + getAmplifierSuffix(effect);
        String duration = formatDuration(effect.getDuration());
        int textX = iconX + CONDITION_ICON_SIZE + 10;
        g.drawString(mc.font, ScpFonts.roboto(name), textX, y + 7, TEXT_WHITE, false);
        g.drawString(mc.font, ScpFonts.roboto(duration), textX, y + 20, TEXT_GRAY, false);

        float ratio = getDurationRatio(effect);
        int barWidth = Math.max(90, Math.min(150, width / 3));
        int barX = x + width - barWidth - 14;
        int barY = y + 19;
        g.fill(barX, barY, barX + barWidth, barY + 3, BAR_BACKGROUND);
        int fill = Math.max(1, Math.min(barWidth, Math.round(barWidth * ratio)));
        g.fill(barX, barY, barX + fill, barY + 3, getConditionBarColor(ratio));
    }

    private void drawIconFrame(GuiGraphics g, int x, int y) {
        int right = x + CONDITION_ICON_SIZE;
        int bottom = y + CONDITION_ICON_SIZE;
        g.fill(x, y, right, bottom, 0x30303638);
        g.fill(x, y, right, y + 1, LINE_GRAY);
        g.fill(x, bottom - 1, right, bottom, LINE_GRAY);
        g.fill(x, y, x + 1, bottom, LINE_GRAY);
        g.fill(right - 1, y, right, bottom, LINE_GRAY);
    }

    private void renderEffectIcon(GuiGraphics g, MobEffectInstance effect, int x, int y) {
        TextureAtlasSprite sprite = mc.getMobEffectTextures().get(effect.getEffect());
        if (sprite == null) return;
        g.blit(x, y, 0, 18, 18, sprite);
    }

    private int getConditionScrollbarX() {
        return conditionsX + conditionsWidth - 8;
    }

    private int getConditionScrollbarY() {
        return conditionsY + CONDITIONS_PAD_TOP;
    }

    private int getConditionScrollbarHeight() {
        return conditionsHeight - CONDITIONS_PAD_TOP - 16;
    }

    private boolean hasScrollableConditions() {
        return getVisibleEffects().size() > getVisibleConditionRows();
    }

    private boolean isMouseOverConditions(double mouseX, double mouseY) {
        return mouseX >= conditionsX && mouseX <= conditionsX + conditionsWidth && mouseY >= conditionsY && mouseY <= conditionsY + conditionsHeight;
    }

    private boolean isMouseOverConditionScrollbar(double mouseX, double mouseY) {
        return mouseX >= getConditionScrollbarX() && mouseX <= getConditionScrollbarX() + 5
                && mouseY >= getConditionScrollbarY() && mouseY <= getConditionScrollbarY() + getConditionScrollbarHeight();
    }

    private void updateConditionsScrollFromMouse(double mouseY) {
        List<MobEffectInstance> effects = getVisibleEffects();
        int visibleRows = getVisibleConditionRows();
        int maxScroll = Math.max(0, effects.size() - visibleRows);
        if (maxScroll <= 0) {
            conditionsScroll = 0;
            return;
        }

        int trackY = getConditionScrollbarY();
        int trackH = getConditionScrollbarHeight();
        double ratio = (mouseY - trackY) / Math.max(1.0D, trackH);
        conditionsScroll = Math.max(0, Math.min(maxScroll, (int) Math.round(ratio * maxScroll)));
    }

    private void drawSectionTitleCentered(GuiGraphics g, int panelX, int panelWidth, int y, String label) {
        Component prefix = ScpFonts.roboto("://STATUS_");
        Component value = ScpFonts.roboto(label);
        int textWidth = mc.font.width(prefix) + mc.font.width(value);
        int x = panelX + Math.max(0, (panelWidth - textWidth) / 2);
        g.drawString(mc.font, prefix, x, y, TEXT_GRAY, false);
        g.drawString(mc.font, value, x + mc.font.width(prefix), y, TEXT_WHITE, false);
    }

    private int getConditionScrollbarThumbHeight(int totalRows, int visibleRows) {
        int trackH = getConditionScrollbarHeight();
        if (totalRows <= 0 || visibleRows <= 0) {
            return trackH;
        }
        float ratio = Math.min(1.0F, visibleRows / (float) totalRows);
        return Math.max(18, Math.min(trackH, Math.round(trackH * ratio)));
    }

    private int getConditionScrollbarThumbY(int totalRows, int visibleRows, int thumbH) {
        int trackY = getConditionScrollbarY();
        int travel = Math.max(0, getConditionScrollbarHeight() - thumbH);
        int maxScroll = Math.max(1, totalRows - visibleRows);
        return trackY + Math.round(travel * (conditionsScroll / (float) maxScroll));
    }

    private float getDurationRatio(MobEffectInstance effect) {
        return StatusEffectTimelineClient.getRemainingRatio(effect);
    }

    private void renderParameters(GuiGraphics g, int mouseX, int mouseY) {
        if (mc.player == null) return;

        int previewX = parametersX + Math.round(parametersWidth * 0.20F);
        int previewY = parametersY + Math.round(parametersHeight * 0.13F);
        int previewW = Math.max(72, Math.min(120, Math.round(parametersWidth * 0.32F)));
        int previewH = Math.max(130, Math.min(190, Math.round(parametersHeight * 0.64F)));

        g.fill(previewX, previewY, previewX + previewW, previewY + previewH, PREVIEW_BACKGROUND_INNER);
        drawFrame(g, previewX, previewY, previewW, previewH);
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, previewX + previewW / 2, previewY + previewH - 18, 54, previewX + previewW / 2 - mouseX, previewY + 32 - mouseY, mc.player);

        int statX = parametersX + Math.round(parametersWidth * 0.64F);
        int statY = previewY + 20;
        renderStat(g, "Max Health", Integer.toString(Math.round(mc.player.getMaxHealth())), statX, statY);
        renderStat(g, "Armor", Integer.toString(mc.player.getArmorValue()), statX, statY + 38);
        renderStat(g, "Toughness", formatAttribute(Attributes.ARMOR_TOUGHNESS), statX, statY + 76);
        renderStat(g, "Attack", formatAttribute(Attributes.ATTACK_DAMAGE), statX, statY + 114);
        renderStat(g, "Stamina", Math.round(PlayerVitalsClient.getStamina()) + "/" + Math.round(PlayerVitalsClient.getMaxStamina()), statX, statY + 152);
        renderStat(g, Component.translatable("status.scp_additions.blood_type").getString(),
                getBloodType(), statX, statY + 190);
    }

    private void drawFrame(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + 1, LINE_GRAY);
        g.fill(x, y + h - 1, x + w, y + h, LINE_GRAY);
        g.fill(x, y, x + 1, y + h, LINE_GRAY);
        g.fill(x + w - 1, y, x + w, y + h, LINE_GRAY);
        int corner = 12;
        g.fill(x, y, x + corner, y + 1, LINE_LIGHT);
        g.fill(x, y, x + 1, y + corner, LINE_LIGHT);
        g.fill(x + w - corner, y, x + w, y + 1, LINE_LIGHT);
        g.fill(x + w - 1, y, x + w, y + corner, LINE_LIGHT);
        g.fill(x, y + h - 1, x + corner, y + h, LINE_LIGHT);
        g.fill(x, y + h - corner, x + 1, y + h, LINE_LIGHT);
        g.fill(x + w - corner, y + h - 1, x + w, y + h, LINE_LIGHT);
        g.fill(x + w - 1, y + h - corner, x + w, y + h, LINE_LIGHT);
    }

    private void renderStat(GuiGraphics g, String label, String value, int x, int y) {
        g.drawString(mc.font, ScpFonts.roboto(label), x, y, TEXT_GRAY, false);
        g.drawString(mc.font, ScpFonts.roboto(value), x, y + 12, TEXT_WHITE, false);
        g.fill(x, y + 31, x + 64, y + 32, LINE_GRAY);
    }

    private String formatAttribute(Attribute attribute) {
        if (mc.player == null) return "0";
        double value = mc.player.getAttributeValue(attribute);
        if (attribute == Attributes.ATTACK_DAMAGE) value = Math.max(0.0D, value - 1.0D);
        return String.format(Locale.ROOT, value % 1.0D == 0.0D ? "%.0f" : "%.1f", value);
    }

    private String getBloodType() {
        String unknown = Component.translatable("status.scp_additions.blood_type_unknown").getString();
        if (mc.player == null) return unknown;
        return mc.player.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY)
                .resolve()
                .map(variables -> {
                    if (variables.Oneg) return "O-";
                    if (variables.Opos) return "O+";
                    if (variables.Aneg) return "A-";
                    if (variables.Apos) return "A+";
                    if (variables.Bneg) return "B-";
                    if (variables.Bpos) return "B+";
                    if (variables.ABneg) return "AB-";
                    if (variables.ABpos) return "AB+";
                    return unknown;
                })
                .orElse(unknown);
    }

    private String getAmplifierSuffix(MobEffectInstance effect) {
        int amp = effect.getAmplifier();
        return amp <= 0 ? "" : " " + toRoman(amp + 1);
    }

    private String formatDuration(int ticks) {
        if (ticks < 0 || ticks > 20 * 60 * 60 * 24) return "∞";
        int seconds = ticks / 20;
        if (seconds >= 3600) return (seconds / 3600) + "h";
        if (seconds >= 60) return (seconds / 60) + "m";
        return seconds + "s";
    }

    private String toRoman(int number) {
        if (number <= 1) return "II";
        if (number == 2) return "III";
        if (number == 3) return "IV";
        if (number == 4) return "V";
        return Integer.toString(number + 1);
    }

    private int getConditionBarColor(float ratio) {
        if (ratio <= 0.25F) return BAR_BAD;
        if (ratio <= 0.55F) return BAR_WARN;
        return BAR_GOOD;
    }

    private enum ConditionTab { POSITIVE, NEGATIVE }
}
