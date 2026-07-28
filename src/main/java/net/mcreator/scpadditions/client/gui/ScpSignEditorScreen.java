package net.mcreator.scpadditions.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.client.ScpFonts;
import net.mcreator.scpadditions.facility.ScpSignData;
import net.mcreator.scpadditions.facility.ScpSignHazards;
import net.mcreator.scpadditions.network.ScpSignSavePacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/** Styled form editor and live 1024x640 preview for the SCP Sign Support. */
public final class ScpSignEditorScreen extends Screen {
    private static final ResourceLocation BASE = new ResourceLocation(
            ScpAdditionsMod.MODID,
            "textures/screens/scpsign/scp_sign_base.png");
    private static final int IMAGE_WIDTH = 1024;
    private static final int IMAGE_HEIGHT = 640;

    private static final int PANEL_BACKGROUND = 0xF01B2024;
    private static final int PANEL_EDGE = 0xFF657078;
    private static final int FIELD_BACKGROUND = 0xFF13181C;
    private static final int FIELD_EDGE = 0xFF4B555C;
    private static final int CONTROL_BACKGROUND = 0xFF343D43;
    private static final int CONTROL_HOVER = 0xFF56636B;
    private static final int CONTROL_EDGE = 0xFF667178;
    private static final int ACCENT = 0xFFC59A2A;
    private static final int ACCENT_TEXT = 0xFFE5D49A;
    private static final int TEXT_PRIMARY = 0xFFE4E8EA;
    private static final int TEXT_MUTED = 0xFF879097;
    private static final int PREVIEW_TEXT = 0xFF000000;

    private static final ImageArea CLEARANCE =
            new ImageArea(783, 83, 57, 40);
    private static final ImageArea SCP_NUMBER =
            new ImageArea(64, 273, 355, 48);
    private static final ImageArea CONTAINMENT =
            new ImageArea(65, 351, 354, 27);
    private static final ImageArea ANOMALY =
            new ImageArea(589, 298, 235, 15);
    private static final ImageArea[] TRAITS = {
            new ImageArea(473, 375, 167, 164),
            new ImageArea(622, 375, 166, 164),
            new ImageArea(771, 375, 167, 164)
    };

    private final BlockPos signPos;
    private final ScpSignData initialData;
    private final List<StyledDropdown<ScpSignHazards.Option>> traitDropdowns =
            new ArrayList<>();

    private EditBox scpNumberField;
    private StyledDropdown<ScpSignData.ContainmentClass> containmentDropdown;
    private EditBox customContainmentField;
    private StyledDropdown<Integer> clearanceDropdown;
    private StyledDropdown<ScpSignData.AnomalyType> anomalyDropdown;
    private EditBox customAnomalyField;
    private StyledDropdown<?> openDropdown;

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int previewLeft;
    private int previewTop;
    private float previewScale;

    private ScpSignEditorScreen(BlockPos signPos, ScpSignData data) {
        super(Component.translatable("screen.scp_additions.scp_sign_editor"));
        this.signPos = signPos.immutable();
        this.initialData = data == null ? ScpSignData.DEFAULT : data;
    }

    public static void open(BlockPos signPos, ScpSignData data) {
        Minecraft.getInstance().setScreen(
                new ScpSignEditorScreen(signPos, data));
    }

    @Override
    protected void init() {
        calculateLayout();
        traitDropdowns.clear();
        openDropdown = null;

        int formX = panelLeft + 18;
        int fieldX = formX + 124;
        int fieldWidth = 220;
        int y = panelTop + 48;

        scpNumberField = configureField(new EditBox(font, fieldX, y,
                fieldWidth, 20, Component.translatable(
                "screen.scp_additions.scp_sign_number")));
        scpNumberField.setMaxLength(ScpSignData.MAX_SCP_NUMBER_LENGTH);
        scpNumberField.setFilter(value -> value.length()
                <= ScpSignData.MAX_SCP_NUMBER_LENGTH
                && value.chars().allMatch(character -> character >= '0'
                        && character <= '9'));
        scpNumberField.setValue(initialData.scpNumber());
        addRenderableWidget(scpNumberField);
        y += 27;

        containmentDropdown = addRenderableWidget(new StyledDropdown<>(
                fieldX, y, fieldWidth, 20,
                List.of(ScpSignData.ContainmentClass.values()),
                initialData.containmentClass(),
                value -> Component.literal(value.displayName()),
                value -> updateCustomVisibility(), null));
        y += 27;

        customContainmentField = configureField(new EditBox(font, fieldX, y,
                fieldWidth, 20, Component.translatable(
                "screen.scp_additions.scp_sign_custom_containment")));
        customContainmentField.setMaxLength(
                ScpSignData.MAX_CONTAINMENT_CLASS_LENGTH);
        customContainmentField.setValue(initialData.customContainmentClass());
        addRenderableWidget(customContainmentField);
        y += 27;

        clearanceDropdown = addRenderableWidget(new StyledDropdown<>(
                fieldX, y, fieldWidth, 20, List.of(1, 2, 3, 4, 5, 6),
                initialData.clearanceLevel(),
                value -> Component.literal(String.format("%02d", value)),
                value -> {
                }, null));
        y += 27;

        anomalyDropdown = addRenderableWidget(new StyledDropdown<>(
                fieldX, y, fieldWidth, 20,
                List.of(ScpSignData.AnomalyType.values()),
                initialData.anomalyType(),
                value -> Component.literal(value.displayName()),
                value -> updateCustomVisibility(), null));
        y += 27;

        customAnomalyField = configureField(new EditBox(font, fieldX, y,
                fieldWidth, 20, Component.translatable(
                "screen.scp_additions.scp_sign_custom_anomaly")));
        customAnomalyField.setMaxLength(ScpSignData.MAX_ANOMALY_TYPE_LENGTH);
        customAnomalyField.setValue(initialData.customAnomalyType());
        addRenderableWidget(customAnomalyField);
        y += 31;

        for (int slot = 0; slot < ScpSignData.HAZARD_SLOTS; slot++) {
            final int selectedSlot = slot;
            ScpSignHazards.Option initial = ScpSignHazards.option(
                    initialData.hazards().get(slot));
            StyledDropdown<ScpSignHazards.Option> dropdown =
                    new StyledDropdown<>(fieldX, y, fieldWidth, 20,
                            ScpSignHazards.OPTIONS, initial,
                            option -> Component.literal(option.displayName()),
                            value -> selectTrait(selectedSlot, value),
                            ScpSignHazards.Option::texture);
            traitDropdowns.add(addRenderableWidget(dropdown));
            y += 27;
        }

        int bottomY = panelTop + panelHeight - 29;
        addRenderableWidget(new EditorButton(panelLeft + panelWidth - 178,
                bottomY, 78, 20, Component.translatable("gui.done"),
                ButtonStyle.PRIMARY, this::saveAndClose));
        addRenderableWidget(new EditorButton(panelLeft + panelWidth - 94,
                bottomY, 78, 20, Component.translatable("gui.cancel"),
                ButtonStyle.NEUTRAL, this::onClose));

        updateCustomVisibility();
    }

    private EditBox configureField(EditBox field) {
        field.setBordered(false);
        field.setTextColor(TEXT_PRIMARY);
        field.setTextColorUneditable(TEXT_MUTED);
        return field;
    }

    private void calculateLayout() {
        panelWidth = Math.min(980, Math.max(680, width - 20));
        panelHeight = Math.min(470, Math.max(410, height - 20));
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;

        int availableWidth = panelWidth - 382;
        int availableHeight = panelHeight - 76;
        previewScale = Math.min(availableWidth / (float) IMAGE_WIDTH,
                availableHeight / (float) IMAGE_HEIGHT);
        previewScale = Math.max(0.15F, previewScale);
        previewLeft = panelLeft + 366
                + Math.max(0, (availableWidth
                - Math.round(IMAGE_WIDTH * previewScale)) / 2);
        previewTop = panelTop + 40
                + Math.max(0, (availableHeight
                - Math.round(IMAGE_HEIGHT * previewScale)) / 2);
    }

    private void updateCustomVisibility() {
        if (customContainmentField != null && containmentDropdown != null) {
            customContainmentField.visible = containmentDropdown.getValue()
                    == ScpSignData.ContainmentClass.CUSTOM;
        }
        if (customAnomalyField != null && anomalyDropdown != null) {
            customAnomalyField.visible = anomalyDropdown.getValue()
                    == ScpSignData.AnomalyType.CUSTOM;
        }
    }

    private void selectTrait(int selectedSlot,
            ScpSignHazards.Option selected) {
        if (selected == null || selected.isNone()) return;
        for (int slot = 0; slot < traitDropdowns.size(); slot++) {
            if (slot != selectedSlot
                    && traitDropdowns.get(slot).getValue().id()
                    .equals(selected.id())) {
                traitDropdowns.get(slot).setValue(
                        ScpSignHazards.NONE, false);
            }
        }
    }

    private ScpSignData currentData() {
        List<String> traits = traitDropdowns.stream()
                .map(dropdown -> dropdown.getValue().id()).toList();
        return new ScpSignData(scpNumberField.getValue(),
                containmentDropdown.getValue(),
                customContainmentField.getValue(),
                clearanceDropdown.getValue(), anomalyDropdown.getValue(),
                customAnomalyField.getValue(), traits);
    }

    private void saveAndClose() {
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                new ScpSignSavePacket(signPos, currentData()));
        onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (openDropdown != null) {
            if (openDropdown.handleExpandedClick(mouseX, mouseY, button)) {
                return true;
            }
            if (!openDropdown.isMouseOver(mouseX, mouseY)) {
                openDropdown.setOpen(false);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (openDropdown != null
                && openDropdown.handleExpandedScroll(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        renderBackground(graphics);
        drawPanel(graphics);
        drawFormLabels(graphics);
        drawFieldFrames(graphics);
        drawPreview(graphics, currentData());
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPanel(GuiGraphics graphics) {
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth,
                panelTop + panelHeight, PANEL_BACKGROUND);
        outline(graphics, panelLeft, panelTop, panelWidth, panelHeight,
                PANEL_EDGE);
        for (int x = panelLeft + 6; x < panelLeft + panelWidth - 4; x += 8) {
            for (int y = panelTop + 6; y < panelTop + panelHeight - 4; y += 8) {
                graphics.fill(x, y, x + 1, y + 1, 0x242F383E);
            }
        }
        graphics.drawString(font, ScpFonts.montserrat(Component.translatable(
                        "screen.scp_additions.scp_sign_editor")),
                panelLeft + 18, panelTop + 14, TEXT_PRIMARY, false);
    }

    private void drawFieldFrames(GuiGraphics graphics) {
        drawField(graphics, scpNumberField);
        if (customContainmentField.visible) {
            drawField(graphics, customContainmentField);
        }
        if (customAnomalyField.visible) {
            drawField(graphics, customAnomalyField);
        }
    }

    private static void drawField(GuiGraphics graphics, EditBox field) {
        graphics.fill(field.getX() - 3, field.getY() - 1,
                field.getX() + field.getWidth() + 3,
                field.getY() + field.getHeight() + 1, FIELD_BACKGROUND);
        outline(graphics, field.getX() - 3, field.getY() - 1,
                field.getWidth() + 6, field.getHeight() + 2, FIELD_EDGE);
    }

    private void drawFormLabels(GuiGraphics graphics) {
        int x = panelLeft + 18;
        int y = panelTop + 54;
        Component[] labels = {
                Component.translatable("screen.scp_additions.scp_sign_number"),
                Component.translatable("screen.scp_additions.scp_sign_containment"),
                Component.translatable("screen.scp_additions.scp_sign_custom_containment"),
                Component.translatable("screen.scp_additions.scp_sign_clearance"),
                Component.translatable("screen.scp_additions.scp_sign_anomaly"),
                Component.translatable("screen.scp_additions.scp_sign_custom_anomaly"),
                Component.literal("Anomaly Trait 1"),
                Component.literal("Anomaly Trait 2"),
                Component.literal("Anomaly Trait 3")
        };
        int[] gaps = {27, 27, 27, 27, 27, 31, 27, 27, 27};
        for (int index = 0; index < labels.length; index++) {
            boolean customHidden = index == 2 && !customContainmentField.visible
                    || index == 5 && !customAnomalyField.visible;
            if (!customHidden) {
                graphics.drawString(font, ScpFonts.roboto(labels[index]),
                        x, y, index >= 6 ? TEXT_PRIMARY : TEXT_MUTED, false);
            }
            y += gaps[index];
        }
    }

    private void drawPreview(GuiGraphics graphics, ScpSignData data) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.pose().pushPose();
        graphics.pose().translate(previewLeft, previewTop, 0.0F);
        graphics.pose().scale(previewScale, previewScale, 1.0F);
        graphics.blit(BASE, 0, 0, 0.0F, 0.0F,
                IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_WIDTH, IMAGE_HEIGHT);

        for (int slot = 0; slot < ScpSignData.HAZARD_SLOTS; slot++) {
            ScpSignHazards.Option option = ScpSignHazards.option(
                    data.hazards().get(slot));
            ResourceLocation texture = option.texture();
            if (!resourceExists(texture)) {
                texture = ScpSignHazards.NONE.texture();
            }
            if (resourceExists(texture)) {
                drawTraitImage(graphics, texture, TRAITS[slot]);
            }
        }

        drawPreviewText(graphics,
                String.format("%02d", data.clearanceLevel()), CLEARANCE, true);
        drawPreviewText(graphics, data.scpLabel(), SCP_NUMBER, false);
        drawPreviewText(graphics, data.containmentLabel(), CONTAINMENT, false);
        drawPreviewText(graphics, data.anomalyLabel(), ANOMALY, true);
        graphics.pose().popPose();
        RenderSystem.disableBlend();
    }

    private void drawPreviewText(GuiGraphics graphics, String value,
            ImageArea area, boolean centered) {
        Component component = ScpFonts.kokoro(value)
                .withStyle(ChatFormatting.BOLD);
        int textWidth = Math.max(1, font.width(component));
        float scale = Math.min(area.width() / (float) textWidth,
                area.height() / 9.0F);
        float x = centered
                ? area.x() + (area.width() - textWidth * scale) * 0.5F
                : area.x();

        graphics.pose().pushPose();
        graphics.pose().translate(x, area.y(), 2.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, component, 0, 0, PREVIEW_TEXT, false);
        graphics.pose().popPose();
    }

    private static void drawTraitImage(GuiGraphics graphics,
            ResourceLocation texture, ImageArea area) {
        graphics.pose().pushPose();
        graphics.pose().translate(area.x(), area.y(), 1.0F);
        graphics.pose().scale(area.width() / 256.0F,
                area.height() / 256.0F, 1.0F);
        graphics.blit(texture, 0, 0, 0.0F, 0.0F,
                256, 256, 256, 256);
        graphics.pose().popPose();
    }

    private static boolean resourceExists(ResourceLocation texture) {
        return texture != null && Minecraft.getInstance().getResourceManager()
                .getResource(texture).isPresent();
    }

    private static void outline(GuiGraphics graphics, int x, int y,
            int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private final class EditorButton extends AbstractButton {
        private final ButtonStyle style;
        private final Runnable action;

        private EditorButton(int x, int y, int width, int height,
                Component label, ButtonStyle style, Runnable action) {
            super(x, y, width, height, label);
            this.style = style;
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            int background = !active ? 0xFF22282D
                    : isHoveredOrFocused() ? CONTROL_HOVER : CONTROL_BACKGROUND;
            int edge = style == ButtonStyle.PRIMARY
                    ? isHoveredOrFocused() ? ACCENT_TEXT : ACCENT
                    : isHoveredOrFocused() ? 0xFFD8E0E4 : CONTROL_EDGE;
            int text = !active ? TEXT_MUTED
                    : style == ButtonStyle.PRIMARY ? ACCENT_TEXT : TEXT_PRIMARY;
            graphics.fill(getX(), getY(), getX() + getWidth(),
                    getY() + getHeight(), background);
            outline(graphics, getX(), getY(), getWidth(), getHeight(), edge);
            if (style == ButtonStyle.PRIMARY) {
                graphics.fill(getX() + 1, getY() + 1, getX() + 4,
                        getY() + getHeight() - 1, edge);
            }
            graphics.drawCenteredString(font, ScpFonts.roboto(getMessage()),
                    getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2, text);
        }
    }

    private final class StyledDropdown<T> extends AbstractButton {
        private static final int ROW_HEIGHT = 26;
        private static final int MAX_VISIBLE_ROWS = 8;
        private static final int ICON_SIZE = 22;

        private final List<T> values;
        private final Function<T, Component> labelFunction;
        private final Consumer<T> onChange;
        private final Function<T, ResourceLocation> iconFunction;
        private T value;
        private boolean open;
        private int scrollOffset;

        private StyledDropdown(int x, int y, int width, int height,
                List<T> values, T initialValue,
                Function<T, Component> labelFunction,
                Consumer<T> onChange,
                Function<T, ResourceLocation> iconFunction) {
            super(x, y, width, height, labelFunction.apply(initialValue));
            this.values = List.copyOf(values);
            this.labelFunction = labelFunction;
            this.onChange = onChange;
            this.iconFunction = iconFunction;
            this.value = initialValue;
        }

        private T getValue() {
            return value;
        }

        private void setValue(T newValue, boolean notify) {
            if (newValue == null || Objects.equals(value, newValue)) return;
            value = newValue;
            setMessage(labelFunction.apply(newValue));
            if (notify) onChange.accept(newValue);
        }

        private void setOpen(boolean shouldOpen) {
            if (open == shouldOpen) return;
            if (shouldOpen) {
                if (openDropdown != null && openDropdown != this) {
                    openDropdown.setOpen(false);
                }
                openDropdown = this;
                ensureSelectionVisible();
            } else if (openDropdown == this) {
                openDropdown = null;
            }
            open = shouldOpen;
        }

        @Override
        public void onPress() {
            setOpen(!open);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            int background = isHoveredOrFocused() || open
                    ? CONTROL_HOVER : CONTROL_BACKGROUND;
            int edge = open ? ACCENT
                    : isHoveredOrFocused() ? 0xFFD8E0E4 : CONTROL_EDGE;
            graphics.fill(getX(), getY(), getX() + getWidth(),
                    getY() + getHeight(), background);
            outline(graphics, getX(), getY(), getWidth(), getHeight(), edge);

            int textX = getX() + 7;
            ResourceLocation icon = iconFor(value);
            if (icon != null) {
                drawSmallIcon(graphics, icon, getX() + 3,
                        getY() + (getHeight() - 18) / 2, 18);
                textX += 20;
            }
            String label = labelFunction.apply(value).getString();
            int available = getX() + getWidth() - 22 - textX;
            String clipped = font.plainSubstrByWidth(label,
                    Math.max(1, available));
            graphics.drawString(font, ScpFonts.roboto(clipped), textX,
                    getY() + (getHeight() - 8) / 2, TEXT_PRIMARY, false);
            graphics.drawCenteredString(font,
                    ScpFonts.roboto(open ? "▲" : "▼"),
                    getX() + getWidth() - 11,
                    getY() + (getHeight() - 8) / 2, TEXT_MUTED);

            if (open) renderExpanded(graphics, mouseX, mouseY);
        }

        private void renderExpanded(GuiGraphics graphics, int mouseX,
                int mouseY) {
            int top = listTop();
            int visible = visibleRows();
            int listHeight = visible * ROW_HEIGHT + 2;
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 300.0F);
            graphics.fill(getX(), top, getX() + getWidth(),
                    top + listHeight, 0xFF171C20);
            outline(graphics, getX(), top, getWidth(), listHeight, ACCENT);

            for (int row = 0; row < visible; row++) {
                int index = scrollOffset + row;
                if (index >= values.size()) break;
                T option = values.get(index);
                int rowY = top + 1 + row * ROW_HEIGHT;
                boolean hovered = mouseX >= getX()
                        && mouseX < getX() + getWidth()
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                boolean selected = Objects.equals(option, value);
                int rowColor = selected ? 0xFF4B3F27
                        : hovered ? 0xFF3D484F : 0xFF242B30;
                graphics.fill(getX() + 1, rowY,
                        getX() + getWidth() - 1,
                        rowY + ROW_HEIGHT, rowColor);

                int textX = getX() + 7;
                ResourceLocation icon = iconFor(option);
                if (icon != null) {
                    drawSmallIcon(graphics, icon, getX() + 2,
                            rowY + 2, ICON_SIZE);
                    textX += ICON_SIZE + 1;
                }
                String label = labelFunction.apply(option).getString();
                int reserve = values.size() > visible ? 10 : 3;
                String clipped = font.plainSubstrByWidth(label,
                        Math.max(1, getX() + getWidth() - reserve - textX));
                graphics.drawString(font, ScpFonts.roboto(clipped), textX,
                        rowY + (ROW_HEIGHT - 8) / 2,
                        selected ? ACCENT_TEXT : TEXT_PRIMARY, false);
            }

            if (values.size() > visible) {
                int trackX = getX() + getWidth() - 5;
                int trackTop = top + 3;
                int trackHeight = listHeight - 6;
                graphics.fill(trackX, trackTop, trackX + 2,
                        trackTop + trackHeight, 0xFF30383E);
                int thumbHeight = Math.max(8,
                        trackHeight * visible / values.size());
                int maxScroll = Math.max(1, values.size() - visible);
                int thumbY = trackTop + (trackHeight - thumbHeight)
                        * scrollOffset / maxScroll;
                graphics.fill(trackX, thumbY, trackX + 2,
                        thumbY + thumbHeight, ACCENT);
            }
            graphics.pose().popPose();
        }

        private boolean handleExpandedClick(double mouseX, double mouseY,
                int button) {
            if (!open || button != 0) return false;
            int top = listTop();
            int visible = visibleRows();
            if (mouseX >= getX() && mouseX < getX() + getWidth()
                    && mouseY >= top
                    && mouseY < top + visible * ROW_HEIGHT + 2) {
                int row = (int) ((mouseY - top - 1) / ROW_HEIGHT);
                if (row >= 0 && row < visible) {
                    int index = scrollOffset + row;
                    if (index < values.size()) {
                        setValue(values.get(index), true);
                    }
                }
                setOpen(false);
                return true;
            }
            return false;
        }

        private boolean handleExpandedScroll(double mouseX, double mouseY,
                double delta) {
            if (!open || values.size() <= visibleRows()) return false;
            int top = listTop();
            int height = visibleRows() * ROW_HEIGHT + 2;
            if (mouseX < getX() || mouseX >= getX() + getWidth()
                    || mouseY < top || mouseY >= top + height) {
                return false;
            }
            int direction = delta > 0.0D ? -1 : 1;
            scrollOffset = Math.max(0, Math.min(maxScroll(),
                    scrollOffset + direction));
            return true;
        }

        private ResourceLocation iconFor(T option) {
            if (iconFunction == null || option == null) return null;
            ResourceLocation icon = iconFunction.apply(option);
            return resourceExists(icon) ? icon : null;
        }

        private int visibleRows() {
            return Math.min(MAX_VISIBLE_ROWS, values.size());
        }

        private int maxScroll() {
            return Math.max(0, values.size() - visibleRows());
        }

        private int listTop() {
            int listHeight = visibleRows() * ROW_HEIGHT + 2;
            int below = getY() + getHeight() + 2;
            return below + listHeight <= height - 6
                    ? below : getY() - listHeight - 2;
        }

        private void ensureSelectionVisible() {
            int selected = Math.max(0, values.indexOf(value));
            if (selected < scrollOffset) scrollOffset = selected;
            if (selected >= scrollOffset + visibleRows()) {
                scrollOffset = selected - visibleRows() + 1;
            }
            scrollOffset = Math.max(0, Math.min(maxScroll(), scrollOffset));
        }

        private void drawSmallIcon(GuiGraphics graphics,
                ResourceLocation texture, int x, int y, int size) {
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 1.0F);
            graphics.pose().scale(size / 256.0F,
                    size / 256.0F, 1.0F);
            graphics.blit(texture, 0, 0, 0.0F, 0.0F,
                    256, 256, 256, 256);
            graphics.pose().popPose();
        }
    }

    private enum ButtonStyle {
        NEUTRAL,
        PRIMARY
    }

    private record ImageArea(int x, int y, int width, int height) {
    }
}
