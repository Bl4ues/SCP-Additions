package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.mcreator.scpadditions.init.Scp106Sounds;

/** Head-relative chase music heard only by the hunted local player. */
public final class Scp106ChaseSound extends AbstractTickableSoundInstance {
    private static final int FADE_OUT_TICKS = 40;

    private int fadeTicksRemaining = -1;
    private boolean playStopCue;

    public Scp106ChaseSound() {
        super(Scp106Sounds.CHASE.get(), SoundSource.MUSIC,
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
        if ((minecraft.player == null || minecraft.level == null
                || !minecraft.player.isAlive()) && !isFadingOut()) {
            beginFadeOut(false);
        }

        if (fadeTicksRemaining < 0) return;
        if (fadeTicksRemaining == 0) {
            volume = 0.0F;
            stop();
            if (playStopCue && minecraft.player != null
                    && minecraft.level != null) {
                minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(Scp106Sounds.STOP.get(),
                                1.0F, 1.0F));
            }
            return;
        }

        volume = Mth.clamp(fadeTicksRemaining / (float) FADE_OUT_TICKS,
                0.0F, 1.0F);
        fadeTicksRemaining--;
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
}
