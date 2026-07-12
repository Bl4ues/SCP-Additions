package net.mcreator.scpadditions.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class ScpInventorySyncPacket {
    private final CompoundTag data;

    public ScpInventorySyncPacket(CompoundTag data) {
        this.data = data == null ? new CompoundTag() : data.copy();
    }

    public static void encode(ScpInventorySyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.data);
    }

    public static ScpInventorySyncPacket decode(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new ScpInventorySyncPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(ScpInventorySyncPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientScpInventorySync.apply(packet.data)));
        context.setPacketHandled(true);
    }
}
