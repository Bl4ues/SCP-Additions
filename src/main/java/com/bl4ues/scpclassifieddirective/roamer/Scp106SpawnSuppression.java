package com.bl4ues.scpclassifieddirective.roamer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;

import java.util.Map;
import java.util.WeakHashMap;

/** Scheduler lockout used after SCP-106 is repelled by a Tesla Gate. */
public final class Scp106SpawnSuppression {
    private static final Map<MinecraftServer, Integer> REMAINING_CHECKS =
            new WeakHashMap<>();

    private Scp106SpawnSuppression() {
    }

    /**
     * The legacy duration parameter is intentionally ignored now that roamer
     * intervals vary by difficulty and player count. Tesla suppression is
     * defined in natural spawn checks instead: Safe blocks three, Euclid and
     * Keter block two, and Thaumiel has no natural checks to suppress.
     */
    public static void suppress(MinecraftServer server, int durationTicks) {
        if (server == null) return;
        int checks = suppressedChecks(server.getWorldData().getDifficulty());
        synchronized (REMAINING_CHECKS) {
            if (checks <= 0) {
                REMAINING_CHECKS.remove(server);
                return;
            }
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

    private static int suppressedChecks(Difficulty difficulty) {
        if (difficulty == null) return 2;
        return switch (difficulty) {
            case PEACEFUL -> 0;
            case EASY -> 3;
            case NORMAL, HARD -> 2;
        };
    }
}
