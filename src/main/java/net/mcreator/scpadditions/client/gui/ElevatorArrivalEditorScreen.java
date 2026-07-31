package net.mcreator.scpadditions.client.gui;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.facility.elevator.ElevatorArrivalDisplayData;
import net.mcreator.scpadditions.network.ElevatorArrivalSavePacket;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/** Screwdriver editor for one floor station's optional arrival title. */
public final class ElevatorArrivalEditorScreen extends Screen {
    private static final int PANEL_W = 340;
    private static final int PANEL_H = 270;
    private static final int MARGIN = 10;

    private static final int NAVY = 0xF000071F;
    private static final int NAVY_LIGHT = 0xE6141E42;
    private static final int FIELD = 0xFF080D1C;
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

    private final BlockPos stationPos;
    private final boolean configured;
    private final String initialCustomZone;
    private final int initialFloorNumber;
    private ElevatorArrivalDisplayData.Zone zone;
    private ElevatorArrivalDisplayData.FloorType floorType;

    private EditBox customZoneField;
    private EditBox floorNumberField;
    private Selector<ElevatorArrivalDisplayData.Zone> zoneDropdown;
    private Selector<ElevatorArrivalDisplayData.FloorType> floorDropdown;
    private ExpandableSelector openSelector;
    private EditorButton deleteButton;

    private ElevatorArrivalEditorScreen(BlockPos stationPos,
            ElevatorArrivalDisplayData data) {
        super(ScpFonts.roboto("Elevator Arrival Display"));
        this.stationPos = stationPos.immutable();
        ElevatorArrivalDisplayData initial = data != null && data.enabled()
                ? data : ElevatorArrivalDisplayData.EDITOR_DEFAULT;
        this.configured = data != null && data.enabled();
        this.initialCustomZone = initial.customZone();
        this.initialFloorNumber = initial.floorNumber();
        this.zone = initial.zone();
        this.floorType = initial.floorType();
    }

    public static void open(BlockPos stationPos,
            ElevatorArrivalDisplayData data) {
        Minecraft.getInstance().setScreen(
                new ElevatorArrivalEditorScreen(stationPos, data));
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        int x = left + 16;
        int width = PANEL_W - 32;
        ElevatorArrivalDisplayData initial = initialData();
        openSelector = null;

        zoneDropdown = addRenderableWidget(new Selector<>(x, top + 91,
                width, 22, List.of(ElevatorArrivalDisplayData.Zone.values()),
                zone, value -> ScpFonts.roboto(value.displayName()), value -> {
                    zone = value;
                    if (zone == ElevatorArrivalDisplayData.Zone.CUSTOM
                            && customZoneField.getValue().isBlank()) {
                        customZoneField.setValue("Custom Zone");
                    }
                    updateCustomVisibility();
                }));

        customZoneField = configureField(new CenteredEditBox(font, x, top + 126,
                width, 20, ScpFonts.roboto("Custom sector name")));
        customZoneField.setMaxLength(
                ElevatorArrivalDisplayData.MAX_CUSTOM_ZONE_LENGTH);
        customZoneField.setValue(initial.customZone());
        addRenderableWidget(customZoneField);

        int floorY = top + 176;
        floorDropdown = addRenderableWidget(new Selector<>(x, floorY,
                176, 22,
                List.of(ElevatorArrivalDisplayData.FloorType.values()),
                floorType,
                value -> ScpFonts.roboto(value.displayName()),
                value -> floorType = value));

        floorNumberField = configureField(new CenteredEditBox(font, x + 186,
                floorY + 1, width - 186, 20,
                ScpFonts.roboto("Floor number")));
        floorNumberField.setMaxLength(3);
        floorNumberField.setFilter(value -> value.isEmpty()
                || value.chars().allMatch(Character::isDigit));
        floorNumberField.setValue(Integer.toString(initial.floorNumber()));
        addRenderableWidget(floorNumberField);

        int bottomY = top + PANEL_H - 34;
        deleteButton = addRenderableWidget(new EditorButton(x, bottomY,
                82, 22, "Delete", ButtonStyle.DANGER, this::delete));
        deleteButton.active = configured;
        addRenderableWidget(new EditorButton(left + PANEL_W - 182,
                bottomY, 82, 22, "Save", ButtonStyle.PRIMARY, this::save));
        addRenderableWidget(new EditorButton(left + PANEL_W - 92,
                bottomY, 76, 22, "Cancel", ButtonStyle.NEUTRAL,
                this::onClose));

        updateCustomVisibility();
    }

    private ElevatorArrivalDisplayData initialData() {
        return new ElevatorArrivalDisplayData(true, zone,
                initialCustomZone, floorType, initialFloorNumber);
    }

    private EditBox configureField(EditBox field) {
        field.setBordered(false);
        field.setTextColor(WHITE);
        field.setTextColorUneditable(MUTED);
        field.setFormatter((value, cursor) ->
                ScpFonts.roboto(value).getVisualOrderText());
        return field;
    }

    private void updateCustomVisibility() {
        if (customZoneField == null) return;
        boolean custom = zone == ElevatorArrivalDisplayData.Zone.CUSTOM;
        customZoneField.visible = custom;
        customZoneField.active = custom;
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
                && openSelector.handleExpandedScroll(mouseX, mouseY, delta)) {
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

        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, NAVY);
        graphics.fill(left, top, left + PANEL_W, top + 31, NAVY_LIGHT);
        graphics.fill(left, top + 30, left + PANEL_W, top + 31, ACCENT);
        outline(graphics, left, top, PANEL_W, PANEL_H, BORDER);

        graphics.drawString(font, ScpFonts.roboto("ELEVATOR ARRIVAL DISPLAY"),
                left + 16, top + 11, WHITE, false);
        graphics.drawString(font, ScpFonts.roboto(configured
                        ? "Editing this floor station" : "No display configured"),
                left + 16, top + 44, MUTED, false);
        graphics.drawString(font, ScpFonts.roboto(
                        stationPos.getX() + ", " + stationPos.getY()
                                + ", " + stationPos.getZ()),
                left + 16, top + 57, ACCENT_TEXT, false);

        graphics.drawString(font, ScpFonts.roboto("SECTOR"),
                left + 16, top + 78, SECTION, false);
        if (customZoneField.visible) {
            graphics.drawString(font, ScpFonts.roboto("Custom name"),
                    left + 16, top + 116, MUTED, false);
            drawField(graphics, customZoneField);
        }

        graphics.drawString(font, ScpFonts.roboto("FLOOR"),
                left + 16, top + 158, SECTION, false);
        graphics.drawString(font, ScpFonts.roboto("Type"),
                left + 16, top + 168, MUTED, false);
        graphics.drawString(font, ScpFonts.roboto("Number"),
                left + 202, top + 168, MUTED, false);
        drawField(graphics, floorNumberField);

        graphics.drawString(font, ScpFonts.roboto(
                        "The title plays only when this station is configured."),
                left + 16, top + 211, MUTED, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static void drawField(GuiGraphics graphics, EditBox field) {
        graphics.fill(field.getX() - 3, field.getY() - 2,
                field.getX() + field.getWidth() + 3,
                field.getY() + field.getHeight() + 2, FIELD);
        outline(graphics, field.getX() - 3, field.getY() - 2,
                field.getWidth() + 6, field.getHeight() + 4, BORDER);
    }

    private void save() {
        String custom = zone == ElevatorArrivalDisplayData.Zone.CUSTOM
                ? customZoneField.getValue() : "";
        if (zone == ElevatorArrivalDisplayData.Zone.CUSTOM
                && custom.isBlank()) {
            custom = "Custom Zone";
        }
        ElevatorArrivalDisplayData data = new ElevatorArrivalDisplayData(
                true, zone, custom, floorType, parseFloorNumber());
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                new ElevatorArrivalSavePacket(stationPos, data));
        onClose();
    }

    private void delete() {
        ScpAdditionsMod.PACKET_HANDLER.sendToServer(
                new ElevatorArrivalSavePacket(stationPos,
                        ElevatorArrivalDisplayData.NONE));
        onClose();
    }

    private int parseFloorNumber() {
        try {
            return Math.max(0, Math.min(
                    ElevatorArrivalDisplayData.MAX_FLOOR_NUMBER,
                    Integer.parseInt(floorNumberField.getValue())));
        } catch (Exception ignored) {
            return 1;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int panelLeft() {
        return Math.max(MARGIN, width - PANEL_W - MARGIN);
    }

    private int panelTop() {
        return Math.max(MARGIN, (height - PANEL_H) / 2);
    }

    private static void outline(GuiGraphics graphics, int x, int y,
            int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static final class CenteredEditBox extends EditBox {
        private CenteredEditBox(Font font, int x, int y, int width,
                int height, Component message) {
            super(font, x, y, width, height, message);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            int offset = Math.max(0, (getHeight() - 9) / 2);
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, offset, 0.0F);
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            graphics.pose().popPose();
        }
    }

    private interface ExpandableSelector {
        void setOpen(boolean open);

        boolean handleExpandedClick(double mouseX, double mouseY, int button);

        boolean handleExpandedScroll(double mouseX, double mouseY,
                double delta);

        boolean isMouseOver(double mouseX, double mouseY);
    }

    private enum ButtonStyle {
        PRIMARY,
        NEUTRAL,
        DANGER
    }

    private final class EditorButton extends AbstractButton {
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
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            int background = !active ? 0xFF161B27
                    : isHoveredOrFocused() ? CONTROL_HOVER : CONTROL;
            int edge = style == ButtonStyle.DANGER ? DANGER
                    : style == ButtonStyle.PRIMARY ? ACCENT
                    : isHoveredOrFocused() ? BORDER_HOVER : BORDER;
            int text = !active ? MUTED
                    : style == ButtonStyle.PRIMARY ? ACCENT_TEXT : WHITE;
            graphics.fill(getX(), getY(), getX() + getWidth(),
                    getY() + getHeight(), background);
            outline(graphics, getX(), getY(), getWidth(), getHeight(), edge);
            if (style != ButtonStyle.NEUTRAL) {
                graphics.fill(getX() + 1, getY() + 1, getX() + 4,
                        getY() + getHeight() - 1, edge);
            }
            graphics.drawCenteredString(font, getMessage(),
                    getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2, text);
        }
    }

    private final class Selector<T> extends AbstractButton
            implements ExpandableSelector {
        private static final int ROW_HEIGHT = 22;
        private static final int MAX_VISIBLE_ROWS = 6;

        private final List<T> values;
        private final Function<T, Component> labels;
        private final Consumer<T> onChange;
        private T value;
        private boolean open;
        private int scrollOffset;

        private Selector(int x, int y, int width, int height,
                List<T> values, T initial,
                Function<T, Component> labels, Consumer<T> onChange) {
            super(x, y, width, height, labels.apply(initial));
            this.values = List.copyOf(values);
            this.value = initial;
            this.labels = labels;
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
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            int background = isHoveredOrFocused() || open
                    ? CONTROL_HOVER : CONTROL;
            int edge = open ? ACCENT
                    : isHoveredOrFocused() ? BORDER_HOVER : BORDER;
            graphics.fill(getX(), getY(), getX() + getWidth(),
                    getY() + getHeight(), background);
            outline(graphics, getX(), getY(), getWidth(), getHeight(), edge);
            String text = font.plainSubstrByWidth(labels.apply(value).getString(),
                    Math.max(1, getWidth() - 30));
            graphics.drawString(font, ScpFonts.roboto(text), getX() + 8,
                    getY() + 7, WHITE, false);
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
                    top + height, FIELD);
            outline(graphics, getX(), top, getWidth(), height, ACCENT);
            for (int row = 0; row < visible; row++) {
                int index = scrollOffset + row;
                if (index >= values.size()) break;
                T option = values.get(index);
                int rowY = top + 1 + row * ROW_HEIGHT;
                boolean hovered = mouseX >= getX()
                        && mouseX < getX() + getWidth()
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                boolean selected = Objects.equals(option, value);
                graphics.fill(getX() + 1, rowY,
                        getX() + getWidth() - 1, rowY + ROW_HEIGHT,
                        selected ? 0xFF4B3F27
                                : hovered ? CONTROL_HOVER : CONTROL);
                graphics.drawString(font, labels.apply(option),
                        getX() + 8, rowY + 7,
                        selected ? ACCENT_TEXT : WHITE, false);
            }
            graphics.pose().popPose();
        }

        @Override
        public boolean handleExpandedClick(double mouseX, double mouseY,
                int button) {
            if (!open || button != 0) return false;
            int top = listTop();
            int visible = visibleRows();
            if (mouseX >= getX() && mouseX < getX() + getWidth()
                    && mouseY >= top
                    && mouseY < top + visible * ROW_HEIGHT + 2) {
                int row = (int) ((mouseY - top - 1) / ROW_HEIGHT);
                int index = scrollOffset + row;
                if (row >= 0 && row < visible && index < values.size()) {
                    value = values.get(index);
                    setMessage(labels.apply(value));
                    onChange.accept(value);
                }
                setOpen(false);
                return true;
            }
            return false;
        }

        @Override
        public boolean handleExpandedScroll(double mouseX, double mouseY,
                double delta) {
            if (!open || values.size() <= visibleRows()) return false;
            int top = listTop();
            int height = visibleRows() * ROW_HEIGHT + 2;
            if (mouseX < getX() || mouseX >= getX() + getWidth()
                    || mouseY < top || mouseY >= top + height) {
                return false;
            }
            scrollOffset = Math.max(0, Math.min(maxScroll(),
                    scrollOffset + (delta > 0.0D ? -1 : 1)));
            return true;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            if (super.isMouseOver(mouseX, mouseY)) return true;
            if (!open) return false;
            int top = listTop();
            return mouseX >= getX() && mouseX < getX() + getWidth()
                    && mouseY >= top
                    && mouseY < top + visibleRows() * ROW_HEIGHT + 2;
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
            return below + height <= ElevatorArrivalEditorScreen.this.height - 6
                    ? below : getY() - height - 2;
        }

        private void ensureSelectionVisible() {
            int selected = Math.max(0, values.indexOf(value));
            if (selected < scrollOffset) scrollOffset = selected;
            if (selected >= scrollOffset + visibleRows()) {
                scrollOffset = selected - visibleRows() + 1;
            }
            scrollOffset = Math.max(0, Math.min(maxScroll(), scrollOffset));
        }
    }
}
