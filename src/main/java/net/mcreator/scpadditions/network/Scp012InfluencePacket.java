package net.mcreator.scpadditions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.client.Scp012ClientState;

import java.util.function.Supplier;

/** Server-to-client target, camera-lock and contact-overlay synchronization. */
public final class Scp012InfluencePacket {
    private final boolean active;
    private final BlockPos target;
    private final float contactProgress;

    public Scp012InfluencePacket(boolean active, BlockPos target,
                                 float contactProgress) {
        this.active = active;
        this.target = target == null ? BlockPos.ZERO : target.immutable();
        this.contactProgress = Mth.clamp(contactProgress, 0.0F, 1.0F);
    }

    public static void encode(Scp012InfluencePacket message,
                              FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.active);
        buffer.writeBlockPos(message.target);
        buffer.writeFloat(message.contactProgress);
    }

    public static Scp012InfluencePacket decode(FriendlyByteBuf buffer) {
        return new Scp012InfluencePacket(buffer.readBoolean(),
                buffer.readBlockPos(), buffer.readFloat());
    }

    public static void handle(Scp012InfluencePacket message,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> Scp012ClientState.update(message.active,
                        message.target, message.contactProgress)));
        context.setPacketHandled(true);
    }
}
