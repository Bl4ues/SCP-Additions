package com.bl4ues.scpclassifieddirective.network;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.Scp079SpeechManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client-to-server text channel used exclusively for SCP-079 generated speech. */
public final class Scp079SpeechNetwork {
    private static final int MAX_WIRE_TEXT = 256;
    private static boolean registered;

    private Scp079SpeechNetwork() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        ScpClassifiedDirectiveMod.addNetworkMessage(SpeechRequest.class,
                SpeechRequest::encode, SpeechRequest::decode,
                SpeechRequest::handle);
    }

    public static void request(String text) {
        if (text == null || text.isBlank()) return;
        String value = text.length() > MAX_WIRE_TEXT
                ? text.substring(0, MAX_WIRE_TEXT) : text;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
                new SpeechRequest(value));
    }

    public record SpeechRequest(String text) {
        private static void encode(SpeechRequest message,
                FriendlyByteBuf buffer) {
            buffer.writeUtf(message.text == null ? "" : message.text,
                    MAX_WIRE_TEXT);
        }

        private static SpeechRequest decode(FriendlyByteBuf buffer) {
            return new SpeechRequest(buffer.readUtf(MAX_WIRE_TEXT));
        }

        private static void handle(SpeechRequest message,
                Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    Scp079SpeechManager.request(player, message.text);
                }
            });
            context.setPacketHandled(true);
        }
    }
}
