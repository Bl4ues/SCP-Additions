package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.event.ScpInventoryMaintenanceEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.bl4ues.scpadditions.compat.network.NetworkEvent;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

import java.util.function.Supplier;

public class UsableSessionDropPacket {

    private final int hotbarSlot;

    public UsableSessionDropPacket(int hotbarSlot) {
        this.hotbarSlot = hotbarSlot;
    }

    public static void encode(UsableSessionDropPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.hotbarSlot);
    }

    public static UsableSessionDropPacket decode(FriendlyByteBuf buf) {
        return new UsableSessionDropPacket(buf.readInt());
    }

    public static void handle(UsableSessionDropPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (!ScpAdditionsModulesConfig.get().inventory.enabled) return;
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            ScpInventoryMaintenanceEvents.dropTrackedUsableSession(player, msg.hotbarSlot);
        });
        ctx.get().setPacketHandled(true);
    }
}
