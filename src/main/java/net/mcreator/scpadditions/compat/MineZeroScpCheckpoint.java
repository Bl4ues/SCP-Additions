package net.mcreator.scpadditions.compat;

import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.mcreator.scpadditions.facility.Scp079RollbackSupport;
import net.mcreator.scpadditions.network.ScpAdditionsModVariables;

import java.util.Set;
import java.util.UUID;

/**
 * MineZero deliberately snapshots vanilla Player inventory/state. SCP Additions
 * also owns Forge capabilities and SavedData, so those are checkpointed here and
 * restored immediately after MineZero rewinds its own world snapshot.
 */
public final class MineZeroScpCheckpoint {
    private static final String DATA_NAME =
            "scp_additions_minezero_compat_checkpoint";

    private MineZeroScpCheckpoint() {
    }

    public static void capture(MinecraftServer server) {
        if (server == null) return;
        SnapshotData data = data(server);
        CompoundTag root = new CompoundTag();
        CompoundTag players = new CompoundTag();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            CompoundTag playerTag = new CompoundTag();
            player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inv ->
                    playerTag.put("ScpInventory", inv.serializeNBT().copy()));
            player.getCapability(
                    ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY)
                    .ifPresent(variables -> {
                        Tag tag = variables.writeNBT();
                        if (tag instanceof CompoundTag compound) {
                            playerTag.put("PlayerVariables", compound.copy());
                        }
                    });
            playerTag.put("Persistent", capturePersistent(player));
            players.put(player.getUUID().toString(), playerTag);
        }
        root.put("Players", players);

        ScpAdditionsModVariables.MapVariables map =
                ScpAdditionsModVariables.MapVariables.get(server.overworld());
        ScpAdditionsModVariables.WorldVariables world =
                ScpAdditionsModVariables.WorldVariables.get(server.overworld());
        root.put("MapVariables", map.save(new CompoundTag()));
        root.put("WorldVariables", world.save(new CompoundTag()));
        root.put("Scp079", Scp079RollbackSupport.capture(server));

        data.snapshot = root;
        data.setDirty();
    }

    public static void restore(MinecraftServer server) {
        if (server == null) return;
        CompoundTag root = data(server).snapshot;
        if (root == null || root.isEmpty()) return;

        CompoundTag players = root.getCompound("Players");
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!players.contains(player.getUUID().toString(), Tag.TAG_COMPOUND)) {
                continue;
            }
            CompoundTag playerTag = players.getCompound(
                    player.getUUID().toString());
            if (playerTag.contains("ScpInventory", Tag.TAG_COMPOUND)) {
                player.getCapability(ScpInventoryCapability.INSTANCE)
                        .ifPresent(inv -> {
                            inv.deserializeNBT(playerTag.getCompound(
                                    "ScpInventory").copy());
                            ModNetwork.syncTo(player, inv);
                        });
            }
            if (playerTag.contains("PlayerVariables", Tag.TAG_COMPOUND)) {
                player.getCapability(
                        ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY)
                        .ifPresent(variables -> {
                            variables.readNBT(playerTag.getCompound(
                                    "PlayerVariables").copy());
                            variables.syncPlayerVariables(player);
                        });
            }
            restorePersistent(player, playerTag.getCompound("Persistent"));
        }

        if (root.contains("MapVariables", Tag.TAG_COMPOUND)) {
            ScpAdditionsModVariables.MapVariables map =
                    ScpAdditionsModVariables.MapVariables.get(server.overworld());
            map.read(root.getCompound("MapVariables"));
            map.syncData(server.overworld());
        }
        if (root.contains("WorldVariables", Tag.TAG_COMPOUND)) {
            ScpAdditionsModVariables.WorldVariables world =
                    ScpAdditionsModVariables.WorldVariables.get(server.overworld());
            world.read(root.getCompound("WorldVariables"));
            world.syncData(server.overworld());
        }
        if (root.contains("Scp079", Tag.TAG_COMPOUND)) {
            Scp079RollbackSupport.restore(server, root.getCompound("Scp079"));
        }
    }

    private static CompoundTag capturePersistent(ServerPlayer player) {
        CompoundTag captured = new CompoundTag();
        CompoundTag source = player.getPersistentData();
        for (String key : source.getAllKeys()) {
            if (!ownedPersistentKey(key)) continue;
            Tag value = source.get(key);
            if (value != null) captured.put(key, value.copy());
        }
        return captured;
    }

    private static void restorePersistent(ServerPlayer player,
            CompoundTag captured) {
        CompoundTag target = player.getPersistentData();
        Set<String> keys = Set.copyOf(target.getAllKeys());
        for (String key : keys) {
            if (ownedPersistentKey(key)) target.remove(key);
        }
        for (String key : captured.getAllKeys()) {
            Tag value = captured.get(key);
            if (value != null) target.put(key, value.copy());
        }
    }

    private static boolean ownedPersistentKey(String key) {
        if (key == null) return false;
        return key.startsWith("scp_additions")
                || key.startsWith("scpadditions")
                || key.equals("candy0") || key.equals("candy1")
                || key.equals("candy2");
    }

    private static SnapshotData data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                SnapshotData::load, SnapshotData::new, DATA_NAME);
    }

    private static final class SnapshotData extends SavedData {
        private CompoundTag snapshot = new CompoundTag();

        private SnapshotData() {
        }

        private static SnapshotData load(CompoundTag tag) {
            SnapshotData data = new SnapshotData();
            if (tag.contains("Snapshot", Tag.TAG_COMPOUND)) {
                data.snapshot = tag.getCompound("Snapshot").copy();
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.put("Snapshot", snapshot.copy());
            return tag;
        }
    }
}
