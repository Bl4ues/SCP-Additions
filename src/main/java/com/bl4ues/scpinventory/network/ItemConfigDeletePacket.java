package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.config.ItemConfigManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.NetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.mcreator.scpadditions.config.ui.ConfigCenterService;

import java.util.function.Supplier;

public class ItemConfigDeletePacket {
    private final String itemId;

    public ItemConfigDeletePacket(String itemId) {
        this.itemId = itemId == null ? "" : itemId;
    }

    public static void encode(ItemConfigDeletePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.itemId);
    }

    public static ItemConfigDeletePacket decode(FriendlyByteBuf buf) {
        return new ItemConfigDeletePacket(buf.readUtf());
    }

    public static void handle(ItemConfigDeletePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (!ConfigCenterService.requireEdit(player)) return;
            ItemConfigManager.deleteRule(player, msg.itemId);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ItemConfigReloadPacket());
        });
        ctx.get().setPacketHandled(true);
    }
}
