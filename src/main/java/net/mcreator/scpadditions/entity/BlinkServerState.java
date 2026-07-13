package net.mcreator.scpadditions.entity;

import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BlinkServerState {
    private static final Map<UUID, Integer> CLOSED_UNTIL_TICK = new ConcurrentHashMap<>();
    // The client refreshes a closed blink every two ticks. A two-tick timeout
    // could expire between heartbeats because of ordinary packet scheduling,
    // briefly reopening the eyes on the server and freezing SCP-173 early.
    // Six ticks tolerate that jitter; the explicit open packet still clears
    // the state immediately when the visual blink ends.
    private static final int CLOSED_STALE_TICKS = 6;

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
