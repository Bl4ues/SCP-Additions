package com.bl4ues.scpclassifieddirective.client.scp079;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;

/** Marker sound so the shared OpenAL layer can apply the facility-Speaker filter. */
public final class Scp079SpeakerCueSoundInstance extends SimpleSoundInstance {
    private final Sound directSound;
    private final WeighedSoundEvents directEvent;

    public Scp079SpeakerCueSoundInstance(ResourceLocation sound, double x,
            double y, double z, float volume, float pitch) {
        this(sound, x, y, z, volume, pitch, false);
    }

    /**
     * directFile is used for 079select_* samples, which SCP-079's camera UI loads
     * straight from the sound folder instead of through sounds.json events.
     */
    public Scp079SpeakerCueSoundInstance(ResourceLocation sound, double x,
            double y, double z, float volume, float pitch,
            boolean directFile) {
        super(sound, SoundSource.BLOCKS, volume, pitch, RandomSource.create(),
                false, 0, SoundInstance.Attenuation.LINEAR, x, y, z, false);
        if (directFile) {
            this.directSound = new Sound(sound.toString(),
                    ConstantFloat.of(1.0F), ConstantFloat.of(1.0F),
                    1, Sound.Type.FILE, false, false, 16);
            this.directEvent = new WeighedSoundEvents(sound, null);
            this.directEvent.addSound(this.directSound);
            this.sound = this.directSound;
        } else {
            this.directSound = null;
            this.directEvent = null;
        }
    }

    @Override
    public WeighedSoundEvents resolve(SoundManager manager) {
        if (directEvent != null) {
            this.sound = directSound;
            return directEvent;
        }
        return super.resolve(manager);
    }
}
