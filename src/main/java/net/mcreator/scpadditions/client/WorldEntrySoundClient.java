package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

/** Plays the world-entry cue non-positionally for the local player. */
public final class WorldEntrySoundClient {
    private WorldEntrySoundClient() {
    }

    public static void play() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                ScpAdditionsModSounds.ENTER.get(), 1.0F));
    }
}
