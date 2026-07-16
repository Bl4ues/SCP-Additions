package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

/** Owns the head-relative SCP-1176 music so it can be stopped on death or disconnect. */
public final class Scp1176MusicClient {
    private static SimpleSoundInstance activeMusic;

    private Scp1176MusicClient() {
    }

    public static void play() {
        stop();

        Minecraft minecraft = Minecraft.getInstance();
        activeMusic = SimpleSoundInstance.forUI(ScpAdditionsModSounds.SCP1176.get(), 1.0F, 1.0F);
        minecraft.getSoundManager().play(activeMusic);
    }

    public static void clientTick() {
        if (activeMusic == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.isAlive()) {
            stop();
        }
    }

    public static void stop() {
        if (activeMusic == null) return;

        Minecraft.getInstance().getSoundManager().stop(activeMusic);
        activeMusic = null;
    }
}
