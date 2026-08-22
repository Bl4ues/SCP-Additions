package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import com.bl4ues.scpclassifieddirective.entity.Scp173Entity;
import com.bl4ues.scpclassifieddirective.entity.Scp173Sounds;

/** Continuous stone scrape attached to a moving SCP-173 instance. */
public final class Scp173MovementLoopSound
        extends AbstractTickableSoundInstance {
    private final Scp173Entity entity;

    public Scp173MovementLoopSound(Scp173Entity entity) {
        super(Scp173Sounds.STONE_SCRAP_LOOP.get(), SoundSource.HOSTILE,
                RandomSource.create());
        this.entity = entity;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.68F;
        this.pitch = 1.0F;
        tick();
    }

    @Override
    public void tick() {
        if (entity == null || entity.isRemoved() || !entity.isAlive()
                || !entity.isActivated() || !entity.isScraping()) {
            stop();
            return;
        }
        this.x = entity.getX();
        this.y = entity.getY() + 0.35D;
        this.z = entity.getZ();
    }

    public void finish() {
        stop();
    }
}
