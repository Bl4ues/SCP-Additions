package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

/** Local, non-positional breathing loop heard from inside the sealed mask. */
public final class HazmatBreathingSound extends AbstractTickableSoundInstance {
    public HazmatBreathingSound() {
        super(ScpAdditionsModSounds.HAZMAT_BREATHING.get(),
                SoundSource.PLAYERS, RandomSource.create());
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
        if (minecraft.player == null || !minecraft.player.isAlive()) {
            stop();
        }
    }

    public void finish() {
        stop();
    }
}
