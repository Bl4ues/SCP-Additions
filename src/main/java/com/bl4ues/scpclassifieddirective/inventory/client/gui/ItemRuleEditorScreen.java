package com.bl4ues.scpclassifieddirective.inventory.client.gui;

import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpConsumableType;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpItemType;
import com.bl4ues.scpclassifieddirective.inventory.network.ItemConfigDeletePacket;
import com.bl4ues.scpclassifieddirective.inventory.network.ItemConfigOpenPacket;
import com.bl4ues.scpclassifieddirective.inventory.network.ItemConfigSavePacket;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ItemRuleEditorScreen extends Screen {
    private static final int PANEL_W = 310;
    private static final int BASE_PANEL_H = 228;
    private static final int CONSUMABLE_PANEL_H = 276;
    private static final int MARGIN = 10;

    private static final int NAVY = 0xF000071F;
    private static final int NAVY_LIGHT = 0xE6141E42;
    private static final int CONTROL = 0xFF111A31;
    private static final int CONTROL_HOVER = 0xFF192744;
    private static final int BORDER = 0xFF46536C;
    private static final int BORDER_HOVER = 0xFF73809A;
    private static final int ACCENT = 0xFFC59A2A;
    private static final int ACCENT_TEXT = 0xFFE5D49A;
    private static final int WHITE = 0xFFF7F8FC;
    private static final int MUTED = 0xFFA9AFBA;
    private static final int SECTION = 0xFFD3D9E4;
    private static final int DANGER = 0xFFD46060;

    private static final ScpItemType[] TYPES =
            Arrays.stream(ScpItemType.values())
                    .filter(type -> type != ScpItemType.CODEX)
                    .toArray(ScpItemType[]::new);

    private final String itemId;
    private final boolean existing;
    private ScpItemType type;
    private ScpConsumableType consumableType;
    private final EnumSet<EquipmentEffect> effects =
            EnumSet.noneOf(EquipmentEffect.class);

    private SingleDropdown<ScpItemType> categoryDropdown;
    private SingleDropdown<ScpConsumableType> consumableTypeDropdown;
    private EffectMultiSelect effectDropdown;
    private ExpandableSelector openSelector;
    private EditorButton forgetButton;
    private boolean confirmForget;

    public ItemRuleEditorScreen(ItemConfigOpenPacket packet) {
        super(ScpFonts.roboto("SCP Item Configuration"));
        this.itemId = packet.itemId();
        this.existing = packet.existing();
        this.type = parseType(packet.type());
        this.consumableType = ScpConsumableType
                .fromConfigToken(packet.consumableType())
                .orElse(ScpConsumableType.FOOD);
        if (packet.noStamina()) effects.add(EquipmentEffect.NO_STAMINA);
        if (packet.protectedEyes()) {
            effects.add(EquipmentEffect.PROTECTED_EYES);
        }
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        int x = left + 16;
        int width = PANEL_W - 32;

        openSelector = null;

        categoryDropdown = addRenderableWidget(new SingleDropdown<>(
                x, top + 91, width, 22,
                List.of(TYPES), type,
                value -> ScpFonts.roboto(value.getEditorDisplayName()),
                value -> {
                    boolean layoutChanged = (type == ScpItemType.CONSUMABLE)
                            != (value == ScpItemType.CONSUMABLE);
                    type = value;
                    if (layoutChanged) {
                        Minecraft.getInstance().execute(this::rebuildWidgets);
                    }
                }));

        int effectY = top + 139;
        if (type == ScpItemType.CONSUMABLE) {
            consumableTypeDropdown = addRenderableWidget(new SingleDropdown<>(
                    x, top + 139, width, 22,
                    List.of(ScpConsumableType.values()), consumableType,
                    value -> ScpFonts.roboto(value.displayName()),
                    value -> consumableType = value));
            effectY = top + 187;
        } else {
            consumableTypeDropdown = null;
        }

        effectDropdown = addRenderableWidget(new EffectMultiSelect(
                x, effectY, width, 22));

        int bottomY = top + panelHeight() - 34;
        forgetButton = addRenderableWidget(new EditorButton(
                x, bottomY, 76, 22, "Forget",
                ButtonStyle.DANGER, this::forgetRule));
        addRenderableWidget(new EditorButton(
                left + PANEL_W - 172, bottomY, 76, 22,
                "Save", ButtonStyle.PRIMARY, this::save));
        addRenderableWidget(new EditorButton(
                left + PANEL_W - 90, bottomY, 74, 22,
                "Cancel", ButtonStyle.NEUTRAL, this::onClose));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (openSelector != null) {
            if (openSelector.handleExpandedClick(mouseX, mouseY, button)) {
                return true;
            }
            if (!openSelector.isMouseOver(mouseX, mouseY)) {
                openSelector.setOpen(false);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
            double delta) {
        if (openSelector != null
                && openSelector.handleExpandedScroll(
                        mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        renderBackground(graphics);
        int left = panelLeft();
        int top = panelTop();

        int panelHeight = panelHeight();
        graphics.fill(left, top, left + PANEL_W, top + panelHeight, NAVY);
        graphics.fill(left, top, left + PANEL_W, top + 31, NAVY_LIGHT);
        graphics.fill(left, top + 30, left + PANEL_W, top + 31, ACCENT);
        outline(graphics, left, top, PANEL_W, panelHeight, BORDER);

        graphics.drawString(font,
                ScpFonts.roboto("SCP ITEM CONFIGURATION"),
                left + 14, top + 11, WHITE, false);

        ItemStack preview = getPreviewStack();
        int iconX = left + 16;
        int iconY = top + 44;
        graphics.fill(iconX - 3, iconY - 3,
                iconX + 19, iconY + 19, NAVY_LIGHT);
        outline(graphics, iconX - 3, iconY - 3,
                22, 22, BORDER);
        graphics.fill(iconX - 3, iconY - 3,
                iconX + 4, iconY - 2, ACCENT);
        if (!preview.isEmpty()) {
            graphics.renderItem(preview, iconX, iconY);
        }

        graphics.drawString(font,
                ScpFonts.roboto(existing
                        ? "Editing explicit rule"
                        : "Creating explicit rule"),
                left + 46, top + 44, MUTED, false);
        graphics.drawString(font,
                ScpFonts.roboto(compact(itemId, 42)),
                left + 46, top + 57, WHITE, false);

        graphics.drawString(font, ScpFonts.roboto("CATEGORY"),
                left + 16, top + 78, SECTION, false);
        if (type == ScpItemType.CONSUMABLE) {
            graphics.drawString(font,
                    ScpFonts.roboto("CONSUMABLE TYPE"),
                    left + 16, top + 126, SECTION, false);
            graphics.drawString(font,
                    ScpFonts.roboto("EQUIPMENT EFFECTS"),
                    left + 16, top + 174, SECTION, false);
        } else {
            graphics.drawString(font,
                    ScpFonts.roboto("EQUIPMENT EFFECTS"),
                    left + 16, top + 126, SECTION, false);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void save() {
        ModNetwork.CHANNEL.sendToServer(new ItemConfigSavePacket(
                itemId, type.name(), consumableType.name(),
                effects.contains(EquipmentEffect.NO_STAMINA),
                effects.contains(EquipmentEffect.PROTECTED_EYES)));
        Minecraft.getInstance().setScreen(null);
    }

    private void forgetRule() {
        if (!confirmForget) {
            confirmForget = true;
            if (forgetButton != null) {
                forgetButton.setMessage(ScpFonts.roboto("Confirm"));
            }
            return;
        }
        ModNetwork.CHANNEL.sendToServer(
                new ItemConfigDeletePacket(itemId));
        Minecraft.getInstance().setScreen(null);
    }

    private ItemStack getPreviewStack() {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) return ItemStack.EMPTY;
        return BuiltInRegistries.ITEM.getOptional(id)
                .map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    private int panelLeft() {
        return Math.max(MARGIN, width - PANEL_W - MARGIN);
    }

    private int panelHeight() {
        return type == ScpItemType.CONSUMABLE
                ? CONSUMABLE_PANEL_H : BASE_PANEL_H;
    }

    private int panelTop() {
        return Math.max(MARGIN, (height - panelHeight()) / 2);
    }

    private static ScpItemType parseType(String value) {
        try {
            ScpItemType parsed = ScpItemType.valueOf(
                    value == null ? "MISCELLANEOUS"
                            : value.trim().toUpperCase());
            return parsed == ScpItemType.CODEX
                    ? ScpItemType.MISCELLANEOUS : parsed;
        } catch (Exception ignored) {
            return ScpItemType.MISCELLANEOUS;
        }
    }

    private static String compact(String text, int max) {
        if (text == null || text.length() <= max) {
            return text == null ? "" : text;
        }
        return text.substring(0, Math.max(0, max - 3)) + "...";
    }

    private static void outline(GuiGraphics graphics, int x, int y,
            int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1,
                x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y,
                x + width, y + height, color);
    }

    private interface ExpandableSelector {
        void setOpen(boolean open);

        boolean handleExpandedClick(double mouseX, double mouseY,
                int button);

        boolean handleExpandedScroll(double mouseX, double mouseY,
                double delta);

        boolean isMouseOver(double mouseX, double mouseY);
    }

    private enum ButtonStyle {
        PRIMARY,
        NEUTRAL,
        DANGER
    }

    private enum EquipmentEffect {
        NO_STAMINA("No Stamina"),
        PROTECTED_EYES("Protected Eyes");

        private final String displayName;

        EquipmentEffect(String displayName) {
            this.displayName = displayName;
        }

        private String displayName() {
            return displayName;
        }
    }

    private class EditorButton extends AbstractButton {
        private final ButtonStyle style;
        private final Runnable action;

        private EditorButton(int x, int y, int width, int height,
                String label, ButtonStyle style, Runnable action) {
            super(x, y, width, height, ScpFonts.roboto(label));
            this.style = style;
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void updateWidgetNarration(
                NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            int background = !active ? 0xFF171B25
                    : isHoveredOrFocused() ? CONTROL_HOVER : CONTROL;
            int edge = style == ButtonStyle.DANGER ? DANGER
                    : style == ButtonStyle.PRIMARY ? ACCENT
                    : isHoveredOrFocused() ? BORDER_HOVER : BORDER;
            int text = !active ? MUTED
                    : style == ButtonStyle.PRIMARY ? ACCENT_TEXT : WHITE;
            graphics.fill(getX(), getY(),
                    getX() + getWidth(), getY() + getHeight(),
                    background);
            outline(graphics, getX(), getY(),
                    getWidth(), getHeight(), edge);
            if (style != ButtonStyle.NEUTRAL) {
                graphics.fill(getX() + 1, getY() + 1,
                        getX() + 4, getY() + getHeight() - 1, edge);
            }
            graphics.drawCenteredString(font,
                    ScpFonts.roboto(getMessage()),
                    getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2, text);
        }
    }

    private final class SingleDropdown<T> extends AbstractButton
            implements ExpandableSelector {
        private static final int ROW_HEIGHT = 22;
        private static final int MAX_VISIBLE_ROWS = 8;

        private final List<T> values;
        private final Function<T, Component> labelFunction;
        private final Consumer<T> onChange;
        private T value;
        private boolean open;
        private int scrollOffset;

        private SingleDropdown(int x, int y, int width, int height,
                List<T> values, T initialValue,
                Function<T, Component> labelFunction,
                Consumer<T> onChange) {
            super(x, y, width, height,
                    labelFunction.apply(initialValue));
            this.values = List.copyOf(values);
            this.value = initialValue;
            this.labelFunction = labelFunction;
            this.onChange = onChange;
        }

        @Override
        public void onPress() {
            setOpen(!open);
        }

        @Override
        public void setOpen(boolean shouldOpen) {
            if (open == shouldOpen) return;
            if (shouldOpen) {
                if (openSelector != null && openSelector != this) {
                    openSelector.setOpen(false);
                }
                openSelector = this;
                ensureSelectionVisible();
            } else if (openSelector == this) {
                openSelector = null;
            }
            open = shouldOpen;
        }

        @Override
        protected void updateWidgetNarration(
                NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics,
                int mouseX, int mouseY, float partialTick) {
            drawSelectorFrame(graphics, this, open);
            String label = labelFunction.apply(value).getString();
            String clipped = font.plainSubstrByWidth(label,
                    Math.max(1, getWidth() - 30));
            graphics.drawString(font, ScpFonts.roboto(clipped),
                    getX() + 8, getY() + 7, WHITE, false);
            graphics.drawCenteredString(font,
                    ScpFonts.roboto(open ? "▲" : "▼"),
                    getX() + getWidth() - 12, getY() + 7, MUTED);
            if (open) renderExpanded(graphics, mouseX, mouseY);
        }

        private void renderExpanded(GuiGraphics graphics,
                int mouseX, int mouseY) {
            int top = listTop();
            int visible = visibleRows();
            int height = visible * ROW_HEIGHT + 2;
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 500.0F);
            graphics.fill(getX(), top, getX() + getWidth(),
                    top + height, 0xFF080D1C);
            outline(graphics, getX(), top,
                    getWidth(), height, ACCENT);
            for (int row = 0; row < visible; row++) {
                int index = scrollOffset + row;
                if (index >= values.size()) break;
                T option = values.get(index);
                int rowY = top + 1 + row * ROW_HEIGHT;
                boolean hovered = mouseX >= getX()
                        && mouseX < getX() + getWidth()
                        && mouseY >= rowY
                        && mouseY < rowY + ROW_HEIGHT;
                boolean selected = Objects.equals(option, value);
                graphics.fill(getX() + 1, rowY,
                        getX() + getWidth() - 1,
                        rowY + ROW_HEIGHT,
                        selected ? 0xFF4B3F27
                                : hovered ? CONTROL_HOVER : CONTROL);
                graphics.drawString(font,
                        labelFunction.apply(option),
                        getX() + 8, rowY + 7,
                        selected ? ACCENT_TEXT : WHITE, false);
            }
            graphics.pose().popPose();
        }

        @Override
        public boolean handleExpandedClick(double mouseX,
                double mouseY, int button) {
            if (!open || button != 0) return false;
            int top = listTop();
            int visible = visibleRows();
            if (mouseX >= getX() && mouseX < getX() + getWidth()
                    && mouseY >= top
                    && mouseY < top + visible * ROW_HEIGHT + 2) {
                int row = (int) ((mouseY - top - 1) / ROW_HEIGHT);
                int index = scrollOffset + row;
                if (row >= 0 && row < visible
                        && index < values.size()) {
                    value = values.get(index);
                    setMessage(labelFunction.apply(value));
                    onChange.accept(value);
                }
                setOpen(false);
                return true;
            }
            return false;
        }

        @Override
        public boolean handleExpandedScroll(double mouseX,
                double mouseY, double delta) {
            if (!open || values.size() <= visibleRows()) return false;
            int top = listTop();
            int height = visibleRows() * ROW_HEIGHT + 2;
            if (mouseX < getX()
                    || mouseX >= getX() + getWidth()
                    || mouseY < top || mouseY >= top + height) {
                return false;
            }
            scrollOffset = Math.max(0, Math.min(maxScroll(),
                    scrollOffset + (delta > 0.0D ? -1 : 1)));
            return true;
        }

        private int visibleRows() {
            return Math.min(MAX_VISIBLE_ROWS, values.size());
        }

        private int maxScroll() {
            return Math.max(0, values.size() - visibleRows());
        }

        private int listTop() {
            int height = visibleRows() * ROW_HEIGHT + 2;
            int below = getY() + getHeight() + 2;
            return below + height <= ItemRuleEditorScreen.this.height - 6
                    ? below : getY() - height - 2;
        }

        private void ensureSelectionVisible() {
            int selected = Math.max(0, values.indexOf(value));
            if (selected < scrollOffset) scrollOffset = selected;
            if (selected >= scrollOffset + visibleRows()) {
                scrollOffset = selected - visibleRows() + 1;
            }
            scrollOffset = Math.max(0,
                    Math.min(maxScroll(), scrollOffset));
        }
    }

    private final class EffectMultiSelect extends AbstractButton
            implements ExpandableSelector {
        private static final int ROW_HEIGHT = 24;
        private boolean open;

        private EffectMultiSelect(int x, int y, int width, int height) {
            super(x, y, width, height,
                    ScpFonts.roboto(effectSummary()));
        }

        @Override
        public void onPress() {
            setOpen(!open);
        }

        @Override
        public void setOpen(boolean shouldOpen) {
            if (open == shouldOpen) return;
            if (shouldOpen) {
                if (openSelector != null && openSelector != this) {
                    openSelector.setOpen(false);
                }
                openSelector = this;
            } else if (openSelector == this) {
                openSelector = null;
            }
            open = shouldOpen;
        }

        @Override
        protected void updateWidgetNarration(
                NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics,
                int mouseX, int mouseY, float partialTick) {
            drawSelectorFrame(graphics, this, open);
            String clipped = font.plainSubstrByWidth(
                    effectSummary(), Math.max(1, getWidth() - 30));
            graphics.drawString(font, ScpFonts.roboto(clipped),
                    getX() + 8, getY() + 7, WHITE, false);
            graphics.drawCenteredString(font,
                    ScpFonts.roboto(open ? "▲" : "▼"),
                    getX() + getWidth() - 12, getY() + 7, MUTED);
            if (open) renderExpanded(graphics, mouseX, mouseY);
        }

        private void renderExpanded(GuiGraphics graphics,
                int mouseX, int mouseY) {
            int top = listTop();
            int height = EquipmentEffect.values().length
                    * ROW_HEIGHT + 2;
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 500.0F);
            graphics.fill(getX(), top,
                    getX() + getWidth(), top + height, 0xFF080D1C);
            outline(graphics, getX(), top,
                    getWidth(), height, ACCENT);
            EquipmentEffect[] options = EquipmentEffect.values();
            for (int index = 0; index < options.length; index++) {
                EquipmentEffect option = options[index];
                int rowY = top + 1 + index * ROW_HEIGHT;
                boolean hovered = mouseX >= getX()
                        && mouseX < getX() + getWidth()
                        && mouseY >= rowY
                        && mouseY < rowY + ROW_HEIGHT;
                boolean selected = effects.contains(option);
                graphics.fill(getX() + 1, rowY,
                        getX() + getWidth() - 1,
                        rowY + ROW_HEIGHT,
                        hovered ? CONTROL_HOVER : CONTROL);
                graphics.drawString(font,
                        ScpFonts.roboto(selected ? "✓" : "□"),
                        getX() + 8, rowY + 8,
                        selected ? ACCENT_TEXT : MUTED, false);
                graphics.drawString(font,
                        ScpFonts.roboto(option.displayName()),
                        getX() + 27, rowY + 8,
                        selected ? ACCENT_TEXT : WHITE, false);
            }
            graphics.pose().popPose();
        }

        @Override
        public boolean handleExpandedClick(double mouseX,
                double mouseY, int button) {
            if (!open || button != 0) return false;
            int top = listTop();
            int height = EquipmentEffect.values().length
                    * ROW_HEIGHT + 2;
            if (mouseX >= getX() && mouseX < getX() + getWidth()
                    && mouseY >= top && mouseY < top + height) {
                int row = (int) ((mouseY - top - 1) / ROW_HEIGHT);
                EquipmentEffect[] options = EquipmentEffect.values();
                if (row >= 0 && row < options.length) {
                    EquipmentEffect option = options[row];
                    if (!effects.remove(option)) effects.add(option);
                    setMessage(ScpFonts.roboto(effectSummary()));
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean handleExpandedScroll(double mouseX,
                double mouseY, double delta) {
            return false;
        }

        private int listTop() {
            int height = EquipmentEffect.values().length
                    * ROW_HEIGHT + 2;
            int below = getY() + getHeight() + 2;
            return below + height <= ItemRuleEditorScreen.this.height - 6
                    ? below : getY() - height - 2;
        }
    }

    private String effectSummary() {
        if (effects.isEmpty()) return "No equipment effects";
        if (effects.size() == 1) {
            return effects.iterator().next().displayName();
        }
        return "No Stamina, Protected Eyes";
    }

    private void drawSelectorFrame(GuiGraphics graphics,
            AbstractButton button, boolean open) {
        int background = button.isHoveredOrFocused() || open
                ? CONTROL_HOVER : CONTROL;
        int edge = open ? ACCENT
                : button.isHoveredOrFocused()
                        ? BORDER_HOVER : BORDER;
        graphics.fill(button.getX(), button.getY(),
                button.getX() + button.getWidth(),
                button.getY() + button.getHeight(), background);
        outline(graphics, button.getX(), button.getY(),
                button.getWidth(), button.getHeight(), edge);
    }
}
