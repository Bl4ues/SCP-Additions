package com.bl4ues.scpclassifieddirective.safezone.client;

import com.bl4ues.scpclassifieddirective.safezone.SafeZone;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
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

    public static void clear() {
        selectionStart = null;
        dimension = null;
        zones = List.of();
    }
}
