package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.client.ItemConfigClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ItemConfigReloadPacket {
    public ItemConfigReloadPacket() {
    }

    public static void encode(ItemConfigReloadPacket msg, FriendlyByteBuf buf) {
    }

    public static ItemConfigReloadPacket decode(FriendlyByteBuf buf) {
        return new ItemConfigReloadPacket();
    }

    public static void handle(ItemConfigReloadPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(ItemConfigClientHandler::reloadItemConfig);
        ctx.get().setPacketHandled(true);
    }
}
