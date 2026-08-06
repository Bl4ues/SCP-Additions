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

    private static MainMenuMusicSound music;
    private static boolean startedFromMenu;
    private static boolean currentSubmissionWasActive;
    private static int inactiveTicks;

    private MainMenuMusicClient() {
    }

    public static boolean isPlaying() {
        return startedFromMenu;
    }

    /** Stops the soundtrack only after EnterSoundClient sees a rendered world. */
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
            // A pause screen must never start a fresh menu soundtrack session.
            if (minecraft.level == null && minecraft.screen != null) {
                startedFromMenu = true;
                submitMusic(minecraft);
            }
            return;
        }

        maintainMusic(minecraft, true);
    }

    /**
     * Client ticks can stall while the receiving-level percentage is rendered.
     * A render-tick watchdog immediately restores the stream if Minecraft's
     * sound engine discards it during the level handoff.
     */
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END && startedFromMenu) {
            maintainMusic(Minecraft.getInstance(), false);
        }
    }

    private static void maintainMusic(Minecraft minecraft,
            boolean advanceInitialRetry) {
        if (!ClientModulePreferences.mainMenuMusicEnabled()) {
            stopMusic(minecraft, true);
            return;
        }

        if (music == null || music.isStopped()) {
            submitMusic(minecraft);
            return;
        }

        if (minecraft.getSoundManager().isActive(music)) {
            currentSubmissionWasActive = true;
            inactiveTicks = 0;
            return;
        }

        // Once an audible stream disappears, this is a world-transition reset,
        // not initial decoding. Restore it on the next rendered frame.
        if (currentSubmissionWasActive) {
            submitMusic(minecraft);
            return;
        }

        if (advanceInitialRetry && ++inactiveTicks >= INITIAL_STREAM_RETRY_TICKS) {
            submitMusic(minecraft);
        }
    }

    private static void submitMusic(Minecraft minecraft) {
        if (music != null) minecraft.getSoundManager().stop(music);
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
        if (resetSession) startedFromMenu = false;
    }
}
