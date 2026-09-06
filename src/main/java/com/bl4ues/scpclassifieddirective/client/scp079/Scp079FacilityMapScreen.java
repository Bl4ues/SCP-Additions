package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState;
import com.bl4ues.scpclassifieddirective.network.Scp079PlayableNetwork;
import com.bl4ues.scpclassifieddirective.network.ScpRoleSelectorNetwork;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Full-screen surveillance map generated entirely from authored room floors. */
public final class Scp079FacilityMapScreen extends Screen {
    private static final ResourceLocation TRACKER_ARROW = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID,
            "textures/gui/scp079/map/scp_tracker_arrow.png");
    private static final int MAP_MARGIN_X = 58;
    private static final int MAP_TOP = 78;
    private static final int MAP_BOTTOM = 64;
    private static final int TOP_Y = 29;
    private static final int TOP_BUTTON_H = 27;
    private static final int TOP_CONTROL_GAP = 12;
    private static final int LEAVE_BUTTON_W = 142;
    private static final int RETURN_BUTTON_W = 164;
    private static final double MIN_ZOOM = 0.55D;
    private static final double MAX_ZOOM = 4.5D;

    private final List<FloorGroup> floors;
    private int floorIndex;
    private boolean floorMenuOpen;
    private boolean leaveConfirmation;
    private FacilityRoomSnapshot hoveredRoom;
    private double mapZoom = 1.0D;
    private double panX;
    private double panY;
    private boolean draggingMap;
    private boolean dragMoved;
    private double dragStartX;
    private double dragStartY;
    private double panStartX;
    private double panStartY;
    private FacilityRoomSnapshot pressedRoom;

    private Scp079FacilityMapScreen() {
        super(Scp079UiTheme.text("SCP-079 Surveillance Map"));
        floors = buildFloors();
        floorIndex = initialFloor(floors);
    }

    public static void open() {
        if (!Scp079PlayableClient.networkAvailable()) return;
        Minecraft.getInstance().setScreen(new Scp079FacilityMapScreen());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF06121A);
        Scp079UiTheme.renderFrame(graphics, width, height);

        Scp079UiTheme.draw(graphics, font, "SURVEILLANCE MAP",
                36, 30, 1.31F, Scp079UiTheme.TEXT);
        Scp079UiTheme.draw(graphics, font, "SCP-079 / FACILITY NETWORK",
                36, 50, 1.04F, Scp079UiTheme.MUTED);

        if (floors.isEmpty()) {
            Scp079UiTheme.drawCentered(graphics, font,
                    "NO MAPPED FACILITY ROOMS", width * 0.5F,
                    height * 0.5F, 1.20F, Scp079UiTheme.MUTED);
        } else {
            FloorGroup floor = floors.get(Mth.clamp(floorIndex, 0,
                    floors.size() - 1));
            renderFloorSelector(graphics, mouseX, mouseY, floor);
            renderMap(graphics, mouseX, mouseY, floor);
        }

        renderTopActions(graphics, mouseX, mouseY);
        Scp079UiTheme.draw(graphics, font, "SCROLL  ZOOM   /   DRAG  PAN",
                36, height - 42, 1.04F, Scp079UiTheme.MUTED);
        Scp079UiTheme.renderPower(graphics, Minecraft.getInstance(),
                Scp079PlayableClient.power());
        if (leaveConfirmation) renderLeaveConfirmation(graphics, mouseX, mouseY);
    }

    private void renderTopActions(GuiGraphics graphics, int mouseX, int mouseY) {
        int leaveX = leaveX();
        boolean leaveHover = !leaveConfirmation && inside(mouseX, mouseY,
                leaveX, TOP_Y, LEAVE_BUTTON_W, TOP_BUTTON_H);
        graphics.fill(leaveX, TOP_Y, leaveX + LEAVE_BUTTON_W,
                TOP_Y + TOP_BUTTON_H,
                leaveHover ? 0xD5472429 : 0xB8231A20);
        border(graphics, leaveX, TOP_Y, LEAVE_BUTTON_W, TOP_BUTTON_H,
                leaveHover ? 0xFFE09A91 : 0xFF72535B);
        Scp079UiTheme.drawCenteredInControl(graphics, font, "LEAVE SCP ROLE",
                leaveX + LEAVE_BUTTON_W * 0.5F, TOP_Y, TOP_BUTTON_H,
                1.06F, leaveHover ? 0xFFFFFFFF : 0xFFCBA7AA);

        if (!Scp079PlayableClient.cameraMode()) return;
        int returnX = returnX();
        boolean hover = !leaveConfirmation && inside(mouseX, mouseY,
                returnX, TOP_Y, RETURN_BUTTON_W, TOP_BUTTON_H);
        graphics.fill(returnX, TOP_Y, returnX + RETURN_BUTTON_W,
                TOP_Y + TOP_BUTTON_H,
                hover ? 0xD51A3545 : 0xB8122835);
        border(graphics, returnX, TOP_Y, RETURN_BUTTON_W, TOP_BUTTON_H,
                hover ? Scp079UiTheme.ACCENT : 0xFF52798C);
        Scp079UiTheme.drawCenteredInControl(graphics, font,
                "RETURN TO LOCAL HOST", returnX + RETURN_BUTTON_W * 0.5F,
                TOP_Y, TOP_BUTTON_H, 1.03F,
                hover ? 0xFFFFFFFF : Scp079UiTheme.ACCENT);
    }

    private void renderFloorSelector(GuiGraphics graphics, int mouseX,
            int mouseY, FloorGroup floor) {
        String label = floor.longLabel;
        int w = floorSelectorWidth(floor);
        int x = floorSelectorX(w);
        boolean hovered = !leaveConfirmation
                && inside(mouseX, mouseY, x, TOP_Y, w, TOP_BUTTON_H);
        graphics.fill(x, TOP_Y, x + w, TOP_Y + TOP_BUTTON_H,
                hovered || floorMenuOpen ? 0xD51A3545 : 0xB8122835);
        border(graphics, x, TOP_Y, w, TOP_BUTTON_H,
                floorMenuOpen ? Scp079UiTheme.ACCENT : 0xFF52798C);
        Scp079UiTheme.drawCenteredInControl(graphics, font, label,
                x + w * 0.5F - 7, TOP_Y, TOP_BUTTON_H,
                1.08F, Scp079UiTheme.TEXT);
        Scp079UiTheme.draw(graphics, font, floorMenuOpen ? "^" : "v",
                x + w - 20, TOP_Y + 10, 1.05F, Scp079UiTheme.ACCENT);

        if (!floorMenuOpen) return;
        int rowY = TOP_Y + TOP_BUTTON_H + 4;
        int rowH = 23;
        int maxRows = Math.min(8, floors.size());
        int start = Math.max(0, Math.min(floorIndex - maxRows / 2,
                floors.size() - maxRows));
        for (int i = start; i < start + maxRows; i++) {
            FloorGroup option = floors.get(i);
            boolean rowHover = !leaveConfirmation
                    && inside(mouseX, mouseY, x, rowY, w, rowH);
            graphics.fill(x, rowY, x + w, rowY + rowH,
                    i == floorIndex ? 0xE1265064
                            : rowHover ? 0xE11A3A4B : 0xE10B202B);
            Scp079UiTheme.drawCenteredInControl(graphics, font,
                    option.longLabel, x + w * 0.5F, rowY, rowH,
                    1.02F, i == floorIndex
                            ? 0xFFFFFFFF : Scp079UiTheme.ACCENT);
            rowY += rowH;
        }
    }

    private void renderMap(GuiGraphics graphics, int mouseX, int mouseY,
            FloorGroup floor) {
        MapTransform transform = transformFor(floor, mapZoom, panX, panY);
        if (transform == null) return;

        hoveredRoom = null;
        FacilityRoomSnapshot currentRoom = FacilityMappingClientState.roomAt(
                Scp079PlayableClient.hostDimension(),
                net.minecraft.core.BlockPos.containing(
                        Scp079PlayableClient.viewPosition()));

        for (FacilityRoomSnapshot room : floor.rooms) {
            Set<Long> cells = roomCells(room);
            boolean hovered = !leaveConfirmation && !floorMenuOpen
                    && roomContainsScreen(cells, mouseX, mouseY, transform);
            if (hovered) hoveredRoom = room;
            boolean current = currentRoom != null
                    && currentRoom.id().equals(room.id());
            int fill = current ? 0xC628A77B
                    : hovered ? 0xC65AA7C8 : 0xA63C6E87;
            int line = current ? 0xFF89FFD0
                    : hovered ? 0xFFE7FAFF : 0xFF73A5BC;

            // Fill the room's geometric union cell by cell. A HashSet guarantees
            // that authored patches which overlap contribute colour exactly once.
            for (long packed : cells) {
                int cellX = unpackX(packed);
                int cellZ = unpackZ(packed);
                graphics.fill(transform.sx(cellX), transform.sy(cellZ),
                        transform.sx(cellX + 1), transform.sy(cellZ + 1), fill);
            }
            renderRoomOutline(graphics, cells, transform, line);

            if (!room.name().isBlank()) {
                RoomBounds rb = RoomBounds.of(room);
                if (rb != null) {
                    int centerX = transform.sx(
                            (rb.minX + rb.maxX + 1) * 0.5D);
                    int centerY = transform.sy(
                            (rb.minZ + rb.maxZ + 1) * 0.5D);
                    String name = room.name().toUpperCase();
                    double roomWidth = (rb.maxX - rb.minX + 1)
                            * transform.scale;
                    if (Scp079UiTheme.scaledWidth(font, name, 1.02F)
                            < roomWidth + 28) {
                        Scp079UiTheme.drawCentered(graphics, font, name,
                                centerX, centerY - 5, 1.02F,
                                Scp079UiTheme.TEXT);
                    }
                }
            }
        }
        renderTrackers(graphics, floor, transform);
    }

    private void renderRoomOutline(GuiGraphics graphics, Set<Long> cells,
            MapTransform t, int color) {
        for (long packed : cells) {
            int x = unpackX(packed);
            int z = unpackZ(packed);
            int x1 = t.sx(x);
            int y1 = t.sy(z);
            int x2 = t.sx(x + 1);
            int y2 = t.sy(z + 1);
            if (!cells.contains(pack(x, z - 1)))
                graphics.fill(x1, y1, x2, y1 + 1, color);
            if (!cells.contains(pack(x, z + 1)))
                graphics.fill(x1, y2 - 1, x2, y2, color);
            if (!cells.contains(pack(x - 1, z)))
                graphics.fill(x1, y1, x1 + 1, y2, color);
            if (!cells.contains(pack(x + 1, z)))
                graphics.fill(x2 - 1, y1, x2, y2, color);
        }
    }

    private void renderTrackers(GuiGraphics graphics, FloorGroup floor,
            MapTransform transform) {
        for (Scp079PlayableNetwork.TrackerEntry marker
                : Scp079TrackingClientState.markers()) {
            if (!marker.dimension().equals(Scp079PlayableClient.hostDimension())
                    || floor.rooms.stream().noneMatch(room ->
                    room.id().equals(marker.roomId()))) continue;
            int x = transform.sx(marker.x());
            int y = transform.sy(marker.z());
            int size = 21;
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0.0F);
            // The authored arrow points up. Minecraft yaw 0 points south/down on
            // this X/Z map, hence the 180-degree basis correction.
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(
                    marker.yaw() - 180.0F));
            Scp079UiTheme.blitIcon64(graphics, TRACKER_ARROW,
                    -size / 2, -size / 2, size,
                    1.0F, 0.31F, 0.20F, 1.0F);
            graphics.pose().popPose();
            Scp079UiTheme.drawCentered(graphics, font,
                    Integer.toString(marker.scpNumber()), x,
                    y + 13, 1.03F, 0xFFFF765D);
        }
    }

    private void renderLeaveConfirmation(GuiGraphics graphics,
            int mouseX, int mouseY) {
        graphics.fill(0, 0, width, height, 0xB8000000);
        int w = Math.min(350, width - 36);
        int h = 132;
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        graphics.fill(x, y, x + w, y + h, 0xF20A1820);
        border(graphics, x, y, w, h, 0xFF5C8493);
        Scp079UiTheme.drawCentered(graphics, font, "LEAVE SCP ROLE?",
                x + w * 0.5F, y + 22, 1.24F, 0xFFFFFFFF);
        Scp079UiTheme.drawCentered(graphics, font,
                "Your original player state will be restored.",
                x + w * 0.5F, y + 48, 1.02F, 0xFF8EAFBA);
        int gap = 10;
        int buttonW = (w - 38 - gap) / 2;
        int buttonY = y + h - 40;
        int cancelX = x + 19;
        int leaveX = cancelX + buttonW + gap;
        drawModalButton(graphics, cancelX, buttonY, buttonW, 27,
                "CANCEL", inside(mouseX, mouseY,
                        cancelX, buttonY, buttonW, 27), false);
        drawModalButton(graphics, leaveX, buttonY, buttonW, 27,
                "LEAVE ROLE", inside(mouseX, mouseY,
                        leaveX, buttonY, buttonW, 27), true);
    }

    private void drawModalButton(GuiGraphics graphics, int x, int y,
            int w, int h, String label, boolean hovered, boolean danger) {
        int fill = danger
                ? hovered ? 0xE0642828 : 0xD53D2022
                : hovered ? 0xE52A5868 : 0xD518303A;
        int line = danger ? 0xFFE28A7F : 0xFF75B7CC;
        graphics.fill(x, y, x + w, y + h, fill);
        border(graphics, x, y, w, h, line);
        Scp079UiTheme.drawCenteredInControl(graphics, font, label,
                x + w * 0.5F, y, h, 1.04F, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (minecraft != null
                && minecraft.options.keyInventory.matchesMouse(button)) {
            if (leaveConfirmation) leaveConfirmation = false;
            else onClose();
            return true;
        }
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        if (leaveConfirmation) {
            return handleLeaveConfirmationClick(mouseX, mouseY);
        }

        int leaveX = leaveX();
        if (inside(mouseX, mouseY, leaveX, TOP_Y,
                LEAVE_BUTTON_W, TOP_BUTTON_H)) {
            leaveConfirmation = true;
            return true;
        }
        if (Scp079PlayableClient.cameraMode()) {
            int returnX = returnX();
            if (inside(mouseX, mouseY, returnX, TOP_Y,
                    RETURN_BUTTON_W, TOP_BUTTON_H)) {
                Scp079PlayableNetwork.requestLocal();
                onClose();
                return true;
            }
        }

        if (!floors.isEmpty()) {
            FloorGroup floor = floors.get(floorIndex);
            int w = floorSelectorWidth(floor);
            int x = floorSelectorX(w);
            if (inside(mouseX, mouseY, x, TOP_Y, w, TOP_BUTTON_H)) {
                floorMenuOpen = !floorMenuOpen;
                return true;
            }
            if (floorMenuOpen) {
                int rowY = TOP_Y + TOP_BUTTON_H + 4;
                int rowH = 23;
                int maxRows = Math.min(8, floors.size());
                int start = Math.max(0, Math.min(
                        floorIndex - maxRows / 2,
                        floors.size() - maxRows));
                for (int i = start; i < start + maxRows; i++) {
                    if (inside(mouseX, mouseY, x, rowY, w, rowH)) {
                        floorIndex = i;
                        floorMenuOpen = false;
                        resetView();
                        return true;
                    }
                    rowY += rowH;
                }
                floorMenuOpen = false;
                return true;
            }
        }

        if (insideMap(mouseX, mouseY)) {
            draggingMap = true;
            dragMoved = false;
            dragStartX = mouseX;
            dragStartY = mouseY;
            panStartX = panX;
            panStartY = panY;
            pressedRoom = hoveredRoom;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
            double dragX, double dragY) {
        if (button == 0 && draggingMap && !leaveConfirmation) {
            panX = panStartX + (mouseX - dragStartX);
            panY = panStartY + (mouseY - dragStartY);
            if (Math.abs(mouseX - dragStartX) > 3.0D
                    || Math.abs(mouseY - dragStartY) > 3.0D) {
                dragMoved = true;
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingMap) {
            draggingMap = false;
            if (!dragMoved && pressedRoom != null
                    && Scp079PlayableClient.networkAvailable()) {
                Scp079PlayableNetwork.requestRoom(pressedRoom.id());
            }
            pressedRoom = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean handleLeaveConfirmationClick(double mouseX,
            double mouseY) {
        int w = Math.min(350, width - 36);
        int h = 132;
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        int gap = 10;
        int buttonW = (w - 38 - gap) / 2;
        int buttonY = y + h - 40;
        int cancelX = x + 19;
        int leaveX = cancelX + buttonW + gap;
        if (inside(mouseX, mouseY, cancelX, buttonY, buttonW, 27)) {
            leaveConfirmation = false;
            return true;
        }
        if (inside(mouseX, mouseY, leaveX, buttonY, buttonW, 27)) {
            ScpRoleSelectorNetwork.requestRole(
                    ScpRoleSelectorNetwork.Role.HUMAN);
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (leaveConfirmation || floors.isEmpty() || delta == 0.0D) return true;
        FloorGroup floor = floors.get(floorIndex);
        MapTransform before = transformFor(floor, mapZoom, panX, panY);
        if (before == null) return true;
        double worldX = (mouseX - before.originX) / before.scale;
        double worldZ = (mouseY - before.originY) / before.scale;
        double factor = delta > 0.0D ? 1.18D : 1.0D / 1.18D;
        double nextZoom = Mth.clamp(mapZoom * factor, MIN_ZOOM, MAX_ZOOM);
        if (Math.abs(nextZoom - mapZoom) < 0.0001D) return true;
        mapZoom = nextZoom;
        MapTransform after = transformFor(floor, mapZoom, panX, panY);
        if (after != null) {
            panX += mouseX - (after.originX + worldX * after.scale);
            panY += mouseY - (after.originY + worldZ * after.scale);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (leaveConfirmation) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE
                    || minecraft != null
                    && minecraft.options.keyInventory.matches(
                            keyCode, scanCode)) {
                leaveConfirmation = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER
                    || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                ScpRoleSelectorNetwork.requestRole(
                        ScpRoleSelectorNetwork.Role.HUMAN);
                Minecraft.getInstance().setScreen(null);
                return true;
            }
            return true;
        }
        if (minecraft != null
                && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            resetView();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void resetView() {
        mapZoom = 1.0D;
        panX = 0.0D;
        panY = 0.0D;
    }

    private int leaveX() {
        return width - LEAVE_BUTTON_W - 36;
    }

    private int returnX() {
        return leaveX() - RETURN_BUTTON_W - TOP_CONTROL_GAP;
    }

    private int floorSelectorWidth(FloorGroup floor) {
        return Math.max(190,
                Scp079UiTheme.scaledWidth(font, floor.longLabel, 1.08F) + 46);
    }

    /**
     * Center the floor selector when space permits, but constrain its right edge
     * to the action-button group. This uses the same calculation for rendering
     * and hit testing, so fullscreen/GUI-scale changes cannot produce overlap or
     * a visually correct control with a stale click box.
     */
    private int floorSelectorX(int selectorWidth) {
        int centered = (width - selectorWidth) / 2;
        int actionLeft = Scp079PlayableClient.cameraMode()
                ? returnX() : leaveX();
        int rightSafe = actionLeft - TOP_CONTROL_GAP - selectorWidth;
        return Math.max(24, Math.min(centered, rightSafe));
    }

    private boolean insideMap(double mouseX, double mouseY) {
        return mouseX >= MAP_MARGIN_X && mouseX <= width - MAP_MARGIN_X
                && mouseY >= MAP_TOP && mouseY <= height - MAP_BOTTOM;
    }

    private List<FloorGroup> buildFloors() {
        ResourceLocation dimension = Scp079PlayableClient.hostDimension();
        Map<String, List<FacilityRoomSnapshot>> grouped = new LinkedHashMap<>();
        for (FacilityRoomSnapshot room
                : FacilityMappingClientState.rooms(dimension)) {
            String label = room.floorLongLabel().isBlank()
                    ? "Unassigned Floor" : room.floorLongLabel();
            grouped.computeIfAbsent(label, ignored -> new ArrayList<>())
                    .add(room);
        }
        List<FloorGroup> result = new ArrayList<>();
        for (Map.Entry<String, List<FacilityRoomSnapshot>> entry
                : grouped.entrySet()) {
            int y = entry.getValue().stream()
                    .flatMap(room -> room.patches().stream())
                    .mapToInt(FacilityFloorPatch::y).min().orElse(0);
            result.add(new FloorGroup(entry.getKey(), y,
                    List.copyOf(entry.getValue())));
        }
        result.sort(Comparator.comparingInt((FloorGroup floor) -> floor.y)
                .reversed().thenComparing(floor -> floor.longLabel,
                        String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
    }

    private int initialFloor(List<FloorGroup> options) {
        if (options.isEmpty()) return 0;
        Vec3 view = Scp079PlayableClient.viewPosition();
        FacilityRoomSnapshot current = FacilityMappingClientState.roomAt(
                Scp079PlayableClient.hostDimension(),
                net.minecraft.core.BlockPos.containing(view));
        if (current != null) {
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).rooms.stream().anyMatch(room ->
                        room.id().equals(current.id()))) return i;
            }
        }
        return 0;
    }

    private MapTransform transformFor(FloorGroup floor, double zoom,
            double offsetX, double offsetY) {
        Bounds bounds = Bounds.of(floor.rooms);
        if (bounds == null) return null;
        int availableW = Math.max(80, width - MAP_MARGIN_X * 2);
        int availableH = Math.max(80, height - MAP_TOP - MAP_BOTTOM);
        double spanX = Math.max(1.0D,
                bounds.maxX - bounds.minX + 1.0D);
        double spanZ = Math.max(1.0D,
                bounds.maxZ - bounds.minZ + 1.0D);
        double baseScale = Math.min(availableW / spanX,
                availableH / spanZ);
        baseScale = Math.min(18.0D, Math.max(1.5D, baseScale));
        double scale = baseScale * zoom;
        double mapW = spanX * scale;
        double mapH = spanZ * scale;
        double originX = (width - mapW) * 0.5D
                - bounds.minX * scale + offsetX;
        double originY = MAP_TOP + (availableH - mapH) * 0.5D
                - bounds.minZ * scale + offsetY;
        return new MapTransform(originX, originY, scale);
    }

    private static Set<Long> roomCells(FacilityRoomSnapshot room) {
        Set<Long> cells = new HashSet<>();
        for (FacilityFloorPatch patch : room.patches()) {
            for (int x = patch.minX(); x <= patch.maxX(); x++) {
                for (int z = patch.minZ(); z <= patch.maxZ(); z++) {
                    cells.add(pack(x, z));
                }
            }
        }
        return cells;
    }

    private static boolean roomContainsScreen(Set<Long> cells,
            double mouseX, double mouseY, MapTransform t) {
        int worldX = Mth.floor((mouseX - t.originX) / t.scale);
        int worldZ = Mth.floor((mouseY - t.originY) / t.scale);
        return cells.contains(pack(worldX, worldZ));
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static int unpackX(long value) { return (int) (value >> 32); }
    private static int unpackZ(long value) { return (int) value; }

    private static boolean inside(double mouseX, double mouseY,
            int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w
                && mouseY >= y && mouseY < y + h;
    }

    private static void border(GuiGraphics graphics, int x, int y,
            int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private record FloorGroup(String longLabel, int y,
            List<FacilityRoomSnapshot> rooms) { }

    private record MapTransform(double originX, double originY, double scale) {
        int sx(double x) { return (int) Math.round(originX + x * scale); }
        int sy(double z) { return (int) Math.round(originY + z * scale); }
    }

    private record Bounds(int minX, int minZ, int maxX, int maxZ) {
        static Bounds of(List<FacilityRoomSnapshot> rooms) {
            int minX = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (FacilityRoomSnapshot room : rooms) {
                for (FacilityFloorPatch patch : room.patches()) {
                    minX = Math.min(minX, patch.minX());
                    minZ = Math.min(minZ, patch.minZ());
                    maxX = Math.max(maxX, patch.maxX());
                    maxZ = Math.max(maxZ, patch.maxZ());
                }
            }
            return minX == Integer.MAX_VALUE ? null
                    : new Bounds(minX, minZ, maxX, maxZ);
        }
    }

    private record RoomBounds(int minX, int minZ, int maxX, int maxZ) {
        static RoomBounds of(FacilityRoomSnapshot room) {
            int minX = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (FacilityFloorPatch patch : room.patches()) {
                minX = Math.min(minX, patch.minX());
                minZ = Math.min(minZ, patch.minZ());
                maxX = Math.max(maxX, patch.maxX());
                maxZ = Math.max(maxZ, patch.maxZ());
            }
            return minX == Integer.MAX_VALUE ? null
                    : new RoomBounds(minX, minZ, maxX, maxZ);
        }
    }
}
