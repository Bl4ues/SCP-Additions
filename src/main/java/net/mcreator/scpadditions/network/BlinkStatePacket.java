package net.mcreator.scpadditions.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.client.BlinkClient;

import java.util.function.Supplier;

public final class BlinkStatePacket {
    private final boolean active;

    public BlinkStatePacket(boolean active) {
        this.active = active;
    }

    public static void encode(BlinkStatePacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.active);
    }

    public static BlinkStatePacket decode(FriendlyByteBuf buffer) {
        return new BlinkStatePacket(buffer.readBoolean());
    }

    public static void handle(BlinkStatePacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    boolean allowed = message.active
                            && minecraft.player != null
                            && !minecraft.player.isCreative()
                            && !minecraft.player.isSpectator();
                    BlinkClient.setActive(allowed);
                }));
        context.setPacketHandled(true);
    }
}
