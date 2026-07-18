package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

/** Non-positional soundtrack heard only by the local player affected by SCP-714. */
public final class Scp714MusicSound extends AbstractTickableSoundInstance {
    private static final int FADE_OUT_TICKS = 40;

    private int fadeTicksRemaining = -1;

    public Scp714MusicSound() {
        super(ScpAdditionsModSounds.SCP_714_MUSIC.get(),
                SoundSource.MUSIC, RandomSource.create());
        this.looping = false;
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
            beginFadeOut();
        }

        if (fadeTicksRemaining < 0) {
            return;
        }
        if (fadeTicksRemaining == 0) {
            volume = 0.0F;
            stop();
            return;
        }

        volume = Mth.clamp(fadeTicksRemaining / (float) FADE_OUT_TICKS,
                0.0F, 1.0F);
        fadeTicksRemaining--;
    }

    public void beginFadeOut() {
        if (fadeTicksRemaining < 0) {
            fadeTicksRemaining = FADE_OUT_TICKS;
        }
    }

    public boolean isFadingOut() {
        return fadeTicksRemaining >= 0;
    }
}
