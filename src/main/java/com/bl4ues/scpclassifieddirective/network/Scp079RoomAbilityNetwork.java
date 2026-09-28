package com.bl4ues.scpclassifieddirective.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.Scp079PlayableManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079RoomAbilityManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

/** Client requests for playable SCP-079 room abilities. */
public final class Scp079RoomAbilityNetwork {
    private static boolean registered;

    private Scp079RoomAbilityNetwork() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ScpClassifiedDirectiveMod.addNetworkMessage(AbilityRequest.class,
                AbilityRequest::encode, AbilityRequest::decode,
                AbilityRequest::handle);
        ScpClassifiedDirectiveMod.addNetworkMessage(BlackoutState.class,
                BlackoutState::encode, BlackoutState::decode,
                BlackoutState::handle);
    }

    public static void request(Scp079RoomAbilityManager.Ability ability) {
        if (ability == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new AbilityRequest(ability));
    }

    public record AbilityRequest(Scp079RoomAbilityManager.Ability ability) {
        private static void encode(AbilityRequest message,
                FriendlyByteBuf buffer) {
            buffer.writeEnum(message.ability);
        }

        private static AbilityRequest decode(FriendlyByteBuf buffer) {
            return new AbilityRequest(buffer.readEnum(
                    Scp079RoomAbilityManager.Ability.class));
        }

        private static void handle(AbilityRequest message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                FacilityRoomSnapshot blackoutRoom = player != null
                        && message.ability == Scp079RoomAbilityManager.Ability.BLACKOUT
                        ? Scp079PlayableManager.currentCameraRoom(player)
                        : null;
                if (player == null
                        || !Scp079RoomAbilityManager.use(player, message.ability)) {
                    return;
                }
                Scp079ActionAudioNetwork.send(player,
                        message.ability == Scp079RoomAbilityManager.Ability.BLACKOUT
                                ? Scp079ActionAudioNetwork.Cue.BLACKOUT
                                : Scp079ActionAudioNetwork.Cue.LOCKDOWN);
                if (message.ability == Scp079RoomAbilityManager.Ability.BLACKOUT
                        && blackoutRoom != null) {
                    ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new BlackoutState(blackoutRoom.id(),
                                    Scp079RoomAbilityManager.BLACKOUT_DURATION_TICKS));
                }
            });
            context.setPacketHandled(true);
        }
    public record BlackoutState(UUID roomId, int durationTicks) {
        private static void encode(BlackoutState message,
                FriendlyByteBuf buffer) {
            buffer.writeUUID(message.roomId);
            buffer.writeVarInt(message.durationTicks);
        }

        private static BlackoutState decode(FriendlyByteBuf buffer) {
            return new BlackoutState(buffer.readUUID(), buffer.readVarInt());
        }

        private static void handle(BlackoutState message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.bl4ues.scpclassifieddirective.client.scp079
                            .Scp079BlackoutAvailabilityClient.confirmBlackout(
                                    message.roomId,
                                    message.durationTicks)));
            context.setPacketHandled(true);
        }
    }

    }
}
