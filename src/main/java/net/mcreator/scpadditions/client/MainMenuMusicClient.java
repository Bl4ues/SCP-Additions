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
    private static final int INITIAL_STREAM_RETRY_TICKS = 200;
    private static final int LOST_STREAM_RETRY_TICKS = 10;

    private static MainMenuMusicSound music;
    private static boolean startedFromMenu;
    private static boolean currentSubmissionWasActive;
    private static int inactiveTicks;

    private MainMenuMusicClient() {
    }

    public static boolean isPlaying() {
        return startedFromMenu;
    }

    /** Stops the menu soundtrack at the same moment as the world-entry cue. */
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
            // Start only from a genuine menu before any world exists. Pause
            // screens must never begin a new menu-music session.
            if (minecraft.level == null && minecraft.screen != null) {
                startedFromMenu = true;
                submitMusic(minecraft);
            }
            return;
        }

        /*
         * World transitions may temporarily discard streamed sounds. Keep the
         * session alive until EnterSoundClient confirms that gameplay is
         * actually visible, but do not recreate the stream every tick while it
         * is still decoding.
         */
        if (music == null || music.isStopped()) {
            submitMusic(minecraft);
            return;
        }

        if (minecraft.getSoundManager().isActive(music)) {
            currentSubmissionWasActive = true;
            inactiveTicks = 0;
            return;
        }

        inactiveTicks++;
        int retryAfter = currentSubmissionWasActive
                ? LOST_STREAM_RETRY_TICKS : INITIAL_STREAM_RETRY_TICKS;
        if (inactiveTicks >= retryAfter) {
            submitMusic(minecraft);
        }
    }

    private static void submitMusic(Minecraft minecraft) {
        if (music != null) {
            minecraft.getSoundManager().stop(music);
        }
        music = new MainMenuMusicSound();
        currentSubmissionWasActive = false;
        inactiveTicks = 0;
        ModMusicExclusivityClient.stopVanillaMusicNow();
        minecraft.getSoundManager().play(music);
    }

    private static void stopMusic(Minecraft minecraft, boolean resetSession) {
        if (music != null) {
            minecraft.getSoundManager().stop(music);
            music = null;
        }
        currentSubmissionWasActive = false;
        inactiveTicks = 0;
        if (resetSession) {
            startedFromMenu = false;
        }
    }
}
