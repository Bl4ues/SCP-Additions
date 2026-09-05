package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Short-range mechanical servo loop for moving surveillance-camera heads. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class SurveillanceCameraAudioClient {
    private static final ResourceLocation MOVEMENT = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "camera_movement");

    private static final int POSITIONAL_ATTENUATION_DISTANCE = 6;
    private static final float POSITIONAL_MAX_VOLUME = 0.45F;
    private static final float OPERATOR_MAX_VOLUME = 0.62F;
    private static final float SERVO_PITCH = 0.94F;
    private static final float FADE_IN_PER_TICK = 0.12F;
    private static final float FADE_OUT_PER_TICK = 0.055F;
    private static final int SILENT_HOLD_TICKS = 34;
    private static final float OPERATOR_MOTION_EPSILON = 0.025F;

    private static final Map<CameraKey, MovementLoop> LOOPS = new HashMap<>();
    private static final Map<CameraKey, OperatorAngles> LAST_OPERATOR_ANGLES =
            new HashMap<>();
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
                LAST_OPERATOR_ANGLES.remove(key);
                iterator.remove();
                continue;
            }

            BlockEntity blockEntity = minecraft.level.getBlockEntity(key.pos);
            if (!(blockEntity instanceof
                    SurveillanceCameraPlaceholderModule.SurveillanceCameraBlockEntity camera)) {
                stop(minecraft, key);
                LAST_OPERATOR_ANGLES.remove(key);
                iterator.remove();
                continue;
            }

            boolean operator = isLocalOperator(camera, minecraft);
            boolean moving = operator
                    ? isOperatorMoving(key, minecraft)
                    : camera.isVisuallyMoving();
            if (!operator) LAST_OPERATOR_ANGLES.remove(key);

            MovementLoop loop = LOOPS.get(key);
            if (loop != null && loop.operatorMode() != operator) {
                stop(minecraft, key);
                loop = null;
            }

            if (loop == null) {
                if (!moving) continue;
                loop = new MovementLoop(key, operator);
                LOOPS.put(key, loop);
                minecraft.getSoundManager().play(loop);
            }
            loop.setMoving(moving);

            if (!minecraft.getSoundManager().isActive(loop)
                    && loop.isFinished()) {
                LOOPS.remove(key, loop);
            }
        }
    }

    private static boolean isLocalOperator(
            SurveillanceCameraPlaceholderModule.SurveillanceCameraBlockEntity camera,
            Minecraft minecraft) {
        if (!Scp079PlayableClient.cameraMode() || minecraft.player == null
                || minecraft.level == null || camera.getLevel() != minecraft.level) {
            return false;
        }
        Vec3 baseEye = SurveillanceCameraPlaceholderModule.eyePosition(
                camera.getBlockPos(), camera.getBlockState());
        return Scp079PlayableClient.viewPosition().distanceToSqr(baseEye) <= 0.64D;
    }

    private static boolean isOperatorMoving(CameraKey key, Minecraft minecraft) {
        float yaw = minecraft.player.getYRot();
        float pitch = minecraft.player.getXRot();
        OperatorAngles previous = LAST_OPERATOR_ANGLES.put(key,
                new OperatorAngles(yaw, pitch));
        if (previous == null) return false;
        return Math.abs(Mth.wrapDegrees(yaw - previous.yaw)) > OPERATOR_MOTION_EPSILON
                || Math.abs(pitch - previous.pitch) > OPERATOR_MOTION_EPSILON;
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
        LAST_OPERATOR_ANGLES.clear();
    }

    private record CameraKey(ResourceLocation dimension, BlockPos pos) {
    }

    private record OperatorAngles(float yaw, float pitch) {
    }

    private static final class MovementLoop extends AbstractTickableSoundInstance {
        private final CameraKey key;
        private final boolean operator;
        private final float maxVolume;
        private final Sound directSound;
        private final WeighedSoundEvents directEvent;
        private boolean moving = true;
        private boolean finished;
        private int silentTicks;
        private float gain;

        private MovementLoop(CameraKey key, boolean operator) {
            super(SoundEvent.createVariableRangeEvent(MOVEMENT),
                    operator ? SoundSource.MASTER : SoundSource.BLOCKS,
                    RandomSource.create());
            this.key = key;
            this.operator = operator;
            this.maxVolume = operator
                    ? OPERATOR_MAX_VOLUME : POSITIONAL_MAX_VOLUME;
            this.directSound = new Sound(MOVEMENT.toString(),
                    ConstantFloat.of(1.0F), ConstantFloat.of(1.0F),
                    1, Sound.Type.FILE, false, false,
                    operator ? 1 : POSITIONAL_ATTENUATION_DISTANCE);
            this.directEvent = new WeighedSoundEvents(MOVEMENT, null);
            this.directEvent.addSound(directSound);
            this.sound = directSound;
            this.looping = true;
            this.delay = 0;
            this.relative = operator;
            this.attenuation = operator
                    ? SoundInstance.Attenuation.NONE
                    : SoundInstance.Attenuation.LINEAR;
            this.volume = 0.0F;
            this.pitch = SERVO_PITCH;
            if (operator) {
                this.x = 0.0D;
                this.y = 0.0D;
                this.z = 0.0D;
            } else {
                this.x = key.pos.getX() + 0.5D;
                this.y = key.pos.getY() + 0.5D;
                this.z = key.pos.getZ() + 0.5D;
            }
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
                finish();
                return;
            }
            BlockEntity blockEntity = minecraft.level.getBlockEntity(key.pos);
            if (!(blockEntity instanceof
                    SurveillanceCameraPlaceholderModule.SurveillanceCameraBlockEntity)) {
                finish();
                return;
            }

            if (moving) {
                silentTicks = 0;
                gain = Math.min(1.0F, gain + FADE_IN_PER_TICK);
            } else {
                silentTicks++;
                gain = Math.max(0.0F, gain - FADE_OUT_PER_TICK);
                // Keep a silent source alive through the camera's short idle
                // pause. The next sweep therefore resumes the same waveform
                // instead of restarting the ogg and producing an audible seam.
                if (gain <= 0.0001F && silentTicks > SILENT_HOLD_TICKS) {
                    finish();
                    return;
                }
            }
            this.volume = maxVolume * gain;
        }

        private void setMoving(boolean value) {
            moving = value;
        }

        private boolean operatorMode() {
            return operator;
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
