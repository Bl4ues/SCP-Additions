package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.mcreator.scpadditions.init.Scp106Sounds;

/** Plays the optional non-positional sound used when joining a world. */
public final class EnterSoundClient {
    private EnterSoundClient() {
    }

    public static void play() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(Scp106Sounds.ENTER.get(),
                        1.0F, 1.0F));
    }
}
