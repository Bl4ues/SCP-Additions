package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
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

    public static void encode(BlinkInputStatePacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.closed);
        buffer.writeBoolean(message.manual);
    }

    public static BlinkInputStatePacket decode(FriendlyByteBuf buffer) {
        return new BlinkInputStatePacket(buffer.readBoolean(),
                buffer.readBoolean());
    }

    public static void handle(BlinkInputStatePacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            boolean allowed = ScpAdditionsModulesConfig.get().blink.enabled
                    && ScpAdditionsModulesConfig.get().scp173.enabled
                    && !player.isCreative() && !player.isSpectator();
            boolean closed = allowed && message.closed;
            boolean manual = closed && message.manual;
            boolean changed = BlinkServerState.setBlinkClosed(player, closed,
                    manual);
            if (changed) {
                Scp173Entity.reactToBlinkState(player, closed, manual);
            }
            if (!allowed && message.closed) {
                ScpEntityNetwork.setBlinkActive(player, false);
            }
        });
        context.setPacketHandled(true);
    }
}
