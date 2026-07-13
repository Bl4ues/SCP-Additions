package net.mcreator.scpadditions.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.mcreator.scpadditions.entity.AbstractScp131Entity;
import net.mcreator.scpadditions.entity.Scp131Sounds;

public final class Scp131LoopSound extends AbstractTickableSoundInstance {
    private final AbstractScp131Entity entity;

    public Scp131LoopSound(AbstractScp131Entity entity, boolean movementLoop) {
        super(movementLoop ? Scp131Sounds.EYE_POD_MOVE.get() : Scp131Sounds.EYE_POD_IDLE.get(),
                SoundSource.NEUTRAL, RandomSource.create());
        this.entity = entity;
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

    public void finish() {
        stop();
    }
}
