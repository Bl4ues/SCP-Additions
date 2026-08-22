package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import com.bl4ues.scpclassifieddirective.client.gui.FacilitySignEditorScreen;
import com.bl4ues.scpclassifieddirective.facility.FacilitySignBlock;
import com.bl4ues.scpclassifieddirective.facility.FacilitySignData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class FacilitySignOpenScreenPacket {
    private final BlockPos pos;
    private final FacilitySignBlock.SignType type;
    private final List<FacilitySignData.Entry> entries;

    public FacilitySignOpenScreenPacket(BlockPos pos,
            FacilitySignBlock.SignType type,
            List<FacilitySignData.Entry> entries) {
        this.pos = pos.immutable();
        this.type = type;
        this.entries = FacilitySignData.normalize(type, entries);
    }

    public static void encode(FacilitySignOpenScreenPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeEnum(message.type);
        writeEntries(buffer, message.entries);
    }

    public static FacilitySignOpenScreenPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        FacilitySignBlock.SignType type =
                buffer.readEnum(FacilitySignBlock.SignType.class);
        return new FacilitySignOpenScreenPacket(pos, type, readEntries(buffer));
    }

    public static void handle(FacilitySignOpenScreenPacket message,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> FacilitySignEditorScreen.open(
                        message.pos, message.type, message.entries)));
        context.setPacketHandled(true);
    }

    static void writeEntries(FriendlyByteBuf buffer,
            List<FacilitySignData.Entry> entries) {
        List<FacilitySignData.Entry> normalized = new ArrayList<>(entries);
        while (normalized.size() < FacilitySignData.ENTRY_COUNT) {
            normalized.add(FacilitySignData.EMPTY_ENTRY);
        }
        for (int i = 0; i < FacilitySignData.ENTRY_COUNT; i++) {
            FacilitySignData.Entry entry = normalized.get(i);
            buffer.writeUtf(entry.number(), FacilitySignData.MAX_NUMBER_LENGTH);
            buffer.writeUtf(entry.text(), FacilitySignBlock.SignType.DOOR.maxTextLength());
        }
    }

    static List<FacilitySignData.Entry> readEntries(FriendlyByteBuf buffer) {
        List<FacilitySignData.Entry> entries = new ArrayList<>();
        for (int i = 0; i < FacilitySignData.ENTRY_COUNT; i++) {
            entries.add(new FacilitySignData.Entry(
                    buffer.readUtf(FacilitySignData.MAX_NUMBER_LENGTH),
                    buffer.readUtf(FacilitySignBlock.SignType.DOOR.maxTextLength())));
        }
        return entries;
    }
}
