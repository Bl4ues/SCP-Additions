package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import com.bl4ues.scpclassifieddirective.death.DeathSpectateCoordinator;

import java.util.function.Supplier;

/** Client request for querying, starting, cycling or stopping a death live feed. */
public record DeathSpectateRequestPacket(int action) {
    public static void encode(DeathSpectateRequestPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.action);
    }

    public static DeathSpectateRequestPacket decode(FriendlyByteBuf buffer) {
        return new DeathSpectateRequestPacket(buffer.readVarInt());
    }

    public static void handle(DeathSpectateRequestPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            context.enqueueWork(() -> DeathSpectateCoordinator.handleRequest(
                    sender, message.action));
        }
        context.setPacketHandled(true);
    }
}
