package com.bl4ues.scpclassifieddirective.keycard;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Authoritative lifetime for an accepted keycard-reader pulse.
 *
 * Legacy accept blocks schedule their own reset ticks. Reusing a reader can leave
 * one of those old ticks queued, which used to make a later authorization expire
 * early. This deadline is independent of whichever scheduled tick happens to
 * fire first: stale ticks are ignored until the current five-second window ends.
 */
public final class KeycardReaderPulse {
    public static final int PASSAGE_TICKS = 5 * 20;

    private static final Map<ServerLevel, Map<BlockPos, Long>> EXPIRES_AT =
            new WeakHashMap<>();

    private KeycardReaderPulse() {
    }

    public static void arm(ServerLevel level, BlockPos pos, Block block) {
        if (level == null || pos == null || block == null) return;
        EXPIRES_AT.computeIfAbsent(level, ignored -> new HashMap<>())
                .put(pos.immutable(), level.getGameTime() + PASSAGE_TICKS);

        /*
         * Schedule the desired five-second tick before the generated block's
         * legacy 200-tick onPlace schedule runs. If Minecraft de-duplicates ticks
         * for the same block/position, the correct earlier deadline wins.
         */
        level.scheduleTick(pos, block, PASSAGE_TICKS);
    }

    public static int remainingTicks(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return 0;
        Map<BlockPos, Long> levelEntries = EXPIRES_AT.get(level);
        if (levelEntries == null) return 0;
        Long expiresAt = levelEntries.get(pos);
        if (expiresAt == null) return 0;
        long remaining = expiresAt - level.getGameTime();
        return remaining > 0L
                ? (int) Math.min(Integer.MAX_VALUE, remaining) : 0;
    }

    public static void clear(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return;
        Map<BlockPos, Long> levelEntries = EXPIRES_AT.get(level);
        if (levelEntries == null) return;
        levelEntries.remove(pos);
        if (levelEntries.isEmpty()) EXPIRES_AT.remove(level);
    }
}
