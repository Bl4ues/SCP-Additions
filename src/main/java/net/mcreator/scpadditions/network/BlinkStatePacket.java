package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.bl4ues.scpadditions.compat.DistExecutor;
import com.bl4ues.scpadditions.compat.network.NetworkEvent;
import net.mcreator.scpadditions.client.BlinkClient;

import java.util.function.Supplier;

public final class BlinkStatePacket {
    private final boolean active;
    public BlinkStatePacket(boolean active) { this.active = active; }
    public static void encode(BlinkStatePacket message, FriendlyByteBuf buffer) { buffer.writeBoolean(message.active); }
    public static BlinkStatePacket decode(FriendlyByteBuf buffer) { return new BlinkStatePacket(buffer.readBoolean()); }
    public static void handle(BlinkStatePacket message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BlinkClient.setActive(message.active)));
        context.setPacketHandled(true);
    }
}
