package net.mcreator.scpadditions.roamer;

import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.WeakHashMap;

/** Scheduler lockout used after SCP-106 is repelled by a Tesla Gate. */
public final class Scp106SpawnSuppression {
    private static final int SPAWN_INTERVAL_TICKS = 5 * 60 * 20;
    private static final Map<MinecraftServer, Integer> REMAINING_CHECKS =
            new WeakHashMap<>();

    private Scp106SpawnSuppression() {
    }

    public static void suppress(MinecraftServer server, int durationTicks) {
        if (server == null || durationTicks <= 0) return;
        int checks = Math.max(1,
                (int) Math.ceil(durationTicks / (double) SPAWN_INTERVAL_TICKS));
        synchronized (REMAINING_CHECKS) {
            REMAINING_CHECKS.merge(server, checks, Math::max);
        }
    }

    /** Consumes one scheduled check while suppression remains active. */
    public static boolean consumeSuppressedCheck(MinecraftServer server) {
        if (server == null) return false;
        synchronized (REMAINING_CHECKS) {
            Integer remaining = REMAINING_CHECKS.get(server);
            if (remaining == null || remaining <= 0) {
                REMAINING_CHECKS.remove(server);
                return false;
            }
            if (remaining == 1) REMAINING_CHECKS.remove(server);
            else REMAINING_CHECKS.put(server, remaining - 1);
            return true;
        }
    }

    public static boolean isSuppressed(MinecraftServer server) {
        if (server == null) return false;
        synchronized (REMAINING_CHECKS) {
            return REMAINING_CHECKS.getOrDefault(server, 0) > 0;
        }
    }
}
