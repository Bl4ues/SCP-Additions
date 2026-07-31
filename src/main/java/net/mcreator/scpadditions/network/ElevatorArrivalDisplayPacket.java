package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.client.ElevatorArrivalOverlay;
import net.mcreator.scpadditions.facility.elevator.ElevatorArrivalDisplayData;

import java.util.function.Supplier;

public final class ElevatorArrivalDisplayPacket {
    private final ElevatorArrivalDisplayData data;
    private final int delayTicks;

    public ElevatorArrivalDisplayPacket(ElevatorArrivalDisplayData data,
            int delayTicks) {
        this.data = data == null ? ElevatorArrivalDisplayData.NONE : data;
        this.delayTicks = Math.max(0, Math.min(200, delayTicks));
    }

    public static void encode(ElevatorArrivalDisplayPacket message,
            FriendlyByteBuf buffer) {
        ElevatorArrivalDisplayData.write(buffer, message.data);
        buffer.writeVarInt(message.delayTicks);
    }

    public static ElevatorArrivalDisplayPacket decode(FriendlyByteBuf buffer) {
        return new ElevatorArrivalDisplayPacket(
                ElevatorArrivalDisplayData.read(buffer),
                buffer.readVarInt());
    }

    public static void handle(ElevatorArrivalDisplayPacket message,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ElevatorArrivalOverlay.prepare(message.data,
                        message.delayTicks)));
        context.setPacketHandled(true);
    }
}
