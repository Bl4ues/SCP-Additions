package net.mcreator.scpadditions.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.mcreator.scpadditions.entity.RoombaEntity;

/** One fading positional motor segment bound to one living Roomba entity. */
public final class RoombaLoopSound extends AbstractTickableSoundInstance {
    public static final int SEGMENT_TICKS = 50;
    public static final int CROSSFADE_TICKS = 8;
    private static final float BASE_VOLUME = 0.275F;

    private final RoombaEntity entity;
    private int age;
    private boolean successorQueued;

    public RoombaLoopSound(RoombaEntity entity) {
        super(RoombaSounds.LOOP, SoundSource.NEUTRAL,
                RandomSource.create());
        this.entity = entity;
        looping = false;
        delay = 0;
        volume = 0.0F;
        pitch = 1.0F;
        updatePosition();
    }

    @Override
    public void tick() {
        if (entity == null || entity.isRemoved() || !entity.isAlive()) {
            stop();
            return;
        }

        updatePosition();
        age++;
        if (age <= CROSSFADE_TICKS) {
            volume = BASE_VOLUME * age / (float) CROSSFADE_TICKS;
        } else if (age >= SEGMENT_TICKS - CROSSFADE_TICKS) {
            volume = BASE_VOLUME * Math.max(0.0F,
                    (SEGMENT_TICKS - age)
                            / (float) CROSSFADE_TICKS);
        } else {
            volume = BASE_VOLUME;
        }

        if (age >= SEGMENT_TICKS) {
            stop();
        }
    }

    public boolean needsSuccessor() {
        return !successorQueued && !isStopped()
                && age >= SEGMENT_TICKS - CROSSFADE_TICKS;
    }

    public void markSuccessorQueued() {
        successorQueued = true;
    }

    public void finish() {
        stop();
    }

    private void updatePosition() {
        x = entity.getX();
        y = entity.getY() + 0.10D;
        z = entity.getZ();
    }
}
