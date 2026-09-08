package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Client mirror of server-authoritative Hacking Device attachments. */
public final class HackingDeviceClientState {
    private static final long SEAT_NANOS = 260_000_000L;
    private static final long REMOVAL_GHOST_NANOS = 520_000_000L;
    private static final double TRAVEL = 0.18D;
    private static final Set<BlockPos> ATTACHED = new HashSet<>();
    private static final Map<BlockPos, Long> ATTACH_STARTED = new HashMap<>();
    private static final Map<BlockPos, RemovalGhost> REMOVAL_GHOSTS =
            new HashMap<>();

    private HackingDeviceClientState() {
    }

    public static synchronized void replace(Collection<BlockPos> positions) {
        ATTACHED.clear();
        ATTACH_STARTED.clear();
        REMOVAL_GHOSTS.clear();
        if (positions != null) {
            for (BlockPos pos : positions) {
                if (pos != null) ATTACHED.add(pos.immutable());
            }
        }
    }

    public static synchronized void update(BlockPos pos, boolean attached) {
        if (pos == null) return;
        BlockPos immutable = pos.immutable();
        long now = System.nanoTime();
        if (attached) {
            ATTACHED.add(immutable);
            ATTACH_STARTED.put(immutable, now);
            REMOVAL_GHOSTS.remove(immutable);
        } else if (ATTACHED.remove(pos)) {
            ATTACH_STARTED.remove(pos);
            REMOVAL_GHOSTS.put(immutable,
                    new RemovalGhost(now, now + REMOVAL_GHOST_NANOS));
        }
    }

    public static synchronized boolean isAttached(BlockPos pos) {
        return pos != null && ATTACHED.contains(pos);
    }

    /** Positive values move the device outward from the reader surface. */
    public static synchronized double seatingOffset(BlockPos pos) {
        if (pos == null) return 0.0D;
        long now = System.nanoTime();
        Long started = ATTACH_STARTED.get(pos);
        if (ATTACHED.contains(pos) && started != null) {
            double t = Mth.clamp((now - started) / (double) SEAT_NANOS,
                    0.0D, 1.0D);
            if (t >= 1.0D) ATTACH_STARTED.remove(pos);
            return TRAVEL * (1.0D - smooth(t));
        }
        RemovalGhost ghost = REMOVAL_GHOSTS.get(pos);
        if (ghost != null) {
            double t = Mth.clamp((now - ghost.started)
                    / (double) SEAT_NANOS, 0.0D, 1.0D);
            return TRAVEL * smooth(t);
        }
        return 0.0D;
    }

    /** Includes the brief visual copy used while the device slides back to hand. */
    public static synchronized Set<BlockPos> snapshot() {
        long now = System.nanoTime();
        REMOVAL_GHOSTS.entrySet().removeIf(entry -> entry.getValue().ends <= now);
        Set<BlockPos> visible = new HashSet<>(ATTACHED);
        visible.addAll(REMOVAL_GHOSTS.keySet());
        return Set.copyOf(visible);
    }

    public static synchronized void clear() {
        ATTACHED.clear();
        ATTACH_STARTED.clear();
        REMOVAL_GHOSTS.clear();
    }

    private static double smooth(double value) {
        double t = Mth.clamp(value, 0.0D, 1.0D);
        return t * t * (3.0D - 2.0D * t);
    }

    private record RemovalGhost(long started, long ends) {
    }
}
