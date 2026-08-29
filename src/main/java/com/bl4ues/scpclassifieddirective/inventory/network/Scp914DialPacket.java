package com.bl4ues.scpclassifieddirective.inventory.network;

import com.bl4ues.scpclassifieddirective.block.entity.Scp914BlockEntity;
import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;
import com.bl4ues.scpclassifieddirective.scp330.Scp330Hands;
import com.bl4ues.scpclassifieddirective.scp914.Scp914Module;
import com.bl4ues.scpclassifieddirective.scp914.Scp914Structure;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sparse client-to-server updates for the physical SCP-914 configuration dial. */
public final class Scp914DialPacket {
    private static final double MAX_DISTANCE = 2.35D;

    private final BlockPos pos;
    private final float angle;
    private final boolean commit;

    public Scp914DialPacket(BlockPos pos, float angle, boolean commit) {
        this.pos = pos == null ? BlockPos.ZERO : pos;
        this.angle = angle;
        this.commit = commit;
    }

    public static void encode(Scp914DialPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeFloat(packet.angle);
        buf.writeBoolean(packet.commit);
    }

    public static Scp914DialPacket decode(FriendlyByteBuf buf) {
        return new Scp914DialPacket(buf.readBlockPos(), buf.readFloat(),
                buf.readBoolean());
    }

    public static void handle(Scp914DialPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null
                    || !ScpClassifiedDirectiveModulesConfig.customInteractionsEnabledFor(player)
                    || Scp330Hands.isDisabled(player)
                    || !player.level().isLoaded(packet.pos)) {
                return;
            }

            BlockState state = player.level().getBlockState(packet.pos);
            if (!state.is(Scp914Module.SCP_914.get())
                    || !(player.level().getBlockEntity(packet.pos)
                    instanceof Scp914BlockEntity machine)
                    || machine.isRefining()) {
                return;
            }

            Vec3 anchor = Scp914Structure.dialAnchor(packet.pos,
                    Scp914Structure.facing(state));
            if (player.getEyePosition().distanceTo(anchor) > MAX_DISTANCE) {
                return;
            }
            machine.applyDialNetworkUpdate(packet.angle, packet.commit);
        });
        context.setPacketHandled(true);
    }
}
