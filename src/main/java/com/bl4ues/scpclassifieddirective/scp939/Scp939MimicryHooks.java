package com.bl4ues.scpclassifieddirective.scp939;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Loader-neutral bridge between SCP-939 AI and optional voice mimicry.
 *
 * Common gameplay code never links against Simple Voice Chat classes directly.
 * The optional compatibility plugin installs a backend at runtime when the SVC
 * server is actually available. Without it, every operation is a safe no-op.
 */
public final class Scp939MimicryHooks {
    private static final Backend NONE = new Backend() {
        @Override
        public boolean request(ServerLevel level, UUID scp939Id, Vec3 position,
                UUID preferredSpeaker) {
            return false;
        }

        @Override
        public boolean setConsent(ServerPlayer player, boolean allowed) {
            return false;
        }

        @Override
        public boolean hasConsent(UUID playerId) {
            return false;
        }

        @Override
        public void forget(UUID playerId) {
        }
    };

    private static volatile Backend backend = NONE;

    private Scp939MimicryHooks() {
    }

    public static void install(Backend newBackend) {
        backend = newBackend == null ? NONE : newBackend;
    }

    public static void uninstall(Backend installedBackend) {
        if (installedBackend != null && backend == installedBackend) {
            backend = NONE;
        }
    }

    public static boolean available() {
        return backend != NONE;
    }

    public static boolean request(ServerLevel level, UUID scp939Id,
            Vec3 position, UUID preferredSpeaker) {
        return backend.request(level, scp939Id, position, preferredSpeaker);
    }

    public static boolean setConsent(ServerPlayer player, boolean allowed) {
        return backend.setConsent(player, allowed);
    }

    public static boolean hasConsent(UUID playerId) {
        return playerId != null && backend.hasConsent(playerId);
    }

    public static void forget(UUID playerId) {
        if (playerId != null) backend.forget(playerId);
    }

    public interface Backend {
        boolean request(ServerLevel level, UUID scp939Id, Vec3 position,
                UUID preferredSpeaker);

        boolean setConsent(ServerPlayer player, boolean allowed);

        boolean hasConsent(UUID playerId);

        void forget(UUID playerId);
    }
}
