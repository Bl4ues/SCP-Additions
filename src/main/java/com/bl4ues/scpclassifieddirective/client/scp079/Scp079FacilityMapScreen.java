package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.blastdoor.BlastDoorModule;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import com.bl4ues.scpclassifieddirective.facility.transform.client.TransformConstructionClientState;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState;
import com.bl4ues.scpclassifieddirective.network.Scp079PlayableNetwork;
import com.bl4ues.scpclassifieddirective.network.ScpRoleSelectorNetwork;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
    private final Map<UUID, RoomGeometry> roomGeometryCache = new HashMap<>();
    private List<MapDoorMarker> cachedDoorMarkers = List.of();
    private int cachedDoorFloor = Integer.MIN_VALUE;
    private long doorTopologyRefreshAt;

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

        // Merge every patch belonging to a room before render/hit testing.
        // Successive mapping selections therefore form one continuous shape
        // instead of stacking translucent polygons and internal outlines.
        Map<FacilityRoomSnapshot, RoomGeometry> geometryByRoom =
                new LinkedHashMap<>();
        for (FacilityRoomSnapshot room : floor.rooms) {
            geometryByRoom.put(room, roomGeometryCache.computeIfAbsent(
                    room.id(), ignored -> RoomGeometry.of(room)));
        }

        FacilityRoomSnapshot previousHover = hoveredRoom;
        hoveredRoom = resolveHoveredRoom(floor, geometryByRoom, previousHover,
                mouseX, mouseY, transform);

        FacilityRoomSnapshot currentRoom =
                Scp079CameraNetworkClientState.activeRoom();
        if (currentRoom == null) {
            currentRoom = FacilityMappingClientState.roomAt(
                    Scp079PlayableClient.hostDimension(),
                    net.minecraft.core.BlockPos.containing(
                            Scp079PlayableClient.viewPosition()));
        }

        List<FacilityRoomSnapshot> drawOrder =
                new ArrayList<>(floor.rooms);
        drawOrder.sort(Comparator
                .comparingInt(Scp079FacilityMapScreen::roomElevation)
                .thenComparing(room -> room.id().toString()));
        if (hoveredRoom != null) {
            FacilityRoomSnapshot hover = hoveredRoom;
            drawOrder.removeIf(room -> room.id().equals(hover.id()));
            drawOrder.add(hover);
        }

        int minY = floor.rooms.stream()
                .mapToInt(Scp079FacilityMapScreen::roomElevation)
                .min().orElse(floor.y);
        int maxY = floor.rooms.stream()
                .mapToInt(Scp079FacilityMapScreen::roomElevation)
                .max().orElse(floor.y);

        for (FacilityRoomSnapshot room : drawOrder) {
            RoomGeometry geometry = geometryByRoom.get(room);
            boolean hovered = hoveredRoom != null
                    && hoveredRoom.id().equals(room.id());
            boolean current = currentRoom != null
                    && currentRoom.id().equals(room.id());
            boolean hasCamera =
                    Scp079CameraNetworkClientState.hasCamera(room.id());

            int fill;
            int line;
            if (current) {
                fill = 0xC628A77B;
                line = 0xFF89FFD0;
            } else if (hovered && hasCamera) {
                fill = 0xD05AA7C8;
                line = 0xFFE7FAFF;
            } else if (hovered) {
                fill = 0xCE355766;
                line = 0xFF91B8C5;
            } else if (hasCamera) {
                fill = 0xA63C6E87;
                line = 0xFF73A5BC;
            } else {
                // Offline rooms remain much darker than a merely lower room.
                fill = 0xA61C3541;
                line = 0xFF425F6C;
            }

            if (!hovered && !current) {
                double tone = elevationTone(roomElevation(room), minY, maxY);
                fill = shade(fill, tone);
                line = shade(line, tone);
            }

            if (geometry != null && !geometry.empty()) {
                renderRoomGeometry(graphics, geometry, transform, fill, line);
            }

            if (!room.name().isBlank() && geometry != null
                    && !geometry.empty()) {
                Rectangle2D bounds = geometry.bounds();
                int centerX = transform.sx(bounds.getCenterX());
                int centerY = transform.sy(bounds.getCenterY());
                String name = room.name().toUpperCase();
                double roomWidth = bounds.getWidth() * transform.scale;
                if (Scp079UiTheme.scaledWidth(font, name, 1.02F)
                        < roomWidth + 28) {
                    int textColor = hasCamera || current
                            ? Scp079UiTheme.TEXT : 0xFF66818D;
                    Scp079UiTheme.drawCentered(graphics, font, name,
                            centerX, centerY - 5, 1.02F, textColor);
                }
            }
        }
        renderDoorMarkers(graphics, floor, transform, geometryByRoom);
        renderTrackers(graphics, floor, transform);
    }

    private FacilityRoomSnapshot resolveHoveredRoom(FloorGroup floor,
            Map<FacilityRoomSnapshot, RoomGeometry> geometryByRoom,
            FacilityRoomSnapshot previous, int mouseX, int mouseY,
            MapTransform transform) {
        if (leaveConfirmation || floorMenuOpen) return null;
        List<FacilityRoomSnapshot> candidates = new ArrayList<>();
        for (FacilityRoomSnapshot room : floor.rooms) {
            RoomGeometry geometry = geometryByRoom.get(room);
            if (geometry != null && roomContainsScreen(geometry,
                    mouseX, mouseY, transform)) {
                candidates.add(room);
            }
        }
        if (candidates.isEmpty()) return null;

        // Once the cursor enters a lower room through an exposed section, keep
        // that layer selected while it moves through an overlap. This makes a
        // partly covered room actually inspectable instead of instantly handing
        // hover back to the top layer.
        if (previous != null) {
            for (FacilityRoomSnapshot candidate : candidates) {
                if (candidate.id().equals(previous.id())) return candidate;
            }
        }

        // Without an established hover, the physically highest room is what the
        // operator sees first, matching the normal render stack.
        return candidates.stream()
                .max(Comparator
                        .comparingInt(Scp079FacilityMapScreen::roomElevation)
                        .thenComparing(room -> room.id().toString()))
                .orElse(null);
    }

    private static int roomElevation(FacilityRoomSnapshot room) {
        return room.patches().stream().mapToInt(FacilityFloorPatch::y)
                .min().orElse(0);
    }

    private static double elevationTone(int y, int minY, int maxY) {
        if (maxY <= minY) return 0.0D;
        double t = Mth.clamp((y - minY) / (double) (maxY - minY),
                0.0D, 1.0D);
        // Subtle enough to read as depth, not as another connectivity state.
        return -0.14D + t * 0.22D;
    }

    private static int shade(int color, double amount) {
        int alpha = color >>> 24 & 0xFF;
        double factor = Math.max(0.0D, 1.0D + amount);
        int red = Mth.clamp((int) Math.round(
                (color >>> 16 & 0xFF) * factor), 0, 255);
        int green = Mth.clamp((int) Math.round(
                (color >>> 8 & 0xFF) * factor), 0, 255);
        int blue = Mth.clamp((int) Math.round(
                (color & 0xFF) * factor), 0, 255);
        return alpha << 24 | red << 16 | green << 8 | blue;
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

    private static void renderRoomGeometry(GuiGraphics graphics,
            RoomGeometry geometry, MapTransform transform,
            int fill, int lineColor) {
        Rectangle2D bounds = geometry.bounds();
        int minY = Math.max(MAP_TOP, transform.sy(bounds.getMinY()));
        int maxY = Math.min(graphics.guiHeight() - MAP_BOTTOM,
                transform.sy(bounds.getMaxY()));
        if (maxY < minY) {
            int swap = minY;
            minY = maxY;
            maxY = swap;
        }
        List<Double> intersections = new ArrayList<>();
        for (int y = minY; y <= maxY; y++) {
            intersections.clear();
            double worldZ = (y + 0.5D - transform.originY)
                    / transform.scale;
            for (List<FacilityFloorPatch.Vertex> contour
                    : geometry.contours()) {
                for (int index = 0; index < contour.size(); index++) {
                    FacilityFloorPatch.Vertex a = contour.get(index);
                    FacilityFloorPatch.Vertex b = contour.get(
                            (index + 1) % contour.size());
                    if ((a.z() > worldZ) == (b.z() > worldZ)) continue;
                    double dz = b.z() - a.z();
                    if (Math.abs(dz) < 1.0E-10D) continue;
                    double ratio = (worldZ - a.z()) / dz;
                    intersections.add(a.x() + (b.x() - a.x()) * ratio);
                }
            }
            intersections.sort(Double::compare);
            for (int index = 0; index + 1 < intersections.size(); index += 2) {
                int x0 = transform.sx(intersections.get(index));
                int x1 = transform.sx(intersections.get(index + 1));
                if (x1 > x0) graphics.fill(x0, y, x1, y + 1, fill);
            }
        }

        for (List<FacilityFloorPatch.Vertex> contour : geometry.contours()) {
            for (int index = 0; index < contour.size(); index++) {
                FacilityFloorPatch.Vertex a = contour.get(index);
                FacilityFloorPatch.Vertex b = contour.get(
                        (index + 1) % contour.size());
                drawMapLine(graphics, transform.sx(a.x()),
                        transform.sy(a.z()), transform.sx(b.x()),
                        transform.sy(b.z()), lineColor);
            }
        }
    }

    private void renderDoorMarkers(GuiGraphics graphics, FloorGroup floor,
            MapTransform transform,
            Map<FacilityRoomSnapshot, RoomGeometry> geometryByRoom) {
        long now = System.currentTimeMillis();
        // Door topology is expensive to discover because vanilla doors are
        // physical blocks. Cache their addresses for five seconds; open/closed
        // state below is resolved live from those addresses every frame.
        if (cachedDoorFloor != floorIndex || now >= doorTopologyRefreshAt) {
            cachedDoorMarkers = collectDoorMarkers(floor, geometryByRoom);
            cachedDoorFloor = floorIndex;
            doorTopologyRefreshAt = now + 5_000L;
        }
        for (MapDoorMarker marker : cachedDoorMarkers) {
            Vec3 span = marker.span();
            double half = marker.width() * 0.5D;
            Vec3 center = new Vec3(marker.x(), 0.0D, marker.z());
            Vec3 a = center.subtract(span.scale(half));
            Vec3 b = center.add(span.scale(half));
            int color = 0xFFB8D8E1;
            if (doorOpen(marker)) {
                drawDoorLine(graphics, transform.sx(a.x),
                        transform.sy(a.z), transform.sx(b.x),
                        transform.sy(b.z), color);
            } else {
                double gapHalf = marker.width() * 0.14D;
                Vec3 left = center.subtract(span.scale(gapHalf));
                Vec3 right = center.add(span.scale(gapHalf));
                drawDoorLine(graphics, transform.sx(a.x),
                        transform.sy(a.z), transform.sx(left.x),
                        transform.sy(left.z), color);
                drawDoorLine(graphics, transform.sx(right.x),
                        transform.sy(right.z), transform.sx(b.x),
                        transform.sy(b.z), color);
            }
        }
    }

    private boolean doorOpen(MapDoorMarker marker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return marker.fallbackOpen();
        return switch (marker.source()) {
            case VANILLA -> {
                BlockState state = marker.pos() == null
                        ? null : minecraft.level.getBlockState(marker.pos());
                yield state != null && FacilityModule.isDoorPassable(state);
            }
            case BLAST -> marker.pos() != null
                    && BlastDoorModule.isOpenOrOpening(
                            minecraft.level, marker.pos());
            case GROUP -> {
                TransformGroup group =
                        TransformConstructionClientState.group(marker.ownerId());
                BlockState state = group == null || marker.groupCell() == null
                        ? null : group.cells().get(marker.groupCell());
                yield state != null && FacilityModule.isDoorPassable(state);
            }
            case SURFACE -> {
                ConstructionSurface surface =
                        TransformConstructionClientState.surface(marker.ownerId());
                ConstructionSurface.SurfaceAttachment attachment =
                        surface == null || marker.surfaceSlot() == null
                        ? null : surface.attachments().get(marker.surfaceSlot());
                yield attachment != null
                        && FacilityModule.isDoorPassable(attachment.state());
            }
        };
    }

    private static void drawDoorLine(GuiGraphics graphics,
            int x0, int y0, int x1, int y1, int color) {
        drawMapLine(graphics, x0, y0, x1, y1, color);
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        if (dx >= dy) {
            drawMapLine(graphics, x0, y0 + 1, x1, y1 + 1, color);
        } else {
            drawMapLine(graphics, x0 + 1, y0, x1 + 1, y1, color);
        }
    }

    private List<MapDoorMarker> collectDoorMarkers(FloorGroup floor,
            Map<FacilityRoomSnapshot, RoomGeometry> geometryByRoom) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return List.of();
        Bounds bounds = Bounds.of(floor.rooms);
        if (bounds == null) return List.of();

        List<MapDoorMarker> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        int minY = floor.rooms.stream().flatMap(room -> room.patches().stream())
                .mapToInt(FacilityFloorPatch::y).min().orElse(floor.y) - 1;
        int maxY = floor.rooms.stream().flatMap(room -> room.patches().stream())
                .mapToInt(FacilityFloorPatch::y).max().orElse(floor.y) + 5;

        for (int x = bounds.minX - 3; x <= bounds.maxX + 3; x++) {
            for (int z = bounds.minZ - 3; z <= bounds.maxZ + 3; z++) {
                if (!nearMappedRoom(x + 0.5D, z + 0.5D, geometryByRoom)) {
                    continue;
                }
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!minecraft.level.hasChunkAt(pos)) continue;
                    BlockState state = minecraft.level.getBlockState(pos);
                    if (FacilityModule.isFacilityDoor(state)
                            && state.hasProperty(
                                    HorizontalDirectionalBlock.FACING)) {
                        if (!seen.add(pos.asLong())) continue;
                        Direction facing = state.getValue(
                                HorizontalDirectionalBlock.FACING);
                        result.add(marker(pos.getX() + 0.5D,
                                pos.getZ() + 0.5D, facing, 0.94D,
                                DoorSource.VANILLA, pos, null, null, null,
                                FacilityModule.isDoorPassable(state)));
                    } else if (BlastDoorModule.isController(state)) {
                        if (!seen.add(pos.asLong())) continue;
                        Direction facing = state.getValue(BlastDoorModule.FACING);
                        result.add(marker(pos.getX() + 0.5D,
                                pos.getZ() + 0.5D, facing, 5.0D,
                                DoorSource.BLAST, pos, null, null, null,
                                BlastDoorModule.isOpenOrOpening(
                                        minecraft.level, pos)));
                    }
                }
            }
        }

        ResourceLocation dimension = minecraft.level.dimension().location();
        for (TransformGroup group
                : TransformConstructionClientState.groups(dimension)) {
            for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                    : group.cells().entrySet()) {
                BlockState state = entry.getValue();
                if (!FacilityModule.isFacilityDoor(state)
                        || !state.hasProperty(
                                HorizontalDirectionalBlock.FACING)) continue;
                Vec3 center = group.cellCenter(entry.getKey());
                if (!nearMappedRoom(center.x, center.z, geometryByRoom)) continue;
                Direction local = state.getValue(
                        HorizontalDirectionalBlock.FACING);
                Vec3 facing = TransformMath.rotate(
                        Vec3.atLowerCornerOf(local.getNormal()),
                        group.rotationX(), group.rotationY(), group.rotationZ());
                result.add(marker(center.x, center.z, facing, 0.94D,
                        DoorSource.GROUP, null, group.id(), entry.getKey(), null,
                        FacilityModule.isDoorPassable(state)));
            }
        }
        for (ConstructionSurface surface
                : TransformConstructionClientState.surfaces(dimension)) {
            for (Map.Entry<ConstructionSurface.SurfaceSlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.attachments().entrySet()) {
                BlockState state = entry.getValue().state();
                if (!FacilityModule.isFacilityDoor(state)
                        || !state.hasProperty(
                                HorizontalDirectionalBlock.FACING)) continue;
                ConstructionSurface.SurfaceSlot slot = entry.getKey();
                double u = (slot.column() + 0.5D) / surface.columns();
                double v = (slot.row() + 0.5D) / surface.rows();
                Vec3 center = surface.gridPoint(u, v);
                if (!nearMappedRoom(center.x, center.z, geometryByRoom)) continue;
                Direction local = state.getValue(
                        HorizontalDirectionalBlock.FACING);
                Vec3 tangent = surface.gridFrameTangent(u, v);
                Vec3 normal = surface.gridNormal(u, v);
                Vec3 facing = tangent.scale(local.getStepX())
                        .add(normal.scale(local.getStepZ()));
                result.add(marker(center.x, center.z, facing, 0.94D,
                        DoorSource.SURFACE, null, surface.id(), null, slot,
                        FacilityModule.isDoorPassable(state)));
            }
        }
        return List.copyOf(result);
    }

    private static boolean nearMappedRoom(double x, double z,
            Map<FacilityRoomSnapshot, RoomGeometry> geometryByRoom) {
        for (RoomGeometry geometry : geometryByRoom.values()) {
            if (geometry.area().intersects(x - 1.1D, z - 1.1D,
                    2.2D, 2.2D)) return true;
        }
        return false;
    }

    private static MapDoorMarker marker(double x, double z,
            Direction facing, double width, DoorSource source, BlockPos pos,
            UUID ownerId, TransformGroup.GridPos groupCell,
            ConstructionSurface.SurfaceSlot surfaceSlot, boolean fallbackOpen) {
        return marker(x, z, Vec3.atLowerCornerOf(facing.getNormal()), width,
                source, pos, ownerId, groupCell, surfaceSlot, fallbackOpen);
    }

    private static MapDoorMarker marker(double x, double z,
            Vec3 facing, double width, DoorSource source, BlockPos pos,
            UUID ownerId, TransformGroup.GridPos groupCell,
            ConstructionSurface.SurfaceSlot surfaceSlot, boolean fallbackOpen) {
        Vec3 horizontal = new Vec3(facing.x, 0.0D, facing.z);
        if (horizontal.lengthSqr() < 1.0E-9D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontal = horizontal.normalize();
        }
        Vec3 span = new Vec3(-horizontal.z, 0.0D, horizontal.x);
        return new MapDoorMarker(x, z, span, width, source, pos, ownerId,
                groupCell, surfaceSlot, fallbackOpen);
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
        FacilityRoomSnapshot current =
                Scp079CameraNetworkClientState.activeRoom();
        if (current == null) {
            current = FacilityMappingClientState.roomAt(
                    Scp079PlayableClient.hostDimension(),
                    net.minecraft.core.BlockPos.containing(view));
        }
        if (current != null) {
            java.util.UUID currentId = current.id();
            for (int i = 0; i < options.size(); i++) {
                for (FacilityRoomSnapshot room : options.get(i).rooms) {
                    if (room.id().equals(currentId)) return i;
                }
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
                    if (patch.containsXZ(x + 0.5D, z + 0.5D)) {
                        cells.add(pack(x, z));
                    }
                }
            }
        }
        return cells;
    }

    private static Set<Long> rectangularRoomCells(FacilityRoomSnapshot room) {
        Set<Long> cells = new HashSet<>();
        for (FacilityFloorPatch patch : room.patches()) {
            if (patch.isPolygon()) continue;
            for (int x = patch.minX(); x <= patch.maxX(); x++) {
                for (int z = patch.minZ(); z <= patch.maxZ(); z++) {
                    cells.add(pack(x, z));
                }
            }
        }
        return cells;
    }

    private static boolean roomContainsScreen(RoomGeometry geometry,
            double mouseX, double mouseY, MapTransform t) {
        double worldX = (mouseX - t.originX) / t.scale;
        double worldZ = (mouseY - t.originY) / t.scale;
        return geometry.contains(worldX, worldZ);
    }

    private static void fillPolygon(GuiGraphics graphics,
            FacilityFloorPatch patch, MapTransform t, int color) {
        List<FacilityFloorPatch.Vertex> vertices = patch.outline();
        if (vertices.size() < 3) return;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (FacilityFloorPatch.Vertex vertex : vertices) {
            int y = t.sy(vertex.z());
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        minY = Math.max(MAP_TOP, minY);
        maxY = Math.min(graphics.guiHeight() - MAP_BOTTOM, maxY);
        List<Double> intersections = new ArrayList<>(vertices.size());
        for (int y = minY; y <= maxY; y++) {
            intersections.clear();
            double scanZ = (y + 0.5D - t.originY) / t.scale;
            for (int index = 0; index < vertices.size(); index++) {
                FacilityFloorPatch.Vertex a = vertices.get(index);
                FacilityFloorPatch.Vertex b = vertices.get(
                        (index + 1) % vertices.size());
                if ((a.z() > scanZ) == (b.z() > scanZ)) continue;
                double dz = b.z() - a.z();
                if (Math.abs(dz) < 1.0E-10D) continue;
                double ratio = (scanZ - a.z()) / dz;
                intersections.add(a.x() + (b.x() - a.x()) * ratio);
            }
            intersections.sort(Double::compare);
            for (int index = 0; index + 1 < intersections.size(); index += 2) {
                int x0 = t.sx(intersections.get(index));
                int x1 = t.sx(intersections.get(index + 1));
                if (x1 > x0) graphics.fill(x0, y, x1, y + 1, color);
            }
        }
    }

    private static void outlinePolygon(GuiGraphics graphics,
            FacilityFloorPatch patch, MapTransform t, int color) {
        List<FacilityFloorPatch.Vertex> vertices = patch.outline();
        for (int index = 0; index < vertices.size(); index++) {
            FacilityFloorPatch.Vertex a = vertices.get(index);
            FacilityFloorPatch.Vertex b = vertices.get(
                    (index + 1) % vertices.size());
            drawMapLine(graphics, t.sx(a.x()), t.sy(a.z()),
                    t.sx(b.x()), t.sy(b.z()), color);
        }
    }

    private static void drawMapLine(GuiGraphics graphics, int x0, int y0,
            int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0);
        int sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        while (true) {
            graphics.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) return;
            int twice = error * 2;
            if (twice >= dy) {
                error += dy;
                x0 += sx;
            }
            if (twice <= dx) {
                error += dx;
                y0 += sy;
            }
        }
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

    private enum DoorSource {
        VANILLA, BLAST, GROUP, SURFACE
    }

    private record MapDoorMarker(double x, double z, Vec3 span,
            double width, DoorSource source, BlockPos pos, UUID ownerId,
            TransformGroup.GridPos groupCell,
            ConstructionSurface.SurfaceSlot surfaceSlot,
            boolean fallbackOpen) {
    }

    private record RoomGeometry(Area area,
            List<List<FacilityFloorPatch.Vertex>> contours,
            Rectangle2D bounds) {
        static RoomGeometry of(FacilityRoomSnapshot room) {
            Area merged = new Area();
            for (FacilityFloorPatch patch : room.patches()) {
                List<FacilityFloorPatch.Vertex> vertices = patch.outline();
                if (vertices.size() < 3) continue;
                Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
                path.moveTo(vertices.get(0).x(), vertices.get(0).z());
                for (int index = 1; index < vertices.size(); index++) {
                    path.lineTo(vertices.get(index).x(), vertices.get(index).z());
                }
                path.closePath();
                merged.add(new Area(path));
            }
            return new RoomGeometry(merged, contours(merged),
                    merged.getBounds2D());
        }

        boolean empty() {
            return area.isEmpty();
        }

        boolean contains(double x, double z) {
            return area.contains(x, z);
        }

        private static List<List<FacilityFloorPatch.Vertex>> contours(
                Area area) {
            List<List<FacilityFloorPatch.Vertex>> result = new ArrayList<>();
            PathIterator iterator = area.getPathIterator(null, 0.008D);
            double[] coords = new double[6];
            List<FacilityFloorPatch.Vertex> current = null;
            while (!iterator.isDone()) {
                int type = iterator.currentSegment(coords);
                if (type == PathIterator.SEG_MOVETO) {
                    if (current != null && current.size() >= 3) {
                        result.add(List.copyOf(current));
                    }
                    current = new ArrayList<>();
                    current.add(new FacilityFloorPatch.Vertex(
                            coords[0], coords[1]));
                } else if (type == PathIterator.SEG_LINETO
                        && current != null) {
                    current.add(new FacilityFloorPatch.Vertex(
                            coords[0], coords[1]));
                } else if (type == PathIterator.SEG_CLOSE
                        && current != null) {
                    if (current.size() >= 3) result.add(List.copyOf(current));
                    current = null;
                }
                iterator.next();
            }
            if (current != null && current.size() >= 3) {
                result.add(List.copyOf(current));
            }
            return List.copyOf(result);
        }
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
