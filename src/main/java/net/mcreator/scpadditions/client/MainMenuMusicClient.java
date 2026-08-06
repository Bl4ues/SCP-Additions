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
        return music != null && !music.isStopped();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        boolean shouldPlay = minecraft.level == null
                && minecraft.screen != null
                && ClientModulePreferences.mainMenuMusicEnabled();

        if (!shouldPlay) {
            stopMusic(minecraft);
            return;
        }

        /*
         * A large streamed OGG is not reported as active while SoundEngine is
         * still decoding it. Keep the submitted instance alive instead of
         * cancelling and recreating it every five seconds before playback can
         * begin.
         */
        if (music != null && !music.isStopped()) return;

        music = new MainMenuMusicSound();
        ModMusicExclusivityClient.stopVanillaMusicNow();
        minecraft.getSoundManager().play(music);
    }

    private static void stopMusic(Minecraft minecraft) {
        if (music == null) return;
        minecraft.getSoundManager().stop(music);
        music = null;
    }
}
