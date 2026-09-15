package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** One non-positional facility ambience loop with a continuously adjustable mix. */
final class FacilityZoneAmbientSound extends AbstractTickableSoundInstance {
    static final float MIN_VOLUME = 0.001F;
    private static final float START_VOLUME = 0.08F;
    private static final float RAMP_STEP = 0.020F;
    private static final float FADE_OUT_STEP = 0.018F;

    private float targetVolume;
    private boolean fadingOut;

    FacilityZoneAmbientSound(SoundEvent event, float targetVolume) {
        super(event, SoundSource.AMBIENT, RandomSource.create());
        looping = true;
        delay = 0;
        volume = START_VOLUME;
        pitch = 1.0F;
        relative = true;
        attenuation = SoundInstance.Attenuation.NONE;
        this.targetVolume = Math.max(MIN_VOLUME, targetVolume);
    }

    @Override
    public boolean canStartSilent() {
        // Facility ambience fades and can be created while category/master
        // volume changes are settling. Do not let SoundEngine discard the loop
        // before its first audible tick.
        return true;
    }

    @Override
    public void tick() {
        float target = fadingOut ? MIN_VOLUME
                : Math.max(MIN_VOLUME, targetVolume);
        float step = fadingOut ? FADE_OUT_STEP : RAMP_STEP;
        volume = approach(volume, target, step);
        if (fadingOut && volume <= MIN_VOLUME + 1.0E-5F) {
            stop();
        }
    }

    void setTargetVolume(float targetVolume) {
        if (fadingOut) return;
        this.targetVolume = Math.max(MIN_VOLUME, targetVolume);
    }

    void beginFadeOut() {
        fadingOut = true;
    }

    private static float approach(float value, float target, float step) {
        if (value < target) return Math.min(target, value + step);
        if (value > target) return Math.max(target, value - step);
        return Math.max(MIN_VOLUME, value);
    }
}
