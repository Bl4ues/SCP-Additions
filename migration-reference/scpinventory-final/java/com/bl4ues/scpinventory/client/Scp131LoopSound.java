package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.entity.AbstractScp131Entity;
import com.bl4ues.scpinventory.sound.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class Scp131LoopSound extends AbstractTickableSoundInstance {
    private final AbstractScp131Entity entity;
    private final boolean movementLoop;

    public Scp131LoopSound(AbstractScp131Entity entity, boolean movementLoop) {
        super(movementLoop ? ModSounds.EYE_POD_MOVE.get() : ModSounds.EYE_POD_IDLE.get(), SoundSource.NEUTRAL, RandomSource.create());
        this.entity = entity;
        this.movementLoop = movementLoop;
        this.looping = true;
        this.delay = 0;
        this.volume = 1.0F;
        this.pitch = 1.0F;
        tick();
    }

    @Override
    public void tick() {
        if (entity == null || entity.isRemoved() || !entity.isAlive()) {
            stop();
            return;
        }

        this.x = entity.getX();
        this.y = entity.getY() + 0.28D;
        this.z = entity.getZ();
    }

    public boolean isMovementLoop() {
        return movementLoop;
    }

    public void finish() {
        stop();
    }
}
