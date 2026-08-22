package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import com.bl4ues.scpclassifieddirective.scp1576.Scp1576Module;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Positional authored audio for winding and active SCP-1576 sessions. */
public final class Scp1576ClientAudio {
    private static final Map<UUID, WindingSound> WINDING = new ConcurrentHashMap<>();
    private static final Map<UUID, SignalSound> SIGNALS = new ConcurrentHashMap<>();

    private Scp1576ClientAudio() {
    }

    public static void startWind(UUID sessionId, UUID hostId,
            ResourceLocation dimension, double x, double y, double z) {
        Minecraft minecraft = Minecraft.getInstance();
        WindingSound previous = WINDING.remove(sessionId);
        if (previous != null) previous.stopImmediately();
        WindingSound sound = new WindingSound(sessionId, hostId, dimension,
                x, y, z);
        WINDING.put(sessionId, sound);
        minecraft.getSoundManager().play(sound);
    }

    public static void cancelWind(UUID sessionId) {
        WindingSound sound = WINDING.get(sessionId);
        if (sound != null) sound.beginFadeOut();
    }

    /** Completed winding may leave the authored mechanical tail intact. */
    public static void finishWind(UUID sessionId) {
        WINDING.remove(sessionId);
    }

    public static void startSignal(UUID sessionId) {
        if (SIGNALS.containsKey(sessionId)) return;
        SignalSound sound = new SignalSound(sessionId);
        SIGNALS.put(sessionId, sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    /** The signal's authored tail is allowed to announce the end naturally. */
    public static void endSignal(UUID sessionId) {
        SignalSound sound = SIGNALS.get(sessionId);
        if (sound != null) sound.detachFromLiveState();
    }

    public static void clear() {
        WINDING.values().forEach(WindingSound::stopImmediately);
        SIGNALS.values().forEach(SignalSound::stopImmediately);
        WINDING.clear();
        SIGNALS.clear();
    }

    private static final class WindingSound extends AbstractTickableSoundInstance {
        private static final int FADE_TICKS = 6;
        private final UUID sessionId;
        private final UUID hostId;
        private final ResourceLocation dimension;
        private int fadeRemaining = -1;
        private float fadeStart = 1.0F;

        private WindingSound(UUID sessionId, UUID hostId,
                ResourceLocation dimension, double x, double y, double z) {
            super(Scp1576Module.WIND.get(), SoundSource.PLAYERS,
                    RandomSource.create());
            this.sessionId = sessionId;
            this.hostId = hostId;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.volume = 1.0F;
            this.pitch = 1.0F;
            this.looping = false;
            this.delay = 0;
            this.relative = false;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
        }

        @Override
        public void tick() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null
                    && minecraft.level.dimension().location().equals(dimension)) {
                Player player = minecraft.level.getPlayerByUUID(hostId);
                if (player != null) {
                    x = player.getX();
                    y = player.getY() + player.getBbHeight() * 0.55D;
                    z = player.getZ();
                }
            }

            if (fadeRemaining >= 0) {
                if (fadeRemaining == 0) {
                    stopImmediately();
                    return;
                }
                volume = fadeStart * (fadeRemaining / (float) FADE_TICKS);
                fadeRemaining--;
            }
        }

        private void beginFadeOut() {
            if (fadeRemaining < 0) {
                fadeStart = volume;
                fadeRemaining = FADE_TICKS;
            }
        }

        private void stopImmediately() {
            stop();
            WINDING.remove(sessionId, this);
        }
    }

    private static final class SignalSound extends AbstractTickableSoundInstance {
        private final UUID sessionId;
        private boolean detached;
        private ResourceLocation lastDimension;

        private SignalSound(UUID sessionId) {
            super(Scp1576Module.SPEAK.get(), SoundSource.PLAYERS,
                    RandomSource.create());
            this.sessionId = sessionId;
            this.volume = 0.01F;
            this.pitch = 1.0F;
            this.looping = false;
            this.delay = 0;
            this.relative = false;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            updateFromState();
        }

        @Override
        public void tick() {
            if (!detached) updateFromState();
            Minecraft minecraft = Minecraft.getInstance();
            boolean sameDimension = minecraft.level != null
                    && lastDimension != null
                    && minecraft.level.dimension().location().equals(lastDimension);
            volume = sameDimension ? 1.0F : 0.0F;
        }

        private void updateFromState() {
            Scp1576ClientState.SessionState state =
                    Scp1576ClientState.get(sessionId);
            if (state == null) return;
            lastDimension = state.dimension();
            x = state.x();
            y = state.y();
            z = state.z();
        }

        private void detachFromLiveState() {
            updateFromState();
            detached = true;
            SIGNALS.remove(sessionId, this);
        }

        private void stopImmediately() {
            stop();
            SIGNALS.remove(sessionId, this);
        }
    }
}
