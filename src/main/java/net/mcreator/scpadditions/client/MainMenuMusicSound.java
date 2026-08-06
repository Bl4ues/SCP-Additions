package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Non-positional looping soundtrack used while the client is outside a world. */
public final class MainMenuMusicSound extends AbstractTickableSoundInstance {
    private static final SoundEvent EVENT = SoundEvent.createVariableRangeEvent(
            new ResourceLocation("scp_additions_menu", "main_menu"));

    public MainMenuMusicSound() {
        super(EVENT, SoundSource.MUSIC, RandomSource.create());
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
