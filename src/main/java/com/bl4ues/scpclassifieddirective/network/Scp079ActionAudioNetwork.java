package com.bl4ues.scpclassifieddirective.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableAudioClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** Server-confirmed listener-only feedback for playable SCP-079 facility actions. */
public final class Scp079ActionAudioNetwork {
    private static boolean registered;

    private Scp079ActionAudioNetwork() { }

    public enum Cue {
        LOCK_OR_TESLA,
        DOOR_TOGGLE,
        ROOM_SWITCH,
        BLACKOUT,
        LOCKDOWN
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ScpClassifiedDirectiveMod.addNetworkMessage(Feedback.class,
                Feedback::encode, Feedback::decode, Feedback::handle);
    }

    public static void send(ServerPlayer player, Cue cue) {
        if (player == null || cue == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new Feedback(cue));
    }

    public record Feedback(Cue cue) {
        private static void encode(Feedback message, FriendlyByteBuf buffer) {
            buffer.writeEnum(message.cue);
        }

        private static Feedback decode(FriendlyByteBuf buffer) {
            return new Feedback(buffer.readEnum(Cue.class));
        }

        private static void handle(Feedback message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        switch (message.cue) {
                            case LOCK_OR_TESLA ->
                                    Scp079PlayableAudioClient.playLockOrTesla();
                            case DOOR_TOGGLE ->
                                    Scp079PlayableAudioClient.playDoorToggle();
                            case ROOM_SWITCH ->
                                    Scp079PlayableAudioClient.playRoomSwitch();
                            case BLACKOUT ->
                                    Scp079PlayableAudioClient.playButton();
                            case LOCKDOWN ->
                                    Scp079PlayableAudioClient.playLockOrTesla();
                        }
                    }));
            context.setPacketHandled(true);
        }
    }
}
