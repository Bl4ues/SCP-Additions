package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.inventory.config.InventoryModuleRuntimeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import com.bl4ues.scpclassifieddirective.init.PlayerVoiceSounds;

/** Shared drowning vocal loop with a smooth recovery fade. */
public final class DrowningLoopSound extends AbstractTickableSoundInstance {
    private static final float START_VOLUME = 0.02F;
    private static final float TARGET_VOLUME = 1.0F;
    private static final float FADE_IN_PER_TICK = 0.16F;
    private static final float FADE_OUT_PER_TICK = 0.065F;

    private boolean fadingOut;
    private boolean finished;

    public DrowningLoopSound() {
        super(PlayerVoiceSounds.DROWNING_LOOP.get(), SoundSource.PLAYERS,
                RandomSource.create());
        this.looping = true;
        this.delay = 0;
        // Minecraft may discard a sound submitted at absolute zero volume.
        // Begin barely audible, then perform the intended fade-in normally.
        this.volume = START_VOLUME;
        this.pitch = 1.0F;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (!canKeepPlaying(player)) {
            fadingOut = true;
        }

        if (fadingOut) {
            volume = Math.max(0.0F, volume - FADE_OUT_PER_TICK);
            if (volume <= 0.001F) {
                finish();
            }
            return;
        }

        volume = Math.min(TARGET_VOLUME, volume + FADE_IN_PER_TICK);
    }

    private static boolean canKeepPlaying(Player player) {
        return player != null
                && player.isAlive()
                && InventoryModuleRuntimeState
                .replacePlayerHurtSoundsForClient()
                && player.isUnderWater()
                && player.getAirSupply() <= 0;
    }

    public void resume() {
        if (!finished) fadingOut = false;
    }

    public void beginFadeOut() {
        if (!finished) fadingOut = true;
    }

    public boolean isFinished() {
        return finished;
    }

    private void finish() {
        finished = true;
        stop();
    }
}
