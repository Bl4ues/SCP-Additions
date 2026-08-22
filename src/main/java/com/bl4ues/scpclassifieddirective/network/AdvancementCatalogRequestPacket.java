package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client request for the complete server advancement catalog. */
public final class AdvancementCatalogRequestPacket {
    public static void encode(AdvancementCatalogRequestPacket message,
            FriendlyByteBuf buffer) {
    }

    public static AdvancementCatalogRequestPacket decode(FriendlyByteBuf buffer) {
        return new AdvancementCatalogRequestPacket();
    }

    public static void handle(AdvancementCatalogRequestPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> {
            if (sender != null) ScpEntityNetwork.sendAdvancementCatalog(sender);
        });
        context.setPacketHandled(true);
    }
}
