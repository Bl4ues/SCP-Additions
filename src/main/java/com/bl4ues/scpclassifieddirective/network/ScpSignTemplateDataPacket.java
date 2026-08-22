package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import com.bl4ues.scpclassifieddirective.client.ScpSignTemplateClient;
import com.bl4ues.scpclassifieddirective.facility.ScpSignTemplates;

import java.util.function.Supplier;

public final class ScpSignTemplateDataPacket {
    private final String id;
    private final String name;
    private final byte[] image;

    public ScpSignTemplateDataPacket(String id, String name, byte[] image) {
        this.id = ScpSignTemplates.cleanId(id);
        this.name = ScpSignTemplates.cleanName(name);
        this.image = image == null ? new byte[0] : image.clone();
    }

    public static void encode(ScpSignTemplateDataPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeUtf(message.id, ScpSignTemplates.MAX_ID_LENGTH);
        buffer.writeUtf(message.name, ScpSignTemplates.MAX_NAME_LENGTH);
        buffer.writeByteArray(message.image);
    }

    public static ScpSignTemplateDataPacket decode(FriendlyByteBuf buffer) {
        return new ScpSignTemplateDataPacket(
                buffer.readUtf(ScpSignTemplates.MAX_ID_LENGTH),
                buffer.readUtf(ScpSignTemplates.MAX_NAME_LENGTH),
                buffer.readByteArray(ScpSignTemplates.MAX_IMAGE_BYTES));
    }

    public static void handle(ScpSignTemplateDataPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ScpSignTemplateClient.acceptImage(message.id,
                        message.name, message.image)));
        context.setPacketHandled(true);
    }
}
