package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079FacilityMapScreen;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableClient;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079SpeakerCueSoundInstance;
import com.bl4ues.scpclassifieddirective.equipment.HazmatSuitAccess;
import com.mojang.blaze3d.audio.Channel;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent;
import net.minecraftforge.client.event.sound.PlayStreamingSourceEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/** Client-side listener perception effects for equipment, SCP states and remote feeds. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class AudioMufflingClient {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final float HAZMAT_STRENGTH = 0.60F;
    private static final float SCP_714_INITIAL_STRENGTH = 0.08F;
    /**
     * A remote surveillance microphone is deliberately narrow and lossy. The
     * low-pass gain also leaves host-monitor audio at roughly half strength.
     */
    private static final float SCP_079_CAMERA_STRENGTH = 0.48F;
    private static final float SCP_079_SPEAKER_STRENGTH = 0.64F;
    private static final float TRANSITION_SPEED = 0.16F;
    private static final float UPDATE_EPSILON = 0.0015F;

    private static final ConcurrentMap<Channel, SoundInstance> SOUNDS_BY_CHANNEL =
            new ConcurrentHashMap<>();
    private static final AtomicBoolean UPDATE_QUEUED = new AtomicBoolean();
    private static final AtomicBoolean UPDATE_DIRTY = new AtomicBoolean();

    private static volatile SoundEngine soundEngine;
    private static SoundEngine filterEngine;
    private static int worldFilter;
    private static int internalFilter;
    private static int speakerFilter;
    private static boolean efxUnavailable;
    private static boolean loggedUnavailable;

    private static float currentHazmatStrength;
    private static float currentScp714Strength;
    private static float currentDeathStrength;
    private static float currentScp079CameraStrength;
    private static float lastQueuedHazmatStrength = -1.0F;
    private static float lastQueuedScp714Strength = -1.0F;
    private static float lastQueuedDeathStrength = -1.0F;
    private static float lastQueuedScp079CameraStrength = -1.0F;
    private static float masterVolume = 1.0F;

    private AudioMufflingClient() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSoundRequested(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (!Scp079PlayableClient.active() || sound == null
                || sound instanceof Scp079SpeakerCueSoundInstance) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof Scp079FacilityMapScreen)) return;
        ResourceLocation id = sound.getLocation();
        if (!ScpClassifiedDirectiveMod.MODID.equals(id.getNamespace())) return;
        if ("scp079_1".equals(id.getPath()) || "scp079_2".equals(id.getPath())) {
            // The map is a remote operating view. SCP-079's own physical CRT
            // click is not part of that monitor unless a camera/speaker hears it.
            event.setSound(null);
        }
    }

    @SubscribeEvent
    public static void onStaticSoundStarted(PlaySoundSourceEvent event) {
        register(event.getEngine(), event.getChannel(), event.getSound());
    }

    @SubscribeEvent
    public static void onStreamingSoundStarted(PlayStreamingSourceEvent event) {
        register(event.getEngine(), event.getChannel(), event.getSound());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        float targetHazmat = 0.0F;
        float targetScp714 = 0.0F;
        float targetDeath = 0.0F;
        boolean remote079View = Scp079PlayableClient.active()
                && (Scp079PlayableClient.cameraMode()
                || minecraft.screen instanceof Scp079FacilityMapScreen);
        float targetScp079Camera = remote079View
                ? SCP_079_CAMERA_STRENGTH : 0.0F;
        masterVolume = minecraft.options.getSoundSourceVolume(SoundSource.MASTER);

        if (minecraft.player != null && minecraft.level != null) {
            if (minecraft.screen instanceof ScpDeathScreen deathScreen) {
                targetDeath = deathScreen.requestedDeathMuffleStrength();
            }
            if (minecraft.player.isAlive()) {
                if (HazmatSuitAccess.isFullyEquipped(minecraft.player)) {
                    targetHazmat = HAZMAT_STRENGTH;
                }
                if (Scp714ClientState.isActive()) {
                    targetScp714 = Mth.lerp(
                            Scp714ClientState.getTargetProgress(),
                            SCP_714_INITIAL_STRENGTH, 1.0F);
                    if (Scp714ClientState.isImmobilized()) {
                        targetScp714 = 1.0F;
                    }
                }
            }
        }

        currentHazmatStrength = approach(currentHazmatStrength, targetHazmat);
        currentScp714Strength = approach(currentScp714Strength, targetScp714);
        currentScp079CameraStrength = approach(currentScp079CameraStrength,
                targetScp079Camera, 0.30F);
        // The death screen owns its deliberate two-second delay and ramp. Apply a
        // slightly faster secondary ease here so spectate/full-death changes blend
        // without adding another visibly long lag on top of that authored curve.
        currentDeathStrength = approach(currentDeathStrength, targetDeath, 0.24F);

        if (Math.abs(currentHazmatStrength - lastQueuedHazmatStrength)
                >= UPDATE_EPSILON
                || Math.abs(currentScp714Strength - lastQueuedScp714Strength)
                >= UPDATE_EPSILON
                || Math.abs(currentDeathStrength - lastQueuedDeathStrength)
                >= UPDATE_EPSILON
                || Math.abs(currentScp079CameraStrength
                        - lastQueuedScp079CameraStrength) >= UPDATE_EPSILON) {
            queueUpdate();
        }
    }

    private static void register(SoundEngine engine, Channel channel,
                                 SoundInstance sound) {
        if (engine == null || channel == null || sound == null) return;
        if (soundEngine != engine) {
            soundEngine = engine;
            SOUNDS_BY_CHANNEL.clear();
            resetFilterHandles(engine);
        }
        SOUNDS_BY_CHANNEL.put(channel, sound);
        queueUpdate();
    }

    private static float approach(float current, float target) {
        return approach(current, target, TRANSITION_SPEED);
    }

    private static float approach(float current, float target, float speed) {
        float next = Mth.lerp(speed, current, target);
        return Math.abs(next - target) < UPDATE_EPSILON ? target : next;
    }

    private static void queueUpdate() {
        UPDATE_DIRTY.set(true);
        SoundEngine engine = soundEngine;
        if (engine == null || !UPDATE_QUEUED.compareAndSet(false, true)) return;
        scheduleUpdate(engine);
    }

    private static void scheduleUpdate(SoundEngine engine) {
        UPDATE_DIRTY.set(false);
        float hazmat = Mth.clamp(currentHazmatStrength, 0.0F, 1.0F);
        float scp714 = Mth.clamp(currentScp714Strength, 0.0F, 1.0F);
        float death = Mth.clamp(currentDeathStrength, 0.0F, 1.0F);
        float scp079Camera = Mth.clamp(currentScp079CameraStrength, 0.0F, 1.0F);
        float requestedMasterVolume = Mth.clamp(masterVolume, 0.0F, 1.0F);
        lastQueuedHazmatStrength = hazmat;
        lastQueuedScp714Strength = scp714;
        lastQueuedDeathStrength = death;
        lastQueuedScp079CameraStrength = scp079Camera;

        engine.channelAccess.executeOnChannels(channels -> {
            try {
                applyToChannels(engine, channels, hazmat, scp714, death,
                        scp079Camera, requestedMasterVolume);
            } finally {
                UPDATE_QUEUED.set(false);
                if (UPDATE_DIRTY.get()) queueUpdate();
            }
        });
    }

    private static void applyToChannels(SoundEngine engine,
                                        Stream<Channel> channelStream,
                                        float hazmat, float scp714,
                                        float death, float scp079Camera,
                                        float requestedMasterVolume) {
        List<Channel> channels = channelStream.toList();
        Set<Channel> activeChannels = Collections.newSetFromMap(
                new IdentityHashMap<>());
        activeChannels.addAll(channels);
        SOUNDS_BY_CHANNEL.keySet().removeIf(channel ->
                !activeChannels.contains(channel));

        float worldStrength = combine(
                combine(combine(hazmat, scp714), death), scp079Camera);
        boolean hasSpeakerCue = SOUNDS_BY_CHANNEL.values().stream()
                .anyMatch(Scp079SpeakerCueSoundInstance.class::isInstance);
        if (worldStrength <= UPDATE_EPSILON && scp714 <= UPDATE_EPSILON
                && !hasSpeakerCue) {
            if (worldFilter != 0 || internalFilter != 0 || speakerFilter != 0) {
                detachFilters(channels);
            }
            engine.listener.setGain(requestedMasterVolume);
            return;
        }

        if (!ensureFilters(engine)) {
            engine.listener.setGain(requestedMasterVolume);
            return;
        }

        engine.listener.setGain(requestedMasterVolume);
        if (!configureFilter(worldFilter, worldStrength)
                || !configureFilter(internalFilter, scp714)
                || !configureFilter(speakerFilter, SCP_079_SPEAKER_STRENGTH)) {
            resetFilterHandles(engine);
            if (!ensureFilters(engine)
                    || !configureFilter(worldFilter, worldStrength)
                    || !configureFilter(internalFilter, scp714)
                    || !configureFilter(speakerFilter,
                    SCP_079_SPEAKER_STRENGTH)) {
                detachFilters(channels);
                engine.listener.setGain(requestedMasterVolume);
                return;
            }
        }

        for (Channel channel : channels) {
            SoundInstance sound = SOUNDS_BY_CHANNEL.get(channel);
            int filter;
            if (sound instanceof Scp079SpeakerCueSoundInstance) {
                filter = speakerFilter;
            } else if (isInternalHazmatSound(sound)) {
                filter = scp714 > UPDATE_EPSILON
                        ? internalFilter : AL10.AL_NONE;
            } else if (isDiegetic(sound)) {
                filter = worldStrength > UPDATE_EPSILON
                        ? worldFilter : AL10.AL_NONE;
            } else {
                // SCP-079's listener-relative select/static/transition sounds use
                // MASTER specifically so the camera microphone filter never muddies
                // its own internal interface feedback. MUSIC is also exempt.
                filter = AL10.AL_NONE;
            }
            AL10.alSourcei(channel.source, EXTEfx.AL_DIRECT_FILTER, filter);
        }
    }

    private static boolean isDiegetic(SoundInstance sound) {
        if (sound == null) return false;
        SoundSource source = sound.getSource();
        return source != SoundSource.MUSIC && source != SoundSource.MASTER;
    }

    private static float combine(float first, float second) {
        return Mth.clamp(1.0F - (1.0F - first) * (1.0F - second),
                0.0F, 1.0F);
    }

    private static boolean configureFilter(int filter, float strength) {
        if (filter == 0) return false;
        clearAlError();
        EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAIN,
                gainForStrength(strength));
        EXTEfx.alFilterf(filter, EXTEfx.AL_LOWPASS_GAINHF,
                highFrequencyGainForStrength(strength));
        return AL10.alGetError() == AL10.AL_NO_ERROR;
    }

    private static float gainForStrength(float strength) {
        if (strength >= 0.999F) return 0.0F;
        return Mth.clamp((float) Math.pow(1.0F - strength, 0.82D),
                0.0F, 1.0F);
    }

    private static float highFrequencyGainForStrength(float strength) {
        if (strength >= 0.999F) return 0.0F;
        return Mth.clamp((float) Math.pow(1.0F - strength, 3.40D),
                0.0F, 1.0F);
    }

    private static boolean ensureFilters(SoundEngine engine) {
        if (filterEngine != engine) resetFilterHandles(engine);
        if (worldFilter != 0 && internalFilter != 0 && speakerFilter != 0) {
            return true;
        }
        if (efxUnavailable) return false;

        long context = ALC10.alcGetCurrentContext();
        long device = context == 0L ? 0L : ALC10.alcGetContextsDevice(context);
        if (context == 0L || device == 0L
                || !ALC10.alcIsExtensionPresent(device, "ALC_EXT_EFX")) {
            markEfxUnavailable();
            return false;
        }

        clearAlError();
        int first = EXTEfx.alGenFilters();
        int second = EXTEfx.alGenFilters();
        int third = EXTEfx.alGenFilters();
        EXTEfx.alFilteri(first, EXTEfx.AL_FILTER_TYPE,
                EXTEfx.AL_FILTER_LOWPASS);
        EXTEfx.alFilteri(second, EXTEfx.AL_FILTER_TYPE,
                EXTEfx.AL_FILTER_LOWPASS);
        EXTEfx.alFilteri(third, EXTEfx.AL_FILTER_TYPE,
                EXTEfx.AL_FILTER_LOWPASS);
        if (AL10.alGetError() != AL10.AL_NO_ERROR) {
            if (first != 0) EXTEfx.alDeleteFilters(first);
            if (second != 0) EXTEfx.alDeleteFilters(second);
            if (third != 0) EXTEfx.alDeleteFilters(third);
            markEfxUnavailable();
            return false;
        }

        worldFilter = first;
        internalFilter = second;
        speakerFilter = third;
        filterEngine = engine;
        return true;
    }

    private static void detachFilters(List<Channel> channels) {
        for (Channel channel : channels) {
            AL10.alSourcei(channel.source, EXTEfx.AL_DIRECT_FILTER,
                    AL10.AL_NONE);
        }
    }

    private static boolean isInternalHazmatSound(SoundInstance sound) {
        if (sound == null) return false;
        ResourceLocation location = sound.getLocation();
        if (!ScpClassifiedDirectiveMod.MODID.equals(location.getNamespace())) return false;
        return switch (location.getPath()) {
            case "hazmat_breathing", "hazmat_equip", "hazmat_remove" -> true;
            default -> false;
        };
    }

    private static void resetFilterHandles(SoundEngine engine) {
        filterEngine = engine;
        worldFilter = 0;
        internalFilter = 0;
        speakerFilter = 0;
        efxUnavailable = false;
    }

    private static void markEfxUnavailable() {
        efxUnavailable = true;
        if (!loggedUnavailable) {
            loggedUnavailable = true;
            LOGGER.warn("OpenAL EFX is unavailable; selective SCP equipment muffling is disabled");
        }
    }

    private static void clearAlError() {
        for (int attempts = 0; attempts < 16
                && AL10.alGetError() != AL10.AL_NO_ERROR; attempts++) {
            // Drain stale OpenAL errors before validating this operation.
        }
    }
}
