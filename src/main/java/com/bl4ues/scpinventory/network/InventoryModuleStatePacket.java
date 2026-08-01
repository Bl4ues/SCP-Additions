package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record InventoryModuleStatePacket(boolean enabled,
        boolean requireEquippedWeaponToAttack,
        boolean reduceScp012VisualEffects, boolean hungerDisabled,
        boolean replacePlayerHurtSounds, boolean useVoiceProfileB,
        boolean muteNonPlayerHitSounds, boolean disableVanillaMusic,
        boolean hideActiveEffectIndicators,
        boolean hideEmptyHand,
        boolean disableExperienceBar,
        boolean customOxygenBar,
        boolean customCrosshairEnabled,
        boolean inGameCrosshairEnabled,
        float crosshairRed, float crosshairGreen,
        float crosshairBlue, float crosshairAlpha) {
    public static void encode(InventoryModuleStatePacket message,
            FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.enabled);
        buffer.writeBoolean(message.requireEquippedWeaponToAttack);
        buffer.writeBoolean(message.reduceScp012VisualEffects);
        buffer.writeBoolean(message.hungerDisabled);
        buffer.writeBoolean(message.replacePlayerHurtSounds);
        buffer.writeBoolean(message.useVoiceProfileB);
        buffer.writeBoolean(message.muteNonPlayerHitSounds);
        buffer.writeBoolean(message.disableVanillaMusic);
        buffer.writeBoolean(message.hideActiveEffectIndicators);
        buffer.writeBoolean(message.hideEmptyHand);
        buffer.writeBoolean(message.disableExperienceBar);
        buffer.writeBoolean(message.customOxygenBar);
        buffer.writeBoolean(message.customCrosshairEnabled);
        buffer.writeBoolean(message.inGameCrosshairEnabled);
        buffer.writeFloat(message.crosshairRed);
        buffer.writeFloat(message.crosshairGreen);
        buffer.writeFloat(message.crosshairBlue);
        buffer.writeFloat(message.crosshairAlpha);
    }

    public static InventoryModuleStatePacket decode(FriendlyByteBuf buffer) {
        return new InventoryModuleStatePacket(buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat());
    }

    public static void handle(InventoryModuleStatePacket message,
            Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> InventoryModuleRuntimeState.updateFromServer(
                message.enabled,
                message.requireEquippedWeaponToAttack,
                message.reduceScp012VisualEffects,
                message.hungerDisabled, message.replacePlayerHurtSounds,
                message.useVoiceProfileB,
                message.muteNonPlayerHitSounds,
                message.disableVanillaMusic,
                message.hideActiveEffectIndicators,
                message.hideEmptyHand,
                message.disableExperienceBar,
                message.customOxygenBar,
                message.customCrosshairEnabled,
                message.inGameCrosshairEnabled,
                message.crosshairRed, message.crosshairGreen,
                message.crosshairBlue, message.crosshairAlpha));
        context.setPacketHandled(true);
    }
}
