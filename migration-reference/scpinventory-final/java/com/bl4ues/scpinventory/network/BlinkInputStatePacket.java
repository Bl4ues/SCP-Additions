package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.entity.BlinkServerState;
import com.bl4ues.scpinventory.entity.Scp173Entity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BlinkInputStatePacket {
    private final boolean closed;

    public BlinkInputStatePacket(boolean closed) {
        this.closed = closed;
    }

    public static void encode(BlinkInputStatePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.closed);
    }

    public static BlinkInputStatePacket decode(FriendlyByteBuf buf) {
        return new BlinkInputStatePacket(buf.readBoolean());
    }

    public static void handle(BlinkInputStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                boolean changed = BlinkServerState.setBlinkClosed(player, msg.closed);
                if (changed) {
                    Scp173Entity.reactToBlinkState(player, msg.closed);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
