package com.bl4ues.scpclassifieddirective.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/** Server-authoritative MineZero installation/toggle state for the config UI. */
public record MineZeroCompatibilityStatusPacket(boolean installed,
        boolean enabled, boolean canEdit) {
    public static void encode(MineZeroCompatibilityStatusPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.installed);
        buffer.writeBoolean(message.enabled);
        buffer.writeBoolean(message.canEdit);
    }

    public static MineZeroCompatibilityStatusPacket decode(FriendlyByteBuf buffer) {
        return new MineZeroCompatibilityStatusPacket(buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(MineZeroCompatibilityStatusPacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> dispatchClient(message));
        context.setPacketHandled(true);
    }

    private static void dispatchClient(MineZeroCompatibilityStatusPacket message) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        try {
            Class<?> target = Class.forName(
                    "com.bl4ues.scpclassifieddirective.client.MineZeroCompatibilityClientState");
            Method method = target.getMethod("receive", boolean.class,
                    boolean.class, boolean.class);
            method.invoke(null, message.installed, message.enabled,
                    message.canEdit);
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.error(
                    "Could not synchronize MineZero compatibility status",
                    exception);
        }
    }
}
