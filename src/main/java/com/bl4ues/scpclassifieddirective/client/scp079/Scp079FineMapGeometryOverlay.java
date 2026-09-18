package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Draws the exact sub-block polygon edge over the SCP-079 map raster. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp079FineMapGeometryOverlay {
    private static final int MAP_MARGIN_X = 58;
    private static final int MAP_TOP = 78;
    private static final int MAP_BOTTOM = 64;
    private static Field floorIndexField;
    private static Field zoomField;
    private static Field panXField;
    private static Field panYField;
    private static boolean reflectionFailed;

    private Scp079FineMapGeometryOverlay() {
    }

    public static void afterRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof Scp079FacilityMapScreen screen)) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || reflectionFailed) return;
        try {
            ensureFields();
            int floorIndex = floorIndexField.getInt(screen);
            double zoom = zoomField.getDouble(screen);
            double panX = panXField.getDouble(screen);
            double panY = panYField.getDouble(screen);
            List<FloorView> floors = buildFloors();
            if (floors.isEmpty()) return;
            FloorView floor = floors.get(Math.max(0,
                    Math.min(floorIndex, floors.size() - 1)));
            int width = minecraft.getWindow().getGuiScaledWidth();
            int height = minecraft.getWindow().getGuiScaledHeight();
            Transform transform = transformFor(floor.rooms(), width, height,
                    zoom, panX, panY);
            if (transform == null) return;
            GuiGraphics graphics = event.getGuiGraphics();
            FacilityRoomSnapshot current = Scp079CameraNetworkClientState.activeRoom();
            for (FacilityRoomSnapshot room : floor.rooms()) {
                boolean hasCamera = Scp079CameraNetworkClientState.hasCamera(
                        room.id());
                boolean active = current != null && current.id().equals(room.id());
                int color = active ? 0xFF89FFD0
                        : hasCamera ? 0xFF73A5BC : 0xFF425F6C;
                for (FacilityFloorPatch patch : room.patches()) {
                    if (!patch.isPolygon()) continue;
                    drawPolygon(graphics, patch, transform, color);
                }
            }
        } catch (ReflectiveOperationException exception) {
            reflectionFailed = true;
            ScpClassifiedDirectiveMod.LOGGER.warn(
                    "Could not read SCP-079 map view state for fine geometry overlay",
                    exception);
        }
    }

    private static void ensureFields() throws ReflectiveOperationException {
        if (floorIndexField != null) return;
        floorIndexField = field("floorIndex");
        zoomField = field("mapZoom");
        panXField = field("panX");
        panYField = field("panY");
    }

    private static Field field(String name) throws ReflectiveOperationException {
        Field field = Scp079FacilityMapScreen.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void drawPolygon(GuiGraphics graphics,
            FacilityFloorPatch patch, Transform transform, int color) {
        List<FacilityFloorPatch.Vertex> vertices = patch.outline();
        for (int i = 0; i < vertices.size(); i++) {
            FacilityFloorPatch.Vertex a = vertices.get(i);
            FacilityFloorPatch.Vertex b = vertices.get((i + 1) % vertices.size());
            drawLine(graphics, transform.sx(a.x()), transform.sy(a.z()),
                    transform.sx(b.x()), transform.sy(b.z()), color);
        }
    }

    private static void drawLine(GuiGraphics graphics, int x0, int y0,
            int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0);
        int sy = y0 < y1 ? 1 : -1;
        int error = dx + dy;
        int x = x0;
        int y = y0;
        while (true) {
            graphics.fill(x, y, x + 1, y + 1, color);
            if (x == x1 && y == y1) break;
            int twice = error * 2;
            if (twice >= dy) {
                error += dy;
                x += sx;
            }
            if (twice <= dx) {
                error += dx;
                y += sy;
            }
        }
    }

    private static List<FloorView> buildFloors() {
        Map<String, List<FacilityRoomSnapshot>> grouped = new LinkedHashMap<>();
        for (FacilityRoomSnapshot room : FacilityMappingClientState.rooms(
                Scp079PlayableClient.hostDimension())) {
            String label = room.floorLongLabel().isBlank()
                    ? "Unassigned Floor" : room.floorLongLabel();
            grouped.computeIfAbsent(label, ignored -> new ArrayList<>()).add(room);
        }
        List<FloorView> result = new ArrayList<>();
        for (Map.Entry<String, List<FacilityRoomSnapshot>> entry
                : grouped.entrySet()) {
            int y = entry.getValue().stream()
                    .flatMap(room -> room.patches().stream())
                    .mapToInt(FacilityFloorPatch::y).min().orElse(0);
            result.add(new FloorView(entry.getKey(), y,
                    List.copyOf(entry.getValue())));
        }
        result.sort(Comparator.comparingInt(FloorView::y).reversed()
                .thenComparing(FloorView::label,
                        String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static Transform transformFor(List<FacilityRoomSnapshot> rooms,
            int screenWidth, int screenHeight, double zoom,
            double panX, double panY) {
        Bounds bounds = Bounds.of(rooms);
        if (bounds == null) return null;
        int availableW = Math.max(80, screenWidth - MAP_MARGIN_X * 2);
        int availableH = Math.max(80, screenHeight - MAP_TOP - MAP_BOTTOM);
        double spanX = Math.max(1.0D, bounds.maxX - bounds.minX + 1.0D);
        double spanZ = Math.max(1.0D, bounds.maxZ - bounds.minZ + 1.0D);
        double baseScale = Math.min(availableW / spanX,
                availableH / spanZ);
        baseScale = Math.min(18.0D, Math.max(1.5D, baseScale));
        double scale = baseScale * zoom;
        double mapW = spanX * scale;
        double mapH = spanZ * scale;
        double originX = (screenWidth - mapW) * 0.5D
                - bounds.minX * scale + panX;
        double originY = MAP_TOP + (availableH - mapH) * 0.5D
                - bounds.minZ * scale + panY;
        return new Transform(originX, originY, scale);
    }

    private record FloorView(String label, int y,
            List<FacilityRoomSnapshot> rooms) {
    }

    private record Transform(double originX, double originY, double scale) {
        int sx(double x) { return (int) Math.round(originX + x * scale); }
        int sy(double z) { return (int) Math.round(originY + z * scale); }
    }

    private record Bounds(int minX, int minZ, int maxX, int maxZ) {
        private static Bounds of(List<FacilityRoomSnapshot> rooms) {
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
}
