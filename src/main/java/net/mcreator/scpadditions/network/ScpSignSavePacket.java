package net.mcreator.scpadditions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.facility.ScpSignData;
import net.mcreator.scpadditions.facility.ScpSignSupportBlockEntity;
import net.mcreator.scpadditions.facility.ScpSignTemplateLibrary;
import net.mcreator.scpadditions.keycard.KeycardReaderInteractionEvents;

import java.util.function.Supplier;

public final class ScpSignSavePacket {
    private final BlockPos pos;
    private final ScpSignData data;

    public ScpSignSavePacket(BlockPos pos, ScpSignData data) {
        this.pos = pos.immutable();
        this.data = data == null ? ScpSignData.DEFAULT : data;
    }

    public static void encode(ScpSignSavePacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        ScpSignOpenScreenPacket.writeData(buffer, message.data);
    }

    public static ScpSignSavePacket decode(FriendlyByteBuf buffer) {
        return new ScpSignSavePacket(buffer.readBlockPos(),
                ScpSignOpenScreenPacket.readData(buffer));
    }

    public static void handle(ScpSignSavePacket message,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null
                    || player.distanceToSqr(Vec3.atCenterOf(message.pos)) > 100.0D
                    || !player.level().hasChunkAt(message.pos)
                    || player.getServer() == null) {
                return;
            }
            if (player.level().getBlockEntity(message.pos)
                    instanceof ScpSignSupportBlockEntity sign) {
                ItemStack screwdriver =
                        KeycardReaderInteractionEvents.screwdriver(player);
                boolean firstPlacementSave =
                        sign.data().equals(ScpSignData.DEFAULT);
                if (screwdriver.isEmpty() && !firstPlacementSave) return;
                ScpSignTemplateLibrary library =
                        ScpSignTemplateLibrary.get(player.getServer());
                String validTemplate = library.validateSelection(
                        message.data.templateId());
                sign.setData(message.data.withTemplateId(validTemplate));
            }
        });
        context.setPacketHandled(true);
    }
}
