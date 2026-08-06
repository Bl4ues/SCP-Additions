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
    private static final int GAMEPLAY_READY_TICKS = 20;
    private static final int REPLAY_GRACE_TICKS = 40;

    private static MainMenuMusicSound music;
    private static boolean startedFromMenu;
    private static boolean wasActive;
    private static int gameplayTicks;
    private static int inactiveTicks;
    private static int replayGraceTicks;

    private MainMenuMusicClient() {
    }

    public static boolean isPlaying() {
        return startedFromMenu && music != null && !music.isStopped();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (!ClientModulePreferences.mainMenuMusicEnabled()) {
            stopMusic(minecraft, true);
            return;
        }

        /*
         * level/player can exist briefly before the loading presentation has
         * finished, and screen can also be null for transitional ticks. Treat
         * gameplay as ready only after a full second of stable in-world state.
         */
        boolean gameplayCandidate = minecraft.level != null
                && minecraft.player != null
                && minecraft.screen == null;
        gameplayTicks = gameplayCandidate ? gameplayTicks + 1 : 0;

        if (startedFromMenu && gameplayTicks >= GAMEPLAY_READY_TICKS) {
            stopMusic(minecraft, true);
            return;
        }

        if (!startedFromMenu) {
            if (minecraft.level == null && minecraft.screen != null) {
                startedFromMenu = true;
                submitMusic(minecraft);
            }
            return;
        }

        /*
         * Minecraft may clear active sounds while replacing the client level.
         * If the menu track had already reached the sound engine, resubmit it
         * during loading instead of interpreting that reset as the end of the
         * menu session.
         */
        if (music == null || music.isStopped()) {
            submitMusic(minecraft);
            return;
        }

        if (minecraft.getSoundManager().isActive(music)) {
            wasActive = true;
            inactiveTicks = 0;
            replayGraceTicks = 0;
            return;
        }

        // The first streamed submission may take time to decode. Do not churn
        // it before it has played at least once.
        if (!wasActive) return;

        if (replayGraceTicks > 0) {
            replayGraceTicks--;
            return;
        }

        if (++inactiveTicks >= 2) {
            minecraft.getSoundManager().stop(music);
            submitMusic(minecraft);
        }
    }

    private static void submitMusic(Minecraft minecraft) {
        music = new MainMenuMusicSound();
        inactiveTicks = 0;
        replayGraceTicks = wasActive ? REPLAY_GRACE_TICKS : 0;
        ModMusicExclusivityClient.stopVanillaMusicNow();
        minecraft.getSoundManager().play(music);
    }

    private static void stopMusic(Minecraft minecraft, boolean resetSession) {
        if (music != null) {
            minecraft.getSoundManager().stop(music);
            music = null;
        }
        if (!resetSession) return;

        startedFromMenu = false;
        wasActive = false;
        gameplayTicks = 0;
        inactiveTicks = 0;
        replayGraceTicks = 0;
    }
}
