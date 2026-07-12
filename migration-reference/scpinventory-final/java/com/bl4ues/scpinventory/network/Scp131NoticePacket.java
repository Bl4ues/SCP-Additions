package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class Scp131NoticePacket {
    private final boolean following;

    public Scp131NoticePacket(boolean following) {
        this.following = following;
    }

    public static void encode(Scp131NoticePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.following);
    }

    public static Scp131NoticePacket decode(FriendlyByteBuf buf) {
        return new Scp131NoticePacket(buf.readBoolean());
    }

    public static void handle(Scp131NoticePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientPacketHandlers.showScp131Notice(msg.following)
                )
        );
        ctx.get().setPacketHandled(true);
    }
}
