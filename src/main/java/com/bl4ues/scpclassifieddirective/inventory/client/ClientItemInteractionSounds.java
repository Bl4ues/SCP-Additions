package com.bl4ues.scpclassifieddirective.inventory.client;

import com.bl4ues.scpclassifieddirective.inventory.config.InventoryModuleRuntimeState;
import com.bl4ues.scpclassifieddirective.inventory.network.ItemInteractionSoundPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import com.bl4ues.scpclassifieddirective.client.ClientModulePreferences;
import com.bl4ues.scpclassifieddirective.sound.GameplaySounds;

/** Plays personal SCP Inventory feedback without changing nearby players' audio. */
public final class ClientItemInteractionSounds {
    private static final long VANILLA_SUPPRESSION_NANOS = 750_000_000L;
    private static final double LOCAL_INTERACTION_RADIUS_SQ = 9.0D;

    private static volatile long suppressPickupUntilNanos = Long.MIN_VALUE;
    private static volatile long suppressFoodUntilNanos = Long.MIN_VALUE;
    private static volatile long suppressDrinkUntilNanos = Long.MIN_VALUE;

    private ClientItemInteractionSounds() {
    }

    public static void play(ItemInteractionSoundPacket.Cue cue) {
        if (cue == null) return;

        if (!customSoundsEnabled()) {
            // Server-confirmed contextual Take interactions do not produce a
            // vanilla ItemEntity pickup locally, so provide the normal cue here
            // when the custom presentation preference is disabled.
            if (cue == ItemInteractionSoundPacket.Cue.PICKUP) {
                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP,
                                1.0F, 1.0F));
            }
            return;
        }

        armVanillaSuppression(cue);
        SoundEvent sound = switch (cue) {
            case PICKUP, EQUIP -> GameplaySounds.ITEM_PICKUP.get();
            case FOOD -> GameplaySounds.ITEM_EAT.get();
            case DRINK -> GameplaySounds.ITEM_DRINK.get();
        };
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, 1.0F, 1.0F));
    }

    /**
     * Suppresses only vanilla feedback produced by the same SCP Inventory
     * interaction that armed the window. Ordinary vanilla pickups and eating
     * remain untouched, and disabling the preference restores them directly.
     */
    public static boolean shouldSuppressVanilla(SoundInstance sound) {
        if (sound == null || !customSoundsEnabled()) return false;
        var location = sound.getLocation();
        if (location == null) return false;

        long now = System.nanoTime();
        boolean matching = location.equals(SoundEvents.ITEM_PICKUP.getLocation())
                && now <= suppressPickupUntilNanos;
        matching |= location.equals(SoundEvents.GENERIC_EAT.getLocation())
                && now <= suppressFoodUntilNanos;
        // The vanilla food-completion cue is a separate player.burp event, not
        // part of generic_eat. Keep it in the same short, local suppression
        // window so the custom SCP Inventory eating sound genuinely replaces
        // the vanilla presentation instead of merely layering over it.
        matching |= location.equals(SoundEvents.PLAYER_BURP.getLocation())
                && now <= suppressFoodUntilNanos;
        matching |= location.equals(SoundEvents.GENERIC_DRINK.getLocation())
                && now <= suppressDrinkUntilNanos;
        return matching && isLocalInteractionSound(sound);
    }

    private static boolean customSoundsEnabled() {
        return InventoryModuleRuntimeState.isEnabledForClient()
                && ClientModulePreferences.customItemInteractionSoundsEnabled();
    }

    private static void armVanillaSuppression(
            ItemInteractionSoundPacket.Cue cue) {
        long until = System.nanoTime() + VANILLA_SUPPRESSION_NANOS;
        switch (cue) {
            case PICKUP -> suppressPickupUntilNanos = until;
            case FOOD -> suppressFoodUntilNanos = until;
            case DRINK -> suppressDrinkUntilNanos = until;
            case EQUIP -> {
                // Equipping had no vanilla local feedback to replace.
            }
        }
    }

    private static boolean isLocalInteractionSound(SoundInstance sound) {
        if (sound.isRelative()) return true;
        Player player = Minecraft.getInstance().player;
        if (player == null) return false;
        double dx = sound.getX() - player.getX();
        double dy = sound.getY() - player.getY();
        double dz = sound.getZ() - player.getZ();
        return dx * dx + dy * dy + dz * dz <= LOCAL_INTERACTION_RADIUS_SQ;
    }
}
