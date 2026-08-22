package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.client.Scp1576ClientState;

import java.util.UUID;
import java.util.function.Supplier;

/** Server-authoritative SCP-1576 winding, source, and voice-window state. */
public final class Scp1576StatePacket {
    public static final int WIND_START = 0;
    public static final int WIND_CANCEL = 1;
    public static final int ACTIVE_START = 2;
    public static final int ACTIVE_UPDATE = 3;
    public static final int ACTIVE_STOP = 4;

    private final int action;
    private final UUID sessionId;
    private final UUID hostId;
    private final String hostName;
    private final ResourceLocation dimension;
    private final double x;
    private final double y;
    private final double z;
    private final boolean voiceOpen;
    private final int ageTicks;
    private final int remainingTicks;

    public Scp1576StatePacket(int action, UUID sessionId, UUID hostId,
            String hostName, ResourceLocation dimension, double x, double y,
            double z, boolean voiceOpen, int ageTicks, int remainingTicks) {
        this.action = action;
        this.sessionId = sessionId;
        this.hostId = hostId;
        this.hostName = hostName == null ? "" : hostName;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.voiceOpen = voiceOpen;
        this.ageTicks = Math.max(0, ageTicks);
        this.remainingTicks = Math.max(0, remainingTicks);
    }

    public static Scp1576StatePacket windStart(UUID sessionId, UUID hostId,
            String hostName, ResourceLocation dimension,
            double x, double y, double z) {
        return new Scp1576StatePacket(WIND_START, sessionId, hostId, hostName,
                dimension, x, y, z, false, 0, 0);
    }

    public static Scp1576StatePacket windCancel(UUID sessionId, UUID hostId,
            ResourceLocation dimension, double x, double y, double z) {
        return new Scp1576StatePacket(WIND_CANCEL, sessionId, hostId, "",
                dimension, x, y, z, false, 0, 0);
    }

    public static Scp1576StatePacket active(int action, UUID sessionId,
            UUID hostId, String hostName, ResourceLocation dimension,
            double x, double y, double z, boolean voiceOpen,
            int ageTicks, int remainingTicks) {
        return new Scp1576StatePacket(action, sessionId, hostId, hostName,
                dimension, x, y, z, voiceOpen, ageTicks, remainingTicks);
    }

    public static void encode(Scp1576StatePacket message,
            FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.action);
        buffer.writeUUID(message.sessionId);
        buffer.writeUUID(message.hostId);
        buffer.writeUtf(message.hostName, 64);
        buffer.writeResourceLocation(message.dimension);
        buffer.writeDouble(message.x);
        buffer.writeDouble(message.y);
        buffer.writeDouble(message.z);
        buffer.writeBoolean(message.voiceOpen);
        buffer.writeVarInt(message.ageTicks);
        buffer.writeVarInt(message.remainingTicks);
    }

    public static Scp1576StatePacket decode(FriendlyByteBuf buffer) {
        return new Scp1576StatePacket(buffer.readVarInt(), buffer.readUUID(),
                buffer.readUUID(), buffer.readUtf(64),
                buffer.readResourceLocation(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readBoolean(),
                buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(Scp1576StatePacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> Scp1576ClientState.handle(message)));
        context.setPacketHandled(true);
    }

    public int action() { return action; }
    public UUID sessionId() { return sessionId; }
    public UUID hostId() { return hostId; }
    public String hostName() { return hostName; }
    public ResourceLocation dimension() { return dimension; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public boolean voiceOpen() { return voiceOpen; }
    public int ageTicks() { return ageTicks; }
    public int remainingTicks() { return remainingTicks; }
}
