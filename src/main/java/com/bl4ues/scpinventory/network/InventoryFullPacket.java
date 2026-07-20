package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.bl4ues.scpadditions.compat.DistExecutor;
import com.bl4ues.scpadditions.compat.network.NetworkEvent;

import java.util.function.Supplier;

public class InventoryFullPacket {

    public InventoryFullPacket() {
    }

    public static void encode(InventoryFullPacket msg, FriendlyByteBuf buf) {
    }

    public static InventoryFullPacket decode(FriendlyByteBuf buf) {
        return new InventoryFullPacket();
    }

    public static void handle(InventoryFullPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientPacketHandlers.showInventoryFullOverlay()
                )
        );
        ctx.get().setPacketHandled(true);
    }
}
