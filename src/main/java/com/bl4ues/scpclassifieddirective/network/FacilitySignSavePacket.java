package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import com.bl4ues.scpclassifieddirective.facility.FacilitySignBlockEntity;
import com.bl4ues.scpclassifieddirective.facility.FacilitySignData;
import com.bl4ues.scpclassifieddirective.keycard.KeycardReaderInteractionEvents;

import java.util.List;
import java.util.function.Supplier;

public final class FacilitySignSavePacket {
    private final BlockPos pos;
    private final List<FacilitySignData.Entry> entries;

    public FacilitySignSavePacket(BlockPos pos,
            List<FacilitySignData.Entry> entries) {
        this.pos = pos.immutable();
        this.entries = List.copyOf(entries);
    }

    public static void encode(FacilitySignSavePacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        FacilitySignOpenScreenPacket.writeEntries(buffer, message.entries);
    }

    public static FacilitySignSavePacket decode(FriendlyByteBuf buffer) {
        return new FacilitySignSavePacket(
                buffer.readBlockPos(),
                FacilitySignOpenScreenPacket.readEntries(buffer));
    }

    public static void handle(FacilitySignSavePacket message,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null
                    || player.distanceToSqr(Vec3.atCenterOf(message.pos)) > 64.0D
                    || !player.level().hasChunkAt(message.pos)) {
                return;
            }

            ItemStack screwdriver = KeycardReaderInteractionEvents.screwdriver(player);
            if (screwdriver.isEmpty()) return;

            if (player.level().getBlockEntity(message.pos)
                    instanceof FacilitySignBlockEntity sign) {
                sign.setEntries(message.entries);
            }
        });
        context.setPacketHandled(true);
    }
}
