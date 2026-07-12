package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.event.ScpInventoryMaintenanceEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UsableSessionReturnPacket {

    private final int hotbarSlot;

    public UsableSessionReturnPacket(int hotbarSlot) {
        this.hotbarSlot = hotbarSlot;
    }

    public static void encode(UsableSessionReturnPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.hotbarSlot);
    }

    public static UsableSessionReturnPacket decode(FriendlyByteBuf buf) {
        return new UsableSessionReturnPacket(buf.readInt());
    }

    public static void handle(UsableSessionReturnPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            ScpInventoryMaintenanceEvents.returnTrackedUsableSession(player, msg.hotbarSlot);
        });
        ctx.get().setPacketHandled(true);
    }
}
