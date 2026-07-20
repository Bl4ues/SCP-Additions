package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.DistExecutor;
import net.neoforged.neoforge.network.NetworkEvent;
import net.mcreator.scpadditions.client.Scp714ClientState;

import java.util.function.Supplier;

/** Server-to-client SCP-714 fatigue progress synchronization. */
public final class Scp714ExposurePacket {
    private final boolean active;
    private final boolean immobilized;
    private final float progress;

    public Scp714ExposurePacket(boolean active, float progress,
            boolean immobilized) {
        this.active = active;
        this.progress = Mth.clamp(progress, 0.0F, 1.0F);
        this.immobilized = immobilized;
    }

    public static void encode(Scp714ExposurePacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.active);
        buffer.writeFloat(message.progress);
        buffer.writeBoolean(message.immobilized);
    }

    public static Scp714ExposurePacket decode(FriendlyByteBuf buffer) {
        return new Scp714ExposurePacket(buffer.readBoolean(),
                buffer.readFloat(), buffer.readBoolean());
    }

    public static void handle(Scp714ExposurePacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> Scp714ClientState.update(message.active,
                        message.progress, message.immobilized)));
        context.setPacketHandled(true);
    }
}
