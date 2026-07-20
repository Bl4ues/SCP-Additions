package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class Scp1176MusicPacket {
    public static void encode(Scp1176MusicPacket message, FriendlyByteBuf buffer) {
    }

    public static Scp1176MusicPacket decode(FriendlyByteBuf buffer) {
        return new Scp1176MusicPacket();
    }

    public static void handle(Scp1176MusicPacket message,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientPacketExecutor.run("playScp1176Music"));
        context.setPacketHandled(true);
    }
}
