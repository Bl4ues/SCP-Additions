package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.core.BlockPos;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Client mirror of server-authoritative Hacking Device attachments. */
public final class HackingDeviceClientState {
    private static final long REMOVAL_GHOST_NANOS = 750_000_000L;
    private static final Set<BlockPos> ATTACHED = new HashSet<>();
    private static final Map<BlockPos, Long> REMOVAL_GHOSTS = new HashMap<>();

    private HackingDeviceClientState() {
    }

    public static synchronized void replace(Collection<BlockPos> positions) {
        ATTACHED.clear();
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
        if (attached) {
            ATTACHED.add(immutable);
            REMOVAL_GHOSTS.remove(immutable);
        } else if (ATTACHED.remove(pos)) {
            REMOVAL_GHOSTS.put(immutable,
                    System.nanoTime() + REMOVAL_GHOST_NANOS);
        }
    }

    public static synchronized boolean isAttached(BlockPos pos) {
        return pos != null && ATTACHED.contains(pos);
    }

    /** Includes the brief visual copy used by the symmetric removal camera move. */
    public static synchronized Set<BlockPos> snapshot() {
        long now = System.nanoTime();
        REMOVAL_GHOSTS.entrySet().removeIf(entry -> entry.getValue() <= now);
        Set<BlockPos> visible = new HashSet<>(ATTACHED);
        visible.addAll(REMOVAL_GHOSTS.keySet());
        return Set.copyOf(visible);
    }

    public static synchronized void clear() {
        ATTACHED.clear();
        REMOVAL_GHOSTS.clear();
    }
}
