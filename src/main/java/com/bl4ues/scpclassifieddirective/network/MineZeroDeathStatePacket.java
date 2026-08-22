package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/** Opens/updates the cooperative MineZero death session on a dead client. */
public record MineZeroDeathStatePacket(boolean openScreen, String cause,
        int livingPlayers, int deadPlayers, int votes, int requiredVotes) {
    public MineZeroDeathStatePacket {
        cause = cause == null ? "" : cause;
    }

    public static void encode(MineZeroDeathStatePacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.openScreen);
        buffer.writeUtf(message.cause, 512);
        buffer.writeVarInt(message.livingPlayers);
        buffer.writeVarInt(message.deadPlayers);
        buffer.writeVarInt(message.votes);
        buffer.writeVarInt(message.requiredVotes);
    }

    public static MineZeroDeathStatePacket decode(FriendlyByteBuf buffer) {
        return new MineZeroDeathStatePacket(buffer.readBoolean(),
                buffer.readUtf(512), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(MineZeroDeathStatePacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> dispatchClient(message));
        context.setPacketHandled(true);
    }

    private static void dispatchClient(MineZeroDeathStatePacket message) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        try {
            Class<?> target = Class.forName(
                    "com.bl4ues.scpclassifieddirective.client.MineZeroClientState");
            Method method = target.getMethod("receiveState", boolean.class,
                    String.class, int.class, int.class, int.class, int.class);
            method.invoke(null, message.openScreen, message.cause,
                    message.livingPlayers, message.deadPlayers, message.votes,
                    message.requiredVotes);
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.error(
                    "Could not synchronize MineZero death state", exception);
        }
    }
}
