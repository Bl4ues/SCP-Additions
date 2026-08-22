package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;

/** Owns the head-relative SCP-1176 music so it can be stopped on death or disconnect. */
public final class Scp1176MusicClient {
    private static SimpleSoundInstance activeMusic;
    private static int ticksSinceStart;

    private Scp1176MusicClient() {
    }

    public static void play() {
        stop();

        Minecraft minecraft = Minecraft.getInstance();
        activeMusic = SimpleSoundInstance.forUI(ScpClassifiedDirectiveModSounds.SCP1176.get(), 1.0F, 1.0F);
        ticksSinceStart = 0;
        ModMusicExclusivityClient.stopVanillaMusicNow();
        minecraft.getSoundManager().play(activeMusic);
    }

    public static void clientTick() {
        if (activeMusic == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.isAlive()) {
            stop();
            return;
        }

        ticksSinceStart++;
        if (ticksSinceStart > 1
                && !minecraft.getSoundManager().isActive(activeMusic)) {
            activeMusic = null;
            ticksSinceStart = 0;
        }
    }

    public static boolean isPlaying() {
        return activeMusic != null;
    }

    public static void stop() {
        if (activeMusic != null) {
            Minecraft.getInstance().getSoundManager().stop(activeMusic);
        }
        activeMusic = null;
        ticksSinceStart = 0;
    }
}
