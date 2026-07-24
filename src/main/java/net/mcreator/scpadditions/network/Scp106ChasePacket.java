package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Starts or fades SCP-106's non-positional soundtrack for one client. */
public record Scp106ChasePacket(boolean active) {
    public static void encode(Scp106ChasePacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.active);
    }

    public static Scp106ChasePacket decode(FriendlyByteBuf buffer) {
        return new Scp106ChasePacket(buffer.readBoolean());
    }

    public static void handle(Scp106ChasePacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientPacketExecutor.run(message.active
                ? "startScp106Chase" : "stopScp106Chase"));
        context.setPacketHandled(true);
    }
}
