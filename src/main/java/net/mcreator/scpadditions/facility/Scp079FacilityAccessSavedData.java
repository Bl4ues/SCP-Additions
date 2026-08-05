package net.mcreator.scpadditions.facility;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashSet;
import java.util.Set;

/** Persistent state for the powered facility diagnostic bus and SCP-079 discovery. */
final class Scp079FacilityAccessSavedData extends SavedData {
    private static final String DATA_NAME = "scp_additions_scp079_facility_access";

    private boolean auxiliaryPowerOnline;
    private boolean protocolExposed;
    private boolean facilityAccess;
    private double discoveryProgress;
    private long cachePurgeLockoutUntilGameTime;

    private final Set<TrackedPosition> hosts = new LinkedHashSet<>();
    private final Set<TrackedPosition> doors = new LinkedHashSet<>();
    private final Set<TrackedPosition> teslaGates = new LinkedHashSet<>();
    private final Set<TrackedPosition> auxiliaryUnits = new LinkedHashSet<>();

    private Scp079FacilityAccessSavedData() {
    }

    static Scp079FacilityAccessSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                Scp079FacilityAccessSavedData::load,
                Scp079FacilityAccessSavedData::new,
                DATA_NAME);
    }

    private static Scp079FacilityAccessSavedData load(CompoundTag tag) {
        Scp079FacilityAccessSavedData data = new Scp079FacilityAccessSavedData();
        data.auxiliaryPowerOnline = tag.getBoolean("AuxiliaryPowerOnline");
        data.protocolExposed = tag.getBoolean("ProtocolExposed");
        data.facilityAccess = tag.getBoolean("FacilityAccess");
        if (tag.contains("DiscoveryProgress", Tag.TAG_ANY_NUMERIC)) {
            data.discoveryProgress = clamp(tag.getDouble("DiscoveryProgress"));
        }
        if (tag.contains("CachePurgeLockoutUntilGameTime",
                Tag.TAG_ANY_NUMERIC)) {
            data.cachePurgeLockoutUntilGameTime = Math.max(0L,
                    tag.getLong("CachePurgeLockoutUntilGameTime"));
        }
        readPositions(tag, "Hosts", data.hosts);
        readPositions(tag, "Doors", data.doors);
        readPositions(tag, "TeslaGates", data.teslaGates);
        readPositions(tag, "AuxiliaryUnits", data.auxiliaryUnits);
        return data;
    }

    boolean auxiliaryPowerOnline() {
        return auxiliaryPowerOnline;
    }

    void setAuxiliaryPowerOnline(boolean value) {
        if (auxiliaryPowerOnline == value) return;
        auxiliaryPowerOnline = value;
        setDirty();
    }

    boolean protocolExposed() {
        return protocolExposed;
    }

    void setProtocolExposed(boolean value) {
        if (protocolExposed == value) return;
        protocolExposed = value;
        setDirty();
    }

    boolean facilityAccess() {
        return facilityAccess;
    }

    void setFacilityAccess(boolean value) {
        if (facilityAccess == value) return;
        facilityAccess = value;
        setDirty();
    }

    double discoveryProgress() {
        return discoveryProgress;
    }

    void setDiscoveryProgress(double value) {
        double clamped = clamp(value);
        if (Math.abs(clamped - discoveryProgress) < 0.000001D) return;
        discoveryProgress = clamped;
        setDirty();
    }

    long cachePurgeLockoutUntilGameTime() {
        return cachePurgeLockoutUntilGameTime;
    }

    void setCachePurgeLockoutUntilGameTime(long value) {
        long sanitized = Math.max(0L, value);
        if (cachePurgeLockoutUntilGameTime == sanitized) return;
        cachePurgeLockoutUntilGameTime = sanitized;
        setDirty();
    }

    Set<TrackedPosition> hosts() {
        return hosts;
    }

    Set<TrackedPosition> doors() {
        return doors;
    }

    Set<TrackedPosition> teslaGates() {
        return teslaGates;
    }

    Set<TrackedPosition> auxiliaryUnits() {
        return auxiliaryUnits;
    }

    void markChanged() {
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("AuxiliaryPowerOnline", auxiliaryPowerOnline);
        tag.putBoolean("ProtocolExposed", protocolExposed);
        tag.putBoolean("FacilityAccess", facilityAccess);
        tag.putDouble("DiscoveryProgress", discoveryProgress);
        tag.putLong("CachePurgeLockoutUntilGameTime",
                cachePurgeLockoutUntilGameTime);
        writePositions(tag, "Hosts", hosts);
        writePositions(tag, "Doors", doors);
        writePositions(tag, "TeslaGates", teslaGates);
        writePositions(tag, "AuxiliaryUnits", auxiliaryUnits);
        return tag;
    }

    private static void writePositions(CompoundTag root, String key,
            Set<TrackedPosition> positions) {
        ListTag list = new ListTag();
        for (TrackedPosition position : positions) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Dimension", position.dimension());
            entry.putLong("Pos", position.packedPos());
            list.add(entry);
        }
        root.put(key, list);
    }

    private static void readPositions(CompoundTag root, String key,
            Set<TrackedPosition> target) {
        ListTag list = root.getList(key, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            String dimension = entry.getString("Dimension");
            if (!dimension.isBlank()) {
                target.add(new TrackedPosition(dimension, entry.getLong("Pos")));
            }
        }
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) return 0.0D;
        return Math.max(0.0D, Math.min(100.0D, value));
    }

    record TrackedPosition(String dimension, long packedPos) {
        static TrackedPosition of(String dimension, long packedPos) {
            return new TrackedPosition(dimension, packedPos);
        }

        int chunkX() {
            return net.minecraft.core.BlockPos.getX(packedPos) >> 4;
        }

        int chunkZ() {
            return net.minecraft.core.BlockPos.getZ(packedPos) >> 4;
        }
    }
}
