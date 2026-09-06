package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import com.bl4ues.scpclassifieddirective.facility.HazardSignBlockEntity;
import com.bl4ues.scpclassifieddirective.facility.ScpSignHazards;
import com.bl4ues.scpclassifieddirective.keycard.KeycardReaderInteractionEvents;

import java.util.function.Supplier;

public final class HazardSignSavePacket {
    private final BlockPos pos;
    private final String hazardId;

    public HazardSignSavePacket(BlockPos pos, String hazardId) {
        this.pos = pos.immutable();
        this.hazardId = ScpSignHazards.normalizeId(hazardId);
    }

    public static void encode(HazardSignSavePacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeUtf(message.hazardId,
                HazardSignOpenScreenPacket.MAX_HAZARD_ID_LENGTH);
    }

    public static HazardSignSavePacket decode(FriendlyByteBuf buffer) {
        return new HazardSignSavePacket(buffer.readBlockPos(),
                buffer.readUtf(HazardSignOpenScreenPacket.MAX_HAZARD_ID_LENGTH));
    }

    public static void handle(HazardSignSavePacket message,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null
                    || player.distanceToSqr(Vec3.atCenterOf(message.pos)) > 100.0D
                    || !player.level().hasChunkAt(message.pos)) {
                return;
            }
            if (player.level().getBlockEntity(message.pos)
                    instanceof HazardSignBlockEntity sign) {
                ItemStack screwdriver =
                        KeycardReaderInteractionEvents.screwdriver(player);
                if (screwdriver.isEmpty() && sign.configured()) return;
                sign.setHazardId(message.hazardId);
            }
        });
        context.setPacketHandled(true);
    }
}
