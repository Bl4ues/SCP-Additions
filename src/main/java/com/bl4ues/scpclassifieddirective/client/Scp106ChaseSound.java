package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import com.bl4ues.scpclassifieddirective.init.Scp106Sounds;

/** Head-relative chase music heard only by the hunted local player. */
public final class Scp106ChaseSound extends AbstractTickableSoundInstance {
    private static final int FADE_IN_TICKS = 36;
    private static final int FADE_OUT_TICKS = 32;
    private static final int STOP_CUE_LEAD_TICKS = FADE_OUT_TICKS;
    private static final float MINIMUM_PLAYABLE_VOLUME = 0.01F;
    private static final float STOP_CUE_VOLUME = 0.14F;

    private int fadeInTicksElapsed;
    private int fadeTicksRemaining = -1;
    private boolean playStopCue;
    private boolean stopCuePlayed;
    private SimpleSoundInstance stopCue;

    public Scp106ChaseSound() {
        super(Scp106Sounds.CHASE.get(), SoundSource.MUSIC,
                RandomSource.create());
        this.looping = true;
        this.delay = 0;
        this.volume = MINIMUM_PLAYABLE_VOLUME;
        this.pitch = 1.0F;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if ((minecraft.player == null || minecraft.level == null
                || !minecraft.player.isAlive()) && !isFadingOut()) {
            beginFadeOut(false);
        }

        float fadeInVolume = getFadeInVolume();
        if (fadeTicksRemaining < 0) {
            if (fadeInTicksElapsed < FADE_IN_TICKS) fadeInTicksElapsed++;
            volume = getFadeInVolume();
            return;
        }
        if (playStopCue && !stopCuePlayed
                && fadeTicksRemaining <= STOP_CUE_LEAD_TICKS
                && minecraft.player != null && minecraft.level != null) {
            stopCue = SimpleSoundInstance.forUI(Scp106Sounds.STOP.get(),
                    1.0F, STOP_CUE_VOLUME);
            minecraft.getSoundManager().play(stopCue);
            stopCuePlayed = true;
        }
        if (fadeTicksRemaining == 0) {
            volume = 0.0F;
            stop();
            return;
        }

        volume = fadeInVolume * Mth.clamp(
                fadeTicksRemaining / (float) FADE_OUT_TICKS,
                0.0F, 1.0F);
        fadeTicksRemaining--;
    }

    private float getFadeInVolume() {
        float progress = Mth.clamp(fadeInTicksElapsed
                / (float) FADE_IN_TICKS, 0.0F, 1.0F);
        return Mth.lerp(progress, MINIMUM_PLAYABLE_VOLUME, 1.0F);
    }

    public void beginFadeOut(boolean withStopCue) {
        if (fadeTicksRemaining < 0) {
            fadeTicksRemaining = FADE_OUT_TICKS;
        }
        playStopCue |= withStopCue;
    }

    public boolean isFadingOut() {
        return fadeTicksRemaining >= 0;
    }

    public boolean hasActiveAudio() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getSoundManager().isActive(this)
                || (stopCue != null
                && minecraft.getSoundManager().isActive(stopCue));
    }

    public void stopImmediately() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().stop(this);
        if (stopCue != null) {
            minecraft.getSoundManager().stop(stopCue);
        }
    }
}
