package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.scp939.Scp939BreathSystem;
import net.mcreator.scpadditions.scp939.Scp939PinSystem;

import java.util.function.Supplier;

public final class Scp939InputPacket {
    public static final int HOLD_BREATH = 0;
    public static final int STRUGGLE_LEFT = 1;
    public static final int STRUGGLE_RIGHT = 2;

    private final int action;
    private final boolean pressed;

    public Scp939InputPacket(int action, boolean pressed) {
        this.action = action;
        this.pressed = pressed;
    }

    public static void encode(Scp939InputPacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.action);
        buffer.writeBoolean(message.pressed);
    }

    public static Scp939InputPacket decode(FriendlyByteBuf buffer) {
        return new Scp939InputPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(Scp939InputPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            switch (message.action) {
                case HOLD_BREATH -> Scp939BreathSystem.setHolding(player,
                        message.pressed);
                case STRUGGLE_LEFT -> {
                    if (message.pressed) Scp939PinSystem.acceptInput(player, 0);
                }
                case STRUGGLE_RIGHT -> {
                    if (message.pressed) Scp939PinSystem.acceptInput(player, 1);
                }
                default -> {
                }
            }
        });
        context.setPacketHandled(true);
    }
}
