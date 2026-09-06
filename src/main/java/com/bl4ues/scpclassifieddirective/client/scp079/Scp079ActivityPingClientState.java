package com.bl4ues.scpclassifieddirective.client.scp079;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Short-lived, visual-only facility activity hints for playable SCP-079. */
public final class Scp079ActivityPingClientState {
    public static final long LIFETIME_NANOS = 3_000_000_000L;

    private static final List<Ping> PINGS = new ArrayList<>();

    private Scp079ActivityPingClientState() {
    }

    public static synchronized void add(ResourceLocation dimension, UUID roomId,
            double x, double z) {
        if (dimension == null || roomId == null) return;
        long now = System.nanoTime();
        prune(now);
        PINGS.add(new Ping(dimension, roomId, x, z, now));
        while (PINGS.size() > 48) PINGS.remove(0);
    }

    public static synchronized List<Ping> active() {
        long now = System.nanoTime();
        prune(now);
        return List.copyOf(PINGS);
    }

    public static synchronized void clear() {
        PINGS.clear();
    }

    private static void prune(long now) {
        PINGS.removeIf(ping -> now - ping.startedAtNanos >= LIFETIME_NANOS);
    }

    public record Ping(ResourceLocation dimension, UUID roomId,
            double x, double z, long startedAtNanos) {
    }
}
