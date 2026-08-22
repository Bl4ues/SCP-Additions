package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import com.bl4ues.scpclassifieddirective.client.ScpSignTemplateClient;
import com.bl4ues.scpclassifieddirective.facility.ScpSignTemplateLibrary;
import com.bl4ues.scpclassifieddirective.facility.ScpSignTemplateSummary;

import java.util.List;
import java.util.function.Supplier;

public final class ScpSignTemplateLibraryPacket {
    private final List<ScpSignTemplateSummary> summaries;
    private final String changedId;
    private final String imageName;
    private final byte[] image;

    public ScpSignTemplateLibraryPacket(
            List<ScpSignTemplateSummary> summaries, String changedId,
            String imageName, byte[] image) {
        this.summaries = summaries == null ? List.of() : List.copyOf(summaries);
        this.changedId = changedId == null ? "" : changedId;
        this.imageName = imageName == null ? "" : imageName;
        this.image = image == null ? new byte[0] : image.clone();
    }

    public static ScpSignTemplateLibraryPacket from(
            ScpSignTemplateLibrary library,
            ScpSignTemplateLibrary.Entry changed) {
        return changed == null
                ? new ScpSignTemplateLibraryPacket(library.summaries(), "",
                "", new byte[0])
                : new ScpSignTemplateLibraryPacket(library.summaries(),
                changed.id(), changed.name(), changed.image());
    }

    public static void encode(ScpSignTemplateLibraryPacket message,
            FriendlyByteBuf buffer) {
        ScpSignTemplatePacketCodec.writeSummaries(buffer, message.summaries);
        buffer.writeUtf(message.changedId, 80);
        ScpSignTemplatePacketCodec.writeOptionalImage(buffer,
                message.changedId, message.imageName, message.image);
    }

    public static ScpSignTemplateLibraryPacket decode(
            FriendlyByteBuf buffer) {
        List<ScpSignTemplateSummary> summaries =
                ScpSignTemplatePacketCodec.readSummaries(buffer);
        String changedId = buffer.readUtf(80);
        ScpSignTemplatePacketCodec.ImagePayload payload =
                ScpSignTemplatePacketCodec.readOptionalImage(buffer);
        return new ScpSignTemplateLibraryPacket(summaries, changedId,
                payload.name(), payload.image());
    }

    public static void handle(ScpSignTemplateLibraryPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    ScpSignTemplateClient.applyLibrary(message.summaries,
                            message.changedId);
                    if (message.image.length > 0) {
                        ScpSignTemplateClient.acceptImage(message.changedId,
                                message.imageName, message.image);
                    }
                }));
        context.setPacketHandled(true);
    }
}
