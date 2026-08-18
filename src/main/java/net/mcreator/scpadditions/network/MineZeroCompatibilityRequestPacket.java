package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.compat.MineZeroCompatibility;
import net.mcreator.scpadditions.compat.ModCompatibilityConfig;
import net.mcreator.scpadditions.config.ui.ConfigCenterService;

import java.util.function.Supplier;

/** Queries or changes the server-owned MineZero compatibility toggle. */
public record MineZeroCompatibilityRequestPacket(boolean change,
        boolean enabled) {
    public static void encode(MineZeroCompatibilityRequestPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.change);
        buffer.writeBoolean(message.enabled);
    }

    public static MineZeroCompatibilityRequestPacket decode(FriendlyByteBuf buffer) {
        return new MineZeroCompatibilityRequestPacket(buffer.readBoolean(),
                buffer.readBoolean());
    }

    public static void handle(MineZeroCompatibilityRequestPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> {
            if (player == null) return;
            if (message.change && MineZeroCompatibility.installed()
                    && ConfigCenterService.canEdit(player)) {
                ModCompatibilityConfig.setMineZeroEnabled(message.enabled);
            }
            sendStatus(player);
        });
        context.setPacketHandled(true);
    }

    public static void sendStatus(ServerPlayer player) {
        if (player == null || player.connection == null) return;
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new MineZeroCompatibilityStatusPacket(
                        MineZeroCompatibility.installed(),
                        MineZeroCompatibility.enabled(),
                        ConfigCenterService.canEdit(player)));
    }
}
