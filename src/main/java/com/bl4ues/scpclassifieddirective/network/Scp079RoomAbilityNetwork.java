package com.bl4ues.scpclassifieddirective.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.Scp079RoomAbilityManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

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
                if (player == null
                        || !Scp079RoomAbilityManager.use(player, message.ability)) {
                    return;
                }
                Scp079ActionAudioNetwork.send(player,
                        message.ability == Scp079RoomAbilityManager.Ability.BLACKOUT
                                ? Scp079ActionAudioNetwork.Cue.BLACKOUT
                                : Scp079ActionAudioNetwork.Cue.LOCKDOWN);
            });
            context.setPacketHandled(true);
        }
    }
}
