package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Event-driven history of meaningful SCP-079 decisions for the developer HUD.
 * Routine evaluations that choose to do nothing are intentionally omitted, so
 * the feed reflects actual manipulation and deliberate strategic abandonment.
 */
public final class Scp079DecisionLog {
    public static final int CLIENT_LIFETIME_TICKS = 300;
    private static final int MAX_HISTORY = 12;
    private static final int MAX_CONTEXT_LENGTH = 160;
    private static final Map<MinecraftServer, History> HISTORIES =
            new WeakHashMap<>();

    private Scp079DecisionLog() {
    }

    public static void record(ServerLevel level, DecisionType type,
            DecisionOutcome outcome, BlockPos pos, double cost,
            String context) {
        MinecraftServer server = level == null ? null : level.getServer();
        if (server == null || type == null || outcome == null) return;

        BlockPos target = pos == null ? BlockPos.ZERO : pos.immutable();
        String dimension = level.dimension().location().toString();
        String safeContext = context == null ? "" : context.trim();
        if (safeContext.length() > MAX_CONTEXT_LENGTH) {
            safeContext = safeContext.substring(0, MAX_CONTEXT_LENGTH);
        }

        synchronized (HISTORIES) {
            History history = HISTORIES.computeIfAbsent(server,
                    ignored -> new History());
            long sequence = ++history.version;
            history.entries.addFirst(new DecisionEntry(sequence, type, outcome,
                    target, dimension, safeContext,
                    (float) Math.max(0.0D, cost), server.getTickCount()));
            while (history.entries.size() > MAX_HISTORY) {
                history.entries.removeLast();
            }
        }
    }

    public static Snapshot snapshot(MinecraftServer server) {
        if (server == null) return new Snapshot(0L, List.of());
        synchronized (HISTORIES) {
            History history = HISTORIES.computeIfAbsent(server,
                    ignored -> new History());
            long now = server.getTickCount();
            history.entries.removeIf(entry ->
                    now - entry.createdTick() >= CLIENT_LIFETIME_TICKS);
            return new Snapshot(history.version,
                    List.copyOf(new ArrayList<>(history.entries)));
        }
    }

    public enum DecisionType {
        OPEN_DOOR,
        CLOSE_DOOR,
        DENY_ACCESS,
        TESLA_SUPPRESSION,
        OPEN_SCP_012_ROUTE,
        OPEN_SCP_012_BOX,
        ABANDON_SCP_012_CONTEST,
        SEPARATE_SCP_131,
        ABORTED_ACTION
    }

    public enum DecisionOutcome {
        EXECUTED,
        ABANDONED,
        ABORTED
    }

    public record DecisionEntry(long sequence, DecisionType type,
            DecisionOutcome outcome, BlockPos pos, String dimension,
            String context, float cost, long createdTick) {
    }

    public record Snapshot(long version, List<DecisionEntry> entries) {
        public Snapshot {
            entries = entries == null ? List.of() : List.copyOf(entries);
        }
    }

    private static final class History {
        private final Deque<DecisionEntry> entries = new ArrayDeque<>();
        private long version;
    }
}
