package net.mcreator.scpadditions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.client.ScpSignTemplateClient;
import net.mcreator.scpadditions.client.gui.ScpSignEditorScreen;
import net.mcreator.scpadditions.facility.ScpSignData;
import net.mcreator.scpadditions.facility.ScpSignTemplateLibrary;
import net.mcreator.scpadditions.facility.ScpSignTemplateSummary;
import net.mcreator.scpadditions.facility.ScpSignTemplates;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ScpSignOpenScreenPacket {
    private static final int MAX_HAZARD_ID_LENGTH = 48;

    private final BlockPos pos;
    private final ScpSignData data;
    private final List<ScpSignTemplateSummary> summaries;
    private final String imageName;
    private final byte[] image;

    public ScpSignOpenScreenPacket(BlockPos pos, ScpSignData data,
            List<ScpSignTemplateSummary> summaries,
            ScpSignTemplateLibrary.Entry selectedEntry) {
        this(pos, data, summaries,
                selectedEntry == null ? "" : selectedEntry.name(),
                selectedEntry == null ? new byte[0] : selectedEntry.image());
    }

    private ScpSignOpenScreenPacket(BlockPos pos, ScpSignData data,
            List<ScpSignTemplateSummary> summaries, String imageName,
            byte[] image) {
        this.pos = pos.immutable();
        this.data = data == null ? ScpSignData.DEFAULT : data;
        this.summaries = summaries == null ? List.of()
                : List.copyOf(summaries);
        this.imageName = imageName == null ? "" : imageName;
        this.image = image == null ? new byte[0] : image.clone();
    }

    public static void encode(ScpSignOpenScreenPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        writeData(buffer, message.data);
        ScpSignTemplatePacketCodec.writeSummaries(buffer, message.summaries);
        ScpSignTemplatePacketCodec.writeOptionalImage(buffer,
                message.data.templateId(), message.imageName, message.image);
    }

    public static ScpSignOpenScreenPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        ScpSignData data = readData(buffer);
        List<ScpSignTemplateSummary> summaries =
                ScpSignTemplatePacketCodec.readSummaries(buffer);
        ScpSignTemplatePacketCodec.ImagePayload payload =
                ScpSignTemplatePacketCodec.readOptionalImage(buffer);
        return new ScpSignOpenScreenPacket(pos, data, summaries,
                payload.name(), payload.image());
    }

    public static void handle(ScpSignOpenScreenPacket message,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    ScpSignTemplateClient.applyLibrary(message.summaries,
                            message.data.templateId());
                    if (message.image.length > 0) {
                        ScpSignTemplateClient.acceptImage(
                                message.data.templateId(), message.imageName,
                                message.image);
                    }
                    ScpSignEditorScreen.open(message.pos, message.data);
                }));
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
        buffer.writeUtf(clean.templateId(), ScpSignTemplates.MAX_ID_LENGTH);
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
        String templateId = buffer.readUtf(ScpSignTemplates.MAX_ID_LENGTH);
        return new ScpSignData(scpNumber, containment, customContainment,
                clearance, anomaly, customAnomaly, hazards, templateId);
    }
}
