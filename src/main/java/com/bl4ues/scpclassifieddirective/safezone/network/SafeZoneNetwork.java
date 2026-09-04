package com.bl4ues.scpclassifieddirective.safezone.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.safezone.DiscoveryMusicManager;
import com.bl4ues.scpclassifieddirective.safezone.SafeZone;
import com.bl4ues.scpclassifieddirective.safezone.SafeZoneManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Packets for Safe Zone Tool selection, editing and client zone state. */
public final class SafeZoneNetwork {
    private static boolean registered;

    private SafeZoneNetwork() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ScpClassifiedDirectiveMod.addNetworkMessage(SelectionStart.class,
                SelectionStart::encode, SelectionStart::decode,
                SelectionStart::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(SelectionState.class,
                SelectionState::encode, SelectionState::decode,
                SelectionState::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(ZoneSync.class,
                ZoneSync::encode, ZoneSync::decode, ZoneSync::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(OpenEditor.class,
                OpenEditor::encode, OpenEditor::decode, OpenEditor::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(UpdateZone.class,
                UpdateZone::encode, UpdateZone::decode, UpdateZone::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(DeleteZone.class,
                DeleteZone::encode, DeleteZone::decode, DeleteZone::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(DiscoveryCue.class,
                DiscoveryCue::encode, DiscoveryCue::decode,
                DiscoveryCue::handle);
    }

    public static void requestSelectionStart(BlockPos pos) {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new SelectionStart(pos));
    }

    public static void sendSelectionState(ServerPlayer player,
            BlockPos first) {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SelectionState(first));
    }

    public static ZoneSync zoneSync(ResourceLocation dimension,
            List<SafeZone> zones) {
        return new ZoneSync(dimension, zones);
    }

    public static void sendZones(ServerPlayer player,
            ResourceLocation dimension, List<SafeZone> zones) {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                zoneSync(dimension, zones));
    }

    public static void openEditor(ServerPlayer player, SafeZone zone) {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new OpenEditor(zone));
    }

    public static void updateZone(UUID id, boolean musicEnabled,
            String trackId) {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new UpdateZone(id, musicEnabled, trackId));
    }

    public static void deleteZone(UUID id) {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new DeleteZone(id));
    }

    public static void sendDiscoveryCue(ServerPlayer player,
            DiscoveryMusicManager.Cue cue) {
        if (player == null || cue == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new DiscoveryCue(cue));
    }

    private static void writeZone(FriendlyByteBuf buffer, SafeZone zone) {
        buffer.writeUUID(zone.id());
        buffer.writeResourceLocation(zone.dimension());
        buffer.writeBlockPos(zone.min());
        buffer.writeBlockPos(zone.max());
        buffer.writeBoolean(zone.musicEnabled());
        buffer.writeUtf(zone.musicTrack(), 64);
        buffer.writeUtf(zone.automaticTrack(), 64);
    }

    private static SafeZone readZone(FriendlyByteBuf buffer) {
        return new SafeZone(buffer.readUUID(), buffer.readResourceLocation(),
                buffer.readBlockPos(), buffer.readBlockPos(),
                buffer.readBoolean(), buffer.readUtf(64),
                buffer.readUtf(64));
    }

    public record SelectionStart(BlockPos pos) {
        public SelectionStart {
            pos = pos.immutable();
        }

        private static void encode(SelectionStart message,
                FriendlyByteBuf buffer) {
            buffer.writeBlockPos(message.pos);
        }

        private static SelectionStart decode(FriendlyByteBuf buffer) {
            return new SelectionStart(buffer.readBlockPos());
        }

        private static void handle(SelectionStart message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> SafeZoneManager.beginSelection(
                    context.getSender(), message.pos));
            context.setPacketHandled(true);
        }
    }

    public record SelectionState(BlockPos first) {
        private static void encode(SelectionState message,
                FriendlyByteBuf buffer) {
            buffer.writeBoolean(message.first != null);
            if (message.first != null) buffer.writeBlockPos(message.first);
        }

        private static SelectionState decode(FriendlyByteBuf buffer) {
            return new SelectionState(buffer.readBoolean()
                    ? buffer.readBlockPos() : null);
        }

        private static void handle(SelectionState message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bl4ues.scpclassifieddirective.safezone.client.SafeZoneClientState
                            .setSelectionStart(message.first)));
            context.setPacketHandled(true);
        }
    }

    public record ZoneSync(ResourceLocation dimension, List<SafeZone> zones) {
        public ZoneSync {
            zones = zones == null ? List.of() : List.copyOf(zones);
        }

        private static void encode(ZoneSync message,
                FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(message.dimension);
            buffer.writeVarInt(message.zones.size());
            for (SafeZone zone : message.zones) writeZone(buffer, zone);
        }

        private static ZoneSync decode(FriendlyByteBuf buffer) {
            ResourceLocation dimension = buffer.readResourceLocation();
            int count = Math.max(0, Math.min(buffer.readVarInt(), 4096));
            List<SafeZone> zones = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                zones.add(readZone(buffer));
            }
            return new ZoneSync(dimension, zones);
        }

        private static void handle(ZoneSync message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bl4ues.scpclassifieddirective.safezone.client.SafeZoneClientState
                            .sync(message.dimension, message.zones)));
            context.setPacketHandled(true);
        }
    }

    public record OpenEditor(SafeZone zone) {
        private static void encode(OpenEditor message,
                FriendlyByteBuf buffer) {
            writeZone(buffer, message.zone);
        }

        private static OpenEditor decode(FriendlyByteBuf buffer) {
            return new OpenEditor(readZone(buffer));
        }

        private static void handle(OpenEditor message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bl4ues.scpclassifieddirective.safezone.client.SafeZoneEditorScreen
                            .open(message.zone)));
            context.setPacketHandled(true);
        }
    }

    public record UpdateZone(UUID id, boolean musicEnabled, String trackId) {
        private static void encode(UpdateZone message,
                FriendlyByteBuf buffer) {
            buffer.writeUUID(message.id);
            buffer.writeBoolean(message.musicEnabled);
            buffer.writeUtf(message.trackId == null ? "" : message.trackId,
                    64);
        }

        private static UpdateZone decode(FriendlyByteBuf buffer) {
            return new UpdateZone(buffer.readUUID(), buffer.readBoolean(),
                    buffer.readUtf(64));
        }

        private static void handle(UpdateZone message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> SafeZoneManager.update(
                    context.getSender(), message.id, message.musicEnabled,
                    message.trackId));
            context.setPacketHandled(true);
        }
    }

    public record DeleteZone(UUID id) {
        private static void encode(DeleteZone message,
                FriendlyByteBuf buffer) {
            buffer.writeUUID(message.id);
        }

        private static DeleteZone decode(FriendlyByteBuf buffer) {
            return new DeleteZone(buffer.readUUID());
        }

        private static void handle(DeleteZone message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> SafeZoneManager.delete(
                    context.getSender(), message.id));
            context.setPacketHandled(true);
        }
    }

    public record DiscoveryCue(DiscoveryMusicManager.Cue cue) {
        private static void encode(DiscoveryCue message,
                FriendlyByteBuf buffer) {
            buffer.writeVarInt(message.cue.ordinal());
        }

        private static DiscoveryCue decode(FriendlyByteBuf buffer) {
            return new DiscoveryCue(DiscoveryMusicManager.Cue.byNetworkId(
                    buffer.readVarInt()));
        }

        private static void handle(DiscoveryCue message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bl4ues.scpclassifieddirective.safezone.client.DiscoveryMusicClient
                            .play(message.cue)));
            context.setPacketHandled(true);
        }
    }
}
