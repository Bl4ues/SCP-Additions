package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import net.minecraft.network.FriendlyByteBuf;
import com.bl4ues.scpadditions.compat.network.NetworkEvent;

import java.util.function.Supplier;

public record InventoryModuleStatePacket(boolean enabled) {
    public static void encode(InventoryModuleStatePacket message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.enabled);
    }

    public static InventoryModuleStatePacket decode(FriendlyByteBuf buffer) {
        return new InventoryModuleStatePacket(buffer.readBoolean());
    }

    public static void handle(InventoryModuleStatePacket message,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> InventoryModuleRuntimeState.updateFromServer(message.enabled));
        context.setPacketHandled(true);
    }
}
