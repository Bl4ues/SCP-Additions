package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Starts the authored menu soundtrack and carries it through world loading. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class MainMenuMusicClient {
    private static MainMenuMusicSound music;
    private static boolean startedFromMenu;

    private MainMenuMusicClient() {
    }

    public static boolean isPlaying() {
        return startedFromMenu && music != null && !music.isStopped();
    }

    /**
     * Ends the menu soundtrack at the same authoritative moment as the world
     * entry cue. This is called even when enter.ogg is disabled, so the config
     * changes only whether the cue is audible, not the transition timing.
     */
    public static void onWorldEntryCue() {
        stopMusic(Minecraft.getInstance(), true);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (!ClientModulePreferences.mainMenuMusicEnabled()) {
            stopMusic(minecraft, true);
            return;
        }

        if (!startedFromMenu) {
            // Start only from a genuine menu/loading screen before a level is
            // available. Pause screens must never begin a new menu session.
            if (minecraft.level == null && minecraft.screen != null) {
                startedFromMenu = true;
                submitMusic(minecraft);
            }
            return;
        }

        /*
         * The sound engine may discard active sounds while replacing the
         * client level. Keep resubmitting the authored track until the server's
         * delayed entry cue explicitly ends the menu session.
         */
        if (music == null || music.isStopped()
                || !minecraft.getSoundManager().isActive(music)) {
            submitMusic(minecraft);
        }
    }

    private static void submitMusic(Minecraft minecraft) {
        if (music != null) {
            minecraft.getSoundManager().stop(music);
        }
        music = new MainMenuMusicSound();
        ModMusicExclusivityClient.stopVanillaMusicNow();
        minecraft.getSoundManager().play(music);
    }

    private static void stopMusic(Minecraft minecraft, boolean resetSession) {
        if (music != null) {
            minecraft.getSoundManager().stop(music);
            music = null;
        }
        if (resetSession) {
            startedFromMenu = false;
        }
    }
}
