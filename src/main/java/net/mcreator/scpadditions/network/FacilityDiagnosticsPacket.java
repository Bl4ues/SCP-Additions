package net.mcreator.scpadditions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.client.FacilityDiagnosticsScreen;
import net.mcreator.scpadditions.facility.Scp079FacilityAccessManager.DiagnosticSnapshot;

import java.util.function.Supplier;

/** Opens the Foundation facility diagnostic screen. */
public record FacilityDiagnosticsPacket(int uncontainedScps,
        int activeTeslaGates, int registeredTeslaGates,
        boolean teslaOverride, int connectedDoors,
        boolean auxiliaryPowerOnline, int cachePurgeCooldownTicks,
        boolean unusualNetworkActivity, BlockPos terminalPos) {

    public FacilityDiagnosticsPacket {
        cachePurgeCooldownTicks = Math.max(0, cachePurgeCooldownTicks);
        terminalPos = terminalPos == null
                ? BlockPos.ZERO : terminalPos.immutable();
    }

    public FacilityDiagnosticsPacket(DiagnosticSnapshot snapshot,
            BlockPos terminalPos) {
        this(snapshot.uncontainedScps(), snapshot.activeTeslaGates(),
                snapshot.registeredTeslaGates(), snapshot.teslaOverride(),
                snapshot.connectedDoors(), snapshot.auxiliaryPowerOnline(),
                snapshot.cachePurgeCooldownTicks(),
                snapshot.unusualNetworkActivity(), terminalPos);
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
        buffer.writeBlockPos(message.terminalPos);
    }

    public static FacilityDiagnosticsPacket decode(FriendlyByteBuf buffer) {
        return new FacilityDiagnosticsPacket(buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(),
                buffer.readBoolean(), buffer.readVarInt(),
                buffer.readBoolean(), buffer.readVarInt(),
                buffer.readBoolean(), buffer.readBlockPos());
    }

    public static void handle(FacilityDiagnosticsPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> FacilityDiagnosticsScreen.open(message)));
        context.setPacketHandled(true);
    }
}
