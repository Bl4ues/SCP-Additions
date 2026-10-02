package com.bl4ues.scpclassifieddirective.inventory.network;

import com.bl4ues.scpclassifieddirective.client.CopycatPanelClientSync;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server-to-client copycat BlockEntity state synchronization. */
public final class CopycatPanelStateSyncPacket {
    private final BlockPos pos;
    private final CompoundTag tag;

    public CopycatPanelStateSyncPacket(BlockPos pos, CompoundTag tag) {
        this.pos = pos == null ? BlockPos.ZERO : pos.immutable();
        this.tag = tag == null ? new CompoundTag() : tag.copy();
    }

    public static void encode(CopycatPanelStateSyncPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeNbt(message.tag);
    }

    public static CopycatPanelStateSyncPacket decode(FriendlyByteBuf buffer) {
        return new CopycatPanelStateSyncPacket(
                buffer.readBlockPos(), buffer.readNbt());
    }

    public static void handle(CopycatPanelStateSyncPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT, () -> () -> CopycatPanelClientSync.apply(
                        message.pos, message.tag)));
        context.setPacketHandled(true);
    }
}
