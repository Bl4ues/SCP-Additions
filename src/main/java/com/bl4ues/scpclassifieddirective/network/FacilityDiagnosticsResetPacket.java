package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import com.bl4ues.scpclassifieddirective.block.SCP079SystemControlBlock;
import com.bl4ues.scpclassifieddirective.facility.Scp079FacilityAccessManager;

import java.util.function.Supplier;

/** Server-authoritative remote-session reset from a nearby diagnostic terminal. */
public record FacilityDiagnosticsResetPacket(BlockPos terminalPos) {
    private static final double MAX_DISTANCE_SQR = 8.0D * 8.0D;

    public FacilityDiagnosticsResetPacket {
        terminalPos = terminalPos == null
                ? BlockPos.ZERO : terminalPos.immutable();
    }

    public static void encode(FacilityDiagnosticsResetPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.terminalPos);
    }

    public static FacilityDiagnosticsResetPacket decode(
            FriendlyByteBuf buffer) {
        return new FacilityDiagnosticsResetPacket(buffer.readBlockPos());
    }

    public static void handle(FacilityDiagnosticsResetPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> {
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            BlockPos pos = message.terminalPos();
            if (!level.hasChunkAt(pos)
                    || !(level.getBlockState(pos).getBlock()
                    instanceof SCP079SystemControlBlock)
                    || player.distanceToSqr(Vec3.atCenterOf(pos))
                    > MAX_DISTANCE_SQR) {
                return;
            }

            Scp079FacilityAccessManager.resetRemoteSession(player);
            ScpEntityNetwork.openFacilityDiagnostics(player,
                    Scp079FacilityAccessManager.currentDiagnosticSnapshot(
                            player), pos);
        });
        context.setPacketHandled(true);
    }
}
