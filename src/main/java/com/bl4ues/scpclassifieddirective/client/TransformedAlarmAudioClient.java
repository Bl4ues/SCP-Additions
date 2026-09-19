package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.facility.alarm.AlarmModule;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * A transformed Alarm has no ticking vanilla-world BlockEntity. Keep its
 * positional siren keyed by the actual authored grid address, not by the
 * potentially occupied vanilla BlockPos under the visual model.
 */
public final class TransformedAlarmAudioClient {
    private static final double MAX_DISTANCE = 12.0D;
    private static final long CYCLE_TICKS = 40L;
    private static final Map<Key, Loop> LOOPS = new HashMap<>();

    private TransformedAlarmAudioClient() {
    }

    public static void group(ClientLevel level, UUID id,
            int x, int y, int z, Vec3 center, boolean active) {
        update(new Key(level, id, x, y, z, 0, false), center, active);
    }

    public static void surface(ClientLevel level, UUID id,
            int column, int row, int side, boolean overlay,
            Vec3 center, boolean active) {
        update(new Key(level, id, column, row, 0, side, overlay),
                center, active);
    }

    private static void update(Key key, Vec3 center, boolean active) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != key.level() || minecraft.player == null) return;
        Iterator<Map.Entry<Key, Loop>> iterator = LOOPS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Key, Loop> existing = iterator.next();
            if (existing.getValue().finished()) iterator.remove();
        }
        Loop current = LOOPS.get(key);
        double distance = minecraft.player.position().distanceTo(center);
        if (!active || distance >= MAX_DISTANCE) {
            if (current != null) current.finishCycle();
            return;
        }
        if (current != null && !current.finished()) {
            current.refresh(center);
            return;
        }
        Loop loop = new Loop(key.level(), center);
        LOOPS.put(key, loop);
        minecraft.getSoundManager().play(loop);
    }

    private record Key(ClientLevel level, UUID owner, int x, int y, int z,
                       int side, boolean overlay) {
    }

    private static final class Loop extends AbstractTickableSoundInstance {
        private final ClientLevel level;
        private final long started;
        private Vec3 center;
        private long lastSeen;
        private long endAt = Long.MAX_VALUE;
        private boolean endRequested;
        private boolean done;

        private Loop(ClientLevel level, Vec3 center) {
            super(AlarmModule.LOOP.get(), SoundSource.BLOCKS,
                    RandomSource.create());
            this.level = level;
            this.center = center;
            this.started = level.getGameTime();
            this.lastSeen = started;
            this.looping = true;
            this.delay = 0;
            this.relative = false;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.pitch = 1.0F;
            this.volume = 1.0F;
            setPosition(center);
        }

        private void setPosition(Vec3 next) {
            this.x = next.x;
            this.y = next.y;
            this.z = next.z;
        }

        private void refresh(Vec3 next) {
            this.center = next;
            this.lastSeen = level.getGameTime();
            this.endRequested = false;
            this.endAt = Long.MAX_VALUE;
            this.pitch = 1.0F;
            setPosition(next);
        }

        private void finishCycle() {
            if (done || endRequested) return;
            long elapsed = Math.max(0L, level.getGameTime() - started);
            long remaining = CYCLE_TICKS - Math.floorMod(elapsed, CYCLE_TICKS);
            endRequested = true;
            endAt = level.getGameTime() + remaining;
        }

        @Override
        public void tick() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != level || minecraft.player == null) {
                endNow();
                return;
            }
            if (level.getGameTime() - lastSeen > 4L) finishCycle();
            if (endRequested && level.getGameTime() >= endAt) {
                endNow();
                return;
            }
            double distance = minecraft.player.position().distanceTo(center);
            if (distance >= MAX_DISTANCE) {
                endNow();
                return;
            }
            float linear = Mth.clamp(
                    (float) (1.0D - distance / MAX_DISTANCE), 0.0F, 1.0F);
            if (endRequested) {
                float remaining = Mth.clamp((endAt - level.getGameTime())
                        / 8.0F, 0.0F, 1.0F);
                this.volume = linear * remaining;
                this.pitch = Mth.lerp(1.0F - remaining, 1.0F, 0.52F);
            } else {
                this.volume = linear;
                this.pitch = 1.0F;
            }
        }

        private boolean finished() {
            return done;
        }

        private void endNow() {
            if (done) return;
            done = true;
            stop();
        }
    }
}
