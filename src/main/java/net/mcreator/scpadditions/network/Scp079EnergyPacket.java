package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.client.Scp079EnergyClientState;
import net.mcreator.scpadditions.roamer.RoamerResult;
import net.mcreator.scpadditions.roamer.RoamerState;
import net.mcreator.scpadditions.roamer.RoamerType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Server-to-client synchronization for optional developer HUDs. */
public final class Scp079EnergyPacket {
    private final boolean energyVisible;
    private final boolean active;
    private final float energy;
    private final boolean spawnTimersVisible;
    private final List<RoamerEntry> roamers;

    public Scp079EnergyPacket(boolean energyVisible, boolean active, float energy,
            boolean spawnTimersVisible, List<RoamerEntry> roamers) {
        this.energyVisible = energyVisible;
        this.active = active;
        this.energy = Mth.clamp(energy, 0.0F, 100.0F);
        this.spawnTimersVisible = spawnTimersVisible;
        this.roamers = roamers == null ? List.of() : List.copyOf(roamers);
    }

    public static void encode(Scp079EnergyPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.energyVisible);
        buffer.writeBoolean(message.active);
        buffer.writeFloat(message.energy);
        buffer.writeBoolean(message.spawnTimersVisible);
        buffer.writeVarInt(message.roamers.size());
        for (RoamerEntry entry : message.roamers) {
            buffer.writeVarInt(entry.type().ordinal());
            buffer.writeVarInt(entry.state().ordinal());
            buffer.writeVarInt(entry.result().ordinal());
            buffer.writeInt(entry.remainingTicks());
        }
    }

    public static Scp079EnergyPacket decode(FriendlyByteBuf buffer) {
        boolean energyVisible = buffer.readBoolean();
        boolean active = buffer.readBoolean();
        float energy = buffer.readFloat();
        boolean spawnTimersVisible = buffer.readBoolean();
        int size = Mth.clamp(buffer.readVarInt(), 0, 64);
        List<RoamerEntry> roamers = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            RoamerType type = enumById(RoamerType.values(),
                    buffer.readVarInt(), RoamerType.SCP_173);
            RoamerState state = enumById(RoamerState.values(),
                    buffer.readVarInt(), RoamerState.DISABLED);
            RoamerResult result = enumById(RoamerResult.values(),
                    buffer.readVarInt(), RoamerResult.NONE);
            roamers.add(new RoamerEntry(type, state, result,
                    Math.max(-1, buffer.readInt())));
        }
        return new Scp079EnergyPacket(energyVisible, active, energy,
                spawnTimersVisible, roamers);
    }

    public static void handle(Scp079EnergyPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> Scp079EnergyClientState.update(
                        message.energyVisible, message.active, message.energy,
                        message.spawnTimersVisible, message.roamers)));
        context.setPacketHandled(true);
    }

    private static <T> T enumById(T[] values, int id, T fallback) {
        return id >= 0 && id < values.length ? values[id] : fallback;
    }

    public record RoamerEntry(RoamerType type, RoamerState state,
            RoamerResult result, int remainingTicks) {
        public RoamerEntry {
            if (type == null) type = RoamerType.SCP_173;
            if (state == null) state = RoamerState.DISABLED;
            if (result == null) result = RoamerResult.NONE;
            remainingTicks = Math.max(-1, remainingTicks);
        }
    }
}
