package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.crafting.ScpCraftingState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.bl4ues.scpadditions.compat.network.NetworkEvent;
import com.bl4ues.scpadditions.compat.network.PacketDistributor;

import java.util.function.Supplier;

/** Requests the player's persistent crafting state when the tab is opened. */
public final class RequestCraftingStatePacket {
    public static void encode(RequestCraftingStatePacket message,
                              FriendlyByteBuf buffer) {
    }

    public static RequestCraftingStatePacket decode(FriendlyByteBuf buffer) {
        return new RequestCraftingStatePacket();
    }

    public static void handle(RequestCraftingStatePacket message,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> {
            if (player != null) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new CraftingStateSyncPacket(ScpCraftingState.toTag(
                                ScpCraftingState.load(player), player.registryAccess())));
            }
        });
        context.setPacketHandled(true);
    }
}
