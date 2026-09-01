package com.bl4ues.scpclassifieddirective.inventory.network;

import com.bl4ues.scpclassifieddirective.client.Scp426ExposureDebugHud;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Synchronizes the authoritative server-side SCP-426 exposure value for debug UI. */
public record Scp426ExposureSyncPacket(double exposureSeconds) {
    public static void encode(Scp426ExposureSyncPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeDouble(message.exposureSeconds);
    }

    public static Scp426ExposureSyncPacket decode(FriendlyByteBuf buffer) {
        return new Scp426ExposureSyncPacket(buffer.readDouble());
    }

    public static void handle(Scp426ExposureSyncPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
                Scp426ExposureDebugHud.updateExposure(message.exposureSeconds));
        context.setPacketHandled(true);
    }
}
