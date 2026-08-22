package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.compat.ModCompatibilityConfig;
import com.bl4ues.scpclassifieddirective.compat.SimpleVoiceChatPresence;
import com.bl4ues.scpclassifieddirective.config.ui.ConfigCenterService;

import java.util.function.Supplier;

/** Queries or changes the server-owned Simple Voice Chat compatibility toggle. */
public record SimpleVoiceChatCompatibilityRequestPacket(boolean change,
        boolean enabled) {
    public static void encode(SimpleVoiceChatCompatibilityRequestPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.change);
        buffer.writeBoolean(message.enabled);
    }

    public static SimpleVoiceChatCompatibilityRequestPacket decode(FriendlyByteBuf buffer) {
        return new SimpleVoiceChatCompatibilityRequestPacket(buffer.readBoolean(),
                buffer.readBoolean());
    }

    public static void handle(SimpleVoiceChatCompatibilityRequestPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> {
            if (player == null) return;
            if (message.change && SimpleVoiceChatPresence.installed()
                    && ConfigCenterService.canEdit(player)) {
                ModCompatibilityConfig.setSimpleVoiceChatEnabled(message.enabled);
            }
            sendStatus(player);
        });
        context.setPacketHandled(true);
    }

    public static void sendStatus(ServerPlayer player) {
        if (player == null || player.connection == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new SimpleVoiceChatCompatibilityStatusPacket(
                        SimpleVoiceChatPresence.installed(),
                        ModCompatibilityConfig.simpleVoiceChatEnabled(),
                        ConfigCenterService.canEdit(player)));
    }
}
