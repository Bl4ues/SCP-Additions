package net.mcreator.scpadditions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.client.Scp012ClientState;

import java.util.function.Supplier;

/** Server-to-client SCP-012 camera, overlay and local-audio synchronization. */
public final class Scp012InfluencePacket {
    private final boolean active;
    private final boolean damageActive;
    private final BlockPos target;
    private final float contactProgress;

    public Scp012InfluencePacket(boolean active, BlockPos target,
                                 float contactProgress,
                                 boolean damageActive) {
        this.active = active;
        this.target = target == null ? BlockPos.ZERO : target.immutable();
        this.contactProgress = Mth.clamp(contactProgress, 0.0F, 1.0F);
        this.damageActive = active && damageActive;
    }

    public static void encode(Scp012InfluencePacket message,
                              FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.active);
        buffer.writeBlockPos(message.target);
        buffer.writeFloat(message.contactProgress);
        buffer.writeBoolean(message.damageActive);
    }

    public static Scp012InfluencePacket decode(FriendlyByteBuf buffer) {
        return new Scp012InfluencePacket(buffer.readBoolean(),
                buffer.readBlockPos(), buffer.readFloat(),
                buffer.readBoolean());
    }

    public static void handle(Scp012InfluencePacket message,
                              Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> Scp012ClientState.update(message.active,
                        message.target, message.contactProgress,
                        message.damageActive)));
        context.setPacketHandled(true);
    }
}
