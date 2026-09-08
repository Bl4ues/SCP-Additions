package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import com.bl4ues.scpclassifieddirective.block.SCP079SystemControlBlock;
import com.bl4ues.scpclassifieddirective.block.entity.SystemTerminalBlockEntity;
import com.bl4ues.scpclassifieddirective.facility.Scp079FacilityAccessManager;
import com.bl4ues.scpclassifieddirective.facility.Scp079FacilityAccessManager.DiagnosticSnapshot;

import java.util.function.Supplier;

/** Server-authoritative actions from a nearby facility diagnostic terminal. */
public record FacilityDiagnosticsResetPacket(int action,
        BlockPos terminalPos) {
    private static final int ACTION_ANALYZE = 0;
    private static final int ACTION_PURGE = 1;
    private static final double MAX_DISTANCE_SQR = 8.0D * 8.0D;

    public FacilityDiagnosticsResetPacket {
        action = action == ACTION_PURGE ? ACTION_PURGE : ACTION_ANALYZE;
        terminalPos = terminalPos == null
                ? BlockPos.ZERO : terminalPos.immutable();
    }

    public static FacilityDiagnosticsResetPacket analyze(BlockPos pos) {
        return new FacilityDiagnosticsResetPacket(ACTION_ANALYZE, pos);
    }

    public static FacilityDiagnosticsResetPacket purge(BlockPos pos) {
        return new FacilityDiagnosticsResetPacket(ACTION_PURGE, pos);
    }

    public static void encode(FacilityDiagnosticsResetPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.action);
        buffer.writeBlockPos(message.terminalPos);
    }

    public static FacilityDiagnosticsResetPacket decode(
            FriendlyByteBuf buffer) {
        return new FacilityDiagnosticsResetPacket(buffer.readVarInt(),
                buffer.readBlockPos());
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

            SystemTerminalBlockEntity terminal =
                    level.getBlockEntity(pos) instanceof SystemTerminalBlockEntity found
                            ? found : null;
            DiagnosticSnapshot before = Scp079FacilityAccessManager
                    .currentDiagnosticSnapshot(player);
            boolean analysisComplete = terminal != null && terminal.analyzed();

            if (message.action() == ACTION_ANALYZE) {
                boolean allowed = before.auxiliaryPowerOnline()
                        && before.cachePurgeCooldownTicks() <= 0;
                DiagnosticSnapshot after = allowed
                        ? Scp079FacilityAccessManager.performDiagnosticScan(player)
                        : before;
                if (allowed) analysisComplete = true;
                if (terminal != null) {
                    terminal.updateSnapshot(after, analysisComplete);
                }
                FacilityDiagnosticsPacket.send(player, after, pos,
                        analysisComplete);
                return;
            }

            boolean purgeAllowed = before.auxiliaryPowerOnline()
                    && before.cachePurgeCooldownTicks() <= 0;
            Scp079FacilityAccessManager.resetRemoteSession(player);
            DiagnosticSnapshot after = Scp079FacilityAccessManager
                    .currentDiagnosticSnapshot(player);
            if (purgeAllowed) analysisComplete = false;
            if (terminal != null) {
                terminal.updateSnapshot(after, analysisComplete);
            }
            FacilityDiagnosticsPacket.send(player, after, pos,
                    analysisComplete);
        });
        context.setPacketHandled(true);
    }
}
