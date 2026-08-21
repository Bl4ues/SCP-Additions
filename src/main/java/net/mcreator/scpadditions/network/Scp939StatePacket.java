package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.client.Scp939ClientState;

import java.util.function.Supplier;

public final class Scp939StatePacket {
    private final boolean breathActive;
    private final float reserve;
    private final boolean holding;
    private final boolean pinned;
    private final int progress;
    private final int failures;
    private final int expectedKey;
    private final int windowTicks;

    public Scp939StatePacket(boolean breathActive, float reserve,
            boolean holding, boolean pinned, int progress, int failures,
            int expectedKey, int windowTicks) {
        this.breathActive = breathActive;
        this.reserve = reserve;
        this.holding = holding;
        this.pinned = pinned;
        this.progress = progress;
        this.failures = failures;
        this.expectedKey = expectedKey;
        this.windowTicks = windowTicks;
    }

    public static void encode(Scp939StatePacket message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.breathActive);
        buffer.writeFloat(message.reserve);
        buffer.writeBoolean(message.holding);
        buffer.writeBoolean(message.pinned);
        buffer.writeVarInt(message.progress);
        buffer.writeVarInt(message.failures);
        buffer.writeVarInt(message.expectedKey);
        buffer.writeVarInt(message.windowTicks);
    }

    public static Scp939StatePacket decode(FriendlyByteBuf buffer) {
        return new Scp939StatePacket(buffer.readBoolean(), buffer.readFloat(),
                buffer.readBoolean(), buffer.readBoolean(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(Scp939StatePacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> Scp939ClientState.update(message.breathActive,
                        message.reserve, message.holding, message.pinned,
                        message.progress, message.failures,
                        message.expectedKey, message.windowTicks)));
        context.setPacketHandled(true);
    }
}
