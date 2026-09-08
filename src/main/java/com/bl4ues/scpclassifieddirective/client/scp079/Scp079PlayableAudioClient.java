package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.Supplier;

/** Listener-relative audio heard only by the player currently controlling SCP-079. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class Scp079PlayableAudioClient {
    private static final float CAMERA_STATIC_VOLUME = 0.60F;
    private static final float MAP_STATIC_VOLUME = 0.46F;
    private static final float BOOT_STATIC_VOLUME = 0.31F;
    private static final float LOCAL_STATIC_VOLUME = 0.22F;
    private static final float MINIMUM_RUNNING_VOLUME = 0.001F;

    private static FeedLoopSound feedStatic;
    private static FeedLoopSound feedTransition;
    private static FeedLoopSound failureBeep;

    private Scp079PlayableAudioClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        boolean active = Scp079PlayableClient.active();
        boolean signalEffect = Scp079CameraEffectsClient.signalEffectActive();

        if (!active && !signalEffect) {
            stop(minecraft, feedStatic);
            stop(minecraft, feedTransition);
            stop(minecraft, failureBeep);
            feedStatic = null;
            feedTransition = null;
            failureBeep = null;
            return;
        }

        if (active) {
            if (feedStatic == null
                    || !minecraft.getSoundManager().isActive(feedStatic)) {
                stop(minecraft, feedStatic);
                feedStatic = new FeedLoopSound("feed_static",
                        Scp079PlayableAudioClient::staticVolume);
                minecraft.getSoundManager().play(feedStatic);
            }
        } else {
            stop(minecraft, feedStatic);
            feedStatic = null;
        }

        if (feedTransition == null
                || !minecraft.getSoundManager().isActive(feedTransition)) {
            stop(minecraft, feedTransition);
            feedTransition = new FeedLoopSound("feed_transition",
                    Scp079CameraEffectsClient::transitionEnvelope);
            minecraft.getSoundManager().play(feedTransition);
        }

        if (Scp079CameraEffectsClient.hostFailureActive()) {
            if (failureBeep == null
                    || !minecraft.getSoundManager().isActive(failureBeep)) {
                stop(minecraft, failureBeep);
                // 079select_1 is a short electronic tone already shipped by
                // the mod. Looping it gives the destroyed-host transition its
                // continuous terminal warning without inventing a new asset.
                failureBeep = new FeedLoopSound("079select_1", () -> 0.34F,
                        0.72F);
                minecraft.getSoundManager().play(failureBeep);
            }
        } else {
            stop(minecraft, failureBeep);
            failureBeep = null;
        }
    }

    public static void playButton() {
        playRaw("079select_1", 1.0F, 1.0F);
    }

    public static void playRoomSwitch() {
        playRaw("079select_2", 1.0F, 1.0F);
    }

    /** Lower, quieter room-selection cue for map/local-host display changes. */
    public static void playDisplaySwitch() {
        playRaw("079select_2", 0.70F, 0.50F);
    }

    public static void playLockOrTesla() {
        playRaw("079select_3", 1.0F, 1.0F);
    }

    public static void playDoorToggle() {
        playRaw("079select_4", 1.0F, 1.0F);
    }

    public static void playBootStart() {
        playRaw("terminalon", 0.58F, 0.82F);
    }

    public static void playBootKey() {
        playRaw("079select_1", 0.24F, 1.34F);
    }

    public static void playBootStatus() {
        playRaw("079select_2", 0.30F, 1.08F);
    }

    public static void playBootSuccess() {
        playRaw("079select_4", 0.36F, 1.18F);
    }

    public static void playBootError() {
        playRaw("079select_3", 0.48F, 0.72F);
    }

    public static void playBootComplete() {
        playRaw("079select_4", 0.58F, 0.88F);
    }

    private static float staticVolume() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof Scp079BootSequenceScreen) {
            return BOOT_STATIC_VOLUME;
        }
        if (minecraft.screen instanceof Scp079FacilityMapScreen
                || minecraft.screen instanceof Scp079LeaveRoleScreen) {
            return MAP_STATIC_VOLUME;
        }
        return Scp079PlayableClient.cameraMode()
                ? CAMERA_STATIC_VOLUME : LOCAL_STATIC_VOLUME;
    }

    private static void playRaw(String path, float volume, float pitch) {
        if (!Scp079PlayableClient.active()) return;
        Minecraft.getInstance().getSoundManager().play(
                new DirectSound(path, volume, pitch));
    }

    private static void stop(Minecraft minecraft, FeedLoopSound sound) {
        if (sound == null) return;
        sound.finish();
        minecraft.getSoundManager().stop(sound);
    }

    private static final class DirectSound extends AbstractSoundInstance {
        private final Sound directSound;
        private final WeighedSoundEvents directEvent;

        private DirectSound(String path, float volume, float pitch) {
            this(new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path),
                    volume, pitch);
        }

        private DirectSound(ResourceLocation id, float volume, float pitch) {
            super(SoundEvent.createVariableRangeEvent(id), SoundSource.MASTER,
                    RandomSource.create());
            this.directSound = new Sound(id.toString(),
                    ConstantFloat.of(1.0F), ConstantFloat.of(1.0F),
                    1, Sound.Type.FILE, false, false, 16);
            this.directEvent = new WeighedSoundEvents(id, null);
            this.directEvent.addSound(this.directSound);
            this.sound = this.directSound;
            this.looping = false;
            this.delay = 0;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = Mth.clamp(volume, 0.0F, 1.0F);
            this.pitch = Mth.clamp(pitch, 0.05F, 2.0F);
        }

        @Override
        public WeighedSoundEvents resolve(SoundManager manager) {
            this.sound = this.directSound;
            return this.directEvent;
        }
    }

    /** Same direct-loop structure used by the Live Personnel Feed. */
    private static final class FeedLoopSound extends AbstractTickableSoundInstance {
        private final Sound directSound;
        private final WeighedSoundEvents directEvent;
        private final Supplier<Float> volumeSupplier;

        private FeedLoopSound(String path, Supplier<Float> volumeSupplier) {
            this(path, volumeSupplier, 1.0F);
        }

        private FeedLoopSound(String path, Supplier<Float> volumeSupplier,
                float pitch) {
            this(new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path),
                    volumeSupplier, pitch);
        }

        private FeedLoopSound(ResourceLocation id,
                Supplier<Float> volumeSupplier, float pitch) {
            super(SoundEvent.createVariableRangeEvent(id), SoundSource.MASTER,
                    RandomSource.create());
            this.volumeSupplier = volumeSupplier;
            this.directSound = new Sound(id.toString(),
                    ConstantFloat.of(1.0F), ConstantFloat.of(1.0F),
                    1, Sound.Type.FILE, false, false, 16);
            this.directEvent = new WeighedSoundEvents(id, null);
            this.directEvent.addSound(this.directSound);
            this.sound = this.directSound;
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.pitch = Mth.clamp(pitch, 0.05F, 2.0F);
            this.volume = requestedVolume();
        }

        @Override
        public WeighedSoundEvents resolve(SoundManager manager) {
            this.sound = this.directSound;
            return this.directEvent;
        }

        @Override
        public void tick() {
            boolean audioContext = Scp079PlayableClient.active()
                    || Scp079CameraEffectsClient.signalEffectActive();
            if (!audioContext) {
                stop();
                return;
            }
            this.volume = requestedVolume();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        private float requestedVolume() {
            Float requested = volumeSupplier.get();
            float value = requested == null ? 0.0F : requested;
            return Math.max(MINIMUM_RUNNING_VOLUME,
                    Mth.clamp(value, 0.0F, 1.0F));
        }

        private void finish() {
            stop();
        }
    }
}
