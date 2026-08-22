package com.bl4ues.scpclassifieddirective.inventory.network;

import com.bl4ues.scpclassifieddirective.inventory.client.ContextConfigClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ContextConfigReloadPacket {
    public ContextConfigReloadPacket() {
    }

    public static void encode(ContextConfigReloadPacket msg, FriendlyByteBuf buf) {
    }

    public static ContextConfigReloadPacket decode(FriendlyByteBuf buf) {
        return new ContextConfigReloadPacket();
    }

    public static void handle(ContextConfigReloadPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(ContextConfigClientHandler::reloadContextConfig);
        ctx.get().setPacketHandled(true);
    }
}
