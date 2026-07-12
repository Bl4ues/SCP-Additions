package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.client.BlinkClient;

import java.util.function.Supplier;

public final class ScareSoundPacket {
    public static void encode(ScareSoundPacket message, FriendlyByteBuf buffer) { }
    public static ScareSoundPacket decode(FriendlyByteBuf buffer) { return new ScareSoundPacket(); }
    public static void handle(ScareSoundPacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> BlinkClient::playScareSound));
        context.setPacketHandled(true);
    }
}
