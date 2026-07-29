package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record InventoryModuleStatePacket(boolean enabled,
        boolean reduceScp012VisualEffects, boolean hungerDisabled,
        boolean replacePlayerHurtSounds, boolean muteNonPlayerHitSounds,
        boolean disableVanillaMusic,
        boolean hideActiveEffectIndicators) {
    public static void encode(InventoryModuleStatePacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.enabled);
        buffer.writeBoolean(message.reduceScp012VisualEffects);
        buffer.writeBoolean(message.hungerDisabled);
        buffer.writeBoolean(message.replacePlayerHurtSounds);
        buffer.writeBoolean(message.muteNonPlayerHitSounds);
        buffer.writeBoolean(message.disableVanillaMusic);
        buffer.writeBoolean(message.hideActiveEffectIndicators);
    }

    public static InventoryModuleStatePacket decode(FriendlyByteBuf buffer) {
        return new InventoryModuleStatePacket(buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(InventoryModuleStatePacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> InventoryModuleRuntimeState.updateFromServer(
                message.enabled, message.reduceScp012VisualEffects,
                message.hungerDisabled, message.replacePlayerHurtSounds,
                message.muteNonPlayerHitSounds,
                message.disableVanillaMusic,
                message.hideActiveEffectIndicators));
        context.setPacketHandled(true);
    }
}
