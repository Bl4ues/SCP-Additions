package net.mcreator.scpadditions.entity;

import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BlinkServerState {
    private static final Map<UUID, BlinkState> BLINK_STATES = new ConcurrentHashMap<>();
    // The client refreshes a closed blink every two ticks. A two-tick timeout
    // could expire between heartbeats because of ordinary packet scheduling,
    // briefly reopening the eyes on the server and freezing SCP-173 early.
    // Six ticks tolerate that jitter; the explicit open packet still clears
    // the state immediately when the visual blink ends.
    private static final int CLOSED_STALE_TICKS = 6;

    private BlinkServerState() {
    }

    public static boolean setBlinkClosed(Player player, boolean closed, boolean manual) {
        if (player == null) {
            return false;
        }
        boolean previous = isBlinkClosed(player);
        if (closed) {
            BLINK_STATES.put(player.getUUID(), new BlinkState(
                    player.tickCount + CLOSED_STALE_TICKS, manual));
        } else {
            BLINK_STATES.remove(player.getUUID());
        }
        return previous != closed;
    }

    public static boolean isBlinkClosed(Player player) {
        if (player == null) {
            return false;
        }
        BlinkState state = BLINK_STATES.get(player.getUUID());
        if (state == null) {
            return false;
        }
        if (state.untilTick() < player.tickCount) {
            BLINK_STATES.remove(player.getUUID());
            return false;
        }
        return true;
    }

    public static boolean isManualBlink(Player player) {
        if (!isBlinkClosed(player)) return false;
        BlinkState state = BLINK_STATES.get(player.getUUID());
        return state != null && state.manual();
    }

    private record BlinkState(int untilTick, boolean manual) { }
}
