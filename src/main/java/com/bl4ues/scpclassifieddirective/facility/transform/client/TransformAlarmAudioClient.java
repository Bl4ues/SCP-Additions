package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.alarm.AlarmModule;
import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Continuous client audio for transformed Alarms.
 *
 * The server remains authoritative over AlarmModule.ACTIVE. This class only
 * mirrors AlarmAudioClient's seamless two-second loop at a transformed Vec3;
 * no periodic server playSound packets are used.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TransformAlarmAudioClient {
    private static final double MAX_DISTANCE = 12.0D;
    private static final long LOOP_TICKS = 40L;
    private static final Set<Key> ACTIVE = new HashSet<>();
    private static final Map<Key, AlarmLoop> LOOPS = new HashMap<>();

    private TransformAlarmAudioClient() {
    }

    public static void sync(List<TransformGroup> groups,
            List<ConstructionSurface> surfaces) {
        ACTIVE.clear();
        if (groups != null) {
            for (TransformGroup group : groups) {
                for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                        : group.cells().entrySet()) {
                    updateActive(Key.group(group.id(), entry.getKey()),
                            entry.getValue());
                }
            }
        }
        if (surfaces != null) {
            for (ConstructionSurface surface : surfaces) {
                for (Map.Entry<ConstructionSurface.SurfaceSlot,
                        ConstructionSurface.SurfaceAttachment> entry
                        : surface.attachments().entrySet()) {
                    updateActive(Key.surface(surface.id(), entry.getKey(), 0),
                            entry.getValue().state());
                }
                for (Map.Entry<ConstructionSurface.SurfaceOverlaySlot,
                        ConstructionSurface.SurfaceAttachment> entry
                        : surface.overlays().entrySet()) {
                    updateActive(Key.surface(surface.id(),
                                    entry.getKey().slot(),
                                    entry.getKey().normalSign()),
                            entry.getValue().state());
                }
            }
        }
        LOOPS.entrySet().removeIf(entry -> {
            if (ACTIVE.contains(entry.getKey())) return false;
            entry.getValue().finishCurrentCycle();
            return false;
        });
    }

    public static void clear() {
        ACTIVE.clear();
        for (AlarmLoop loop : LOOPS.values()) loop.finish();
        LOOPS.clear();
    }

    public static void groupCellChanged(UUID groupId,
            TransformGroup.GridPos cell, BlockState state) {
        updateActive(Key.group(groupId, cell), state);
    }

    public static void groupCellRemoved(UUID groupId,
            TransformGroup.GridPos cell) {
        remove(Key.group(groupId, cell));
    }

    public static void surfaceChanged(UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot, int normalSign,
            BlockState state) {
        updateActive(Key.surface(surfaceId, slot, normalSign), state);
    }

    public static void surfaceRemoved(UUID surfaceId,
            ConstructionSurface.SurfaceSlot slot, int normalSign) {
        remove(Key.surface(surfaceId, slot, normalSign));
    }

    private static void updateActive(Key key, BlockState state) {
        if (key == null) return;
        boolean active = state != null && AlarmModule.isController(state)
                && state.hasProperty(AlarmModule.ACTIVE)
                && state.getValue(AlarmModule.ACTIVE)
                && !state.getValue(AlarmModule.SILENT);
        if (active) {
            ACTIVE.add(key);
            AlarmLoop loop = LOOPS.get(key);
            if (loop != null && !loop.isFinished()) {
                loop.cancelPendingFinish();
            }
        } else {
            remove(key);
        }
    }

    private static void remove(Key key) {
        ACTIVE.remove(key);
        AlarmLoop loop = LOOPS.get(key);
        if (loop != null && !loop.isFinished()) loop.finishCurrentCycle();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            clear();
            return;
        }

        LOOPS.entrySet().removeIf(entry -> entry.getValue().isFinished());
        for (Key key : List.copyOf(ACTIVE)) {
            Vec3 position = key.position();
            BlockState state = key.state();
            if (position == null || state == null
                    || !AlarmModule.isController(state)
                    || !state.getValue(AlarmModule.ACTIVE)
                    || state.getValue(AlarmModule.SILENT)) {
                remove(key);
                continue;
            }
            if (minecraft.player.position().distanceTo(position)
                    >= MAX_DISTANCE) {
                AlarmLoop existing = LOOPS.get(key);
                if (existing != null) existing.finish();
                continue;
            }
            AlarmLoop existing = LOOPS.get(key);
            if (existing != null && !existing.isFinished()) {
                existing.cancelPendingFinish();
                continue;
            }
            AlarmLoop loop = new AlarmLoop(level, key);
            LOOPS.put(key, loop);
            minecraft.getSoundManager().play(loop);
        }
    }

    private static final class AlarmLoop
            extends AbstractTickableSoundInstance {
        private final ClientLevel level;
        private final Key key;
        private final long startedAtTick;
        private boolean finished;
        private boolean finishRequested;
        private long fadeStartTick = Long.MAX_VALUE;
        private long finishAtTick = Long.MAX_VALUE;

        private AlarmLoop(ClientLevel level, Key key) {
            super(AlarmModule.LOOP.get(), SoundSource.BLOCKS,
                    RandomSource.create());
            this.level = level;
            this.key = key;
            this.startedAtTick = level.getGameTime();
            this.looping = true;
            this.delay = 0;
            this.pitch = 1.0F;
            this.relative = false;
            this.attenuation = SoundInstance.Attenuation.NONE;
            Vec3 position = key.position();
            if (position != null) setPosition(position);
            this.volume = 1.0F;
        }

        @Override
        public void tick() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != level || minecraft.player == null) {
                finish();
                return;
            }
            Vec3 position = key.position();
            BlockState state = key.state();
            if (position == null || state == null
                    || !AlarmModule.isController(state)) {
                finish();
                return;
            }
            setPosition(position);

            boolean active = state.hasProperty(AlarmModule.ACTIVE)
                    && state.getValue(AlarmModule.ACTIVE)
                && !state.getValue(AlarmModule.SILENT);
            if (!active) finishCurrentCycle();
            else cancelPendingFinish();

            if (finishRequested && level.getGameTime() >= finishAtTick) {
                finish();
                return;
            }

            double distance = minecraft.player.position().distanceTo(position);
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
                                / (float) total, 0.0F, 1.0F);
                float fade = raw * raw * (3.0F - 2.0F * raw);
                this.volume = distanceVolume * (1.0F - fade);
                this.pitch = Mth.lerp(fade, 1.0F, 0.52F);
            } else {
                this.volume = distanceVolume;
                this.pitch = 1.0F;
            }
        }

        private void setPosition(Vec3 position) {
            this.x = position.x;
            this.y = position.y;
            this.z = position.z;
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

    private record Key(UUID groupId, TransformGroup.GridPos groupCell,
            UUID surfaceId, ConstructionSurface.SurfaceSlot surfaceSlot,
            int normalSign) {
        static Key group(UUID id, TransformGroup.GridPos cell) {
            return id == null || cell == null ? null
                    : new Key(id, cell, null, null, 0);
        }

        static Key surface(UUID id, ConstructionSurface.SurfaceSlot slot,
                int normalSign) {
            return id == null || slot == null ? null
                    : new Key(null, null, id, slot,
                            normalSign == 0 ? 0 : (normalSign < 0 ? -1 : 1));
        }

        private BlockState state() {
            if (groupId != null) {
                TransformGroup group =
                        TransformConstructionClientState.group(groupId);
                return group == null ? null : group.cells().get(groupCell);
            }
            ConstructionSurface surface =
                    TransformConstructionClientState.surface(surfaceId);
            if (surface == null) return null;
            ConstructionSurface.SurfaceAttachment attachment =
                    normalSign == 0
                            ? surface.attachments().get(surfaceSlot)
                            : surface.overlay(surfaceSlot, normalSign);
            return attachment == null ? null : attachment.state();
        }

        private Vec3 position() {
            if (groupId != null) {
                TransformGroup group =
                        TransformConstructionClientState.group(groupId);
                return group == null ? null : group.cellCenter(groupCell);
            }
            ConstructionSurface surface =
                    TransformConstructionClientState.surface(surfaceId);
            if (surface == null) return null;
            int side = normalSign == 0
                    ? TransformSurfaceGeometry.MAIN_SIDE : normalSign;
            return TransformSurfaceGeometry.cellCenter(surface, surfaceSlot,
                    side, normalSign != 0);
        }
    }
}
