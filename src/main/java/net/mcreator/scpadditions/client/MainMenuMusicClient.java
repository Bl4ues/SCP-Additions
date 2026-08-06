package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Starts and maintains the authored soundtrack while no world is open. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class MainMenuMusicClient {
    private static MainMenuMusicSound music;

    private MainMenuMusicClient() {
    }

    public static boolean isPlaying() {
        return music != null;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        boolean shouldPlay = minecraft.level == null
                && ClientModulePreferences.mainMenuMusicEnabled();

        if (!shouldPlay) {
            stopMusic(minecraft);
            return;
        }

        if (music == null
                || !minecraft.getSoundManager().isActive(music)) {
            music = new MainMenuMusicSound();
            ModMusicExclusivityClient.stopVanillaMusicNow();
            minecraft.getSoundManager().play(music);
        }
    }

    private static void stopMusic(Minecraft minecraft) {
        if (music == null) return;
        minecraft.getSoundManager().stop(music);
        music = null;
    }
}
