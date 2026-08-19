package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ClientItemInteractionSounds;
import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.sound.GameplaySounds;

/** Applies local presentation-sound preferences before sounds reach the engine. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class ClientPreferenceSoundFilter {
    private ClientPreferenceSoundFilter() {
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getOriginalSound();
        if (sound == null || sound.getLocation() == null) return;

        // ClientItemInteractionSounds already tracks the short interaction window
        // and source proximity. The previous implementation never actually wired
        // that predicate into PlaySoundEvent, so vanilla cues escaped untouched.
        if (ClientItemInteractionSounds.shouldSuppressVanilla(sound)) {
            event.setSound(null);
            return;
        }

        if (!InventoryModuleRuntimeState.saveGameSoundEnabledForClient()
                && sound.getLocation().equals(
                GameplaySounds.SAVE_GAME.get().getLocation())) {
            event.setSound(null);
        }
    }
}
