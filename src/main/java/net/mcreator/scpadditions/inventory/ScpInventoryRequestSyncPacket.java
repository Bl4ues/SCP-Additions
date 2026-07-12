package net.mcreator.scpadditions.inventory;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client-to-server request for the latest authoritative capability snapshot. */
public final class ScpInventoryRequestSyncPacket {
    public static void encode(ScpInventoryRequestSyncPacket packet,
            FriendlyByteBuf buffer) {
    }

    public static ScpInventoryRequestSyncPacket decode(FriendlyByteBuf buffer) {
        return new ScpInventoryRequestSyncPacket();
    }

    public static void handle(ScpInventoryRequestSyncPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> {
            if (sender != null) {
                ScpInventoryNetwork.sync(sender);
            }
        });
        context.setPacketHandled(true);
    }
}
