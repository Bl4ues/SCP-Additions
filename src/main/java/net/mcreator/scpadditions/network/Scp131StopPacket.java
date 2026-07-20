package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.NetworkEvent;
import net.mcreator.scpadditions.entity.AbstractScp131Entity;

import java.util.function.Supplier;

public final class Scp131StopPacket {
    public static void encode(Scp131StopPacket message, FriendlyByteBuf buffer) {
    }

    public static Scp131StopPacket decode(FriendlyByteBuf buffer) {
        return new Scp131StopPacket();
    }

    public static void handle(Scp131StopPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && AbstractScp131Entity.stopFollowersFor(player)) {
                ScpEntityNetwork.showScp131Notice(player, false);
            }
        });
        context.setPacketHandled(true);
    }
}
