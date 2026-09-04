package com.bl4ues.scpclassifieddirective.safezone;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** World-persistent playback state for one-time discovery music. */
public final class DiscoveryMusicSavedData extends SavedData {
    private static final String DATA_NAME =
            "scp_classified_directive_discovery_music";
    private static final String CORE_ROOM_PLAYERS_KEY = "CoreRoomPlayers";
    private static final String PLAYER_KEY = "Player";
    private static final String SCP_131_FOUND_KEY = "Scp131Found";

    private final Set<UUID> coreRoomPlayers = new HashSet<>();
    private boolean scp131Found;

    private DiscoveryMusicSavedData() {
    }

    public static DiscoveryMusicSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                DiscoveryMusicSavedData::load,
                DiscoveryMusicSavedData::new, DATA_NAME);
    }

    private static DiscoveryMusicSavedData load(CompoundTag tag) {
        DiscoveryMusicSavedData data = new DiscoveryMusicSavedData();
        data.scp131Found = tag.getBoolean(SCP_131_FOUND_KEY);
        ListTag players = tag.getList(CORE_ROOM_PLAYERS_KEY,
                Tag.TAG_COMPOUND);
        for (int index = 0; index < players.size(); index++) {
            CompoundTag entry = players.getCompound(index);
            if (entry.hasUUID(PLAYER_KEY)) {
                data.coreRoomPlayers.add(entry.getUUID(PLAYER_KEY));
            }
        }
        return data;
    }

    public synchronized boolean hasSeenCoreRoom(UUID playerId) {
        return playerId != null && coreRoomPlayers.contains(playerId);
    }

    public synchronized boolean markCoreRoomSeen(UUID playerId) {
        if (playerId == null || !coreRoomPlayers.add(playerId)) return false;
        setDirty();
        return true;
    }

    public synchronized boolean markScp131Found() {
        if (scp131Found) return false;
        scp131Found = true;
        setDirty();
        return true;
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag) {
        tag.putBoolean(SCP_131_FOUND_KEY, scp131Found);
        ListTag players = new ListTag();
        for (UUID playerId : coreRoomPlayers) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID(PLAYER_KEY, playerId);
            players.add(entry);
        }
        tag.put(CORE_ROOM_PLAYERS_KEY, players);
        return tag;
    }
}
