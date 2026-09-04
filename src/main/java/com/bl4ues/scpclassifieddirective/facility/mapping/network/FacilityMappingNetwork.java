package com.bl4ues.scpclassifieddirective.facility.mapping.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorStationOption;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
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

/** Packets for the Facility Mapping Tool and client map geometry. */
public final class FacilityMappingNetwork {
    private static boolean registered;

    private FacilityMappingNetwork() {
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
        ScpClassifiedDirectiveMod.addNetworkMessage(RoomSync.class,
                RoomSync::encode, RoomSync::decode, RoomSync::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(OpenEditor.class,
                OpenEditor::encode, OpenEditor::decode, OpenEditor::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(UpdateRoom.class,
                UpdateRoom::encode, UpdateRoom::decode, UpdateRoom::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(DeleteRoom.class,
                DeleteRoom::encode, DeleteRoom::decode, DeleteRoom::handle);
    }

    public static void requestSelectionStart(BlockPos pos) {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new SelectionStart(pos));
    }

    public static void sendSelectionState(ServerPlayer player, BlockPos first) {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SelectionState(first));
    }

    public static RoomSync roomSync(ResourceLocation dimension,
            List<FacilityRoomSnapshot> rooms) {
        return new RoomSync(dimension, rooms);
    }

    public static void sendRooms(ServerPlayer player, ResourceLocation dimension,
            List<FacilityRoomSnapshot> rooms) {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                roomSync(dimension, rooms));
    }

    public static void openEditor(ServerPlayer player,
            FacilityRoomSnapshot room,
            List<FacilityFloorStationOption> stations) {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new OpenEditor(room, stations));
    }

    public static void updateRoom(UUID id, String name, BlockPos station) {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new UpdateRoom(id, name, station));
    }

    public static void deleteRoom(UUID id) {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new DeleteRoom(id));
    }

    private static void writePatch(FriendlyByteBuf buffer,
            FacilityFloorPatch patch) {
        buffer.writeInt(patch.minX());
        buffer.writeInt(patch.y());
        buffer.writeInt(patch.minZ());
        buffer.writeInt(patch.maxX());
        buffer.writeInt(patch.maxZ());
    }

    private static FacilityFloorPatch readPatch(FriendlyByteBuf buffer) {
        return new FacilityFloorPatch(buffer.readInt(), buffer.readInt(),
                buffer.readInt(), buffer.readInt(), buffer.readInt());
    }

    private static void writeRoom(FriendlyByteBuf buffer,
            FacilityRoomSnapshot room) {
        buffer.writeUUID(room.id());
        buffer.writeResourceLocation(room.dimension());
        buffer.writeVarInt(room.patches().size());
        for (FacilityFloorPatch patch : room.patches()) writePatch(buffer, patch);
        buffer.writeUtf(room.name(), 64);
        buffer.writeBoolean(room.floorStation() != null);
        if (room.floorStation() != null) buffer.writeBlockPos(room.floorStation());
        buffer.writeUtf(room.floorLongLabel(), 96);
        buffer.writeUtf(room.floorShortLabel(), 32);
    }

    private static FacilityRoomSnapshot readRoom(FriendlyByteBuf buffer) {
        UUID id = buffer.readUUID();
        ResourceLocation dimension = buffer.readResourceLocation();
        int count = Math.max(0, Math.min(buffer.readVarInt(), 2048));
        List<FacilityFloorPatch> patches = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            patches.add(readPatch(buffer));
        }
        String name = buffer.readUtf(64);
        BlockPos station = buffer.readBoolean() ? buffer.readBlockPos() : null;
        return new FacilityRoomSnapshot(id, dimension, patches, name, station,
                buffer.readUtf(96), buffer.readUtf(32));
    }

    private static void writeStation(FriendlyByteBuf buffer,
            FacilityFloorStationOption station) {
        buffer.writeBlockPos(station.pos());
        buffer.writeUtf(station.longLabel(), 96);
        buffer.writeUtf(station.shortLabel(), 32);
    }

    private static FacilityFloorStationOption readStation(
            FriendlyByteBuf buffer) {
        return new FacilityFloorStationOption(buffer.readBlockPos(),
                buffer.readUtf(96), buffer.readUtf(32));
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
            context.enqueueWork(() -> FacilityMappingManager.beginSelection(
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
                    () -> () -> com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState
                            .setSelectionStart(message.first)));
            context.setPacketHandled(true);
        }
    }

    public record RoomSync(ResourceLocation dimension,
            List<FacilityRoomSnapshot> rooms) {
        public RoomSync {
            rooms = rooms == null ? List.of() : List.copyOf(rooms);
        }

        private static void encode(RoomSync message, FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(message.dimension);
            buffer.writeVarInt(message.rooms.size());
            for (FacilityRoomSnapshot room : message.rooms) writeRoom(buffer, room);
        }

        private static RoomSync decode(FriendlyByteBuf buffer) {
            ResourceLocation dimension = buffer.readResourceLocation();
            int count = Math.max(0, Math.min(buffer.readVarInt(), 4096));
            List<FacilityRoomSnapshot> rooms = new ArrayList<>(count);
            for (int index = 0; index < count; index++) rooms.add(readRoom(buffer));
            return new RoomSync(dimension, rooms);
        }

        private static void handle(RoomSync message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityMappingClientState
                            .sync(message.dimension, message.rooms)));
            context.setPacketHandled(true);
        }
    }

    public record OpenEditor(FacilityRoomSnapshot room,
            List<FacilityFloorStationOption> stations) {
        public OpenEditor {
            stations = stations == null ? List.of() : List.copyOf(stations);
        }

        private static void encode(OpenEditor message, FriendlyByteBuf buffer) {
            writeRoom(buffer, message.room);
            buffer.writeVarInt(message.stations.size());
            for (FacilityFloorStationOption station : message.stations) {
                writeStation(buffer, station);
            }
        }

        private static OpenEditor decode(FriendlyByteBuf buffer) {
            FacilityRoomSnapshot room = readRoom(buffer);
            int count = Math.max(0, Math.min(buffer.readVarInt(), 1024));
            List<FacilityFloorStationOption> stations = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                stations.add(readStation(buffer));
            }
            return new OpenEditor(room, stations);
        }

        private static void handle(OpenEditor message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bl4ues.scpclassifieddirective.facility.mapping.client.FacilityRoomEditorScreen
                            .open(message.room, message.stations)));
            context.setPacketHandled(true);
        }
    }

    public record UpdateRoom(UUID id, String name, BlockPos station) {
        private static void encode(UpdateRoom message, FriendlyByteBuf buffer) {
            buffer.writeUUID(message.id);
            buffer.writeUtf(message.name == null ? "" : message.name, 64);
            buffer.writeBoolean(message.station != null);
            if (message.station != null) buffer.writeBlockPos(message.station);
        }

        private static UpdateRoom decode(FriendlyByteBuf buffer) {
            UUID id = buffer.readUUID();
            String name = buffer.readUtf(64);
            BlockPos station = buffer.readBoolean() ? buffer.readBlockPos() : null;
            return new UpdateRoom(id, name, station);
        }

        private static void handle(UpdateRoom message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> FacilityMappingManager.updateRoom(
                    context.getSender(), message.id, message.name,
                    message.station));
            context.setPacketHandled(true);
        }
    }

    public record DeleteRoom(UUID id) {
        private static void encode(DeleteRoom message, FriendlyByteBuf buffer) {
            buffer.writeUUID(message.id);
        }

        private static DeleteRoom decode(FriendlyByteBuf buffer) {
            return new DeleteRoom(buffer.readUUID());
        }

        private static void handle(DeleteRoom message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> FacilityMappingManager.deleteRoom(
                    context.getSender(), message.id));
            context.setPacketHandled(true);
        }
    }
}
