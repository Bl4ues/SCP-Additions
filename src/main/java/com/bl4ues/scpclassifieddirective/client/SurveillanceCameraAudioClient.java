package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.surveillance.SurveillanceCameraPlaceholderModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Positional mechanical loop for moving surveillance-camera heads. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class SurveillanceCameraAudioClient {
    private static final ResourceLocation MOVEMENT = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "camera_movement");
    private static final Map<CameraKey, MovementLoop> LOOPS = new HashMap<>();
    private static final Set<CameraKey> TRACKED = new HashSet<>();

    private SurveillanceCameraAudioClient() {
    }

    public static void observe(
            SurveillanceCameraPlaceholderModule.SurveillanceCameraBlockEntity camera) {
        if (camera == null || camera.getLevel() == null
                || !camera.getLevel().isClientSide) return;
        TRACKED.add(new CameraKey(camera.getLevel().dimension().location(),
                camera.getBlockPos().immutable()));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            stopAll(minecraft);
            return;
        }

        ResourceLocation currentDimension = minecraft.level.dimension().location();
        Iterator<CameraKey> iterator = TRACKED.iterator();
        while (iterator.hasNext()) {
            CameraKey key = iterator.next();
            if (!key.dimension.equals(currentDimension)
                    || !minecraft.level.hasChunkAt(key.pos)) {
                stop(minecraft, key);
                iterator.remove();
                continue;
            }

            BlockEntity blockEntity = minecraft.level.getBlockEntity(key.pos);
            if (!(blockEntity instanceof
                    SurveillanceCameraPlaceholderModule.SurveillanceCameraBlockEntity camera)) {
                stop(minecraft, key);
                iterator.remove();
                continue;
            }

            MovementLoop loop = LOOPS.get(key);
            boolean moving = camera.isVisuallyMoving();
            if (moving) {
                if (loop == null || !minecraft.getSoundManager().isActive(loop)) {
                    if (loop != null) loop.finish();
                    loop = new MovementLoop(key);
                    LOOPS.put(key, loop);
                    minecraft.getSoundManager().play(loop);
                }
            } else if (loop != null) {
                stop(minecraft, key);
            }
        }
    }

    private static void stop(Minecraft minecraft, CameraKey key) {
        MovementLoop loop = LOOPS.remove(key);
        if (loop == null) return;
        loop.finish();
        minecraft.getSoundManager().stop(loop);
    }

    private static void stopAll(Minecraft minecraft) {
        for (MovementLoop loop : LOOPS.values()) {
            loop.finish();
            minecraft.getSoundManager().stop(loop);
        }
        LOOPS.clear();
        TRACKED.clear();
    }

    private record CameraKey(ResourceLocation dimension, BlockPos pos) {
    }

    private static final class MovementLoop extends AbstractTickableSoundInstance {
        private final CameraKey key;
        private final Sound directSound;
        private final WeighedSoundEvents directEvent;

        private MovementLoop(CameraKey key) {
            super(SoundEvent.createVariableRangeEvent(MOVEMENT),
                    SoundSource.BLOCKS, RandomSource.create());
            this.key = key;
            this.directSound = new Sound(MOVEMENT.toString(),
                    ConstantFloat.of(1.0F), ConstantFloat.of(1.0F),
                    1, Sound.Type.FILE, false, false, 16);
            this.directEvent = new WeighedSoundEvents(MOVEMENT, null);
            this.directEvent.addSound(directSound);
            this.sound = directSound;
            this.looping = true;
            this.delay = 0;
            this.relative = false;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            this.volume = 0.72F;
            this.pitch = 1.0F;
            this.x = key.pos.getX() + 0.5D;
            this.y = key.pos.getY() + 0.5D;
            this.z = key.pos.getZ() + 0.5D;
        }

        @Override
        public WeighedSoundEvents resolve(SoundManager manager) {
            this.sound = directSound;
            return directEvent;
        }

        @Override
        public void tick() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null
                    || !minecraft.level.dimension().location().equals(key.dimension)
                    || !minecraft.level.hasChunkAt(key.pos)) {
                stop();
                return;
            }
            BlockEntity blockEntity = minecraft.level.getBlockEntity(key.pos);
            if (!(blockEntity instanceof
                    SurveillanceCameraPlaceholderModule.SurveillanceCameraBlockEntity camera)
                    || !camera.isVisuallyMoving()) {
                stop();
            }
        }

        private void finish() {
            stop();
        }
    }
}
