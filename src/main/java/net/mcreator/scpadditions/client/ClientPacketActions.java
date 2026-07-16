package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

/** Client-only endpoints invoked through the common packet bridge. */
public final class ClientPacketActions {
    private ClientPacketActions() {
    }

    public static void playScareSound() {
        BlinkClient.playScareSound();
    }

    public static void playEnterSound() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(
                SimpleSoundInstance.forUI(ScpAdditionsModSounds.ENTER.get(), 1.0F, 1.0F));
    }

    public static void playScp1176Music() {
        Scp1176MusicClient.play();
    }
}
