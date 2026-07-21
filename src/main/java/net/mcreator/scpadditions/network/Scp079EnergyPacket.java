package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.client.Scp079EnergyClientState;

import java.util.function.Supplier;

/** Server-to-client synchronization for the optional SCP-079 debug HUD. */
public final class Scp079EnergyPacket {
    private final boolean visible;
    private final boolean active;
    private final float energy;

    public Scp079EnergyPacket(boolean visible, boolean active, float energy) {
        this.visible = visible;
        this.active = active;
        this.energy = Mth.clamp(energy, 0.0F, 100.0F);
    }

    public static void encode(Scp079EnergyPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.visible);
        buffer.writeBoolean(message.active);
        buffer.writeFloat(message.energy);
    }

    public static Scp079EnergyPacket decode(FriendlyByteBuf buffer) {
        return new Scp079EnergyPacket(buffer.readBoolean(),
                buffer.readBoolean(), buffer.readFloat());
    }

    public static void handle(Scp079EnergyPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> Scp079EnergyClientState.update(message.visible,
                        message.active, message.energy)));
        context.setPacketHandled(true);
    }
}
