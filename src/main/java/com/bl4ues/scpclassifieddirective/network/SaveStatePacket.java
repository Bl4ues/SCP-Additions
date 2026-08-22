package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/** Synchronizes the last save method and optionally starts the Saving... HUD. */
public record SaveStatePacket(String methodId, boolean showOverlay) {
    public SaveStatePacket {
        methodId = methodId == null ? "world_spawn" : methodId;
    }

    public static void encode(SaveStatePacket message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.methodId, 64);
        buffer.writeBoolean(message.showOverlay);
    }

    public static SaveStatePacket decode(FriendlyByteBuf buffer) {
        return new SaveStatePacket(buffer.readUtf(64), buffer.readBoolean());
    }

    public static void handle(SaveStatePacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> dispatchClient(message));
        context.setPacketHandled(true);
    }

    private static void dispatchClient(SaveStatePacket message) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        try {
            Class<?> target = Class.forName(
                    "com.bl4ues.scpclassifieddirective.client.SaveGameClientState");
            Method method = target.getMethod("receive",
                    String.class, boolean.class);
            method.invoke(null, message.methodId, message.showOverlay);
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.error(
                    "Could not synchronize SCP: Classified Directive save state",
                    exception);
        }
    }
}
