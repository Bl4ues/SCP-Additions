package net.mcreator.scpadditions.roamer;

import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.WeakHashMap;

/** Temporary scheduler lockout used after SCP-106 is repelled by a Tesla Gate. */
public final class Scp106SpawnSuppression {
    private static final Map<MinecraftServer, Integer> SUPPRESSED_UNTIL =
            new WeakHashMap<>();

    private Scp106SpawnSuppression() {
    }

    public static void suppress(MinecraftServer server, int durationTicks) {
        if (server == null || durationTicks <= 0) return;
        synchronized (SUPPRESSED_UNTIL) {
            int until = server.getTickCount() + durationTicks;
            SUPPRESSED_UNTIL.merge(server, until, Math::max);
        }
    }

    public static boolean isSuppressed(MinecraftServer server) {
        if (server == null) return false;
        synchronized (SUPPRESSED_UNTIL) {
            Integer until = SUPPRESSED_UNTIL.get(server);
            if (until == null) return false;
            if (server.getTickCount() >= until) {
                SUPPRESSED_UNTIL.remove(server);
                return false;
            }
            return true;
        }
    }
}
