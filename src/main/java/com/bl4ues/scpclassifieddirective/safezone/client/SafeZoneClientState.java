package com.bl4ues.scpclassifieddirective.safezone.client;

import com.bl4ues.scpclassifieddirective.safezone.SafeZone;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/** Client copy used only for selection rendering, editing and local music. */
public final class SafeZoneClientState {
    private static BlockPos selectionStart;
    private static ResourceLocation dimension;
    private static List<SafeZone> zones = List.of();

    private SafeZoneClientState() {
    }

    public static void setSelectionStart(BlockPos pos) {
        selectionStart = pos == null ? null : pos.immutable();
    }

    public static BlockPos selectionStart() {
        return selectionStart;
    }

    public static void sync(ResourceLocation newDimension,
            List<SafeZone> newZones) {
        dimension = newDimension;
        zones = newZones == null ? List.of() : List.copyOf(newZones);
    }

    public static SafeZone zoneAt(ResourceLocation currentDimension,
            Vec3 position) {
        if (currentDimension == null || position == null
                || !currentDimension.equals(dimension)) return null;
        return zones.stream()
                .filter(zone -> zone.dimension().equals(currentDimension)
                        && zone.bounds().contains(position))
                .min(Comparator.comparingLong(SafeZone::volume)
                        .thenComparing(zone -> zone.id().toString()))
                .orElse(null);
    }

    public static List<SafeZone> nearbyZones(ResourceLocation currentDimension,
            Vec3 position, double range, int limit) {
        if (currentDimension == null || position == null
                || !currentDimension.equals(dimension)
                || range < 0.0D || limit <= 0) {
            return List.of();
        }
        double rangeSqr = range * range;
        return zones.stream()
                .filter(zone -> zone.dimension().equals(currentDimension))
                .filter(zone -> distanceToSqr(zone.bounds(), position)
                        <= rangeSqr)
                .sorted(Comparator
                        .comparingDouble((SafeZone zone) -> distanceToSqr(
                                zone.bounds(), position))
                        .thenComparing(zone -> zone.id().toString()))
                .limit(limit)
                .toList();
    }

    private static double distanceToSqr(AABB box, Vec3 position) {
        double x = Math.max(box.minX - position.x,
                Math.max(0.0D, position.x - box.maxX));
        double y = Math.max(box.minY - position.y,
                Math.max(0.0D, position.y - box.maxY));
        double z = Math.max(box.minZ - position.z,
                Math.max(0.0D, position.z - box.maxZ));
        return x * x + y * y + z * z;
    }

    public static void clear() {
        selectionStart = null;
        dimension = null;
        zones = List.of();
    }
}
