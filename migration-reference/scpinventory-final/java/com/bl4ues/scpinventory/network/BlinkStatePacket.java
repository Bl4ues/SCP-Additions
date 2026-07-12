package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BlinkStatePacket {
    private final boolean active;

    public BlinkStatePacket(boolean active) {
        this.active = active;
    }

    public static void encode(BlinkStatePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
    }

    public static BlinkStatePacket decode(FriendlyByteBuf buf) {
        return new BlinkStatePacket(buf.readBoolean());
    }

    public static void handle(BlinkStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientPacketHandlers.setBlinkWatcherActive(msg.active)
                )
        );
        ctx.get().setPacketHandled(true);
    }
}
