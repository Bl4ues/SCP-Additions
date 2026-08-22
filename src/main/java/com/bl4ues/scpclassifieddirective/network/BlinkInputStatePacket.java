package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;
import com.bl4ues.scpclassifieddirective.entity.BlinkServerState;
import com.bl4ues.scpclassifieddirective.entity.Scp173Entity;
import com.bl4ues.scpclassifieddirective.entity.Scp173MovementController;

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

            boolean allowed = ScpClassifiedDirectiveModulesConfig.get().blink.enabled
                    && ScpClassifiedDirectiveModulesConfig.get().scp173.enabled
                    && !player.isCreative() && !player.isSpectator();
            boolean closed = allowed && message.closed;
            boolean manual = closed && message.manual;
            boolean changed = BlinkServerState.setBlinkClosed(player, closed,
                    manual);
            if (changed) {
                if (closed) {
                    Scp173MovementController
                            .prioritizeBlinkingPlayer(player);
                }
                Scp173Entity.reactToBlinkState(player, closed, manual);
            }
            if (!allowed && message.closed) {
                ScpEntityNetwork.setBlinkActive(player, false);
            }
        });
        context.setPacketHandled(true);
    }
}
