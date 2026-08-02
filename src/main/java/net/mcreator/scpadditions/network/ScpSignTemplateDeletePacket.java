package net.mcreator.scpadditions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.facility.ScpSignTemplateLibrary;
import net.mcreator.scpadditions.facility.ScpSignTemplates;

import java.util.function.Supplier;

public final class ScpSignTemplateDeletePacket {
    private final BlockPos signPos;
    private final String id;

    public ScpSignTemplateDeletePacket(BlockPos signPos, String id) {
        this.signPos = signPos.immutable();
        this.id = ScpSignTemplates.cleanId(id);
    }

    public static void encode(ScpSignTemplateDeletePacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.signPos);
        buffer.writeUtf(message.id, ScpSignTemplates.MAX_ID_LENGTH);
    }

    public static ScpSignTemplateDeletePacket decode(FriendlyByteBuf buffer) {
        return new ScpSignTemplateDeletePacket(buffer.readBlockPos(),
                buffer.readUtf(ScpSignTemplates.MAX_ID_LENGTH));
    }

    public static void handle(ScpSignTemplateDeletePacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (!ScpSignTemplateEditAccess.allowed(player, message.signPos)
                    || player.getServer() == null
                    || !ScpSignTemplates.isCustom(message.id)) {
                return;
            }
            ScpSignTemplateLibrary library =
                    ScpSignTemplateLibrary.get(player.getServer());
            if (!library.delete(message.id)) return;
            ScpAdditionsMod.PACKET_HANDLER.send(
                    PacketDistributor.ALL.noArg(),
                    new ScpSignTemplateLibraryPacket(library.summaries(),
                            message.id, "", new byte[0]));
        });
        context.setPacketHandled(true);
    }
}
