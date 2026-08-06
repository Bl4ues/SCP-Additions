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
    private static final int INITIAL_DELAY_TICKS = 20;
    private static final int INACTIVE_RETRY_TICKS = 100;

    private static MainMenuMusicSound music;
    private static int startDelay = INITIAL_DELAY_TICKS;
    private static int inactiveTicks;

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
            startDelay = INITIAL_DELAY_TICKS;
            return;
        }

        if (music != null) {
            if (music.isStopped()) {
                music = null;
                inactiveTicks = 0;
                startDelay = INITIAL_DELAY_TICKS;
                return;
            }

            // Streamed sounds may need several ticks before SoundEngine marks
            // them active. Do not replace the instance every tick while it is
            // still loading, which continuously restarted the track before it
            // could become audible.
            if (minecraft.getSoundManager().isActive(music)) {
                inactiveTicks = 0;
            } else if (++inactiveTicks >= INACTIVE_RETRY_TICKS) {
                minecraft.getSoundManager().stop(music);
                music = null;
                inactiveTicks = 0;
                startDelay = INITIAL_DELAY_TICKS;
            }
            return;
        }

        if (startDelay > 0) {
            startDelay--;
            return;
        }

        music = new MainMenuMusicSound();
        inactiveTicks = 0;
        ModMusicExclusivityClient.stopVanillaMusicNow();
        minecraft.getSoundManager().play(music);
    }

    private static void stopMusic(Minecraft minecraft) {
        if (music != null) {
            minecraft.getSoundManager().stop(music);
            music = null;
        }
        inactiveTicks = 0;
    }
}
