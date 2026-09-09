package com.bl4ues.scpclassifieddirective.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.HackingDeviceClientState;
import com.bl4ues.scpclassifieddirective.client.HackingDeviceMinigameClient;
import com.bl4ues.scpclassifieddirective.hacking.HackingDevicePuzzle;
import com.bl4ues.scpclassifieddirective.hacking.HackingDeviceSessionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/** Synchronizes physical attachment plus the server-authoritative hacking session. */
public final class HackingDeviceNetwork {
    public enum ResultKind {
        ROUND_OK,
        DENIED,
        LOCKED,
        SUCCESS
    }

    private static boolean registered;

    private HackingDeviceNetwork() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ScpClassifiedDirectiveMod.addNetworkMessage(AttachmentUpdate.class,
                AttachmentUpdate::encode, AttachmentUpdate::decode,
                AttachmentUpdate::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(AttachmentSnapshot.class,
                AttachmentSnapshot::encode, AttachmentSnapshot::decode,
                AttachmentSnapshot::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(StartSession.class,
                StartSession::encode, StartSession::decode,
                StartSession::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(SessionResult.class,
                SessionResult::encode, SessionResult::decode,
                SessionResult::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(BeginCooldown.class,
                BeginCooldown::encode, BeginCooldown::decode,
                BeginCooldown::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(SubmitCandidate.class,
                SubmitCandidate::encode, SubmitCandidate::decode,
                SubmitCandidate::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(ExitSession.class,
                ExitSession::encode, ExitSession::decode,
                ExitSession::handle);
    }

    public static void broadcastAttachment(ServerLevel level, BlockPos pos,
            boolean attached) {
        if (level == null || pos == null) return;
        AttachmentUpdate message = new AttachmentUpdate(pos, attached);
        for (ServerPlayer player : level.players()) {
            ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                    PacketDistributor.PLAYER.with(() -> player), message);
        }
    }

    public static void sync(ServerPlayer player, Collection<BlockPos> positions) {
        if (player == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new AttachmentSnapshot(positions == null
                        ? List.of() : List.copyOf(positions)));
    }

    public static void startSession(ServerPlayer player, BlockPos pos,
            int accessLevel, int round, int failures,
            HackingDevicePuzzle puzzle) {
        if (player == null || pos == null || puzzle == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new StartSession(pos, accessLevel, round, failures, puzzle));
    }

    public static void sessionResult(ServerPlayer player, BlockPos pos,
            ResultKind result, int round, int failures,
            HackingDevicePuzzle puzzle) {
        if (player == null || pos == null || result == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SessionResult(pos, result, round, failures, puzzle));
    }

    public static void beginCooldown(ServerPlayer player, BlockPos pos,
            long countdownEnd, long readyAt) {
        if (player == null || pos == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new BeginCooldown(pos, countdownEnd, readyAt));
    }

    public static void submitCandidate(BlockPos pos, int answer) {
        if (pos == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new SubmitCandidate(pos, answer));
    }

    public static void exitSession(BlockPos pos) {
        if (pos == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new ExitSession(pos));
    }

    public record AttachmentUpdate(BlockPos pos, boolean attached) {
        private static void encode(AttachmentUpdate message,
                FriendlyByteBuf buffer) {
            buffer.writeBlockPos(message.pos);
            buffer.writeBoolean(message.attached);
        }

        private static AttachmentUpdate decode(FriendlyByteBuf buffer) {
            return new AttachmentUpdate(buffer.readBlockPos(),
                    buffer.readBoolean());
        }

        private static void handle(AttachmentUpdate message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> HackingDeviceClientState.update(message.pos,
                            message.attached)));
            context.setPacketHandled(true);
        }
    }

    public record AttachmentSnapshot(List<BlockPos> positions) {
        private static void encode(AttachmentSnapshot message,
                FriendlyByteBuf buffer) {
            buffer.writeVarInt(message.positions.size());
            for (BlockPos pos : message.positions) buffer.writeBlockPos(pos);
        }

        private static AttachmentSnapshot decode(FriendlyByteBuf buffer) {
            int size = Math.min(buffer.readVarInt(), 32768);
            List<BlockPos> positions = new ArrayList<>(size);
            for (int i = 0; i < size; i++) positions.add(buffer.readBlockPos());
            return new AttachmentSnapshot(List.copyOf(positions));
        }

        private static void handle(AttachmentSnapshot message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> HackingDeviceClientState.replace(
                            message.positions)));
            context.setPacketHandled(true);
        }
    }

    public record StartSession(BlockPos pos, int accessLevel, int round,
            int failures, HackingDevicePuzzle puzzle) {
        private static void encode(StartSession message,
                FriendlyByteBuf buffer) {
            buffer.writeBlockPos(message.pos);
            buffer.writeVarInt(message.accessLevel);
            buffer.writeVarInt(message.round);
            buffer.writeVarInt(message.failures);
            message.puzzle.encode(buffer);
        }

        private static StartSession decode(FriendlyByteBuf buffer) {
            return new StartSession(buffer.readBlockPos(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt(),
                    HackingDevicePuzzle.decode(buffer));
        }

        private static void handle(StartSession message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        HackingDeviceMinigameClient.start(message.pos,
                                message.accessLevel, message.round,
                                message.failures, message.puzzle);
                        com.bl4ues.scpclassifieddirective.client.HackingDeviceFocusClient
                                .beginSession(message.pos);
                    }));
            context.setPacketHandled(true);
        }
    }

    public record SessionResult(BlockPos pos, ResultKind result, int round,
            int failures, HackingDevicePuzzle puzzle) {
        private static void encode(SessionResult message,
                FriendlyByteBuf buffer) {
            buffer.writeBlockPos(message.pos);
            buffer.writeEnum(message.result);
            buffer.writeVarInt(message.round);
            buffer.writeVarInt(message.failures);
            buffer.writeBoolean(message.puzzle != null);
            if (message.puzzle != null) message.puzzle.encode(buffer);
        }

        private static SessionResult decode(FriendlyByteBuf buffer) {
            BlockPos pos = buffer.readBlockPos();
            ResultKind result = buffer.readEnum(ResultKind.class);
            int round = buffer.readVarInt();
            int failures = buffer.readVarInt();
            HackingDevicePuzzle puzzle = buffer.readBoolean()
                    ? HackingDevicePuzzle.decode(buffer) : null;
            return new SessionResult(pos, result, round, failures, puzzle);
        }

        private static void handle(SessionResult message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        if (message.pos.equals(HackingDeviceMinigameClient.pos())) {
                            HackingDeviceMinigameClient.onResult(message.result,
                                    message.round, message.failures,
                                    message.puzzle);
                        }
                    }));
            context.setPacketHandled(true);
        }
    }

    public record BeginCooldown(BlockPos pos, long countdownEnd, long readyAt) {
        private static void encode(BeginCooldown message,
                FriendlyByteBuf buffer) {
            buffer.writeBlockPos(message.pos);
            buffer.writeLong(message.countdownEnd);
            buffer.writeLong(message.readyAt);
        }

        private static BeginCooldown decode(FriendlyByteBuf buffer) {
            return new BeginCooldown(buffer.readBlockPos(), buffer.readLong(),
                    buffer.readLong());
        }

        private static void handle(BeginCooldown message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        if (message.pos.equals(HackingDeviceMinigameClient.pos())) {
                            HackingDeviceMinigameClient.beginCooldown(
                                    message.countdownEnd, message.readyAt);
                        }
                    }));
            context.setPacketHandled(true);
        }
    }

    public record SubmitCandidate(BlockPos pos, int answer) {
        private static void encode(SubmitCandidate message,
                FriendlyByteBuf buffer) {
            buffer.writeBlockPos(message.pos);
            buffer.writeVarInt(message.answer);
        }

        private static SubmitCandidate decode(FriendlyByteBuf buffer) {
            return new SubmitCandidate(buffer.readBlockPos(),
                    buffer.readVarInt());
        }

        private static void handle(SubmitCandidate message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            ServerPlayer sender = context.getSender();
            context.enqueueWork(() -> HackingDeviceSessionManager.submit(sender,
                    message.pos, message.answer));
            context.setPacketHandled(true);
        }
    }

    public record ExitSession(BlockPos pos) {
        private static void encode(ExitSession message,
                FriendlyByteBuf buffer) {
            buffer.writeBlockPos(message.pos);
        }

        private static ExitSession decode(FriendlyByteBuf buffer) {
            return new ExitSession(buffer.readBlockPos());
        }

        private static void handle(ExitSession message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            ServerPlayer sender = context.getSender();
            context.enqueueWork(() -> HackingDeviceSessionManager.exit(sender,
                    message.pos));
            context.setPacketHandled(true);
        }
    }
}
