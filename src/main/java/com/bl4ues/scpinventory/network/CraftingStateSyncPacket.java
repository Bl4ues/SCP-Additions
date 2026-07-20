package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.client.ScpCraftingClientState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.DistExecutor;
import net.neoforged.neoforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server-to-client portable crafting state synchronization. */
public final class CraftingStateSyncPacket {
    private final CompoundTag stateTag;

    public CraftingStateSyncPacket(CompoundTag stateTag) {
        this.stateTag = stateTag == null ? new CompoundTag() : stateTag.copy();
    }

    public static void encode(CraftingStateSyncPacket message,
                              FriendlyByteBuf buffer) {
        buffer.writeNbt(message.stateTag);
    }

    public static CraftingStateSyncPacket decode(FriendlyByteBuf buffer) {
        return new CraftingStateSyncPacket(buffer.readNbt());
    }

    public static void handle(CraftingStateSyncPacket message,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ScpCraftingClientState.apply(message.stateTag)));
        context.setPacketHandled(true);
    }
}
