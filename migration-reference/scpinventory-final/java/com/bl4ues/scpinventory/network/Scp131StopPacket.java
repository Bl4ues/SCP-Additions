package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.entity.AbstractScp131Entity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class Scp131StopPacket {
    public Scp131StopPacket() {
    }

    public static void encode(Scp131StopPacket msg, FriendlyByteBuf buf) {
    }

    public static Scp131StopPacket decode(FriendlyByteBuf buf) {
        return new Scp131StopPacket();
    }

    public static void handle(Scp131StopPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && AbstractScp131Entity.stopFollowersFor(player)) {
                ModNetwork.showScp131Notice(player, false);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
