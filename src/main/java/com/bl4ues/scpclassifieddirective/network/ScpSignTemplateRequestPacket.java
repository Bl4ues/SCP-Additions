package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.ScpSignTemplateLibrary;
import com.bl4ues.scpclassifieddirective.facility.ScpSignTemplates;

import java.util.function.Supplier;

public final class ScpSignTemplateRequestPacket {
    private final String id;

    public ScpSignTemplateRequestPacket(String id) {
        this.id = ScpSignTemplates.cleanId(id);
    }

    public static void encode(ScpSignTemplateRequestPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeUtf(message.id, ScpSignTemplates.MAX_ID_LENGTH);
    }

    public static ScpSignTemplateRequestPacket decode(FriendlyByteBuf buffer) {
        return new ScpSignTemplateRequestPacket(
                buffer.readUtf(ScpSignTemplates.MAX_ID_LENGTH));
    }

    public static void handle(ScpSignTemplateRequestPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.getServer() == null) return;
            ScpSignTemplateLibrary.Entry entry =
                    ScpSignTemplateLibrary.get(player.getServer())
                            .entry(message.id);
            if (entry == null) return;
            ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new ScpSignTemplateDataPacket(entry.id(), entry.name(),
                            entry.image()));
        });
        context.setPacketHandled(true);
    }
}
