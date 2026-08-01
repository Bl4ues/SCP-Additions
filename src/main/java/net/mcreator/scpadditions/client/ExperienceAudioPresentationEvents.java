package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Removes XP collection feedback while the complete XP presentation is disabled. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class ExperienceAudioPresentationEvents {
    private static final String PICKUP_SOUND = "entity.experience_orb.pickup";
    private static final String LEVEL_UP_SOUND = "entity.player.levelup";

    private ExperienceAudioPresentationEvents() {
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (!InventoryModuleRuntimeState.disableExperienceBarForClient()) {
            return;
        }

        SoundInstance sound = event.getOriginalSound();
        if (sound == null) return;
        ResourceLocation location = sound.getLocation();
        if (location == null || !"minecraft".equals(location.getNamespace())) {
            return;
        }

        String path = location.getPath();
        if (PICKUP_SOUND.equals(path) || LEVEL_UP_SOUND.equals(path)) {
            event.setSound(null);
        }
    }
}
