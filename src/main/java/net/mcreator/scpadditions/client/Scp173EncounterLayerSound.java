package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** One continuously synchronized layer of SCP-173's local encounter score. */
public final class Scp173EncounterLayerSound
        extends AbstractTickableSoundInstance {
    public static final float BACKGROUND_VOLUME = 0.02F;
    public static final float FOREGROUND_VOLUME = 1.0F;

    private static final float CROSSFADE_STEP = 0.025F;
    private static final float STOP_FADE_STEP = 0.0125F;

    private float targetVolume = BACKGROUND_VOLUME;
    private boolean fadingOut;

    public Scp173EncounterLayerSound(ResourceLocation soundId) {
        super(SoundEvent.createVariableRangeEvent(soundId),
                SoundSource.MUSIC, RandomSource.create());
        this.looping = true;
        this.delay = 0;
        // Never start at absolute zero. Minecraft may discard a sound before
        // later ticks can raise its volume.
        this.volume = BACKGROUND_VOLUME;
        this.pitch = 1.0F;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if ((minecraft.player == null || minecraft.level == null
                || !minecraft.player.isAlive()) && !fadingOut) {
            beginFadeOut();
        }

        if (fadingOut) {
            volume = approach(volume, 0.0F, STOP_FADE_STEP);
            if (volume <= 0.0001F) {
                volume = 0.0F;
                stop();
            }
            return;
        }

        volume = approach(volume, targetVolume, CROSSFADE_STEP);
    }

    public void setForeground(boolean foreground) {
        setTargetVolume(foreground ? FOREGROUND_VOLUME : BACKGROUND_VOLUME);
    }

    public void setTargetVolume(float target) {
        if (fadingOut) return;
        targetVolume = Mth.clamp(target, BACKGROUND_VOLUME,
                FOREGROUND_VOLUME);
    }

    public void beginFadeOut() {
        fadingOut = true;
    }

    public boolean isFadingOut() {
        return fadingOut;
    }

    public boolean hasActiveAudio() {
        return Minecraft.getInstance().getSoundManager().isActive(this);
    }

    public void stopImmediately() {
        Minecraft.getInstance().getSoundManager().stop(this);
    }

    private static float approach(float value, float target, float step) {
        if (value < target) return Math.min(target, value + step);
        if (value > target) return Math.max(target, value - step);
        return value;
    }
}
