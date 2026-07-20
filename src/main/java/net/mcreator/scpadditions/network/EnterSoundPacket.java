package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import com.bl4ues.scpadditions.compat.network.NetworkEvent;

import java.util.function.Supplier;

public final class EnterSoundPacket {
    public static void encode(EnterSoundPacket message, FriendlyByteBuf buffer) {
    }

    public static EnterSoundPacket decode(FriendlyByteBuf buffer) {
        return new EnterSoundPacket();
    }

    public static void handle(EnterSoundPacket message,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.setPacketHandled(true);
    }
}
