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

public final class ScpSignTemplateUploadPacket {
    private final BlockPos signPos;
    private final String name;
    private final byte[] image;

    public ScpSignTemplateUploadPacket(BlockPos signPos, String name,
            byte[] image) {
        this.signPos = signPos.immutable();
        this.name = ScpSignTemplates.cleanName(name);
        this.image = image == null ? new byte[0] : image.clone();
    }

    public static void encode(ScpSignTemplateUploadPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.signPos);
        buffer.writeUtf(message.name, ScpSignTemplates.MAX_NAME_LENGTH);
        buffer.writeByteArray(message.image);
    }

    public static ScpSignTemplateUploadPacket decode(FriendlyByteBuf buffer) {
        return new ScpSignTemplateUploadPacket(buffer.readBlockPos(),
                buffer.readUtf(ScpSignTemplates.MAX_NAME_LENGTH),
                buffer.readByteArray(ScpSignTemplates.MAX_IMAGE_BYTES));
    }

    public static void handle(ScpSignTemplateUploadPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (!ScpSignTemplateEditAccess.allowed(player, message.signPos)
                    || player.getServer() == null) {
                return;
            }
            ScpSignTemplateLibrary library =
                    ScpSignTemplateLibrary.get(player.getServer());
            ScpSignTemplateLibrary.Entry created = library.create(
                    message.name, message.image);
            if (created == null) return;
            ScpAdditionsMod.PACKET_HANDLER.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    ScpSignTemplateLibraryPacket.from(library, created));
        });
        context.setPacketHandled(true);
    }
}
