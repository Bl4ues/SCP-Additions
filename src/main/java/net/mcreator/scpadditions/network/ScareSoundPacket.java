package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import com.bl4ues.scpadditions.compat.network.NetworkEvent;

import java.util.function.Supplier;

public final class ScareSoundPacket {
    public static void encode(ScareSoundPacket message, FriendlyByteBuf buffer) {
    }

    public static ScareSoundPacket decode(FriendlyByteBuf buffer) {
        return new ScareSoundPacket();
    }

    public static void handle(ScareSoundPacket message,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientPacketExecutor.run("playScareSound"));
        context.setPacketHandled(true);
    }
}
