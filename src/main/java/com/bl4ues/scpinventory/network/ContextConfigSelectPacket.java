package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.context.ContextEntityConfigManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ContextConfigSelectPacket {
    public ContextConfigSelectPacket() {
    }

    public static void encode(ContextConfigSelectPacket msg, FriendlyByteBuf buf) {
    }

    public static ContextConfigSelectPacket decode(FriendlyByteBuf buf) {
        return new ContextConfigSelectPacket();
    }

    public static void handle(ContextConfigSelectPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && !player.isSpectator()) {
                ContextEntityConfigManager.openGuiForLookedTarget(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
