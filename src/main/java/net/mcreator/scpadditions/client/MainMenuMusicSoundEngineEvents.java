package net.mcreator.scpadditions.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.SoundEngineLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Invalidates the persistent menu stream when Minecraft reloads OpenAL. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MainMenuMusicSoundEngineEvents {
    private MainMenuMusicSoundEngineEvents() {
    }

    @SubscribeEvent
    public static void onSoundEngineLoad(SoundEngineLoadEvent event) {
        PersistentMenuMusicPlayer.onSoundEngineLoad(event.getEngine());
    }
}
