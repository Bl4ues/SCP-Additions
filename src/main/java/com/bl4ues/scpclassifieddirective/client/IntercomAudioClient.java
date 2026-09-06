package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.facility.intercom.IntercomModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/** Owns the quiet five-second authored Intercom loop while the unit is active. */
public final class IntercomAudioClient {
    private static final Map<Key, IntercomLoop> LOOPS = new HashMap<>();

    private IntercomAudioClient() {
    }

    public static void update(Level level, BlockPos pos, boolean active) {
        if (!(level instanceof ClientLevel client)) return;
        Key key = new Key(client, pos.immutable());
        IntercomLoop current = LOOPS.get(key);
        if (!active) {
            if (current != null) current.stopNow();
            return;
        }
        if (current != null && !current.isStopped()) return;
        IntercomLoop created = new IntercomLoop(key);
        LOOPS.put(key, created);
        Minecraft.getInstance().getSoundManager().play(created);
    }

    private static void finished(Key key, IntercomLoop sound) {
        LOOPS.remove(key, sound);
    }

    private record Key(ClientLevel level, BlockPos pos) {
    }

    private static final class IntercomLoop extends AbstractTickableSoundInstance {
        private final Key key;
        private boolean stopped;

        private IntercomLoop(Key key) {
            super(IntercomModule.LOOP.get(), SoundSource.BLOCKS,
                    RandomSource.create());
            this.key = key;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.24F;
            this.pitch = 1.0F;
            this.relative = false;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            this.x = key.pos.getX() + 0.5D;
            this.y = key.pos.getY() + 0.5D;
            this.z = key.pos.getZ() + 0.5D;
        }

        private boolean isStopped() {
            return stopped;
        }

        private void stopNow() {
            if (stopped) return;
            stopped = true;
            stop();
            IntercomAudioClient.finished(key, this);
        }

        @Override
        public void tick() {
            Minecraft minecraft = Minecraft.getInstance();
            boolean valid = minecraft.level == key.level
                    && key.level.hasChunkAt(key.pos)
                    && key.level.getBlockState(key.pos)
                    .is(IntercomModule.BLOCK.get())
                    && key.level.getBlockState(key.pos)
                    .getValue(IntercomModule.ACTIVE);
            if (!valid) stopNow();
        }
    }
}
