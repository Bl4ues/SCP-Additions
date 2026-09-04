package com.bl4ues.scpclassifieddirective.safezone.client;

import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.bl4ues.scpclassifieddirective.safezone.SafeZone;
import com.bl4ues.scpclassifieddirective.safezone.SafeZoneTrack;
import com.bl4ues.scpclassifieddirective.safezone.network.SafeZoneNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Compact operator editor opened by shift-right-clicking an existing zone. */
public final class SafeZoneEditorScreen extends Screen {
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 190;
    private static final int PANEL = 0xF0081022;
    private static final int BORDER = 0xFF46536C;
    private static final int ACCENT = 0xFFC59A2A;
    private static final int WHITE = 0xFFF7F8FC;
    private static final int GRAY = 0xFFA9B0BF;
    private static final int CONTROL = 0xFF202738;
    private static final int CONTROL_HOVER = 0xFF30394B;
    private static final int CONTROL_DISABLED = 0xFF181D29;

    private final SafeZone zone;
    private final List<SafeZoneTrack> availableTracks;
    private boolean musicEnabled;
    private int selectedTrackIndex;
    private boolean confirmDelete;
    private Button musicButton;
    private TrackDropdown trackDropdown;
    private Button deleteButton;

    private SafeZoneEditorScreen(SafeZone zone) {
        super(ScpFonts.roboto("Safe Zone Editor"));
        this.zone = zone;
        this.musicEnabled = zone.musicEnabled();
        this.availableTracks = SafeZoneTrack.availableTracks(
                zone.automaticTrack());
        this.selectedTrackIndex = indexOf(zone.musicTrack());
    }

    public static void open(SafeZone zone) {
        Minecraft.getInstance().setScreen(new SafeZoneEditorScreen(zone));
    }

    @Override
    protected void init() {
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        musicButton = addRenderableWidget(Button.builder(
                musicLabel(), button -> toggleMusic())
                .bounds(left + 18, top + 68, 138, 20).build());
        trackDropdown = addRenderableWidget(new TrackDropdown(
                left + 164, top + 68, 138, 20));
        updateTrackAvailability();

        addRenderableWidget(Button.builder(ScpFonts.roboto("Save"),
                button -> save())
                .bounds(left + 18, top + 148, 88, 20).build());
        deleteButton = addRenderableWidget(Button.builder(deleteLabel(),
                button -> delete())
                .bounds(left + 116, top + 148, 88, 20).build());
        addRenderableWidget(Button.builder(ScpFonts.roboto("Cancel"),
                button -> onClose())
                .bounds(left + 214, top + 148, 88, 20).build());
    }

    private void toggleMusic() {
        if (availableTracks.isEmpty()) return;
        musicEnabled = !musicEnabled;
        musicButton.setMessage(musicLabel());
        updateTrackAvailability();
    }

    private void save() {
        SafeZoneNetwork.updateZone(zone.id(), musicEnabled,
                selectedTrackId());
        onClose();
    }

    private void delete() {
        if (!confirmDelete) {
            confirmDelete = true;
            deleteButton.setMessage(deleteLabel());
            return;
        }
        SafeZoneNetwork.deleteZone(zone.id());
        onClose();
    }

    private Component musicLabel() {
        return ScpFonts.roboto("Area Music: "
                + (musicEnabled ? "ON" : "OFF"));
    }

    private Component trackLabel() {
        if (availableTracks.isEmpty()) {
            return ScpFonts.roboto("No standard tracks yet");
        }
        return ScpFonts.roboto(
                availableTracks.get(selectedTrackIndex).displayName());
    }

    private Component deleteLabel() {
        return ScpFonts.roboto(confirmDelete
                ? "Confirm Delete" : "Delete");
    }

    private int indexOf(String id) {
        for (int index = 0; index < availableTracks.size(); index++) {
            if (availableTracks.get(index).id().equals(id)) return index;
        }
        return 0;
    }

    private String selectedTrackId() {
        return availableTracks.isEmpty() ? ""
                : availableTracks.get(selectedTrackIndex).id();
    }

    private void updateTrackAvailability() {
        if (trackDropdown == null) return;
        trackDropdown.active = musicEnabled && !availableTracks.isEmpty();
        if (!trackDropdown.active) trackDropdown.close();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (trackDropdown != null
                && trackDropdown.handlePopupClick(mouseX, mouseY, button)) {
            return true;
        }
        if (trackDropdown != null && trackDropdown.isOpen()
                && !trackDropdown.isMouseOver(mouseX, mouseY)) {
            trackDropdown.close();
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
            double delta) {
        if (trackDropdown != null
                && trackDropdown.handlePopupScroll(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && trackDropdown != null
                && trackDropdown.isOpen()) {
            trackDropdown.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        renderBackground(graphics);
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        graphics.fill(left, top, left + PANEL_WIDTH,
                top + PANEL_HEIGHT, PANEL);
        drawBorder(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.fill(left + 1, top + 1, left + 5,
                top + PANEL_HEIGHT - 1, ACCENT);

        graphics.drawString(font, ScpFonts.roboto("SAFE ZONE EDITOR"),
                left + 18, top + 14, WHITE, false);
        graphics.drawString(font, ScpFonts.roboto(
                        "From " + compact(zone.min()) + " to "
                                + compact(zone.max())),
                left + 18, top + 36, GRAY, false);
        graphics.drawString(font, ScpFonts.roboto(
                        zone.volume() + " protected blocks"),
                left + 18, top + 49, GRAY, false);

        super.render(graphics, mouseX, mouseY, partialTick);
        if (trackDropdown != null) {
            trackDropdown.renderPopup(graphics, mouseX, mouseY);
        }
    }

    private static String compact(net.minecraft.core.BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y,
            int width, int height) {
        drawBorder(graphics, x, y, width, height, BORDER);
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y,
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

    private final class TrackDropdown extends AbstractButton {
        private static final int ROW_HEIGHT = 18;
        private static final int MAX_VISIBLE_ROWS = 3;

        private boolean open;
        private int scrollOffset;

        private TrackDropdown(int x, int y, int width, int height) {
            super(x, y, width, height, trackLabel());
            ensureSelectionVisible();
        }

        @Override
        public void onPress() {
            if (!active || availableTracks.isEmpty()) return;
            open = !open;
            if (open) ensureSelectionVisible();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            int background = !active ? CONTROL_DISABLED
                    : isHoveredOrFocused() || open
                    ? CONTROL_HOVER : CONTROL;
            graphics.fill(getX(), getY(), getX() + getWidth(),
                    getY() + getHeight(), background);
            drawBorder(graphics, getX(), getY(), getWidth(), getHeight(),
                    open ? ACCENT : active ? BORDER : 0xFF363D4E);

            String label = getMessage().getString();
            String clipped = font.plainSubstrByWidth(label,
                    Math.max(1, getWidth() - 27));
            graphics.drawString(font, ScpFonts.roboto(clipped),
                    getX() + 7, getY() + (getHeight() - 8) / 2,
                    active ? WHITE : GRAY, false);
            graphics.drawCenteredString(font,
                    ScpFonts.roboto(open ? "▲" : "▼"),
                    getX() + getWidth() - 11,
                    getY() + (getHeight() - 8) / 2,
                    active ? ACCENT : GRAY);
        }

        private void renderPopup(GuiGraphics graphics, int mouseX,
                int mouseY) {
            if (!isOpen()) return;
            int visible = visibleRows();
            int top = popupTop();
            int popupHeight = visible * ROW_HEIGHT + 2;
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 500.0F);
            graphics.fill(getX(), top, getX() + getWidth(),
                    top + popupHeight, PANEL);
            drawBorder(graphics, getX(), top, getWidth(), popupHeight,
                    ACCENT);
            for (int row = 0; row < visible; row++) {
                int index = scrollOffset + row;
                SafeZoneTrack track = availableTracks.get(index);
                int rowY = top + 1 + row * ROW_HEIGHT;
                boolean hovered = mouseX >= getX()
                        && mouseX < getX() + getWidth()
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                boolean selected = index == selectedTrackIndex;
                graphics.fill(getX() + 1, rowY,
                        getX() + getWidth() - 1, rowY + ROW_HEIGHT,
                        selected ? 0xFF4B3F27
                                : hovered ? CONTROL_HOVER : CONTROL);
                String label = font.plainSubstrByWidth(track.displayName(),
                        Math.max(1, getWidth() - 14));
                graphics.drawString(font, ScpFonts.roboto(label),
                        getX() + 7, rowY + (ROW_HEIGHT - 8) / 2,
                        selected ? 0xFFFFD77A : WHITE, false);
            }
            graphics.pose().popPose();
        }

        private boolean handlePopupClick(double mouseX, double mouseY,
                int button) {
            if (!isOpen() || button != 0) return false;
            int top = popupTop();
            for (int row = 0; row < visibleRows(); row++) {
                int rowY = top + 1 + row * ROW_HEIGHT;
                if (mouseX >= getX() && mouseX < getX() + getWidth()
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                    selectedTrackIndex = scrollOffset + row;
                    setMessage(trackLabel());
                    close();
                    return true;
                }
            }
            return false;
        }

        private boolean handlePopupScroll(double mouseX, double mouseY,
                double delta) {
            if (!isOpen() || availableTracks.size() <= MAX_VISIBLE_ROWS
                    || mouseX < getX() || mouseX >= getX() + getWidth()
                    || mouseY < popupTop()
                    || mouseY >= popupTop() + visibleRows() * ROW_HEIGHT + 2) {
                return false;
            }
            int direction = delta > 0.0D ? -1 : delta < 0.0D ? 1 : 0;
            scrollOffset = Math.max(0, Math.min(maxScroll(),
                    scrollOffset + direction));
            return direction != 0;
        }

        private boolean isOpen() {
            return open && active;
        }

        private void close() {
            open = false;
        }

        private int popupTop() {
            return getY() + getHeight() + 2;
        }

        private int visibleRows() {
            return Math.min(MAX_VISIBLE_ROWS, availableTracks.size());
        }

        private int maxScroll() {
            return Math.max(0, availableTracks.size() - visibleRows());
        }

        private void ensureSelectionVisible() {
            if (selectedTrackIndex < scrollOffset) {
                scrollOffset = selectedTrackIndex;
            } else if (selectedTrackIndex
                    >= scrollOffset + visibleRows()) {
                scrollOffset = selectedTrackIndex - visibleRows() + 1;
            }
            scrollOffset = Math.max(0, Math.min(maxScroll(), scrollOffset));
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
