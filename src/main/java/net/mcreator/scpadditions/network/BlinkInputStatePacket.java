package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.bl4ues.scpadditions.compat.network.NetworkEvent;
import net.mcreator.scpadditions.entity.BlinkServerState;
import net.mcreator.scpadditions.entity.Scp173Entity;

import java.util.function.Supplier;

public final class BlinkInputStatePacket {
    private final boolean closed;
    private final boolean manual;

    public BlinkInputStatePacket(boolean closed, boolean manual) {
        this.closed = closed;
        this.manual = manual;
    }
    public static void encode(BlinkInputStatePacket message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.closed);
        buffer.writeBoolean(message.manual);
    }
    public static BlinkInputStatePacket decode(FriendlyByteBuf buffer) {
        return new BlinkInputStatePacket(buffer.readBoolean(), buffer.readBoolean());
    }
    public static void handle(BlinkInputStatePacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                boolean changed = BlinkServerState.setBlinkClosed(player, message.closed, message.manual);
                if (changed) Scp173Entity.reactToBlinkState(player, message.closed, message.manual);
            }
        });
        context.setPacketHandled(true);
    }
}
