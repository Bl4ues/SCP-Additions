package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.client.Scp079EnergyClientState;

import java.util.function.Supplier;

/** Server-to-client synchronization for optional developer HUDs. */
public final class Scp079EnergyPacket {
    private final boolean energyVisible;
    private final boolean active;
    private final float energy;
    private final boolean spawnTimersVisible;
    private final int scp173RemainingTicks;
    private final SpawnStatus scp173Status;

    public Scp079EnergyPacket(boolean energyVisible, boolean active, float energy,
            boolean spawnTimersVisible, int scp173RemainingTicks,
            SpawnStatus scp173Status) {
        this.energyVisible = energyVisible;
        this.active = active;
        this.energy = Mth.clamp(energy, 0.0F, 100.0F);
        this.spawnTimersVisible = spawnTimersVisible;
        this.scp173RemainingTicks = Math.max(-1, scp173RemainingTicks);
        this.scp173Status = scp173Status == null
                ? SpawnStatus.COUNTDOWN : scp173Status;
    }

    public static void encode(Scp079EnergyPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.energyVisible);
        buffer.writeBoolean(message.active);
        buffer.writeFloat(message.energy);
        buffer.writeBoolean(message.spawnTimersVisible);
        buffer.writeInt(message.scp173RemainingTicks);
        buffer.writeVarInt(message.scp173Status.ordinal());
    }

    public static Scp079EnergyPacket decode(FriendlyByteBuf buffer) {
        return new Scp079EnergyPacket(buffer.readBoolean(),
                buffer.readBoolean(), buffer.readFloat(), buffer.readBoolean(),
                buffer.readInt(), SpawnStatus.byId(buffer.readVarInt()));
    }

    public static void handle(Scp079EnergyPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> Scp079EnergyClientState.update(
                        message.energyVisible, message.active, message.energy,
                        message.spawnTimersVisible,
                        message.scp173RemainingTicks,
                        message.scp173Status)));
        context.setPacketHandled(true);
    }

    public enum SpawnStatus {
        COUNTDOWN,
        SPAWNED,
        DESPAWNED_TIMER_RESET,
        CHANCE_FAILED,
        NO_VALID_POSITION,
        BLOCKED_BY_EXISTING,
        MODULE_DISABLED,
        NATURAL_SPAWN_DISABLED,
        PAUSED_CREATIVE,
        PAUSED_SPECTATOR,
        NOT_IMPLEMENTED;

        public static SpawnStatus byId(int id) {
            SpawnStatus[] values = values();
            return id >= 0 && id < values.length ? values[id] : COUNTDOWN;
        }

        public boolean showsTimer() {
            return switch (this) {
                case MODULE_DISABLED, NATURAL_SPAWN_DISABLED,
                        PAUSED_CREATIVE, PAUSED_SPECTATOR,
                        NOT_IMPLEMENTED -> false;
                default -> true;
            };
        }
    }
}
