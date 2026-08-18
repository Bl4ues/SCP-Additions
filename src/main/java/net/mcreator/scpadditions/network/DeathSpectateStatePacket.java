package net.mcreator.scpadditions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.Supplier;

/** Server-selected live-feed target plus the authoritative survivor count. */
public record DeathSpectateStatePacket(boolean active, UUID targetId,
        String targetName, int availableTargets) {
    private static final UUID NIL = new UUID(0L, 0L);

    public DeathSpectateStatePacket {
        targetId = targetId == null ? NIL : targetId;
        targetName = targetName == null ? "" : targetName;
        availableTargets = Math.max(0, availableTargets);
    }

    public static void encode(DeathSpectateStatePacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.active);
        buffer.writeUUID(message.targetId);
        buffer.writeUtf(message.targetName, 64);
        buffer.writeVarInt(message.availableTargets);
    }

    public static DeathSpectateStatePacket decode(FriendlyByteBuf buffer) {
        return new DeathSpectateStatePacket(buffer.readBoolean(),
                buffer.readUUID(), buffer.readUtf(64), buffer.readVarInt());
    }

    public static void handle(DeathSpectateStatePacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> dispatchClient(message));
        context.setPacketHandled(true);
    }

    private static void dispatchClient(DeathSpectateStatePacket message) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        try {
            Class<?> target = Class.forName(
                    "net.mcreator.scpadditions.client.MineZeroSpectateClient");
            Method method = target.getMethod("receiveServerState",
                    boolean.class, UUID.class, String.class, int.class);
            method.invoke(null, message.active, message.targetId,
                    message.targetName, message.availableTargets);
        } catch (ReflectiveOperationException exception) {
            ScpAdditionsMod.LOGGER.error(
                    "Could not synchronize death spectate target", exception);
        }
    }
}
