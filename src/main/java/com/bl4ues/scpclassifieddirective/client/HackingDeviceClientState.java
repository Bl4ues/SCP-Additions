package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.core.BlockPos;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Client mirror of server-authoritative Hacking Device attachments. */
public final class HackingDeviceClientState {
    private static final Set<BlockPos> ATTACHED = new HashSet<>();

    private HackingDeviceClientState() {
    }

    public static synchronized void replace(Collection<BlockPos> positions) {
        ATTACHED.clear();
        if (positions != null) {
            for (BlockPos pos : positions) {
                if (pos != null) ATTACHED.add(pos.immutable());
            }
        }
    }

    public static synchronized void update(BlockPos pos, boolean attached) {
        if (pos == null) return;
        if (attached) ATTACHED.add(pos.immutable());
        else ATTACHED.remove(pos);
    }

    public static synchronized boolean isAttached(BlockPos pos) {
        return pos != null && ATTACHED.contains(pos);
    }

    public static synchronized Set<BlockPos> snapshot() {
        return Set.copyOf(ATTACHED);
    }

    public static synchronized void clear() {
        ATTACHED.clear();
    }
}
