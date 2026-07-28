package net.mcreator.scpadditions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.client.gui.ScpSignEditorScreen;
import net.mcreator.scpadditions.facility.ScpSignData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ScpSignOpenScreenPacket {
    private static final int MAX_HAZARD_ID_LENGTH = 48;

    private final BlockPos pos;
    private final ScpSignData data;

    public ScpSignOpenScreenPacket(BlockPos pos, ScpSignData data) {
        this.pos = pos.immutable();
        this.data = data == null ? ScpSignData.DEFAULT : data;
    }

    public static void encode(ScpSignOpenScreenPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        writeData(buffer, message.data);
    }

    public static ScpSignOpenScreenPacket decode(FriendlyByteBuf buffer) {
        return new ScpSignOpenScreenPacket(buffer.readBlockPos(),
                readData(buffer));
    }

    public static void handle(ScpSignOpenScreenPacket message,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ScpSignEditorScreen.open(message.pos, message.data)));
        context.setPacketHandled(true);
    }

    static void writeData(FriendlyByteBuf buffer, ScpSignData data) {
        ScpSignData clean = data == null ? ScpSignData.DEFAULT : data;
        buffer.writeUtf(clean.scpNumber(), ScpSignData.MAX_SCP_NUMBER_LENGTH);
        buffer.writeEnum(clean.containmentClass());
        buffer.writeUtf(clean.customContainmentClass(),
                ScpSignData.MAX_CONTAINMENT_CLASS_LENGTH);
        buffer.writeByte(clean.clearanceLevel());
        buffer.writeEnum(clean.anomalyType());
        buffer.writeUtf(clean.customAnomalyType(),
                ScpSignData.MAX_ANOMALY_TYPE_LENGTH);
        for (int slot = 0; slot < ScpSignData.HAZARD_SLOTS; slot++) {
            buffer.writeUtf(clean.hazards().get(slot), MAX_HAZARD_ID_LENGTH);
        }
    }

    static ScpSignData readData(FriendlyByteBuf buffer) {
        String scpNumber = buffer.readUtf(ScpSignData.MAX_SCP_NUMBER_LENGTH);
        ScpSignData.ContainmentClass containment = buffer.readEnum(
                ScpSignData.ContainmentClass.class);
        String customContainment = buffer.readUtf(
                ScpSignData.MAX_CONTAINMENT_CLASS_LENGTH);
        int clearance = buffer.readUnsignedByte();
        ScpSignData.AnomalyType anomaly = buffer.readEnum(
                ScpSignData.AnomalyType.class);
        String customAnomaly = buffer.readUtf(
                ScpSignData.MAX_ANOMALY_TYPE_LENGTH);
        List<String> hazards = new ArrayList<>(ScpSignData.HAZARD_SLOTS);
        for (int slot = 0; slot < ScpSignData.HAZARD_SLOTS; slot++) {
            hazards.add(buffer.readUtf(MAX_HAZARD_ID_LENGTH));
        }
        return new ScpSignData(scpNumber, containment, customContainment,
                clearance, anomaly, customAnomaly, hazards);
    }
}
