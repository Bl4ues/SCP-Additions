package com.bl4ues.scpclassifieddirective.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.HackingDeviceAudioClient;
import com.bl4ues.scpclassifieddirective.client.HackingDeviceClientState;
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

/** Synchronizes attached Hacking Devices and placement/removal focus cues. */
public final class HackingDeviceNetwork {
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
        ScpClassifiedDirectiveMod.addNetworkMessage(FocusCue.class,
                FocusCue::encode, FocusCue::decode, FocusCue::handle);
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

    public static void focus(ServerPlayer player, BlockPos pos,
            boolean attached) {
        if (player == null || pos == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new FocusCue(pos, attached));
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
                    () -> () -> {
                        HackingDeviceClientState.update(message.pos,
                                message.attached);
                        HackingDeviceAudioClient.playAttachmentCue(message.pos,
                                message.attached);
                    }));
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

    public record FocusCue(BlockPos pos, boolean attached) {
        private static void encode(FocusCue message, FriendlyByteBuf buffer) {
            buffer.writeBlockPos(message.pos);
            buffer.writeBoolean(message.attached);
        }

        private static FocusCue decode(FriendlyByteBuf buffer) {
            return new FocusCue(buffer.readBlockPos(), buffer.readBoolean());
        }

        private static void handle(FocusCue message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bl4ues.scpclassifieddirective.client.HackingDeviceFocusClient
                            .onServerCue(message.pos, message.attached)));
            context.setPacketHandled(true);
        }
    }
}
