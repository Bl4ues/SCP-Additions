package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ScareSoundPacket {
    public static void encode(ScareSoundPacket msg, FriendlyByteBuf buf) {
    }

    public static ScareSoundPacket decode(FriendlyByteBuf buf) {
        return new ScareSoundPacket();
    }

    public static void handle(ScareSoundPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientPacketHandlers::playScareSound)
        );
        ctx.get().setPacketHandled(true);
    }
}
