package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import com.bl4ues.scpclassifieddirective.facility.elevator.CoreRoomElevatorModule;
import com.bl4ues.scpclassifieddirective.facility.elevator.ElevatorArrivalDisplayData;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorStationIndex;
import com.bl4ues.scpclassifieddirective.keycard.KeycardReaderInteractionEvents;

import java.util.function.Supplier;

public final class ElevatorArrivalSavePacket {
    private final BlockPos pos;
    private final ElevatorArrivalDisplayData data;

    public ElevatorArrivalSavePacket(BlockPos pos,
            ElevatorArrivalDisplayData data) {
        this.pos = pos.immutable();
        this.data = data == null ? ElevatorArrivalDisplayData.NONE : data;
    }

    public static void encode(ElevatorArrivalSavePacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        ElevatorArrivalDisplayData.write(buffer, message.data);
    }

    public static ElevatorArrivalSavePacket decode(FriendlyByteBuf buffer) {
        return new ElevatorArrivalSavePacket(buffer.readBlockPos(),
                ElevatorArrivalDisplayData.read(buffer));
    }

    public static void handle(ElevatorArrivalSavePacket message,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null
                    || player.distanceToSqr(Vec3.atCenterOf(message.pos)) > 100.0D
                    || !player.level().hasChunkAt(message.pos)
                    || KeycardReaderInteractionEvents.screwdriver(player)
                            .isEmpty()) {
                return;
            }
            if (player.level().getBlockEntity(message.pos)
                    instanceof CoreRoomElevatorModule.StationBlockEntity station) {
                station.setArrivalDisplay(message.data);
                if (player.level() instanceof ServerLevel level) {
                    FacilityFloorStationIndex.refresh(level, message.pos);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
