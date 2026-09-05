package com.bl4ues.scpclassifieddirective.inventory.client.pda;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModSounds;
import com.bl4ues.scpclassifieddirective.inventory.client.gui.ScpInventoryScreen;
import com.bl4ues.scpclassifieddirective.sound.GameplaySounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
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

/** Owns listener-relative power and interaction audio for the physical PDA. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        value = Dist.CLIENT)
public final class InventoryPdaAudioClient {
    private static final long POWER_BEEP_NANOS = 300_000_000L;
    private static final float LOOP_VOLUME = 0.28F;
    private static final int LOOP_FADE_IN_TICKS = 6;
    private static final int LOOP_FADE_OUT_TICKS = 12;
    private static long loopReadyNanos = -1L;
    private static PdaLoopSound activeLoop;

    private InventoryPdaAudioClient() {
    }

    public static void powerOn() {
        Minecraft minecraft = Minecraft.getInstance();
        if (activeLoop != null) {
            minecraft.getSoundManager().stop(activeLoop);
            activeLoop = null;
        }
        minecraft.getSoundManager().play(new DirectSound("pda_on", 0.72F));
        loopReadyNanos = System.nanoTime() + POWER_BEEP_NANOS;
    }

    public static void beginClose() {
        loopReadyNanos = -1L;
        if (activeLoop != null) activeLoop.beginFadeOut();
    }

    public static void playPickup() {
        if (!GameplaySounds.ITEM_PICKUP.isPresent()) return;
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(
                        GameplaySounds.ITEM_PICKUP.get(), 1.0F, 0.72F));
    }

    public static void playSelect() {
        if (!ScpClassifiedDirectiveModSounds.SELECT.isPresent()) return;
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(
                        ScpClassifiedDirectiveModSounds.SELECT.get(),
                        1.0F, 0.35F));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        boolean powered = minecraft.screen instanceof ScpInventoryScreen screen
                && screen.isPdaPowered() && !screen.isPdaClosing();
        if (!powered) {
            loopReadyNanos = -1L;
            if (activeLoop != null) activeLoop.beginFadeOut();
        } else if (loopReadyNanos > 0L
                && System.nanoTime() >= loopReadyNanos) {
            loopReadyNanos = -1L;
            activeLoop = new PdaLoopSound();
            minecraft.getSoundManager().play(activeLoop);
            return;
        } else if (activeLoop != null
                && !minecraft.getSoundManager().isActive(activeLoop)) {
            activeLoop = null;
        }
    }

    private static class DirectSound extends AbstractSoundInstance {
        protected final Sound directSound;
        private final WeighedSoundEvents directEvent;

        private DirectSound(String path, float volume) {
            this(new ResourceLocation(ScpClassifiedDirectiveMod.MODID, path),
                    volume);
        }

        protected DirectSound(ResourceLocation id, float volume) {
            super(SoundEvent.createVariableRangeEvent(id), SoundSource.MASTER,
                    RandomSource.create());
            directSound = new Sound(id.toString(), ConstantFloat.of(1.0F),
                    ConstantFloat.of(1.0F), 1, Sound.Type.FILE,
                    false, false, 16);
            directEvent = new WeighedSoundEvents(id, null);
            directEvent.addSound(directSound);
            sound = directSound;
            looping = false;
            delay = 0;
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
            this.volume = Mth.clamp(volume, 0.001F, 1.0F);
            pitch = 1.0F;
        }

        @Override
        public WeighedSoundEvents resolve(SoundManager manager) {
            sound = directSound;
            return directEvent;
        }
    }

    private static final class PdaLoopSound
            extends AbstractTickableSoundInstance {
        private static final float MINIMUM_VOLUME = 0.001F;
        private final Sound directSound;
        private final WeighedSoundEvents directEvent;
        private int age;
        private int fadeTicks = -1;
        private float fadeStartVolume;

        private PdaLoopSound() {
            super(SoundEvent.createVariableRangeEvent(new ResourceLocation(
                            ScpClassifiedDirectiveMod.MODID, "pda_loop")),
                    SoundSource.MASTER, RandomSource.create());
            ResourceLocation id = new ResourceLocation(
                    ScpClassifiedDirectiveMod.MODID, "pda_loop");
            directSound = new Sound(id.toString(), ConstantFloat.of(1.0F),
                    ConstantFloat.of(1.0F), 1, Sound.Type.FILE,
                    false, false, 16);
            directEvent = new WeighedSoundEvents(id, null);
            directEvent.addSound(directSound);
            sound = directSound;
            looping = true;
            delay = 0;
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
            volume = MINIMUM_VOLUME;
            pitch = 1.0F;
        }

        @Override
        public WeighedSoundEvents resolve(SoundManager manager) {
            sound = directSound;
            return directEvent;
        }

        @Override
        public void tick() {
            if (fadeTicks >= 0) {
                if (fadeTicks == 0) {
                    volume = 0.0F;
                    stop();
                    return;
                }
                volume = Math.max(MINIMUM_VOLUME,
                        fadeStartVolume * fadeTicks / LOOP_FADE_OUT_TICKS);
                fadeTicks--;
                return;
            }
            age++;
            volume = Math.max(MINIMUM_VOLUME,
                    LOOP_VOLUME * Mth.clamp(
                            age / (float) LOOP_FADE_IN_TICKS, 0.0F, 1.0F));
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        private void beginFadeOut() {
            if (fadeTicks >= 0) return;
            fadeStartVolume = volume;
            fadeTicks = LOOP_FADE_OUT_TICKS;
        }
    }
}
