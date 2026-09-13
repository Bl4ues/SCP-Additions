package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.facility.alarm.AlarmModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Per-block alarm loops. The source file is authored as a seamless two-second
 * loop; distance attenuation is handled explicitly so the siren is genuinely
 * inaudible at twelve blocks instead of inheriting vanilla's wider range.
 */
public final class AlarmAudioClient {
    private static final double MAX_DISTANCE = 12.0D;
    private static final long LOOP_TICKS = 40L;
    private static final Map<Key, AlarmLoop> LOOPS = new HashMap<>();

    private AlarmAudioClient() {
    }

    public static void update(Level level, BlockPos pos, boolean active) {
        if (!(level instanceof ClientLevel client)) return;
        cleanup();

        Key key = new Key(client, pos.immutable());
        Minecraft minecraft = Minecraft.getInstance();
        AlarmLoop existing = LOOPS.get(key);

        if (minecraft.player == null || minecraft.level != client) {
            stop(key);
            return;
        }

        if (!active) {
            if (existing != null && !existing.isFinished()) {
                existing.finishCurrentCycle();
            }
            return;
        }

        double distance = minecraft.player.position().distanceTo(
                net.minecraft.world.phys.Vec3.atCenterOf(pos));
        if (distance >= MAX_DISTANCE) {
            stop(key);
            return;
        }

        if (existing != null && !existing.isFinished()) {
            existing.cancelPendingFinish();
            return;
        }

        AlarmLoop loop = new AlarmLoop(client, pos);
        LOOPS.put(key, loop);
        minecraft.getSoundManager().play(loop);
    }

    private static void stop(Key key) {
        AlarmLoop loop = LOOPS.remove(key);
        if (loop != null) loop.finish();
    }

    private static void cleanup() {
        Iterator<Map.Entry<Key, AlarmLoop>> iterator =
                LOOPS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Key, AlarmLoop> entry = iterator.next();
            if (entry.getValue().isFinished()) iterator.remove();
        }
    }

    private record Key(ClientLevel level, BlockPos pos) {
    }

    private static final class AlarmLoop
            extends AbstractTickableSoundInstance {
        private final ClientLevel level;
        private final BlockPos pos;
        private final long startedAtTick;
        private boolean finished;
        private boolean finishRequested;
        private long fadeStartTick = Long.MAX_VALUE;
        private long finishAtTick = Long.MAX_VALUE;

        private AlarmLoop(ClientLevel level, BlockPos pos) {
            super(AlarmModule.LOOP.get(), SoundSource.BLOCKS,
                    RandomSource.create());
            this.level = level;
            this.pos = pos.immutable();
            this.startedAtTick = level.getGameTime();
            this.looping = true;
            this.delay = 0;
            this.pitch = 1.0F;
            this.relative = false;

            // Keep 3D positioning but avoid a second hidden falloff curve.
            // Volume below is the exact linear 0..12-block attenuation.
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.x = pos.getX() + 0.5D;
            this.y = pos.getY() + 0.5D;
            this.z = pos.getZ() + 0.5D;
            this.volume = 1.0F;
        }

        @Override
        public void tick() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != level || minecraft.player == null
                    || !level.hasChunkAt(pos)) {
                finish();
                return;
            }

            BlockState state = level.getBlockState(pos);
            if (state.getBlock() != AlarmModule.BLOCK.get()) {
                finish();
                return;
            }

            boolean active = state.getValue(AlarmModule.ACTIVE);
            if (!active) {
                finishCurrentCycle();
            } else {
                cancelPendingFinish();
            }

            if (finishRequested
                    && level.getGameTime() >= finishAtTick) {
                finish();
                return;
            }

            double distance = minecraft.player.position().distanceTo(
                    net.minecraft.world.phys.Vec3.atCenterOf(pos));
            if (distance >= MAX_DISTANCE) {
                finish();
                return;
            }

            float distanceVolume = Mth.clamp(
                    (float) (1.0D - distance / MAX_DISTANCE),
                    0.001F, 1.0F);

            if (finishRequested) {
                long total = Math.max(1L, finishAtTick - fadeStartTick);
                float raw = Mth.clamp(
                        (level.getGameTime() - fadeStartTick)
                                / (float) total,
                        0.0F, 1.0F);
                // Smoothstep avoids the audible corner of a linear envelope.
                float fade = raw * raw * (3.0F - 2.0F * raw);
                this.volume = distanceVolume * (1.0F - fade);
                this.pitch = Mth.lerp(fade, 1.0F, 0.52F);
            } else {
                this.volume = distanceVolume;
                this.pitch = 1.0F;
            }
        }

        private void finishCurrentCycle() {
            if (finished || finishRequested) return;
            long elapsed = Math.max(0L,
                    level.getGameTime() - startedAtTick);
            long phase = Math.floorMod(elapsed, LOOP_TICKS);
            long remaining = LOOP_TICKS - phase;
            finishRequested = true;
            fadeStartTick = level.getGameTime();
            finishAtTick = level.getGameTime() + remaining;
        }

        private void cancelPendingFinish() {
            finishRequested = false;
            fadeStartTick = Long.MAX_VALUE;
            finishAtTick = Long.MAX_VALUE;
            pitch = 1.0F;
        }

        private boolean isFinished() {
            return finished;
        }

        private void finish() {
            if (finished) return;
            finished = true;
            stop();
        }
    }
}
