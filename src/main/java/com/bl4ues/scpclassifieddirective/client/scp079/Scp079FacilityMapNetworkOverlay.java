package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * SCP-079-only facility map overlay.
 *
 * Kept outside the Mixin package on purpose: helper records referenced by
 * merged Mixin bytecode are illegal to load directly under Mixin 0.8.5.
 */
public final class Scp079FacilityMapNetworkOverlay {
    private static final int MAP_MARGIN_X = 58;
    private static final int MAP_TOP = 78;
    private static final int MAP_BOTTOM = 64;

    private Scp079FacilityMapNetworkOverlay() {
    }

    public static void render(GuiGraphics graphics, int floorIndex,
            double mapZoom, double panX, double panY,
            int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        List<FloorView> floors = buildFloors();
        if (floors.isEmpty()) return;

        FloorView floor = floors.get(Math.max(0,
                Math.min(floorIndex, floors.size() - 1)));
        Transform transform = transformFor(floor.rooms(), screenWidth,
                screenHeight, mapZoom, panX, panY);
        if (transform == null) return;

        for (FacilityRoomSnapshot room : floor.rooms()) {
            if (Scp079CameraNetworkClientState.hasCamera(room.id())) continue;

            // Authored floor patches may overlap. Darkening each patch directly
            // stacks translucent fills and makes no-camera rooms look striped or
            // blotchy, so render the geometric union exactly once per cell.
            for (long packed : roomCells(room)) {
                int x = unpackX(packed);
                int z = unpackZ(packed);
                graphics.fill(transform.sx(x), transform.sy(z),
                        transform.sx(x + 1), transform.sy(z + 1),
                        0xB806121A);
            }
        }

        renderActivityPings(graphics, floor, transform);
        renderHostMarker(graphics, floor, transform, minecraft);
    }

    private static void renderActivityPings(GuiGraphics graphics,
            FloorView floor, Transform transform) {
        Set<UUID> roomIds = new HashSet<>();
        for (FacilityRoomSnapshot room : floor.rooms()) roomIds.add(room.id());
        long now = System.nanoTime();
        for (Scp079ActivityPingClientState.Ping ping
                : Scp079ActivityPingClientState.active()) {
            if (!ping.dimension().equals(Scp079PlayableClient.hostDimension())
                    || !roomIds.contains(ping.roomId())) continue;
            double age = Math.max(0.0D, Math.min(1.0D,
                    (now - ping.startedAtNanos())
                            / (double) Scp079ActivityPingClientState.LIFETIME_NANOS));
            int centerX = transform.sx(ping.x());
            int centerY = transform.sy(ping.z());
            renderPingRing(graphics, centerX, centerY, age, 0.0D);
            renderPingRing(graphics, centerX, centerY, age, 0.22D);
        }
    }

    private static void renderPingRing(GuiGraphics graphics, int centerX,
            int centerY, double age, double delay) {
        if (age < delay) return;
        double progress = Math.min(1.0D, (age - delay) / (1.0D - delay));
        int radius = 3 + (int) Math.round(progress * 18.0D);
        int alpha = Math.max(0, Math.min(220,
                (int) Math.round((1.0D - progress) * 220.0D)));
        if (alpha <= 2) return;
        int color = (alpha << 24) | 0x00BDEEFF;
        int points = Math.max(20, radius * 3);
        for (int index = 0; index < points; index++) {
            double angle = Math.PI * 2.0D * index / points;
            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int y = centerY + (int) Math.round(Math.sin(angle) * radius);
            graphics.fill(x, y, x + 2, y + 2, color);
        }
    }

    private static void renderHostMarker(GuiGraphics graphics,
            FloorView floor, Transform transform, Minecraft minecraft) {
        BlockPos host = Scp079PlayableClient.hostPos();
        FacilityRoomSnapshot hostRoom = FacilityMappingClientState.roomAt(
                Scp079PlayableClient.hostDimension(), host);
        if (hostRoom == null || floor.rooms().stream().noneMatch(room ->
                room.id().equals(hostRoom.id()))) return;

        // The marker is the exact physical host block. It intentionally does not
        // snap toward a room center or a nearby authored floor cell.
        int centerX = transform.sx(host.getX() + 0.5D);
        int centerY = transform.sy(host.getZ() + 0.5D);
        int size = Math.max(2, Math.min(7,
                (int) Math.floor(transform.scale() * 0.48D)));
        int half = Math.max(1, size / 2);
        graphics.fill(centerX - half, centerY - half,
                centerX - half + size, centerY - half + size, 0xFFFFFFFF);
        Scp079UiTheme.drawCentered(graphics, minecraft.font, "079",
                centerX, centerY + half + 5, 1.03F, 0xFFFFFFFF);
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

    private static long pack(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static int unpackX(long value) {
        return (int) (value >> 32);
    }

    private static int unpackZ(long value) {
        return (int) value;
    }

    private static List<FloorView> buildFloors() {
        Map<String, List<FacilityRoomSnapshot>> grouped = new LinkedHashMap<>();
        for (FacilityRoomSnapshot room : FacilityMappingClientState.rooms(
                Scp079PlayableClient.hostDimension())) {
            String label = room.floorLongLabel().isBlank()
                    ? "Unassigned Floor" : room.floorLongLabel();
            grouped.computeIfAbsent(label, ignored -> new ArrayList<>())
                    .add(room);
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
            int screenWidth, int screenHeight, double mapZoom,
            double panX, double panY) {
        Bounds bounds = Bounds.of(rooms);
        if (bounds == null) return null;

        int width = Math.max(1, screenWidth);
        int height = Math.max(1, screenHeight);
        int availableW = Math.max(80, width - MAP_MARGIN_X * 2);
        int availableH = Math.max(80, height - MAP_TOP - MAP_BOTTOM);
        double spanX = Math.max(1.0D, bounds.maxX - bounds.minX + 1.0D);
        double spanZ = Math.max(1.0D, bounds.maxZ - bounds.minZ + 1.0D);
        double baseScale = Math.min(availableW / spanX,
                availableH / spanZ);
        baseScale = Math.min(18.0D, Math.max(1.5D, baseScale));
        double scale = baseScale * mapZoom;
        double mapW = spanX * scale;
        double mapH = spanZ * scale;
        double originX = (width - mapW) * 0.5D
                - bounds.minX * scale + panX;
        double originY = MAP_TOP + (availableH - mapH) * 0.5D
                - bounds.minZ * scale + panY;
        return new Transform(originX, originY, scale);
    }

    private record FloorView(String label, int y,
            List<FacilityRoomSnapshot> rooms) { }

    private record Transform(double originX, double originY, double scale) {
        int sx(double x) {
            return (int) Math.round(originX + x * scale);
        }

        int sy(double z) {
            return (int) Math.round(originY + z * scale);
        }
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
