package com.bl4ues.scpclassifieddirective.facility.mapping.client;

import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorStationOption;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.network.FacilityMappingNetwork;
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Operator editor for room name and Core Room Floor Station assignment. */
public final class FacilityRoomEditorScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 208;
    private static final int PANEL = 0xF0081022;
    private static final int BORDER = 0xFF46536C;
    private static final int ACCENT = 0xFF5BA7C7;
    private static final int WHITE = 0xFFF7F8FC;
    private static final int GRAY = 0xFFA9B0BF;

    private final FacilityRoomSnapshot room;
    private final List<FacilityFloorStationOption> stations;
    private int stationIndex;
    private EditBox nameBox;
    private Button stationButton;
    private Button deleteButton;
    private boolean confirmDelete;

    private FacilityRoomEditorScreen(FacilityRoomSnapshot room,
            List<FacilityFloorStationOption> stations) {
        super(ScpFonts.roboto("Facility Room Editor"));
        this.room = room;
        List<FacilityFloorStationOption> options = new ArrayList<>();
        options.add(new FacilityFloorStationOption(BlockPos.ZERO,
                "Unassigned Floor", "UNASSIGNED"));
        if (stations != null) options.addAll(stations);
        this.stations = List.copyOf(options);
        this.stationIndex = findStationIndex(room.floorStation());
    }

    public static void open(FacilityRoomSnapshot room,
            List<FacilityFloorStationOption> stations) {
        Minecraft.getInstance().setScreen(
                new FacilityRoomEditorScreen(room, stations));
    }

    @Override
    protected void init() {
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        nameBox = new EditBox(font, left + 20, top + 66,
                PANEL_WIDTH - 40, 20, ScpFonts.roboto("Room name"));
        nameBox.setMaxLength(64);
        nameBox.setValue(room.name());
        addRenderableWidget(nameBox);

        stationButton = addRenderableWidget(Button.builder(stationLabel(),
                button -> cycleStation())
                .bounds(left + 20, top + 112, PANEL_WIDTH - 40, 20).build());

        addRenderableWidget(Button.builder(ScpFonts.roboto("Save"),
                button -> save())
                .bounds(left + 20, top + 166, 94, 20).build());
        deleteButton = addRenderableWidget(Button.builder(deleteLabel(),
                button -> delete())
                .bounds(left + 133, top + 166, 94, 20).build());
        addRenderableWidget(Button.builder(ScpFonts.roboto("Cancel"),
                button -> onClose())
                .bounds(left + 246, top + 166, 94, 20).build());
        setInitialFocus(nameBox);
    }

    private void cycleStation() {
        if (stations.isEmpty()) return;
        stationIndex = (stationIndex + 1) % stations.size();
        stationButton.setMessage(stationLabel());
    }

    private void save() {
        FacilityFloorStationOption station = stations.get(stationIndex);
        BlockPos stationPos = stationIndex == 0 ? null : station.pos();
        FacilityMappingNetwork.updateRoom(room.id(), nameBox.getValue(),
                stationPos);
        onClose();
    }

    private void delete() {
        if (!confirmDelete) {
            confirmDelete = true;
            deleteButton.setMessage(deleteLabel());
            return;
        }
        FacilityMappingNetwork.deleteRoom(room.id());
        onClose();
    }

    private int findStationIndex(BlockPos station) {
        if (station == null) return 0;
        for (int index = 1; index < stations.size(); index++) {
            if (stations.get(index).pos().equals(station)) return index;
        }
        return 0;
    }

    private Component stationLabel() {
        FacilityFloorStationOption station = stations.get(stationIndex);
        return ScpFonts.roboto("Floor: " + station.longLabel());
    }

    private Component deleteLabel() {
        return ScpFonts.roboto(confirmDelete ? "Confirm Delete" : "Delete");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        renderBackground(graphics);
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, PANEL);
        drawBorder(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT, BORDER);
        graphics.fill(left + 1, top + 1, left + 5,
                top + PANEL_HEIGHT - 1, ACCENT);
        graphics.drawString(font, ScpFonts.roboto("FACILITY ROOM MAPPING"),
                left + 20, top + 15, WHITE, false);
        graphics.drawString(font, ScpFonts.roboto(
                        room.patches().size() + " floor selection"
                                + (room.patches().size() == 1 ? "" : "s")),
                left + 20, top + 34, GRAY, false);
        graphics.drawString(font, ScpFonts.roboto("Room name"),
                left + 20, top + 54, GRAY, false);
        graphics.drawString(font, ScpFonts.roboto(
                        "Click the floor field to cycle configured Floor Stations."),
                left + 20, top + 98, GRAY, false);
        graphics.drawString(font, ScpFonts.roboto(
                        "Map label: " + stations.get(stationIndex).shortLabel()
                                + " / " + nameBox.getValue()),
                left + 20, top + 141, GRAY, false);
        super.render(graphics, mouseX, mouseY, partialTick);
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
}
