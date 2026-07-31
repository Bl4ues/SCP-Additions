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

    public ElevatorArrivalDisplayPacket(ElevatorArrivalDisplayData data) {
        this.data = data == null ? ElevatorArrivalDisplayData.NONE : data;
    }

    public static void encode(ElevatorArrivalDisplayPacket message,
            FriendlyByteBuf buffer) {
        ElevatorArrivalDisplayData.write(buffer, message.data);
    }

    public static ElevatorArrivalDisplayPacket decode(FriendlyByteBuf buffer) {
        return new ElevatorArrivalDisplayPacket(
                ElevatorArrivalDisplayData.read(buffer));
    }

    public static void handle(ElevatorArrivalDisplayPacket message,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ElevatorArrivalOverlay.show(message.data)));
        context.setPacketHandled(true);
    }
}
