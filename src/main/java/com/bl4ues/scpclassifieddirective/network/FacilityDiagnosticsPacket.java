package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.FacilityDiagnosticsScreen;
import com.bl4ues.scpclassifieddirective.facility.Scp079FacilityAccessManager.DiagnosticSnapshot;

import java.util.function.Supplier;

/** Opens the Foundation facility diagnostic terminal interaction. */
public record FacilityDiagnosticsPacket(int uncontainedScps,
        int activeTeslaGates, int registeredTeslaGates,
        boolean teslaOverride, int connectedDoors,
        boolean auxiliaryPowerOnline, int cachePurgeCooldownTicks,
        boolean unusualNetworkActivity, boolean analysisComplete,
        BlockPos terminalPos) {

    public FacilityDiagnosticsPacket {
        cachePurgeCooldownTicks = Math.max(0, cachePurgeCooldownTicks);
        terminalPos = terminalPos == null
                ? BlockPos.ZERO : terminalPos.immutable();
    }

    public FacilityDiagnosticsPacket(DiagnosticSnapshot snapshot,
            BlockPos terminalPos) {
        this(snapshot, terminalPos, false);
    }

    public FacilityDiagnosticsPacket(DiagnosticSnapshot snapshot,
            BlockPos terminalPos, boolean analysisComplete) {
        this(snapshot.uncontainedScps(), snapshot.activeTeslaGates(),
                snapshot.registeredTeslaGates(), snapshot.teslaOverride(),
                snapshot.connectedDoors(), snapshot.auxiliaryPowerOnline(),
                snapshot.cachePurgeCooldownTicks(),
                snapshot.unusualNetworkActivity(), analysisComplete,
                terminalPos);
    }

    public static void send(ServerPlayer player, DiagnosticSnapshot snapshot,
            BlockPos terminalPos, boolean analysisComplete) {
        if (player == null || snapshot == null || terminalPos == null) return;
        ScpClassifiedDirectiveMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> player),
                new FacilityDiagnosticsPacket(snapshot, terminalPos,
                        analysisComplete));
    }

    public static void encode(FacilityDiagnosticsPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeVarInt(Math.max(0, message.uncontainedScps));
        buffer.writeVarInt(Math.max(0, message.activeTeslaGates));
        buffer.writeVarInt(Math.max(0, message.registeredTeslaGates));
        buffer.writeBoolean(message.teslaOverride);
        buffer.writeVarInt(Math.max(0, message.connectedDoors));
        buffer.writeBoolean(message.auxiliaryPowerOnline);
        buffer.writeVarInt(Math.max(0, message.cachePurgeCooldownTicks));
        buffer.writeBoolean(message.unusualNetworkActivity);
        buffer.writeBoolean(message.analysisComplete);
        buffer.writeBlockPos(message.terminalPos);
    }

    public static FacilityDiagnosticsPacket decode(FriendlyByteBuf buffer) {
        return new FacilityDiagnosticsPacket(buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(),
                buffer.readBoolean(), buffer.readVarInt(),
                buffer.readBoolean(), buffer.readVarInt(),
                buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBlockPos());
    }

    public static void handle(FacilityDiagnosticsPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> FacilityDiagnosticsScreen.open(message)));
        context.setPacketHandled(true);
    }
}
