package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ClientItemInteractionSounds;
import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.PlayerVoiceSounds;
import net.mcreator.scpadditions.init.ScpAdditionsModSounds;

import java.util.UUID;

/** Client-only replacement and filtering for player combat audio. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class PlayerDamageAudioClient {
    private static final String PLAYER_HURT_PREFIX = "entity.player.hurt";
    private static final String PLAYER_DEATH_PREFIX = "entity.player.death";
    private static final String PLAYER_ATTACK_PREFIX = "entity.player.attack.";
    private static final String GENERIC_HURT_SOUND = "entity.generic.hurt";
    private static final double LOCAL_PLAYER_SOUND_RADIUS_SQ = 9.0D;
    private static final double TRACKED_MOB_SOUND_RADIUS_SQ = 9.0D;
    private static final long MOB_IMPACT_WINDOW_NANOS = 500_000_000L;
    private static final float HEALTH_EPSILON = 1.0E-3F;
    private static final float GASP_AIR_THRESHOLD = 0.30F;

    private static volatile long suppressMobImpactUntilNanos = Long.MIN_VALUE;
    private static volatile int suppressMobImpactTargetId = -1;
    private static UUID trackedPlayerId;
    private static int previousHurtTime;
    private static float previousEffectiveHealth = Float.NaN;
    private static int previousAirSupply = -1;
    private static int lowestAirSupply = Integer.MAX_VALUE;
    private static boolean wasUnderWater;
    private static boolean recoveryGaspArmed;
    private static boolean awaitingRecoveryGasp;
    private static DrowningLoopSound drowningLoop;

    private PlayerDamageAudioClient() {
    }

    /**
     * Mark the local mob attack before Minecraft creates its attack sound.
     * This listener receives canceled attacks as well so an attack rejected by
     * the held-weapon module cannot still produce the vanilla impact click.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onInteractionKeyMapping(
            InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.hitResult instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity)
                || hit.getEntity() instanceof Player) {
            return;
        }

        suppressMobImpactUntilNanos = System.nanoTime()
                + MOB_IMPACT_WINDOW_NANOS;
        suppressMobImpactTargetId = hit.getEntity().getId();
    }

    /** Server/integrated-server fallback for attacks not exposed by input. */
    @SubscribeEvent(receiveCanceled = true)
    public static void onAttackEntity(AttackEntityEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !event.getEntity().getUUID().equals(
                minecraft.player.getUUID())) {
            return;
        }
        if (event.getTarget() instanceof LivingEntity
                && !(event.getTarget() instanceof Player)) {
            suppressMobImpactUntilNanos = System.nanoTime()
                    + MOB_IMPACT_WINDOW_NANOS;
            suppressMobImpactTargetId = event.getTarget().getId();
        }
    }

    /**
     * The vanilla hurt sound is filtered independently from the replacement.
     * Replacing the SoundInstance inside PlaySoundEvent proved unreliable for
     * this stereo local sound, so a new damage pulse is detected from the
     * synchronized local player state and played directly through SoundManager.
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (minecraft.level == null || player == null) {
            resetDamageTracking();
            return;
        }

        UUID playerId = player.getUUID();
        int hurtTime = player.hurtTime;
        float effectiveHealth = player.getHealth() + player.getAbsorptionAmount();

        if (!playerId.equals(trackedPlayerId)
                || Float.isNaN(previousEffectiveHealth)) {
            fadeOutDrowningLoop();
            trackedPlayerId = playerId;
            previousHurtTime = hurtTime;
            previousEffectiveHealth = effectiveHealth;
            previousAirSupply = player.getAirSupply();
            lowestAirSupply = previousAirSupply;
            wasUnderWater = player.isUnderWater();
            recoveryGaspArmed = false;
            awaitingRecoveryGasp = false;
            return;
        }

        boolean newHurtPulse = player.isAlive()
                && (hurtTime > previousHurtTime
                || (hurtTime > 0
                && effectiveHealth + HEALTH_EPSILON
                < previousEffectiveHealth));
        boolean customVoices = InventoryModuleRuntimeState
                .replacePlayerHurtSoundsForClient();
        boolean drowningDamage = newHurtPulse && isDrowningDamage(player);

        if (customVoices && drowningDamage) {
            ensureDrowningLoop();
        } else if (customVoices && newHurtPulse) {
            playLocalHurt();
        }

        if (!customVoices || !player.isAlive()
                || !player.isUnderWater()
                || player.getAirSupply() > 0) {
            fadeOutDrowningLoop();
        }
        clearFinishedDrowningLoop();

        updateRecoveryGasp(player);
        previousHurtTime = hurtTime;
        previousEffectiveHealth = effectiveHealth;
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance original = event.getOriginalSound();
        if (original == null) return;

        if (ClientItemInteractionSounds.shouldSuppressVanilla(original)) {
            event.setSound(null);
            return;
        }

        ResourceLocation location = original.getLocation();
        if (location == null) return;

        boolean minecraftSound = "minecraft".equals(location.getNamespace());
        boolean localPlayerSound = isLocalPlayerSound(original);
        String path = location.getPath();

        if (InventoryModuleRuntimeState.replacePlayerHurtSoundsForClient()
                && minecraftSound && localPlayerSound) {
            if (path.startsWith(PLAYER_HURT_PREFIX)
                    || path.startsWith(PLAYER_DEATH_PREFIX)) {
                event.setSound(null);
                return;
            }
        }

        /*
         * Attack sounds are commonly positioned at the struck entity rather
         * than at the attacker, so the local-player distance test is invalid
         * here. The remembered input window and current crosshair target cover
         * both successful attacks and attacks canceled before a packet is sent.
         *
         * Custom mobs that inherit LivingEntity's generic hurt fallback (such
         * as SCP-106) also emit entity.generic.hurt in addition to the player's
         * attack cue. Suppress that fallback only when it is spatially tied to
         * the exact mob marked by the local attack window.
         */
        if (InventoryModuleRuntimeState.muteNonPlayerHitSoundsForClient()
                && minecraftSound) {
            boolean mobImpact = isNonPlayerMobImpact()
                    || isCurrentNonPlayerMobTarget();
            if (path.startsWith(PLAYER_ATTACK_PREFIX) && mobImpact) {
                event.setSound(null);
                return;
            }
            if (GENERIC_HURT_SOUND.equals(path)
                    && isTrackedMobImpactSound(original)) {
                event.setSound(null);
            }
        }
    }

    /** Plays a clean sample of the currently selected hurt voice. */
    public static void previewSelectedVoice() {
        playRelative(selectedHurtSound(), 1.0F, 1.0F);
    }

    private static void playLocalHurt() {
        RandomSource random = RandomSource.create();
        float pitch = 0.96F + random.nextFloat() * 0.08F;
        playRelative(selectedHurtSound(), 1.0F, pitch, random);
    }

    private static void ensureDrowningLoop() {
        clearFinishedDrowningLoop();
        if (drowningLoop == null) {
            drowningLoop = new DrowningLoopSound();
            Minecraft.getInstance().getSoundManager().play(drowningLoop);
        } else {
            drowningLoop.resume();
        }
    }

    private static void fadeOutDrowningLoop() {
        if (drowningLoop != null) drowningLoop.beginFadeOut();
    }

    private static void clearFinishedDrowningLoop() {
        if (drowningLoop != null && drowningLoop.isFinished()) {
            drowningLoop = null;
        }
    }

    private static void updateRecoveryGasp(Player player) {
        int maximumAir = Math.max(1, player.getMaxAirSupply());
        int currentAir = Math.max(0, player.getAirSupply());
        int threshold = Math.round(maximumAir * GASP_AIR_THRESHOLD);
        boolean underWater = player.isUnderWater();

        if (underWater) {
            if (!wasUnderWater) {
                lowestAirSupply = currentAir;
                recoveryGaspArmed = false;
                awaitingRecoveryGasp = false;
            }
            lowestAirSupply = Math.min(lowestAirSupply, currentAir);
            if (lowestAirSupply <= threshold) {
                recoveryGaspArmed = true;
            }
        } else {
            if (wasUnderWater && recoveryGaspArmed) {
                awaitingRecoveryGasp = true;
            }

            boolean recovering = previousAirSupply >= 0
                    && currentAir > previousAirSupply;
            if (awaitingRecoveryGasp && recovering) {
                if (InventoryModuleRuntimeState
                        .replacePlayerHurtSoundsForClient()) {
                    playRelative(selectedGaspSound(), 1.0F, 1.0F);
                }
                awaitingRecoveryGasp = false;
                recoveryGaspArmed = false;
                lowestAirSupply = maximumAir;
            } else if (currentAir >= maximumAir
                    && !awaitingRecoveryGasp) {
                recoveryGaspArmed = false;
                lowestAirSupply = maximumAir;
            }
        }

        wasUnderWater = underWater;
        previousAirSupply = currentAir;
    }

    private static SoundEvent selectedHurtSound() {
        return InventoryModuleRuntimeState.useVoiceProfileBForClient()
                ? PlayerVoiceSounds.VOICE_PROFILE_B_HURT.get()
                : ScpAdditionsModSounds.PLAYER_HURT.get();
    }

    private static SoundEvent selectedGaspSound() {
        return InventoryModuleRuntimeState.useVoiceProfileBForClient()
                ? PlayerVoiceSounds.VOICE_PROFILE_B_GASP.get()
                : PlayerVoiceSounds.VOICE_PROFILE_A_GASP.get();
    }

    private static void playRelative(SoundEvent sound, float volume,
            float pitch) {
        playRelative(sound, volume, pitch, RandomSource.create());
    }

    private static void playRelative(SoundEvent sound, float volume,
            float pitch, RandomSource random) {
        Minecraft.getInstance().getSoundManager().play(
                new SimpleSoundInstance(sound.getLocation(),
                        SoundSource.PLAYERS, volume, pitch, random,
                        false, 0, SoundInstance.Attenuation.NONE,
                        0.0D, 0.0D, 0.0D, true));
    }

    private static boolean isDrowningDamage(Player player) {
        DamageSource source = player.getLastDamageSource();
        return source != null && source.is(DamageTypes.DROWN)
                || player.isUnderWater() && player.getAirSupply() <= 0;
    }

    private static void resetDamageTracking() {
        fadeOutDrowningLoop();
        clearFinishedDrowningLoop();
        trackedPlayerId = null;
        previousHurtTime = 0;
        previousEffectiveHealth = Float.NaN;
        previousAirSupply = -1;
        lowestAirSupply = Integer.MAX_VALUE;
        wasUnderWater = false;
        recoveryGaspArmed = false;
        awaitingRecoveryGasp = false;
        suppressMobImpactUntilNanos = Long.MIN_VALUE;
        suppressMobImpactTargetId = -1;
    }

    private static boolean isLocalPlayerSound(SoundInstance sound) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return false;
        if (sound.isRelative()) return true;
        double dx = sound.getX() - minecraft.player.getX();
        double dy = sound.getY() - minecraft.player.getY();
        double dz = sound.getZ() - minecraft.player.getZ();
        return dx * dx + dy * dy + dz * dz
                <= LOCAL_PLAYER_SOUND_RADIUS_SQ;
    }

    private static boolean isNonPlayerMobImpact() {
        return System.nanoTime() <= suppressMobImpactUntilNanos;
    }

    private static boolean isCurrentNonPlayerMobTarget() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.hitResult instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity
                && !(hit.getEntity() instanceof Player);
    }

    private static boolean isTrackedMobImpactSound(SoundInstance sound) {
        if (!isNonPlayerMobImpact() || sound.isRelative()) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || suppressMobImpactTargetId < 0) {
            return false;
        }
        var target = minecraft.level.getEntity(suppressMobImpactTargetId);
        if (!(target instanceof LivingEntity) || target instanceof Player) {
            return false;
        }

        double dx = sound.getX() - target.getX();
        double dy = sound.getY()
                - (target.getY() + target.getBbHeight() * 0.5D);
        double dz = sound.getZ() - target.getZ();
        return dx * dx + dy * dy + dz * dz
                <= TRACKED_MOB_SOUND_RADIUS_SQ;
    }
}
