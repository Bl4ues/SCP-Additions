package com.bl4ues.scpclassifieddirective.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079RoomInteractionPolicy;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
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
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Server-authoritative switch path used only by physical camera prompts. */
public final class Scp079AdjacentCameraSwitchNetwork {
    private static final int ADJACENT_ROOM_GAP = 2;
    private static boolean registered;

    private Scp079AdjacentCameraSwitchNetwork() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ScpClassifiedDirectiveMod.addNetworkMessage(Request.class,
                Request::encode, Request::decode, Request::handle);
    }

    public static void request(UUID roomId) {
        if (roomId == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(new Request(roomId));
    }

    private static boolean switchToAdjacentRoom(ServerPlayer player, UUID roomId) {
        if (player == null || roomId == null
                || !Scp079PlayableManager.isCameraMode(player)) return false;
        MinecraftServer server = player.getServer();
        if (server == null) return false;
        ServerLevel level = player.serverLevel();
        List<FacilityRoomSnapshot> rooms = FacilityMappingManager.roomSnapshots(level);

        FacilityCameraDefinition currentCamera =
                Scp079PlayableManager.currentCamera(player);
        FacilityRoomSnapshot currentRoom =
                FacilityMappingManager.roomSnapshotForCamera(level,
                        currentCamera);
        FacilityRoomSnapshot targetRoom = roomById(rooms, roomId);
        if (currentRoom == null || targetRoom == null
                || currentRoom.id().equals(targetRoom.id())
                || !adjacent(currentRoom, targetRoom)) {
            return false;
        }

        FacilityCameraDefinition target = FacilitySurveillanceRegistry
                .nextForRoom(level, targetRoom.id(), null);
        return target != null
                && Scp079PlayableManager.switchToCamera(player, target.id());
    }

    private static FacilityRoomSnapshot roomById(
            List<FacilityRoomSnapshot> rooms, UUID id) {
        for (FacilityRoomSnapshot room : rooms) {
            if (id.equals(room.id())) return room;
        }
        return null;
    }

    private static boolean adjacent(FacilityRoomSnapshot a,
            FacilityRoomSnapshot b) {
        if (!a.floorLongLabel().equalsIgnoreCase(b.floorLongLabel())) return false;
        for (FacilityFloorPatch pa : a.patches()) {
            for (FacilityFloorPatch pb : b.patches()) {
                if (Math.abs(pa.y() - pb.y()) > 3) continue;
                int gapX = intervalGap(pa.minX(), pa.maxX(),
                        pb.minX(), pb.maxX());
                int gapZ = intervalGap(pa.minZ(), pa.maxZ(),
                        pb.minZ(), pb.maxZ());
                if (Math.max(gapX, gapZ) <= ADJACENT_ROOM_GAP) return true;
            }
        }
        return false;
    }

    private static int intervalGap(int amin, int amax, int bmin, int bmax) {
        if (amax < bmin) return bmin - amax - 1;
        if (bmax < amin) return amin - bmax - 1;
        return 0;
    }

    public record Request(UUID roomId) {
        private static void encode(Request message, FriendlyByteBuf buffer) {
            buffer.writeUUID(message.roomId);
        }

        private static Request decode(FriendlyByteBuf buffer) {
            return new Request(buffer.readUUID());
        }

        private static void handle(Request message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (switchToAdjacentRoom(player, message.roomId)) {
                    Scp079ActionAudioNetwork.send(player,
                            Scp079ActionAudioNetwork.Cue.ROOM_SWITCH);
                }
            });
            context.setPacketHandled(true);
        }
    }
}
