package net.mcreator.scpadditions.facility;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;

/** Snapshot/restore bridge for SCP-079 persistent state used by world rollback mods. */
public final class Scp079RollbackSupport {
    private Scp079RollbackSupport() {
    }

    public static CompoundTag capture(MinecraftServer server) {
        CompoundTag root = new CompoundTag();
        if (server == null) return root;
        root.put("FacilityAccess", Scp079FacilityAccessSavedData.get(server)
                .save(new CompoundTag()));
        root.put("Processing", Scp079ProcessingSavedData.get(server)
                .save(new CompoundTag()));
        return root;
    }

    public static void restore(MinecraftServer server, CompoundTag root) {
        if (server == null || root == null) return;
        if (root.contains("FacilityAccess", Tag.TAG_COMPOUND)) {
            restoreFacility(Scp079FacilityAccessSavedData.get(server),
                    root.getCompound("FacilityAccess"));
        }
        if (root.contains("Processing", Tag.TAG_COMPOUND)) {
            CompoundTag processing = root.getCompound("Processing");
            Scp079ProcessingSavedData data = Scp079ProcessingSavedData.get(server);
            data.setPower(processing.getDouble("ProcessingPower"));
            data.setDirty();
        }
    }

    private static void restoreFacility(Scp079FacilityAccessSavedData data,
            CompoundTag tag) {
        data.setAuxiliaryPowerOnline(tag.getBoolean("AuxiliaryPowerOnline"));
        data.setProtocolExposed(tag.getBoolean("ProtocolExposed"));
        data.setFacilityAccess(tag.getBoolean("FacilityAccess"));
        data.setDiscoveryProgress(tag.getDouble("DiscoveryProgress"));
        data.setCachePurgeLockoutUntilGameTime(
                tag.getLong("CachePurgeLockoutUntilGameTime"));
        readPositions(tag, "Hosts", data.hosts());
        readPositions(tag, "Doors", data.doors());
        readPositions(tag, "TeslaGates", data.teslaGates());
        readPositions(tag, "AuxiliaryUnits", data.auxiliaryUnits());
        readPositions(tag, "PoweredAuxiliaryUnits",
                data.poweredAuxiliaryUnits());
        data.markChanged();
    }

    private static void readPositions(CompoundTag root, String key,
            java.util.Set<Scp079FacilityAccessSavedData.TrackedPosition> target) {
        target.clear();
        ListTag list = root.getList(key, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            String dimension = entry.getString("Dimension");
            if (!dimension.isBlank()) {
                target.add(Scp079FacilityAccessSavedData.TrackedPosition.of(
                        dimension, entry.getLong("Pos")));
            }
        }
    }
}
