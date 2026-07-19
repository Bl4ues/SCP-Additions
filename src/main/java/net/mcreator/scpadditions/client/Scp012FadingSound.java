package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Relative SCP-012 sound with smooth authored fade-in and fade-out control. */
public final class Scp012FadingSound extends AbstractTickableSoundInstance {
    private static final float MINIMUM_START_VOLUME = 0.01F;

    private final float targetVolume;
    private final int fadeInTicks;
    private final int fadeOutTicks;
    private int age;
    private int fadeTicksRemaining = -1;
    private float fadeStartVolume;

    public Scp012FadingSound(SoundEvent event, boolean looping,
                             float targetVolume, int fadeInTicks,
                             int fadeOutTicks) {
        super(event, SoundSource.AMBIENT, RandomSource.create());
        this.looping = looping;
        this.delay = 0;
        this.targetVolume = Math.max(0.0F, targetVolume);
        this.fadeInTicks = Math.max(0, fadeInTicks);
        this.fadeOutTicks = Math.max(1, fadeOutTicks);
        this.pitch = 1.0F;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;

        // SoundEngine may discard an instance that begins at exactly zero.
        // Start inaudibly low instead, then continue the authored fade normally.
        if (this.fadeInTicks == 0) {
            this.volume = this.targetVolume;
        } else {
            this.age = 1;
            this.volume = Math.min(this.targetVolume,
                    Math.max(MINIMUM_START_VOLUME,
                            this.targetVolume / this.fadeInTicks));
        }
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if ((minecraft.player == null || minecraft.level == null
                || !minecraft.player.isAlive()) && !isFadingOut()) {
            beginFadeOut();
        }

        if (fadeTicksRemaining >= 0) {
            if (fadeTicksRemaining == 0) {
                volume = 0.0F;
                stop();
                return;
            }
            volume = fadeStartVolume * Mth.clamp(
                    fadeTicksRemaining / (float) fadeOutTicks,
                    0.0F, 1.0F);
            fadeTicksRemaining--;
            return;
        }

        age++;
        if (fadeInTicks > 0) {
            volume = targetVolume * Mth.clamp(age / (float) fadeInTicks,
                    0.0F, 1.0F);
        } else {
            volume = targetVolume;
        }
    }

    public void beginFadeOut() {
        if (fadeTicksRemaining < 0) {
            fadeStartVolume = volume;
            fadeTicksRemaining = fadeOutTicks;
        }
    }

    public boolean isFadingOut() {
        return fadeTicksRemaining >= 0;
    }
}
