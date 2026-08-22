package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.network.FriendlyByteBuf;
import com.bl4ues.scpclassifieddirective.facility.ScpSignTemplateSummary;
import com.bl4ues.scpclassifieddirective.facility.ScpSignTemplates;

import java.util.ArrayList;
import java.util.List;

final class ScpSignTemplatePacketCodec {
    private ScpSignTemplatePacketCodec() {
    }

    static void writeSummaries(FriendlyByteBuf buffer,
            List<ScpSignTemplateSummary> summaries) {
        List<ScpSignTemplateSummary> clean = summaries == null
                ? List.of() : summaries.stream()
                .filter(summary -> summary != null && summary.custom())
                .limit(ScpSignTemplates.MAX_CUSTOM_TEMPLATES)
                .toList();
        buffer.writeVarInt(clean.size());
        for (ScpSignTemplateSummary summary : clean) {
            buffer.writeUtf(summary.id(), ScpSignTemplates.MAX_ID_LENGTH);
            buffer.writeUtf(summary.name(), ScpSignTemplates.MAX_NAME_LENGTH);
        }
    }

    static List<ScpSignTemplateSummary> readSummaries(
            FriendlyByteBuf buffer) {
        int count = Math.min(buffer.readVarInt(),
                ScpSignTemplates.MAX_CUSTOM_TEMPLATES);
        List<ScpSignTemplateSummary> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            result.add(new ScpSignTemplateSummary(
                    buffer.readUtf(ScpSignTemplates.MAX_ID_LENGTH),
                    buffer.readUtf(ScpSignTemplates.MAX_NAME_LENGTH), true));
        }
        return List.copyOf(result);
    }

    static void writeOptionalImage(FriendlyByteBuf buffer,
            String id, String name, byte[] image) {
        boolean present = ScpSignTemplates.isCustom(id)
                && image != null && image.length > 0
                && image.length <= ScpSignTemplates.MAX_IMAGE_BYTES;
        buffer.writeBoolean(present);
        if (!present) return;
        buffer.writeUtf(id, ScpSignTemplates.MAX_ID_LENGTH);
        buffer.writeUtf(name, ScpSignTemplates.MAX_NAME_LENGTH);
        buffer.writeByteArray(image);
    }

    static ImagePayload readOptionalImage(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) return ImagePayload.EMPTY;
        return new ImagePayload(
                buffer.readUtf(ScpSignTemplates.MAX_ID_LENGTH),
                buffer.readUtf(ScpSignTemplates.MAX_NAME_LENGTH),
                buffer.readByteArray(ScpSignTemplates.MAX_IMAGE_BYTES));
    }

    record ImagePayload(String id, String name, byte[] image) {
        static final ImagePayload EMPTY = new ImagePayload("", "",
                new byte[0]);

        boolean present() {
            return image != null && image.length > 0
                    && ScpSignTemplates.isCustom(id);
        }
    }
}
