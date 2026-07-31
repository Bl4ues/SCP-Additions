package net.mcreator.scpadditions.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.client.gui.ElevatorArrivalEditorScreen;
import net.mcreator.scpadditions.facility.elevator.ElevatorArrivalDisplayData;

import java.util.function.Supplier;

public final class ElevatorArrivalOpenScreenPacket {
    private final BlockPos pos;
    private final ElevatorArrivalDisplayData data;

    public ElevatorArrivalOpenScreenPacket(BlockPos pos,
            ElevatorArrivalDisplayData data) {
        this.pos = pos.immutable();
        this.data = data == null ? ElevatorArrivalDisplayData.NONE : data;
    }

    public static void encode(ElevatorArrivalOpenScreenPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        ElevatorArrivalDisplayData.write(buffer, message.data);
    }

    public static ElevatorArrivalOpenScreenPacket decode(
            FriendlyByteBuf buffer) {
        return new ElevatorArrivalOpenScreenPacket(buffer.readBlockPos(),
                ElevatorArrivalDisplayData.read(buffer));
    }

    public static void handle(ElevatorArrivalOpenScreenPacket message,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ElevatorArrivalEditorScreen.open(
                        message.pos, message.data)));
        context.setPacketHandled(true);
    }
}
