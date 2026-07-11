package net.mcreator.scpadditions.entity;

import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BlinkServerState {
    private static final Map<UUID, Integer> CLOSED_UNTIL_TICK = new ConcurrentHashMap<>();
    private static final int CLOSED_STALE_TICKS = 2;

    private BlinkServerState() {
    }

    public static boolean setBlinkClosed(Player player, boolean closed) {
        if (player == null) {
            return false;
        }
        boolean previous = isBlinkClosed(player);
        if (closed) {
            CLOSED_UNTIL_TICK.put(player.getUUID(), player.tickCount + CLOSED_STALE_TICKS);
        } else {
            CLOSED_UNTIL_TICK.remove(player.getUUID());
        }
        return previous != closed;
    }

    public static boolean isBlinkClosed(Player player) {
        if (player == null) {
            return false;
        }
        Integer until = CLOSED_UNTIL_TICK.get(player.getUUID());
        if (until == null) {
            return false;
        }
        if (until < player.tickCount) {
            CLOSED_UNTIL_TICK.remove(player.getUUID());
            return false;
        }
        return true;
    }
}
