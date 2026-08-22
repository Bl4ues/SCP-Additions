package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/** Small client-bound presentation events for save feedback that do not change save state. */
public record SaveFeedbackPacket(int kind) {
    public static final int SAVE_BLOCKED = 1;

    public static void encode(SaveFeedbackPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.kind);
    }

    public static SaveFeedbackPacket decode(FriendlyByteBuf buffer) {
        return new SaveFeedbackPacket(buffer.readVarInt());
    }

    public static void handle(SaveFeedbackPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> dispatchClient(message));
        context.setPacketHandled(true);
    }

    private static void dispatchClient(SaveFeedbackPacket message) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        try {
            Class<?> target = Class.forName(
                    "com.bl4ues.scpclassifieddirective.client.SaveGameClientState");
            Method method = target.getMethod("receiveFeedback", int.class);
            method.invoke(null, message.kind);
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.error(
                    "Could not display SCP: Classified Directive save feedback",
                    exception);
        }
    }
}
