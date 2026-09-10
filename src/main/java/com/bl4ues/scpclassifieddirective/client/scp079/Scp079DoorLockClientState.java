package com.bl4ues.scpclassifieddirective.client.scp079;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks only temporary door denials confirmed by the playable SCP-079 server
 * action. Decorative locked panels never enter this state.
 */
public final class Scp079DoorLockClientState {
    private static final Map<Long, Long> LOCKED_UNTIL = new HashMap<>();

    private Scp079DoorLockClientState() {
    }

    public static void mark(BlockPos doorPos, long untilGameTime) {
        if (doorPos == null) return;
        LOCKED_UNTIL.put(doorPos.asLong(), untilGameTime);
    }

    public static boolean isLocked(BlockPos doorPos) {
        if (doorPos == null) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            LOCKED_UNTIL.clear();
            return false;
        }
        long key = doorPos.asLong();
        Long until = LOCKED_UNTIL.get(key);
        if (until == null) return false;
        if (minecraft.level.getGameTime() >= until) {
            LOCKED_UNTIL.remove(key);
            return false;
        }
        return true;
    }

    public static void clear() {
        LOCKED_UNTIL.clear();
    }
}
