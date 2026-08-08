package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import com.bl4ues.scpinventory.network.ItemInteractionSoundPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.mcreator.scpadditions.client.ClientModulePreferences;
import net.mcreator.scpadditions.sound.GameplaySounds;

/** Plays personal SCP Inventory feedback without changing nearby players' audio. */
public final class ClientItemInteractionSounds {
    private ClientItemInteractionSounds() {
    }

    public static void play(ItemInteractionSoundPacket.Cue cue) {
        if (cue == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        boolean custom = InventoryModuleRuntimeState.isEnabledForClient()
                && ClientModulePreferences.customItemInteractionSoundsEnabled();

        if (custom) {
            SoundEvent sound = switch (cue) {
                case PICKUP, EQUIP -> GameplaySounds.ITEM_PICKUP.get();
                case FOOD -> GameplaySounds.ITEM_EAT.get();
                case DRINK -> GameplaySounds.ITEM_DRINK.get();
            };
            minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(sound, 1.0F, 1.0F));
            return;
        }

        switch (cue) {
            case PICKUP -> {
                float pitch = minecraft.player == null ? 1.4F
                        : ((minecraft.player.getRandom().nextFloat()
                        - minecraft.player.getRandom().nextFloat())
                        * 0.7F + 1.0F) * 2.0F;
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                        SoundEvents.ITEM_PICKUP, pitch, 0.2F));
            }
            case FOOD -> minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.GENERIC_EAT,
                            1.0F, 0.8F));
            case DRINK -> minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.GENERIC_DRINK,
                            1.0F, 0.8F));
            case EQUIP -> {
                // Equipment was silent before this optional feedback module.
            }
        }
    }
}
