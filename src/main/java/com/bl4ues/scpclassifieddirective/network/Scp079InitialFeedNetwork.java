package com.bl4ues.scpclassifieddirective.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079BootSequenceClient;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayerPower;
import com.bl4ues.scpclassifieddirective.facility.Scp079RoomInteractionPolicy;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilitySurveillanceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** Resolves the first surveillance feed after SCP-079's local boot sequence. */
public final class Scp079InitialFeedNetwork {
    private static final double INITIAL_CAMERA_COST = 3.0D;
    private static boolean registered;

    private Scp079InitialFeedNetwork() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ScpClassifiedDirectiveMod.addNetworkMessage(Request.class,
                Request::encode, Request::decode, Request::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(Result.class,
                Result::encode, Result::decode, Result::handle);
    }

    public static void request() {
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(new Request());
    }

    private static FacilityRoomSnapshot hostRoom(ServerLevel level,
            BlockPos hostPos) {
        if (level == null || hostPos == null) return null;
        for (FacilityRoomSnapshot room : FacilityMappingManager.roomSnapshots(level)) {
            if (room.containsColumn(hostPos)) return room;
        }
        for (FacilityRoomSnapshot room : FacilityMappingManager.roomSnapshots(level)) {
            if (Scp079RoomInteractionPolicy.withinExpandedFloor(
                    room, hostPos, 1)) return room;
        }
        return null;
    }

    public record Request() {
        private static void encode(Request message, FriendlyByteBuf buffer) {
        }

        private static Request decode(FriendlyByteBuf buffer) {
            return new Request();
        }

        private static void handle(Request message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                boolean opened = false;
                if (player != null && Scp079PlayableManager.isController(player)
                        && !Scp079PlayableManager.isCameraMode(player)) {
                    ServerLevel level = player.serverLevel();
                    BlockPos hostPos = Scp079PlayableManager.hostPosition(player);
                    FacilityRoomSnapshot room = hostRoom(level, hostPos);
                    if (room != null && !FacilitySurveillanceRegistry
                            .camerasForRoom(level, room.id()).isEmpty()) {
                        opened = Scp079PlayableManager.switchToRoom(
                                player, room.id())
                                && Scp079PlayableManager.isCameraMode(player);
                        if (opened) {
                            // Initial acquisition is presentation, not navigation:
                            // return the normal room-switch charge after the
                            // authoritative switch has succeeded.
                            Scp079PlayerPower.refund(level, INITIAL_CAMERA_COST);
                        }
                    }
                }
                if (player != null) {
                    boolean result = opened;
                    ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new Result(result));
                }
            });
            context.setPacketHandled(true);
        }
    }

    public record Result(boolean cameraOpened) {
        private static void encode(Result message, FriendlyByteBuf buffer) {
            buffer.writeBoolean(message.cameraOpened);
        }

        private static Result decode(FriendlyByteBuf buffer) {
            return new Result(buffer.readBoolean());
        }

        private static void handle(Result message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> Scp079BootSequenceClient.receiveInitialFeed(
                            message.cameraOpened)));
            context.setPacketHandled(true);
        }
    }
}
