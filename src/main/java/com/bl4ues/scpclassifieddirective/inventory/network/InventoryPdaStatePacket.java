package com.bl4ues.scpclassifieddirective.inventory.network;

import com.bl4ues.scpclassifieddirective.inventory.client.pda.InventoryPdaThirdPersonClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Replicates presentation only; no inventory or screen content leaves its owner. */
public record InventoryPdaStatePacket(UUID playerId, boolean open) {
    private static final UUID REQUEST_ID = new UUID(0L, 0L);

    public static InventoryPdaStatePacket request(boolean open) {
        return new InventoryPdaStatePacket(REQUEST_ID, open);
    }

    public static void encode(InventoryPdaStatePacket packet,
            FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId);
        buffer.writeBoolean(packet.open);
    }

    public static InventoryPdaStatePacket decode(FriendlyByteBuf buffer) {
        return new InventoryPdaStatePacket(buffer.readUUID(),
                buffer.readBoolean());
    }

    public static void handle(InventoryPdaStatePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                InventoryPdaServerState.setOpen(sender, packet.open);
                return;
            }
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> InventoryPdaThirdPersonClient.setOpen(
                            packet.playerId, packet.open));
        });
        context.setPacketHandled(true);
    }
}
