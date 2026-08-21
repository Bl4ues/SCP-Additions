package net.mcreator.scpadditions.network;

import net.minecraft.server.level.ServerPlayer;

/** Network facade for SCP-939 interactions. */
public final class Scp939Network {
    private Scp939Network() {
    }

    public static void sendPinState(ServerPlayer player, boolean pinned,
            int progress, int failures, int expectedKey, int windowTicks) {
        // Concrete packet sync is installed by the interaction layer.
    }
}
