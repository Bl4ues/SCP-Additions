package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.facility.speaker.SpeakerModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/** Owns one seamless positional static loop per active speaker block. */
public final class SpeakerAudioClient {
    private static final Map<Key, SpeakerLoop> LOOPS = new HashMap<>();

    private SpeakerAudioClient() {
    }

    public static void update(Level level, BlockPos pos, boolean active) {
        if (!(level instanceof ClientLevel client)) return;
        Key key = new Key(client, pos.immutable());
        SpeakerLoop current = LOOPS.get(key);
        if (!active) {
            if (current != null) current.requestStop();
            return;
        }
        if (current != null && !current.isFinished()) return;
        SpeakerLoop created = new SpeakerLoop(key);
        LOOPS.put(key, created);
        Minecraft.getInstance().getSoundManager().play(created);
    }

    private static void finished(Key key, SpeakerLoop sound) {
        LOOPS.remove(key, sound);
    }

    private record Key(ClientLevel level, BlockPos pos) {
    }

    private static final class SpeakerLoop extends AbstractTickableSoundInstance {
        private static final float TARGET_VOLUME = 1.55F;
        private static final float START_VOLUME = 0.08F;
        private final Key key;
        private boolean stopRequested;
        private boolean finished;

        private SpeakerLoop(Key key) {
            super(SpeakerModule.LOOP.get(), SoundSource.BLOCKS,
                    RandomSource.create());
            this.key = key;
            this.looping = true;
            this.delay = 0;
            this.volume = START_VOLUME;
            this.pitch = 1.0F;
            this.relative = false;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            this.x = key.pos.getX() + 0.5D;
            this.y = key.pos.getY() + 0.5D;
            this.z = key.pos.getZ() + 0.5D;
        }

        private void requestStop() {
            stopRequested = true;
        }

        private boolean isFinished() {
            return finished;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            Minecraft minecraft = Minecraft.getInstance();
            boolean valid = minecraft.level == key.level
                    && key.level.hasChunkAt(key.pos)
                    && key.level.getBlockState(key.pos)
                    .is(SpeakerModule.BLOCK.get())
                    && key.level.getBlockState(key.pos)
                    .getValue(SpeakerModule.ACTIVE);
            if (!stopRequested && valid) {
                volume = Mth.approach(volume, TARGET_VOLUME, 0.12F);
                return;
            }
            volume = Mth.approach(volume, 0.0F, 0.16F);
            if (volume <= 0.001F) {
                finished = true;
                stop();
                SpeakerAudioClient.finished(key, this);
            }
        }
    }
}
