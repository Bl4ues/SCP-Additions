package com.bl4ues.scpclassifieddirective.facility;

import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Marks the authored SCP-079 host-destruction death so human corpse logic skips it. */
public final class Scp079HostFailureDeathGuard {
    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();

    private Scp079HostFailureDeathGuard() {
    }

    public static void begin(ServerPlayer player) {
        if (player != null) ACTIVE.add(player.getUUID());
    }

    public static void end(ServerPlayer player) {
        if (player != null) ACTIVE.remove(player.getUUID());
    }

    public static boolean active(ServerPlayer player) {
        return player != null && ACTIVE.contains(player.getUUID());
    }
}
