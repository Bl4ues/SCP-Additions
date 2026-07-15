package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.config.ItemConfigManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.config.ui.ConfigCenterService;

import java.util.function.Supplier;

public class ItemConfigOpenRequestPacket {
    private final String itemId;

    public ItemConfigOpenRequestPacket(String itemId) {
        this.itemId = itemId == null ? "" : itemId;
    }

    public static void encode(ItemConfigOpenRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.itemId);
    }

    public static ItemConfigOpenRequestPacket decode(FriendlyByteBuf buf) {
        return new ItemConfigOpenRequestPacket(buf.readUtf());
    }

    public static void handle(ItemConfigOpenRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (ConfigCenterService.requireEdit(player)) {
                ItemConfigManager.openEditor(player, msg.itemId);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
