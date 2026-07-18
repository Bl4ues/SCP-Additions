package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Starts SCP-714's authored soundtrack once per local exposure. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class Scp714MusicClient {
    private static Scp714MusicSound music;
    private static boolean playedForCurrentExposure;

    private Scp714MusicClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean active = minecraft.player != null && minecraft.level != null
                && minecraft.player.isAlive() && Scp714ClientState.isActive();

        if (active) {
            if (!playedForCurrentExposure) {
                startMusic(minecraft);
                playedForCurrentExposure = true;
            } else if (music != null
                    && !minecraft.getSoundManager().isActive(music)) {
                // The authored file is intentionally not looped or restarted.
                music = null;
            }
            return;
        }

        if (music != null) {
            music.beginFadeOut();
            if (!minecraft.getSoundManager().isActive(music)) {
                music = null;
            }
        }
        if (music == null) {
            playedForCurrentExposure = false;
        }
    }

    private static void startMusic(Minecraft minecraft) {
        if (music != null) {
            minecraft.getSoundManager().stop(music);
        }
        music = new Scp714MusicSound();
        minecraft.getSoundManager().play(music);
    }
}
