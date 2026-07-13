package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.entity.Scp173Entity;

import java.util.function.Supplier;

public final class Scp173ObservationPacket {
    private final int entityId;
    private final boolean visible;

    public Scp173ObservationPacket(int entityId, boolean visible) {
        this.entityId = entityId;
        this.visible = visible;
    }

    public static void encode(Scp173ObservationPacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId);
        buffer.writeBoolean(message.visible);
    }

    public static Scp173ObservationPacket decode(FriendlyByteBuf buffer) {
        return new Scp173ObservationPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(Scp173ObservationPacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            Entity entity = player.serverLevel().getEntity(message.entityId);
            if (entity instanceof Scp173Entity scp173) scp173.updateClientObservation(player, message.visible);
        });
        context.setPacketHandled(true);
    }
}
