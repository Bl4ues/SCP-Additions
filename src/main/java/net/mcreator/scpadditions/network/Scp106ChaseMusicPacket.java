package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Controls the local, non-positional SCP-106 chase soundtrack. */
public final class Scp106ChaseMusicPacket {
    private final UUID sourceId;
    private final boolean active;

    public Scp106ChaseMusicPacket(UUID sourceId, boolean active) {
        this.sourceId = sourceId;
        this.active = active;
    }

    public static void encode(Scp106ChaseMusicPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeUUID(message.sourceId);
        buffer.writeBoolean(message.active);
    }

    public static Scp106ChaseMusicPacket decode(FriendlyByteBuf buffer) {
        return new Scp106ChaseMusicPacket(buffer.readUUID(),
                buffer.readBoolean());
    }

    public static void handle(Scp106ChaseMusicPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
                ClientPacketExecutor.setScp106ChaseMusic(
                        message.sourceId, message.active));
        context.setPacketHandled(true);
    }
}
