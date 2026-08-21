package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp939Entity;
import net.mcreator.scpadditions.scp939.Scp939BreathSystem;
import net.mcreator.scpadditions.scp939.Scp939MimicryHooks;

import java.util.UUID;
import java.util.function.Supplier;

public final class Scp939InputPacket {
    public static final int HOLD_BREATH = 0;
    public static final int STRUGGLE_LEFT = 1;
    public static final int STRUGGLE_RIGHT = 2;
    public static final int MIMIC_CONSENT = 3;

    private static final int CONSENT_RETRIES = 6;
    private static final int CONSENT_RETRY_DELAY_TICKS = 20;

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
                    if (message.pressed) Scp939Entity.handleStruggleInput(player,
                            0);
                }
                case STRUGGLE_RIGHT -> {
                    if (message.pressed) Scp939Entity.handleStruggleInput(player,
                            1);
                }
                case MIMIC_CONSENT -> applyMimicConsent(player,
                        message.pressed, CONSENT_RETRIES);
                default -> {
                }
            }
        });
        context.setPacketHandled(true);
    }

    private static void applyMimicConsent(ServerPlayer player,
            boolean allowed, int retriesRemaining) {
        if (player == null) return;
        if (Scp939MimicryHooks.available()
                && Scp939MimicryHooks.setConsent(player, allowed)) {
            return;
        }
        if (retriesRemaining <= 0) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;
        UUID playerId = player.getUUID();
        ScpAdditionsMod.queueServerWork(CONSENT_RETRY_DELAY_TICKS, () -> {
            ServerPlayer current = server.getPlayerList().getPlayer(playerId);
            if (current != null) {
                applyMimicConsent(current, allowed, retriesRemaining - 1);
            }
        });
    }
}
