package com.bl4ues.scpclassifieddirective.inventory.network;

import com.bl4ues.scpclassifieddirective.inventory.event.PlaceableHotbarSessionEvents;
import com.bl4ues.scpclassifieddirective.inventory.event.ScpInventoryMaintenanceEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;

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
            if (!ScpClassifiedDirectiveModulesConfig.get().inventory.enabled) return;
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            if (PlaceableHotbarSessionEvents.dropTrackedPlaceableSession(
                    player, msg.hotbarSlot)) {
                return;
            }
            ScpInventoryMaintenanceEvents.dropTrackedUsableSession(
                    player, msg.hotbarSlot);
        });
        ctx.get().setPacketHandled(true);
    }
}
