package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.client.ItemConfigClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ItemConfigOpenPacket {
    private final String itemId;
    private final boolean existing;
    private final String type;
    private final boolean noStamina;
    private final boolean protectedEyes;

    public ItemConfigOpenPacket(String itemId, boolean existing, String type,
            boolean noStamina, boolean protectedEyes) {
        this.itemId = itemId == null ? "" : itemId;
        this.existing = existing;
        this.type = type == null ? "MISCELLANEOUS" : type;
        this.noStamina = noStamina;
        this.protectedEyes = protectedEyes;
    }

    public static void encode(ItemConfigOpenPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.itemId);
        buf.writeBoolean(msg.existing);
        buf.writeUtf(msg.type);
        buf.writeBoolean(msg.noStamina);
        buf.writeBoolean(msg.protectedEyes);
    }

    public static ItemConfigOpenPacket decode(FriendlyByteBuf buf) {
        return new ItemConfigOpenPacket(buf.readUtf(), buf.readBoolean(), buf.readUtf(),
                buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(ItemConfigOpenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ItemConfigClientHandler.open(msg));
        ctx.get().setPacketHandled(true);
    }

    public String itemId() {
        return itemId;
    }

    public boolean existing() {
        return existing;
    }

    public String type() {
        return type;
    }

    public boolean noStamina() {
        return noStamina;
    }

    public boolean protectedEyes() {
        return protectedEyes;
    }
}
