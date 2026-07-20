package com.bl4ues.scpadditions.compat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

/**
 * Compatibility event surface used while porting the Forge 1.20.1 gameplay
 * handlers to NeoForge 1.21.1. The fields intentionally match the old Forge
 * TickEvent API so the existing gameplay logic can remain unchanged.
 */
public final class TickEvent {
    private TickEvent() {
    }

    public enum Phase {
        START,
        END
    }

    public static final class ClientTickEvent extends Event {
        public final Phase phase;

        public ClientTickEvent(Phase phase) {
            this.phase = phase;
        }
    }

    public static final class ServerTickEvent extends Event {
        public final Phase phase;
        public final MinecraftServer server;

        public ServerTickEvent(Phase phase, MinecraftServer server) {
            this.phase = phase;
            this.server = server;
        }
    }

    public static final class PlayerTickEvent extends Event {
        public final Phase phase;
        public final Player player;

        public PlayerTickEvent(Phase phase, Player player) {
            this.phase = phase;
            this.player = player;
        }
    }

    public static final class LevelTickEvent extends Event {
        public final Phase phase;
        public final Level level;

        public LevelTickEvent(Phase phase, Level level) {
            this.phase = phase;
            this.level = level;
        }
    }
}
