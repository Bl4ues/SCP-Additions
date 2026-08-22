package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import com.bl4ues.scpclassifieddirective.entity.Scp939Entity;

/** Reusable fading loop for SCP-939 local music, breathing, and maul audio. */
public final class Scp939LoopSound extends AbstractTickableSoundInstance {
    private static final float MAX_VOLUME = 4.0F;

    private final Scp939Entity entity;
    private final float volumeMultiplier;
    private final float fadeInStep;
    private final float fadeOutStep;
    private float targetVolume;
    private boolean fadingOut;
    private boolean finished;

    public Scp939LoopSound(ResourceLocation soundId, SoundSource source,
            boolean relative, Scp939Entity entity, float initialVolume,
            float targetVolume, float fadeInStep, float fadeOutStep) {
        super(SoundEvent.createVariableRangeEvent(soundId), source,
                RandomSource.create());
        this.entity = entity;
        this.volumeMultiplier = encounterMixMultiplier(soundId);
        this.fadeInStep = Math.max(0.0001F,
                fadeInStep * this.volumeMultiplier);
        this.fadeOutStep = Math.max(0.0001F,
                fadeOutStep * this.volumeMultiplier);
        this.targetVolume = scaledVolume(targetVolume);
        this.looping = true;
        this.delay = 0;
        this.volume = Mth.clamp(initialVolume * this.volumeMultiplier,
                0.001F, MAX_VOLUME);
        this.pitch = 1.0F;
        this.relative = relative;
        this.attenuation = relative
                ? SoundInstance.Attenuation.NONE
                : SoundInstance.Attenuation.LINEAR;
        updatePosition();
    }

    @Override
    public void tick() {
        if (finished) return;
        if (entity != null) {
            if (!entity.isAlive() || entity.isRemoved()) {
                beginFadeOut();
            } else {
                updatePosition();
            }
        }

        if (fadingOut) {
            volume = approach(volume, 0.0F, fadeOutStep);
            if (volume <= 0.001F) {
                volume = 0.0F;
                finished = true;
                stop();
            }
            return;
        }

        volume = approach(volume, targetVolume, fadeInStep);
    }

    public void setTargetVolume(float targetVolume) {
        if (finished) return;
        this.targetVolume = scaledVolume(targetVolume);
        this.fadingOut = false;
    }

    /** Used by prey breathing, which intentionally resumes without a fade-in. */
    public void setTargetVolumeImmediately(float targetVolume) {
        if (finished) return;
        this.targetVolume = scaledVolume(targetVolume);
        this.volume = Math.max(0.001F, this.targetVolume);
        this.fadingOut = false;
    }

    public void beginFadeOut() {
        if (!finished) fadingOut = true;
    }

    public boolean isFadingOut() {
        return fadingOut;
    }

    public boolean isFinished() {
        return finished;
    }

    public void stopImmediately() {
        if (finished) return;
        finished = true;
        volume = 0.0F;
        Minecraft.getInstance().getSoundManager().stop(this);
    }

    private float scaledVolume(float rawVolume) {
        return Mth.clamp(rawVolume * volumeMultiplier, 0.0F, MAX_VOLUME);
    }

    /**
     * Ambient tension, chase music, and the prey breathing bed are authored
     * intentionally louder than the positional creature loops. Keep the boost
     * here so later target-volume changes cannot accidentally undo it.
     */
    private static float encounterMixMultiplier(ResourceLocation soundId) {
        if (soundId == null) return 1.0F;
        String path = soundId.getPath();
        return "ambient".equals(path) || "chase".equals(path)
                || "prey_breath".equals(path) ? 2.0F : 1.0F;
    }

    private void updatePosition() {
        if (entity == null) return;
        x = entity.getX();
        y = entity.getY() + entity.getBbHeight() * 0.45D;
        z = entity.getZ();
    }

    private static float approach(float value, float target, float step) {
        if (value < target) return Math.min(target, value + step);
        if (value > target) return Math.max(target, value - step);
        return value;
    }
}
