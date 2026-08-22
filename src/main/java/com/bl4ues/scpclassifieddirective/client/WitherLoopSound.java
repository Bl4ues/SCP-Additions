package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import com.bl4ues.scpclassifieddirective.init.Scp106Sounds;

/** Positional Wither loop that follows one affected player and fades cleanly. */
public final class WitherLoopSound extends AbstractTickableSoundInstance {
    private static final float MINIMUM_START_VOLUME = 0.01F;
    private static final float TARGET_VOLUME = 0.85F;
    private static final int FADE_IN_TICKS = 20;
    private static final int FADE_OUT_TICKS = 30;

    private final Player player;
    private int fadeInTicks;
    private int fadeOutTicksRemaining = -1;
    private float fadeInStartVolume = MINIMUM_START_VOLUME;
    private float fadeOutStartVolume;

    public WitherLoopSound(Player player) {
        super(Scp106Sounds.WITHER.get(), SoundSource.PLAYERS,
                RandomSource.create());
        this.player = player;
        this.looping = true;
        this.delay = 0;
        // SoundEngine discards instances that begin at exactly zero volume.
        this.volume = MINIMUM_START_VOLUME;
        this.pitch = 1.0F;
        this.relative = false;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        updatePosition();
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean active = minecraft.level != null
                && player != null
                && !player.isRemoved()
                && player.isAlive()
                && player.level() == minecraft.level
                && player.hasEffect(MobEffects.WITHER);

        if (player != null && !player.isRemoved()) {
            updatePosition();
        }

        if (active) {
            resumeFadeIn();
            return;
        }
        fadeOut();
    }

    private void resumeFadeIn() {
        if (fadeOutTicksRemaining >= 0) {
            fadeOutTicksRemaining = -1;
            fadeInTicks = 0;
            fadeInStartVolume = Math.max(MINIMUM_START_VOLUME, volume);
        }
        if (volume >= TARGET_VOLUME) {
            volume = TARGET_VOLUME;
            return;
        }

        fadeInTicks++;
        volume = Mth.lerp(Mth.clamp(
                        fadeInTicks / (float) FADE_IN_TICKS, 0.0F, 1.0F),
                fadeInStartVolume, TARGET_VOLUME);
    }

    private void fadeOut() {
        if (fadeOutTicksRemaining < 0) {
            fadeOutStartVolume = Math.max(MINIMUM_START_VOLUME, volume);
            fadeOutTicksRemaining = FADE_OUT_TICKS;
        }
        if (fadeOutTicksRemaining <= 0) {
            stop();
            return;
        }

        volume = Math.max(MINIMUM_START_VOLUME,
                fadeOutStartVolume * fadeOutTicksRemaining
                        / (float) FADE_OUT_TICKS);
        fadeOutTicksRemaining--;
    }

    private void updatePosition() {
        this.x = player.getX();
        this.y = player.getY() + player.getBbHeight() * 0.5D;
        this.z = player.getZ();
    }

    public void finishImmediately() {
        stop();
    }
}
