package net.mcreator.scpadditions.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.mcreator.scpadditions.entity.RoombaEntity;

/** One positional motor loop bound to one living Roomba entity. */
public final class RoombaLoopSound extends AbstractTickableSoundInstance {
    private final RoombaEntity entity;

    public RoombaLoopSound(RoombaEntity entity) {
        super(RoombaSounds.LOOP, SoundSource.NEUTRAL,
                RandomSource.create());
        this.entity = entity;
        looping = true;
        delay = 0;
        volume = 0.55F;
        pitch = 1.0F;
        tick();
    }

    @Override
    public void tick() {
        if (entity == null || entity.isRemoved() || !entity.isAlive()) {
            stop();
            return;
        }
        x = entity.getX();
        y = entity.getY() + 0.10D;
        z = entity.getZ();
    }

    public void finish() {
        stop();
    }
}
