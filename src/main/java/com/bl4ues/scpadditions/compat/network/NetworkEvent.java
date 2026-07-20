package com.bl4ues.scpadditions.compat.network;

import java.util.concurrent.CompletableFuture;

import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Minimal Forge NetworkEvent compatibility surface used by existing packets. */
public final class NetworkEvent {
    private NetworkEvent() {
    }

    public static final class Context {
        private final IPayloadContext delegate;

        Context(IPayloadContext delegate) {
            this.delegate = delegate;
        }

        public CompletableFuture<Void> enqueueWork(Runnable task) {
            return delegate.enqueueWork(task);
        }

        public ServerPlayer getSender() {
            return delegate.player() instanceof ServerPlayer player ? player : null;
        }

        public Direction getDirection() {
            return new Direction(delegate.flow());
        }

        /** NeoForge payload handlers are considered handled after returning. */
        public void setPacketHandled(boolean handled) {
        }
    }

    public static final class Direction {
        private final PacketFlow flow;

        private Direction(PacketFlow flow) {
            this.flow = flow;
        }

        public ReceptionSide getReceptionSide() {
            return new ReceptionSide(flow == PacketFlow.SERVERBOUND);
        }
    }

    public static final class ReceptionSide {
        private final boolean server;

        private ReceptionSide(boolean server) {
            this.server = server;
        }

        public boolean isServer() {
            return server;
        }

        public boolean isClient() {
            return !server;
        }
    }
}
