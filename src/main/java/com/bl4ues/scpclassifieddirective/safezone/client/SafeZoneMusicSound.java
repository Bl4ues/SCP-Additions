package com.bl4ues.scpclassifieddirective.safezone.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** One continuously looping Safe Zone track with smooth non-zero fade-in. */
final class SafeZoneMusicSound extends AbstractTickableSoundInstance {
    private static final float INITIAL_VOLUME = 0.001F;
    private static final int FADE_IN_TICKS = 60;
    private static final int FADE_OUT_TICKS = 60;

    private int fadeInTicks;
    private int fadeOutTicks = -1;
    private float fadeOutStartVolume = 1.0F;

    SafeZoneMusicSound(SoundEvent event) {
        super(event, SoundSource.MUSIC, RandomSource.create());
        looping = true;
        delay = 0;
        volume = INITIAL_VOLUME;
        pitch = 1.0F;
        relative = true;
        attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        if (fadeOutTicks >= 0) {
            if (fadeOutTicks == 0) {
                stop();
                return;
            }
            float fraction = fadeOutTicks / (float) FADE_OUT_TICKS;
            volume = Math.max(INITIAL_VOLUME,
                    fadeOutStartVolume * fraction);
            fadeOutTicks--;
            return;
        }

        if (fadeInTicks < FADE_IN_TICKS) {
            fadeInTicks++;
            volume = Math.max(INITIAL_VOLUME,
                    Mth.clamp(fadeInTicks / (float) FADE_IN_TICKS,
                            0.0F, 1.0F));
        } else {
            volume = 1.0F;
        }
    }

    void beginFadeOut() {
        if (fadeOutTicks >= 0) return;
        fadeOutStartVolume = Math.max(INITIAL_VOLUME, volume);
        fadeOutTicks = FADE_OUT_TICKS;
    }
}
