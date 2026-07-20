package com.bl4ues.scpadditions.compat.network;

import java.util.function.Supplier;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Target builders matching the Forge 1.20.1 PacketDistributor call shape.
 * SimpleChannel translates these targets to the NeoForge 1.21.1 distributor.
 */
public final class PacketDistributor {
    public static final PlayerTarget PLAYER = new PlayerTarget();
    public static final AllTarget ALL = new AllTarget();
    public static final DimensionTarget DIMENSION = new DimensionTarget();
    public static final TrackingEntityAndSelfTarget TRACKING_ENTITY_AND_SELF =
            new TrackingEntityAndSelfTarget();
    public static final TrackingEntityTarget TRACKING_ENTITY = new TrackingEntityTarget();

    private PacketDistributor() {
    }

    public enum Kind {
        PLAYER,
        ALL,
        DIMENSION,
        TRACKING_ENTITY_AND_SELF,
        TRACKING_ENTITY
    }

    public record PacketTarget(Kind kind, Object value) {
    }

    public static final class PlayerTarget {
        public PacketTarget with(Supplier<ServerPlayer> supplier) {
            return new PacketTarget(Kind.PLAYER, supplier.get());
        }
    }

    public static final class AllTarget {
        public PacketTarget noArg() {
            return new PacketTarget(Kind.ALL, null);
        }
    }

    public static final class DimensionTarget {
        public PacketTarget with(Supplier<ResourceKey<Level>> supplier) {
            return new PacketTarget(Kind.DIMENSION, supplier.get());
        }
    }

    public static final class TrackingEntityAndSelfTarget {
        public PacketTarget with(Supplier<? extends Entity> supplier) {
            return new PacketTarget(Kind.TRACKING_ENTITY_AND_SELF, supplier.get());
        }
    }

    public static final class TrackingEntityTarget {
        public PacketTarget with(Supplier<? extends Entity> supplier) {
            return new PacketTarget(Kind.TRACKING_ENTITY, supplier.get());
        }
    }
}
