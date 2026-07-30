package net.mcreator.scpadditions.facility.elevator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Maintains positional idle and travel audio attached to loaded carriages. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class CoreRoomElevatorAudioClient {
    private static final double AUDIO_RADIUS = 48.0D;
    private static final Map<Integer, CarriageSound> CABIN_LOOPS = new HashMap<>();
    private static final Map<Integer, CarriageSound> MOVEMENT_SOUNDS = new HashMap<>();

    private CoreRoomElevatorAudioClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            stopAll();
            return;
        }

        ClientLevel level = minecraft.level;
        AABB search = minecraft.player.getBoundingBox().inflate(AUDIO_RADIUS);
        Set<Integer> present = new HashSet<>();
        for (CoreRoomElevatorCarriageEntity carriage : level.getEntitiesOfClass(
                CoreRoomElevatorCarriageEntity.class, search,
                entity -> entity.isAlive() && !entity.isRemoved())) {
            present.add(carriage.getId());
            ensureCabinLoop(level, carriage);
            if (carriage.phase() == ElevatorFoundation.Phase.MOVING) {
                ensureMovementSound(level, carriage);
            } else {
                stop(MOVEMENT_SOUNDS.remove(carriage.getId()));
            }
        }

        CABIN_LOOPS.entrySet().removeIf(entry -> {
            if (present.contains(entry.getKey()) && !entry.getValue().isFinished()) {
                return false;
            }
            stop(entry.getValue());
            return true;
        });
        MOVEMENT_SOUNDS.entrySet().removeIf(entry -> {
            if (present.contains(entry.getKey()) && !entry.getValue().isFinished()) {
                return false;
            }
            stop(entry.getValue());
            return true;
        });
    }

    private static void ensureCabinLoop(ClientLevel level,
            CoreRoomElevatorCarriageEntity carriage) {
        CarriageSound sound = CABIN_LOOPS.get(carriage.getId());
        if (sound != null && !sound.isFinished()) return;
        sound = new CarriageSound(level, carriage,
                CoreRoomElevatorModule.ELEVATOR_CABIN_LOOP.get(), true, 0.32F);
        CABIN_LOOPS.put(carriage.getId(), sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    private static void ensureMovementSound(ClientLevel level,
            CoreRoomElevatorCarriageEntity carriage) {
        CarriageSound sound = MOVEMENT_SOUNDS.get(carriage.getId());
        if (sound != null && !sound.isFinished()) return;
        sound = new CarriageSound(level, carriage,
                CoreRoomElevatorModule.ELEVATOR_MOVING.get(), false, 0.9F);
        MOVEMENT_SOUNDS.put(carriage.getId(), sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    private static void stopAll() {
        CABIN_LOOPS.values().forEach(CoreRoomElevatorAudioClient::stop);
        MOVEMENT_SOUNDS.values().forEach(CoreRoomElevatorAudioClient::stop);
        CABIN_LOOPS.clear();
        MOVEMENT_SOUNDS.clear();
    }

    private static void stop(CarriageSound sound) {
        if (sound != null) sound.finish();
    }

    private static final class CarriageSound extends AbstractTickableSoundInstance {
        private final ClientLevel level;
        private final CoreRoomElevatorCarriageEntity carriage;
        private final boolean persistentLoop;
        private boolean finished;

        private CarriageSound(ClientLevel level,
                CoreRoomElevatorCarriageEntity carriage, SoundEvent event,
                boolean looping, float volume) {
            super(event, SoundSource.BLOCKS, RandomSource.create());
            this.level = level;
            this.carriage = carriage;
            this.persistentLoop = looping;
            this.looping = looping;
            this.delay = 0;
            this.volume = volume;
            this.pitch = 1.0F;
            this.relative = false;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            updatePosition();
        }

        @Override
        public void tick() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != level || minecraft.player == null
                    || carriage.isRemoved() || !carriage.isAlive()) {
                finish();
                return;
            }
            if (!persistentLoop
                    && carriage.phase() != ElevatorFoundation.Phase.MOVING) {
                finish();
                return;
            }
            updatePosition();
        }

        private void updatePosition() {
            x = carriage.getX();
            y = carriage.getY() + 1.5D;
            z = carriage.getZ();
        }

        private boolean isFinished() {
            return finished;
        }

        private void finish() {
            finished = true;
            stop();
        }
    }
}
