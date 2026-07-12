package net.mcreator.scpadditions.inventory;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ScpInventoryFullPacket {
    public static void encode(ScpInventoryFullPacket packet,
            FriendlyByteBuf buffer) {
    }

    public static ScpInventoryFullPacket decode(FriendlyByteBuf buffer) {
        return new ScpInventoryFullPacket();
    }

    public static void handle(ScpInventoryFullPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> ClientScpInventoryFullOverlay::show));
        context.setPacketHandled(true);
    }
}
