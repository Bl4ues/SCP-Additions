package com.bl4ues.scpclassifieddirective.client;

import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Library;
import com.mojang.blaze3d.audio.OggAudioStream;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.LoopingAudioStream;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Plays menu music on a Library channel that is intentionally not registered
 * in SoundEngine's ChannelAccess. World handoff calls SoundEngine.stopAll(),
 * which clears managed channels; this channel therefore survives until the
 * explicit world-entry cue stops it.
 */
final class PersistentMenuMusicPlayer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation MUSIC_FILE = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "sounds/main_menu.ogg");
    private static final AtomicBoolean WORK_QUEUED = new AtomicBoolean();
    private static final AtomicInteger ENGINE_GENERATION =
            new AtomicInteger();

    private static volatile boolean requested;
    private static volatile float requestedVolume = 1.0F;
    private static volatile SoundEngine soundEngine;

    /*
     * These fields are created, pumped and destroyed on Minecraft's sound
     * executor. They are volatile only so reload callbacks can invalidate
     * stale references before another task is queued.
     */
    private static volatile Library library;
    private static volatile Channel channel;
    private static volatile AudioStream stream;

    private static boolean loggedFailure;

    private PersistentMenuMusicPlayer() {
    }

    static void onSoundEngineLoad(SoundEngine engine) {
        soundEngine = engine;
        ENGINE_GENERATION.incrementAndGet();

        // SoundEngine reload already destroys every Library channel.
        library = null;
        channel = null;
        stream = null;
        WORK_QUEUED.set(false);

        if (requested) {
            queueWork();
        }
    }

    static void startOrMaintain(Minecraft minecraft) {
        requested = true;
        requestedVolume = (float) minecraft.options.getSoundSourceVolume(
                SoundSource.MUSIC);
        queueWork();
    }

    static void stop() {
        requested = false;

        /*
         * Do not coalesce this task with maintenance. The entry cue must always
         * enqueue an explicit stop before enter.ogg is submitted to the same
         * sound executor, even if a render-tick update is already pending.
         */
        SoundEngine engine = soundEngine;
        if (engine == null) return;

        int generation = ENGINE_GENERATION.get();
        engine.channelAccess.executeOnChannels(ignored -> {
            if (soundEngine == engine
                    && ENGINE_GENERATION.get() == generation
                    && !requested) {
                stopOnSoundThread();
            }
        });
    }

    private static void queueWork() {
        SoundEngine engine = soundEngine;
        if (engine == null || !WORK_QUEUED.compareAndSet(false, true)) {
            return;
        }

        int generation = ENGINE_GENERATION.get();
        engine.channelAccess.executeOnChannels(ignored -> {
            try {
                if (soundEngine != engine
                        || ENGINE_GENERATION.get() != generation) {
                    return;
                }

                if (requested) {
                    maintainOnSoundThread(engine);
                } else {
                    stopOnSoundThread();
                }
            } finally {
                WORK_QUEUED.set(false);
            }
        });
    }

    private static void maintainOnSoundThread(SoundEngine engine) {
        Library activeLibrary = engine.library;
        if (library != activeLibrary) {
            library = activeLibrary;
            channel = null;
            stream = null;
        }

        Channel activeChannel = channel;
        if (activeChannel == null) {
            startOnSoundThread(activeLibrary);
            activeChannel = channel;
        }

        if (activeChannel == null) return;

        try {
            activeChannel.setVolume(requestedVolume);
            activeChannel.updateStream();

            if (!activeChannel.playing()) {
                // Resume the same decoder position after an OpenAL underflow.
                activeChannel.play();
            }
        } catch (RuntimeException exception) {
            releaseOnSoundThread();
            logFailure("Failed to maintain persistent main-menu music",
                    exception);
        }
    }

    private static void startOnSoundThread(Library activeLibrary) {
        InputStream input = null;
        AudioStream newStream = null;
        Channel newChannel = null;

        try {
            input = Minecraft.getInstance().getResourceManager()
                    .open(MUSIC_FILE);
            newStream = new LoopingAudioStream(OggAudioStream::new, input);
            input = null; // LoopingAudioStream owns it now.

            newChannel = activeLibrary.acquireChannel(
                    Library.Pool.STREAMING);
            if (newChannel == null) {
                newStream.close();
                return;
            }

            newChannel.setRelative(true);
            newChannel.disableAttenuation();
            newChannel.setPitch(1.0F);
            newChannel.setVolume(requestedVolume);
            newChannel.attachBufferStream(newStream);
            newChannel.play();

            library = activeLibrary;
            stream = newStream;
            channel = newChannel;
            loggedFailure = false;
        } catch (IOException | RuntimeException exception) {
            if (newChannel != null) {
                try {
                    activeLibrary.releaseChannel(newChannel);
                } catch (RuntimeException ignored) {
                    try {
                        newChannel.destroy();
                    } catch (RuntimeException ignoredAgain) {
                    }
                }
            } else if (newStream != null) {
                closeQuietly(newStream);
            } else if (input != null) {
                closeQuietly(input);
            }

            logFailure("Failed to start persistent main-menu music",
                    exception);
        }
    }

    private static void stopOnSoundThread() {
        releaseOnSoundThread();
    }

    private static void releaseOnSoundThread() {
        Library activeLibrary = library;
        Channel activeChannel = channel;
        AudioStream activeStream = stream;

        library = null;
        channel = null;
        stream = null;

        if (activeChannel != null) {
            try {
                activeChannel.stop();
                if (activeLibrary != null) {
                    activeLibrary.releaseChannel(activeChannel);
                } else {
                    activeChannel.destroy();
                }
                return;
            } catch (RuntimeException ignored) {
                try {
                    activeChannel.destroy();
                } catch (RuntimeException ignoredAgain) {
                    // The sound engine may already have destroyed it on reload.
                }
            }
        }

        if (activeStream != null) {
            closeQuietly(activeStream);
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    private static void logFailure(String message, Exception exception) {
        if (!loggedFailure) {
            loggedFailure = true;
            LOGGER.warn(message, exception);
        }
    }
}
