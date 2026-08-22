package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import com.bl4ues.scpclassifieddirective.facility.FacilitySignBlock;
import com.bl4ues.scpclassifieddirective.facility.FacilitySignBlockEntity;
import com.bl4ues.scpclassifieddirective.facility.FacilitySignClipboard;
import com.bl4ues.scpclassifieddirective.facility.FacilitySignData;
import com.bl4ues.scpclassifieddirective.keycard.KeycardReaderInteractionEvents;

import java.util.List;
import java.util.function.Supplier;

public final class FacilitySignClipboardPacket {
    private final BlockPos pos;
    private final Kind kind;
    private final int entryIndex;
    private final List<FacilitySignData.Entry> entries;

    public FacilitySignClipboardPacket(BlockPos pos, Kind kind,
            int entryIndex, List<FacilitySignData.Entry> entries) {
        this.pos = pos.immutable();
        this.kind = kind;
        this.entryIndex = entryIndex;
        this.entries = List.copyOf(entries);
    }

    public static FacilitySignClipboardPacket entry(BlockPos pos, int index,
            List<FacilitySignData.Entry> entries) {
        return new FacilitySignClipboardPacket(pos, Kind.ENTRY, index, entries);
    }

    public static FacilitySignClipboardPacket sign(BlockPos pos,
            List<FacilitySignData.Entry> entries) {
        return new FacilitySignClipboardPacket(pos, Kind.SIGN, -1, entries);
    }

    public static void encode(FacilitySignClipboardPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeEnum(message.kind);
        buffer.writeByte(message.entryIndex);
        FacilitySignOpenScreenPacket.writeEntries(buffer, message.entries);
    }

    public static FacilitySignClipboardPacket decode(FriendlyByteBuf buffer) {
        return new FacilitySignClipboardPacket(
                buffer.readBlockPos(),
                buffer.readEnum(Kind.class),
                buffer.readByte(),
                FacilitySignOpenScreenPacket.readEntries(buffer));
    }

    public static void handle(FacilitySignClipboardPacket message,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null
                    || player.distanceToSqr(Vec3.atCenterOf(message.pos)) > 64.0D
                    || !player.level().hasChunkAt(message.pos)
                    || !(player.level().getBlockEntity(message.pos)
                    instanceof FacilitySignBlockEntity sign)) {
                return;
            }

            ItemStack screwdriver = KeycardReaderInteractionEvents.screwdriver(player);
            if (screwdriver.isEmpty()) return;
            FacilitySignBlock.SignType type = sign.type();

            if (message.kind == Kind.ENTRY) {
                if (message.entryIndex < 0
                        || message.entryIndex >= FacilitySignData.ENTRY_COUNT) return;
                FacilitySignClipboard.copyEntry(
                        screwdriver, type, FacilitySignData.sanitize(
                                type, message.entries.get(message.entryIndex)));
            } else {
                FacilitySignClipboard.copySign(
                        screwdriver, type, message.entries);
            }
        });
        context.setPacketHandled(true);
    }

    public enum Kind {
        ENTRY,
        SIGN
    }
}
