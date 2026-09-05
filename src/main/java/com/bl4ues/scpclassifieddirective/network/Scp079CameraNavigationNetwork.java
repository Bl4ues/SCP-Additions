package com.bl4ues.scpclassifieddirective.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079RoomInteractionPolicy;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilityCameraDefinition;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilitySurveillanceRegistry;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilitySurveillanceSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Network topology and directional camera navigation for playable SCP-079. */
public final class Scp079CameraNavigationNetwork {
    private static boolean registered;

    private Scp079CameraNavigationNetwork() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ScpClassifiedDirectiveMod.addNetworkMessage(TopologyRequest.class,
                TopologyRequest::encode, TopologyRequest::decode,
                TopologyRequest::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(TopologyState.class,
                TopologyState::encode, TopologyState::decode,
                TopologyState::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(SwitchCameraRequest.class,
                SwitchCameraRequest::encode, SwitchCameraRequest::decode,
                SwitchCameraRequest::handle);
    }

    public static void requestTopology() {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new TopologyRequest());
    }

    public static void requestCamera(UUID cameraId) {
        if (cameraId == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new SwitchCameraRequest(cameraId));
    }

    private static void sendTopology(ServerPlayer player) {
        if (player == null || !Scp079PlayableManager.isController(player)) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        ServerLevel level = player.serverLevel();
        List<FacilityRoomSnapshot> rooms = FacilityMappingManager.roomSnapshots(level);
        List<CameraNode> nodes = new ArrayList<>();
        for (FacilityCameraDefinition raw : FacilitySurveillanceSavedData
                .get(server).all()) {
            if (!raw.dimension().equals(level.dimension().location())) continue;
            FacilityCameraDefinition camera = FacilitySurveillanceRegistry.camera(
                    level, raw.id());
            if (camera == null) continue;
            FacilityRoomSnapshot room = roomForCamera(rooms, camera);
            if (room == null) continue;
            nodes.add(new CameraNode(camera.id(), room.id(), room.name(),
                    camera.anchorPos(), camera.eyePosition().x,
                    camera.eyePosition().y, camera.eyePosition().z,
                    camera.baseYaw()));
        }
        nodes.sort(Comparator.comparing(CameraNode::roomName,
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(node -> node.cameraId().toString()));
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new TopologyState(List.copyOf(nodes)));
    }

    private static FacilityRoomSnapshot roomForCamera(
            List<FacilityRoomSnapshot> rooms, FacilityCameraDefinition camera) {
        BlockPos eye = BlockPos.containing(camera.eyePosition());
        for (FacilityRoomSnapshot room : rooms) {
            if (room.containsColumn(eye)) return room;
        }
        for (FacilityRoomSnapshot room : rooms) {
            if (Scp079RoomInteractionPolicy.withinExpandedFloor(room,
                    camera.anchorPos(), 1)) return room;
        }
        return null;
    }

    public record CameraNode(UUID cameraId, UUID roomId, String roomName,
            BlockPos anchorPos, double x, double y, double z, float baseYaw) {
        public CameraNode {
            roomName = roomName == null ? "" : roomName;
            anchorPos = anchorPos == null ? BlockPos.ZERO : anchorPos.immutable();
        }

        private static void write(FriendlyByteBuf buffer, CameraNode node) {
            buffer.writeUUID(node.cameraId);
            buffer.writeUUID(node.roomId);
            buffer.writeUtf(node.roomName, 96);
            buffer.writeBlockPos(node.anchorPos);
            buffer.writeDouble(node.x);
            buffer.writeDouble(node.y);
            buffer.writeDouble(node.z);
            buffer.writeFloat(node.baseYaw);
        }

        private static CameraNode read(FriendlyByteBuf buffer) {
            return new CameraNode(buffer.readUUID(), buffer.readUUID(),
                    buffer.readUtf(96), buffer.readBlockPos(),
                    buffer.readDouble(), buffer.readDouble(),
                    buffer.readDouble(), buffer.readFloat());
        }
    }

    public record TopologyRequest() {
        private static void encode(TopologyRequest message,
                FriendlyByteBuf buffer) { }

        private static TopologyRequest decode(FriendlyByteBuf buffer) {
            return new TopologyRequest();
        }

        private static void handle(TopologyRequest message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> sendTopology(context.getSender()));
            context.setPacketHandled(true);
        }
    }

    public record TopologyState(List<CameraNode> nodes) {
        public TopologyState {
            nodes = nodes == null ? List.of() : List.copyOf(nodes);
        }

        private static void encode(TopologyState message,
                FriendlyByteBuf buffer) {
            int count = Math.min(512, message.nodes.size());
            buffer.writeVarInt(count);
            for (int index = 0; index < count; index++) {
                CameraNode.write(buffer, message.nodes.get(index));
            }
        }

        private static TopologyState decode(FriendlyByteBuf buffer) {
            int count = Math.max(0, Math.min(512, buffer.readVarInt()));
            List<CameraNode> nodes = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                nodes.add(CameraNode.read(buffer));
            }
            return new TopologyState(nodes);
        }

        private static void handle(TopologyState message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bl4ues.scpclassifieddirective.client.scp079.Scp079CameraNetworkClientState
                            .update(message.nodes)));
            context.setPacketHandled(true);
        }
    }

    public record SwitchCameraRequest(UUID cameraId) {
        private static void encode(SwitchCameraRequest message,
                FriendlyByteBuf buffer) {
            buffer.writeUUID(message.cameraId);
        }

        private static SwitchCameraRequest decode(FriendlyByteBuf buffer) {
            return new SwitchCameraRequest(buffer.readUUID());
        }

        private static void handle(SwitchCameraRequest message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    Scp079PlayableManager.switchToCamera(player,
                            message.cameraId);
                }
            });
            context.setPacketHandled(true);
        }
    }
}
