package com.bl4ues.scpclassifieddirective.client.scp079;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Marker sound so the shared OpenAL layer can apply the facility-Speaker filter. */
public final class Scp079SpeakerCueSoundInstance extends SimpleSoundInstance {
    public Scp079SpeakerCueSoundInstance(ResourceLocation sound, double x,
            double y, double z, float volume, float pitch) {
        super(sound, SoundSource.BLOCKS, volume, pitch, RandomSource.create(),
                false, 0, SoundInstance.Attenuation.LINEAR, x, y, z, false);
    }
}
