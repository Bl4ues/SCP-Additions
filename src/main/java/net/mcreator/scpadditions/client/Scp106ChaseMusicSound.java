package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

/** Local, non-positional chase loop with a controlled fade-out. */
public final class Scp106ChaseMusicSound
        extends AbstractTickableSoundInstance {
    private static final int FADE_OUT_TICKS = 50;
    private int fadeTicksRemaining = -1;

    public Scp106ChaseMusicSound() {
        super(ScpAdditionsModSounds.SCP_106_CHASE.get(),
                SoundSource.MUSIC, RandomSource.create());
        looping = true;
        delay = 0;
        volume = 1.0F;
        pitch = 1.0F;
        relative = true;
        attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if ((minecraft.player == null || minecraft.level == null)
                && !isFadingOut()) {
            beginFadeOut();
        }
        if (fadeTicksRemaining < 0) return;
        if (fadeTicksRemaining == 0) {
            volume = 0.0F;
            stop();
            return;
        }
        volume = Mth.clamp(fadeTicksRemaining
                / (float) FADE_OUT_TICKS, 0.0F, 1.0F);
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
