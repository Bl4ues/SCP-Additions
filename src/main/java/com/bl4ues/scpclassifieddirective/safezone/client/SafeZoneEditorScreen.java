package com.bl4ues.scpclassifieddirective.safezone.client;

import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.bl4ues.scpclassifieddirective.safezone.SafeZone;
import com.bl4ues.scpclassifieddirective.safezone.SafeZoneTrack;
import com.bl4ues.scpclassifieddirective.safezone.network.SafeZoneNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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

    private final SafeZone zone;
    private final List<SafeZoneTrack> manualTracks;
    private boolean musicEnabled;
    private int manualTrackIndex;
    private boolean confirmDelete;
    private Button musicButton;
    private Button trackButton;
    private Button deleteButton;

    private SafeZoneEditorScreen(SafeZone zone) {
        super(ScpFonts.roboto("Safe Zone Editor"));
        this.zone = zone;
        this.musicEnabled = zone.musicEnabled();
        this.manualTracks = SafeZoneTrack.manualTracks();
        this.manualTrackIndex = indexOf(zone.musicTrack());
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
        trackButton = addRenderableWidget(Button.builder(
                trackLabel(), button -> cycleTrack())
                .bounds(left + 164, top + 68, 138, 20).build());
        trackButton.active = !zone.hasAutomaticTrack()
                && !manualTracks.isEmpty();

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
        if (!zone.hasAutomaticTrack() && manualTracks.isEmpty()) return;
        musicEnabled = !musicEnabled;
        musicButton.setMessage(musicLabel());
    }

    private void cycleTrack() {
        if (manualTracks.isEmpty() || zone.hasAutomaticTrack()) return;
        manualTrackIndex = (manualTrackIndex + 1) % manualTracks.size();
        trackButton.setMessage(trackLabel());
    }

    private void save() {
        String trackId = zone.hasAutomaticTrack()
                ? zone.musicTrack() : selectedManualTrackId();
        SafeZoneNetwork.updateZone(zone.id(), musicEnabled, trackId);
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
        if (zone.hasAutomaticTrack()) {
            SafeZoneTrack automatic = SafeZoneTrack.byId(
                    zone.automaticTrack());
            return ScpFonts.roboto(automatic == null
                    ? "Automatic soundtrack"
                    : automatic.displayName());
        }
        if (manualTracks.isEmpty()) {
            return ScpFonts.roboto("No standard tracks yet");
        }
        return ScpFonts.roboto(
                manualTracks.get(manualTrackIndex).displayName());
    }

    private Component deleteLabel() {
        return ScpFonts.roboto(confirmDelete
                ? "Confirm Delete" : "Delete");
    }

    private int indexOf(String id) {
        for (int index = 0; index < manualTracks.size(); index++) {
            if (manualTracks.get(index).id().equals(id)) return index;
        }
        return 0;
    }

    private String selectedManualTrackId() {
        return manualTracks.isEmpty() ? ""
                : manualTracks.get(manualTrackIndex).id();
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

        if (zone.hasAutomaticTrack()) {
            graphics.drawString(font, ScpFonts.roboto(
                            "Track selected automatically from the SCP inside this zone."),
                    left + 18, top + 99, GRAY, false);
        } else if (manualTracks.isEmpty()) {
            graphics.drawString(font, ScpFonts.roboto(
                            "Standard Safe Zone tracks can be added to the catalog later."),
                    left + 18, top + 99, GRAY, false);
        }
        graphics.drawString(font, ScpFonts.roboto(
                        "Hostile spawns and pursuit are blocked inside this area."),
                left + 18, top + 121, WHITE, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static String compact(net.minecraft.core.BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y,
            int width, int height) {
        graphics.fill(x, y, x + width, y + 1, BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, BORDER);
        graphics.fill(x, y, x + 1, y + height, BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, BORDER);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
