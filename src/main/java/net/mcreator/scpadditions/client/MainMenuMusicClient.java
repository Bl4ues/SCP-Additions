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

    private MainMenuMusicClient() {
    }

    public static boolean isPlaying() {
        return music != null && !music.isStopped();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        boolean enabled = ClientModulePreferences.mainMenuMusicEnabled();
        boolean worldReady = minecraft.level != null
                && minecraft.player != null
                && minecraft.screen == null;

        // Keep an existing menu track alive through connection, registry and
        // terrain loading. Stop it only when gameplay has actually taken over.
        if (!enabled || worldReady) {
            stopMusic(minecraft);
            return;
        }

        if (music != null && !music.isStopped()) return;

        // Never start a fresh menu track after a world already exists. This
        // prevents pause screens and connection screens from restarting it.
        if (minecraft.level != null || minecraft.screen == null) return;

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
