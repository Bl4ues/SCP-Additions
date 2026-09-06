package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import com.bl4ues.scpclassifieddirective.client.gui.HazardSignEditorScreen;
import com.bl4ues.scpclassifieddirective.facility.ScpSignHazards;

import java.util.function.Supplier;

public final class HazardSignOpenScreenPacket {
    public static final int MAX_HAZARD_ID_LENGTH = 48;

    private final BlockPos pos;
    private final String hazardId;

    public HazardSignOpenScreenPacket(BlockPos pos, String hazardId) {
        this.pos = pos.immutable();
        this.hazardId = ScpSignHazards.normalizeId(hazardId);
    }

    public static void encode(HazardSignOpenScreenPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeUtf(message.hazardId, MAX_HAZARD_ID_LENGTH);
    }

    public static HazardSignOpenScreenPacket decode(FriendlyByteBuf buffer) {
        return new HazardSignOpenScreenPacket(buffer.readBlockPos(),
                buffer.readUtf(MAX_HAZARD_ID_LENGTH));
    }

    public static void handle(HazardSignOpenScreenPacket message,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> HazardSignEditorScreen.open(
                        message.pos, message.hazardId)));
        context.setPacketHandled(true);
    }
}
