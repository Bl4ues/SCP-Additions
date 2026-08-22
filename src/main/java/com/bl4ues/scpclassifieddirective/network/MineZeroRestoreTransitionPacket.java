package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/** Synchronizes the pre/post rewind presentation around a MineZero restore. */
public record MineZeroRestoreTransitionPacket(boolean beginning) {
    public static void encode(MineZeroRestoreTransitionPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.beginning);
    }

    public static MineZeroRestoreTransitionPacket decode(FriendlyByteBuf buffer) {
        return new MineZeroRestoreTransitionPacket(buffer.readBoolean());
    }

    public static void handle(MineZeroRestoreTransitionPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> dispatchClient(message.beginning));
        context.setPacketHandled(true);
    }

    private static void dispatchClient(boolean beginning) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        try {
            Class<?> target = Class.forName(
                    "com.bl4ues.scpclassifieddirective.client.MineZeroClientState");
            Method method = target.getMethod(beginning
                    ? "beginRestore" : "finishRestore");
            method.invoke(null);
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.error(
                    "Could not synchronize MineZero restore transition",
                    exception);
        }
    }
}
