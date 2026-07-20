package com.bl4ues.scpadditions.compat.network;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.server.level.ServerPlayer;

public final class NetworkEvent {
    private NetworkEvent() {}
    public static final class Context {
        private final Executor executor; private final ServerPlayer sender; private final boolean serverReception;
        Context(Executor executor, ServerPlayer sender, boolean serverReception) { this.executor=executor; this.sender=sender; this.serverReception=serverReception; }
        public CompletableFuture<Void> enqueueWork(Runnable task) { return CompletableFuture.runAsync(task, executor); }
        public ServerPlayer getSender() { return sender; }
        public Direction getDirection() { return new Direction(serverReception); }
        public void setPacketHandled(boolean handled) {}
    }
    public static final class Direction {
        private final boolean server;
        private Direction(boolean server) { this.server=server; }
        public ReceptionSide getReceptionSide() { return new ReceptionSide(server); }
    }
    public static final class ReceptionSide {
        private final boolean server;
        private ReceptionSide(boolean server) { this.server=server; }
        public boolean isServer() { return server; }
        public boolean isClient() { return !server; }
    }
}
