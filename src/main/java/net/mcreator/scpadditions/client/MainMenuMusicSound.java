package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.mcreator.scpadditions.init.MainMenuSounds;

/** Non-positional looping soundtrack used while the client is outside a world. */
public final class MainMenuMusicSound extends AbstractTickableSoundInstance {
    public MainMenuMusicSound() {
        super(MainMenuSounds.MAIN_MENU.get(), SoundSource.MUSIC,
                RandomSource.create());
        this.looping = true;
        this.delay = 0;
        this.volume = 1.0F;
        this.pitch = 1.0F;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null
                || !ClientModulePreferences.mainMenuMusicEnabled()) {
            stop();
        }
    }
}
