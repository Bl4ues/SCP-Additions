package com.bl4ues.scpclassifieddirective.inventory.network;

import com.bl4ues.scpclassifieddirective.inventory.config.ItemConfigManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import com.bl4ues.scpclassifieddirective.config.ui.ConfigCenterService;

import java.util.function.Supplier;

public class ItemConfigSavePacket {
    private final String itemId;
    private final String type;
    private final String consumableType;
    private final boolean noStamina;
    private final boolean protectedEyes;

    public ItemConfigSavePacket(String itemId, String type,
            String consumableType, boolean noStamina, boolean protectedEyes) {
        this.itemId = itemId == null ? "" : itemId;
        this.type = type == null ? "MISCELLANEOUS" : type;
        this.consumableType = consumableType == null ? "FOOD" : consumableType;
        this.noStamina = noStamina;
        this.protectedEyes = protectedEyes;
    }

    public ItemConfigSavePacket(String itemId, String type,
            boolean noStamina, boolean protectedEyes) {
        this(itemId, type, "FOOD", noStamina, protectedEyes);
    }

    public static void encode(ItemConfigSavePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.itemId);
        buf.writeUtf(msg.type);
        buf.writeUtf(msg.consumableType);
        buf.writeBoolean(msg.noStamina);
        buf.writeBoolean(msg.protectedEyes);
    }

    public static ItemConfigSavePacket decode(FriendlyByteBuf buf) {
        String itemId = buf.readUtf();
        String type = buf.readUtf();
        String consumableType = buf.readUtf();
        boolean noStamina = buf.readBoolean();
        boolean protectedEyes = buf.readBoolean();
        return new ItemConfigSavePacket(itemId, type, consumableType,
                noStamina, protectedEyes);
    }

    public static void handle(ItemConfigSavePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (!ConfigCenterService.requireEdit(player)) return;
            ItemConfigManager.saveRule(player, msg.itemId, msg.type,
                    msg.consumableType, msg.noStamina, msg.protectedEyes);
        });
        ctx.get().setPacketHandled(true);
    }
}
