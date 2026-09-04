package com.bl4ues.scpclassifieddirective.client.scp079;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.Util;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.Supplier;

/** Listener-relative audio heard only by the player currently controlling SCP-079. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class Scp079PlayableAudioClient {
    private static final float STATIC_LOOP_VOLUME = 0.56F;
    private static final float MINIMUM_RUNNING_VOLUME = 0.001F;
    private static final long TRANSITION_FADE_IN_MS = 140L;
    private static final long TRANSITION_DURATION_MS = 420L;
    private static final long TRANSITION_FADE_OUT_MS = 260L;

    private static FeedLoopSound feedStatic;
    private static FeedLoopSound feedTransition;
    private static boolean feedWasActive;
    private static Vec3 previousFeedPosition;
    private static long transitionStartedAt = -1L;

    private Scp079PlayableAudioClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        boolean feedActive = Scp079PlayableClient.cameraMode();
        long now = Util.getMillis();

        if (!feedActive) {
            stop(minecraft, feedStatic);
            stop(minecraft, feedTransition);
            feedStatic = null;
            feedTransition = null;
            feedWasActive = false;
            previousFeedPosition = null;
            transitionStartedAt = -1L;
            return;
        }

        Vec3 position = Scp079PlayableClient.viewPosition();
        if (!feedWasActive || previousFeedPosition == null
                || position.distanceToSqr(previousFeedPosition) > 0.25D) {
            transitionStartedAt = now;
        }
        feedWasActive = true;
        previousFeedPosition = position;

        if (feedStatic == null
                || !minecraft.getSoundManager().isActive(feedStatic)) {
            stop(minecraft, feedStatic);
            feedStatic = new FeedLoopSound("feed_static",
                    () -> STATIC_LOOP_VOLUME);
            minecraft.getSoundManager().play(feedStatic);
        }
        if (feedTransition == null
                || !minecraft.getSoundManager().isActive(feedTransition)) {
            stop(minecraft, feedTransition);
            feedTransition = new FeedLoopSound("feed_transition",
                    Scp079PlayableAudioClient::transitionVolume);
            minecraft.getSoundManager().play(feedTransition);
        }
    }

    public static void playButton() {
        playRaw("079select_1", 1.0F, 1.0F);
    }

    public static void playRoomSwitch() {
        playRaw("079select_2", 1.0F, 1.0F);
        transitionStartedAt = Util.getMillis();
    }

    public static void playLockOrTesla() {
        playRaw("079select_3", 1.0F, 1.0F);
    }

    public static void playDoorToggle() {
        playRaw("079select_4", 1.0F, 1.0F);
    }

    private static void playRaw(String path, float volume, float pitch) {
        if (!Scp079PlayableClient.active()) return;
        Minecraft.getInstance().getSoundManager().play(
                new DirectSound(path, volume, pitch));
    }

    private static float transitionVolume() {
        if (!feedWasActive || transitionStartedAt < 0L) return 0.0F;
        long elapsed = Math.max(0L, Util.getMillis() - transitionStartedAt);
        if (elapsed >= TRANSITION_DURATION_MS) return 0.0F;
        float fadeIn = smootherStep(Mth.clamp(
                elapsed / (float) TRANSITION_FADE_IN_MS, 0.0F, 1.0F));
        long remaining = TRANSITION_DURATION_MS - elapsed;
        float fadeOut = smootherStep(Mth.clamp(
                remaining / (float) TRANSITION_FADE_OUT_MS, 0.0F, 1.0F));
        return Mth.clamp(fadeIn * fadeOut, 0.0F, 1.0F);
    }

    private static float smootherStep(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static void stop(Minecraft minecraft, FeedLoopSound sound) {
        if (sound == null) return;
        sound.finish();
        minecraft.getSoundManager().stop(sound);
    }

    /** Raw, listener-relative OGG used for the four authored SCP-079 UI cues. */
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
            this.pitch = pitch;
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
            this(new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path),
                    volumeSupplier);
        }

        private FeedLoopSound(ResourceLocation id,
                Supplier<Float> volumeSupplier) {
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
            this.pitch = 1.0F;
            this.volume = requestedVolume();
        }

        @Override
        public WeighedSoundEvents resolve(SoundManager manager) {
            this.sound = this.directSound;
            return this.directEvent;
        }

        @Override
        public void tick() {
            if (!Scp079PlayableClient.cameraMode()) {
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
