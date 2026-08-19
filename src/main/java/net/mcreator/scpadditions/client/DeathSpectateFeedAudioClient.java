package net.mcreator.scpadditions.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
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
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.function.Supplier;

/** Local-only ambience for the death-screen live personnel feed. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class DeathSpectateFeedAudioClient {
    /** -60 dB: effectively inaudible, but non-zero so OpenAL keeps the source. */
    private static final float MINIMUM_RUNNING_VOLUME = 0.001F;
    private static final long TRANSITION_FADE_IN_MS = 140L;
    // Mirrors the existing CONNECTION LOST presentation in MineZeroSpectateClient.
    private static final long LOST_CONNECTION_MS = 1250L;
    private static final long LOST_FADE_OUT_MS = 320L;

    private static FeedLoopSound feedStatic;
    private static FeedLoopSound feedTransition;
    private static boolean feedWasOpen;
    private static boolean connectionWasLost;
    private static String previousTargetName = "";
    private static long transitionStartedAt = -1L;
    private static long connectionLostAt = -1L;

    private DeathSpectateFeedAudioClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        boolean feedOpen = minecraft.screen instanceof ScpDeathScreen
                && MineZeroSpectateClient.active();
        long now = Util.getMillis();

        if (!feedOpen) {
            stop(minecraft, feedStatic);
            stop(minecraft, feedTransition);
            feedStatic = null;
            feedTransition = null;
            feedWasOpen = false;
            connectionWasLost = false;
            previousTargetName = "";
            transitionStartedAt = -1L;
            connectionLostAt = -1L;
            return;
        }

        String targetName = MineZeroSpectateClient.targetName();
        boolean connectionLost = MineZeroSpectateClient.connectionLost();
        if (!feedWasOpen) {
            transitionStartedAt = now;
        } else if (!connectionLost
                && "Acquiring personnel feed...".equals(targetName)
                && !targetName.equals(previousTargetName)) {
            // cycle() enters this state before the server supplies the next target.
            transitionStartedAt = now;
        }
        if (connectionLost && !connectionWasLost) {
            connectionLostAt = now;
            transitionStartedAt = now;
        }

        feedWasOpen = true;
        connectionWasLost = connectionLost;
        previousTargetName = targetName;

        if (feedStatic == null
                || !minecraft.getSoundManager().isActive(feedStatic)) {
            stop(minecraft, feedStatic);
            feedStatic = new FeedLoopSound("feed_static", () -> 1.0F);
            minecraft.getSoundManager().play(feedStatic);
        }

        if (feedTransition == null
                || !minecraft.getSoundManager().isActive(feedTransition)) {
            stop(minecraft, feedTransition);
            feedTransition = new FeedLoopSound("feed_transition",
                    DeathSpectateFeedAudioClient::transitionVolume);
            minecraft.getSoundManager().play(feedTransition);
        }
    }

    private static float transitionVolume() {
        if (!feedWasOpen || transitionStartedAt < 0L) return 0.0F;
        long now = Util.getMillis();
        float fadeIn = smootherStep(Mth.clamp(
                (now - transitionStartedAt) / (float) TRANSITION_FADE_IN_MS,
                0.0F, 1.0F));

        float fadeOut;
        if (connectionWasLost && connectionLostAt >= 0L) {
            long elapsed = Math.max(0L, now - connectionLostAt);
            long remaining = Math.max(0L, LOST_CONNECTION_MS - elapsed);
            fadeOut = smootherStep(Mth.clamp(
                    remaining / (float) LOST_FADE_OUT_MS, 0.0F, 1.0F));
        } else {
            // The visual hand-off already contains its authored hold and fade-out.
            fadeOut = MineZeroSpectateClient.switchInterference();
        }
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

    /**
     * A looping, listener-relative raw OGG. Resolving the file directly keeps
     * these two UI-only sounds local to the watching client and avoids turning
     * them into positional world sound events.
     */
    private static final class FeedLoopSound extends AbstractTickableSoundInstance {
        private final Sound directSound;
        private final WeighedSoundEvents directEvent;
        private final Supplier<Float> volumeSupplier;

        private FeedLoopSound(String path, Supplier<Float> volumeSupplier) {
            this(new ResourceLocation(ScpAdditionsMod.MODID, path), volumeSupplier);
        }

        private FeedLoopSound(ResourceLocation id, Supplier<Float> volumeSupplier) {
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
            Minecraft minecraft = Minecraft.getInstance();
            if (!(minecraft.screen instanceof ScpDeathScreen)
                    || !MineZeroSpectateClient.active()) {
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
