package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.config.InventoryModuleRuntimeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp939Entity;
import net.mcreator.scpadditions.scp939.Scp939AwarenessState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Client-side authored audio state for SCP-939 encounters. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class Scp939AudioClient {
    private static final ResourceLocation BITE = id("scp939_bite");
    private static final ResourceLocation HURT = id("scp939_hurt");
    private static final ResourceLocation MAUL = id("scp939_maul");
    private static final ResourceLocation SNARL = id("scp939_snarl");
    private static final ResourceLocation LISTEN = id("scp939_listen");
    private static final ResourceLocation PREY_BREATH = id("scp939_prey_breath");
    private static final ResourceLocation AMBIENT = id("scp939_ambient");
    private static final ResourceLocation CHASE = id("scp939_chase");
    private static final ResourceLocation MALE_GASP = new ResourceLocation(
            ScpAdditionsMod.MODID, "voice_profile_a_gasp");
    private static final ResourceLocation FEMALE_GASP = new ResourceLocation(
            ScpAdditionsMod.MODID, "voice_profile_b_gasp");

    private static final double LOCAL_AUDIO_RANGE = 128.0D;
    private static final int AMBIENT_POST_SIGHT_LINGER_TICKS = 20 * 30;
    private static final int CHASE_UNLOADED_GRACE_TICKS = 20 * 4;
    private static final float PREY_BREATH_VOLUME = 0.72F;

    private static final Map<Integer, Byte> PREVIOUS_ACTIONS = new HashMap<>();
    private static final Map<Integer, Float> PREVIOUS_HEALTH = new HashMap<>();
    private static final Set<Integer> POUNCE_SNARL_PLAYED = new HashSet<>();
    private static final Map<Integer, Scp939LoopSound> MAUL_LOOPS =
            new HashMap<>();

    private static Scp939LoopSound preyBreath;
    private static Scp939LoopSound ambient;
    private static Scp939LoopSound chase;
    private static boolean ambientTriggered;
    private static int ambientInvisibleTicks;
    private static Integer chaseEntityId;
    private static int chaseMissingTicks;
    private static boolean previousHoldingBreath;

    private Scp939AudioClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !player.isAlive()
                || player.isCreative() || player.isSpectator()) {
            reset();
            return;
        }
        if (minecraft.isPaused()) return;

        AABB area = player.getBoundingBox().inflate(LOCAL_AUDIO_RANGE);
        List<Scp939Entity> loaded = minecraft.level.getEntitiesOfClass(
                Scp939Entity.class, area,
                entity -> entity.isAlive() && !entity.isRemoved());

        updateEntitySounds(minecraft, loaded);
        updatePreyBreathing(minecraft);
        updateAmbient(minecraft, player, loaded);
        updateChase(minecraft, player, loaded);
        previousHoldingBreath = Scp939ClientState.holdingBreath();
    }

    /** Used by the shared music exclusivity layer. */
    public static boolean isMusicPlaying() {
        return (ambient != null && !ambient.isFinished())
                || (chase != null && !chase.isFinished());
    }

    private static void updateEntitySounds(Minecraft minecraft,
            List<Scp939Entity> loaded) {
        Set<Integer> liveIds = new HashSet<>();
        Set<Integer> maulingIds = new HashSet<>();

        for (Scp939Entity entity : loaded) {
            int id = entity.getId();
            liveIds.add(id);
            byte action = entity.getAction();
            Byte previousAction = PREVIOUS_ACTIONS.put(id, action);

            float health = entity.getHealth();
            Float previousHealth = PREVIOUS_HEALTH.put(id, health);
            boolean damaged = previousHealth != null
                    && health + 0.001F < previousHealth;
            if (damaged) {
                RandomSource pitchRandom = RandomSource.create();
                playAt(minecraft, HURT, entity, 0.95F,
                        0.96F + pitchRandom.nextFloat() * 0.08F);
            }

            boolean actionChanged = previousAction == null
                    || previousAction.byteValue() != action;
            if (actionChanged) {
                if (action == Scp939Entity.ACTION_BITE
                        || action == Scp939Entity.ACTION_PIN_LAND) {
                    playAt(minecraft, BITE, entity, 1.0F, 1.0F);
                } else if (action == Scp939Entity.ACTION_LISTEN) {
                    playAt(minecraft, LISTEN, entity, 0.90F, 1.0F);
                } else if (action == Scp939Entity.ACTION_KICKED && !damaged) {
                    playAt(minecraft, HURT, entity, 1.0F, 1.0F);
                }
            }

            if (action == Scp939Entity.ACTION_POUNCE) {
                if (!entity.onGround() && POUNCE_SNARL_PLAYED.add(id)) {
                    playAt(minecraft, SNARL, entity, 1.0F, 1.0F);
                }
            } else {
                POUNCE_SNARL_PLAYED.remove(id);
            }

            // The loop begins with the impact/bite cue as the victim is knocked
            // down, then stays alive seamlessly through the dedicated MAUL state.
            if (action == Scp939Entity.ACTION_PIN_LAND
                    || action == Scp939Entity.ACTION_MAUL) {
                maulingIds.add(id);
                Scp939LoopSound loop = MAUL_LOOPS.get(id);
                if (loop == null || loop.isFinished()) {
                    // Minecraft may discard sounds started at literal zero, so
                    // the fade begins from a nearly inaudible non-zero floor.
                    loop = new Scp939LoopSound(MAUL, SoundSource.HOSTILE,
                            false, entity, 0.015F, 0.95F, 0.045F, 0.055F);
                    MAUL_LOOPS.put(id, loop);
                    minecraft.getSoundManager().play(loop);
                } else {
                    loop.setTargetVolume(0.95F);
                }
            }
        }

        for (Map.Entry<Integer, Scp939LoopSound> entry : MAUL_LOOPS.entrySet()) {
            if (!maulingIds.contains(entry.getKey())) {
                entry.getValue().beginFadeOut();
            }
        }
        MAUL_LOOPS.entrySet().removeIf(entry -> entry.getValue().isFinished());
        PREVIOUS_ACTIONS.keySet().retainAll(liveIds);
        PREVIOUS_HEALTH.keySet().retainAll(liveIds);
        POUNCE_SNARL_PLAYED.retainAll(liveIds);
    }

    private static void updatePreyBreathing(Minecraft minecraft) {
        boolean active = Scp939ClientState.breathActive()
                && !Scp939ClientState.pinned();
        boolean holding = Scp939ClientState.holdingBreath();

        if (active && !holding) {
            if (preyBreath == null || preyBreath.isFinished()) {
                preyBreath = new Scp939LoopSound(PREY_BREATH,
                        SoundSource.PLAYERS, true, null,
                        PREY_BREATH_VOLUME, PREY_BREATH_VOLUME,
                        1.0F, 0.12F);
                minecraft.getSoundManager().play(preyBreath);
            } else {
                // Ordinary breathing resumes immediately; only suppression fades.
                preyBreath.setTargetVolumeImmediately(PREY_BREATH_VOLUME);
            }
        } else if (preyBreath != null) {
            preyBreath.beginFadeOut();
        }

        if (preyBreath != null && preyBreath.isFinished()) preyBreath = null;

        // Exhaustion is the one forced gasp in the 939 breath system. A held
        // key distinguishes it from the player deliberately releasing breath.
        if (active && previousHoldingBreath && !holding
                && Scp939Keybinds.HOLD_BREATH.isDown()) {
            playRelative(minecraft,
                    InventoryModuleRuntimeState.useVoiceProfileBForClient()
                            ? FEMALE_GASP : MALE_GASP,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private static void updateAmbient(Minecraft minecraft, LocalPlayer player,
            List<Scp939Entity> loaded) {
        boolean visible = false;
        double nearest = Double.POSITIVE_INFINITY;
        for (Scp939Entity entity : loaded) {
            nearest = Math.min(nearest, Math.sqrt(entity.distanceToSqr(player)));
            if (!visible && visuallyConfirmed(player, entity)) visible = true;
        }

        if (visible) {
            ambientTriggered = true;
            ambientInvisibleTicks = 0;
        } else if (ambientTriggered) {
            ambientInvisibleTicks++;
        }
        if (!ambientTriggered) return;

        if (ambientInvisibleTicks > AMBIENT_POST_SIGHT_LINGER_TICKS) {
            if (ambient != null) ambient.beginFadeOut();
            if (ambient == null || ambient.isFinished()) {
                ambient = null;
                ambientTriggered = false;
                ambientInvisibleTicks = 0;
            }
            return;
        }

        float target = loaded.isEmpty() ? 0.035F : ambientVolume(nearest);
        if (ambient == null || ambient.isFinished()) {
            ambient = new Scp939LoopSound(AMBIENT, SoundSource.MUSIC,
                    true, null, 0.02F, target, 0.018F, 0.012F);
            ModMusicExclusivityClient.stopVanillaMusicNow();
            minecraft.getSoundManager().play(ambient);
        } else {
            ambient.setTargetVolume(target);
        }
    }

    private static void updateChase(Minecraft minecraft, LocalPlayer player,
            List<Scp939Entity> loaded) {
        Scp939Entity owner = null;
        if (chaseEntityId != null) {
            for (Scp939Entity entity : loaded) {
                if (entity.getId() == chaseEntityId.intValue()) {
                    owner = entity;
                    break;
                }
            }
        }

        if (owner == null) {
            Scp939Entity candidate = bestChaseCandidate(player, loaded);
            if (candidate != null) {
                chaseEntityId = candidate.getId();
                owner = candidate;
                chaseMissingTicks = 0;
            }
        }

        if (owner != null) {
            chaseMissingTicks = 0;
            // Once local pursuit is established, keep the score through lost
            // search/investigation until the acoustic brain truly returns IDLE.
            if (owner.getAwarenessState() == Scp939AwarenessState.IDLE) {
                stopChase();
                return;
            }
            ensureChase(minecraft);
            return;
        }

        if (chaseEntityId != null && ++chaseMissingTicks
                <= CHASE_UNLOADED_GRACE_TICKS) {
            ensureChase(minecraft);
            return;
        }
        stopChase();
    }

    private static Scp939Entity bestChaseCandidate(LocalPlayer player,
            List<Scp939Entity> loaded) {
        Scp939Entity best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (Scp939Entity entity : loaded) {
            if (entity.getAwarenessState()
                    != Scp939AwarenessState.CONFIRMED_HUNT) continue;
            double distance = Math.sqrt(entity.distanceToSqr(player));
            if (distance > 72.0D || !looksLikeLocalPursuit(player, entity,
                    distance)) continue;
            if (distance < bestDistance) {
                best = entity;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static boolean looksLikeLocalPursuit(LocalPlayer player,
            Scp939Entity entity, double distance) {
        if (distance <= 14.0D) return true;
        Vec3 toPlayer = horizontal(player.position().subtract(entity.position()));
        if (toPlayer.lengthSqr() < 0.0001D) return true;

        Vec3 motion = horizontal(entity.getDeltaMovement());
        if (motion.lengthSqr() > 0.0004D
                && motion.normalize().dot(toPlayer.normalize()) >= 0.32D) {
            return true;
        }
        Vec3 facing = horizontal(entity.getLookAngle());
        return facing.lengthSqr() > 0.0001D
                && facing.normalize().dot(toPlayer.normalize()) >= 0.48D;
    }

    private static void ensureChase(Minecraft minecraft) {
        if (chase == null || chase.isFinished()) {
            chase = new Scp939LoopSound(CHASE, SoundSource.MUSIC,
                    true, null, 0.02F, 0.88F, 0.045F, 0.025F);
            ModMusicExclusivityClient.stopVanillaMusicNow();
            minecraft.getSoundManager().play(chase);
        } else {
            chase.setTargetVolume(0.88F);
        }
    }

    private static void stopChase() {
        if (chase != null) chase.beginFadeOut();
        chaseEntityId = null;
        chaseMissingTicks = 0;
        if (chase != null && chase.isFinished()) chase = null;
    }

    private static boolean visuallyConfirmed(LocalPlayer player,
            Scp939Entity entity) {
        Vec3 eye = player.getEyePosition();
        Vec3 target = entity.position().add(0.0D,
                entity.getBbHeight() * 0.55D, 0.0D);
        Vec3 direction = target.subtract(eye);
        if (direction.lengthSqr() < 0.0001D) return true;
        direction = direction.normalize();
        if (player.getViewVector(1.0F).normalize().dot(direction) < 0.24D) {
            return false;
        }
        return player.hasLineOfSight(entity);
    }

    private static float ambientVolume(double distance) {
        float closeness = 1.0F - Mth.clamp((float) ((distance - 4.0D) / 60.0D),
                0.0F, 1.0F);
        closeness *= closeness;
        return 0.045F + closeness * 0.66F;
    }

    private static void playAt(Minecraft minecraft, ResourceLocation sound,
            Scp939Entity entity, float volume, float pitch) {
        minecraft.getSoundManager().play(new SimpleSoundInstance(sound,
                SoundSource.HOSTILE, volume, pitch, RandomSource.create(),
                false, 0, SoundInstance.Attenuation.LINEAR,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.45D,
                entity.getZ(), false));
    }

    private static void playRelative(Minecraft minecraft,
            ResourceLocation sound, SoundSource source, float volume,
            float pitch) {
        minecraft.getSoundManager().play(new SimpleSoundInstance(sound, source,
                volume, pitch, RandomSource.create(), false, 0,
                SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true));
    }

    private static Vec3 horizontal(Vec3 value) {
        return new Vec3(value.x, 0.0D, value.z);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(ScpAdditionsMod.MODID, path);
    }

    private static void reset() {
        if (preyBreath != null) preyBreath.stopImmediately();
        if (ambient != null) ambient.stopImmediately();
        if (chase != null) chase.stopImmediately();
        for (Scp939LoopSound loop : new ArrayList<>(MAUL_LOOPS.values())) {
            loop.stopImmediately();
        }
        PREVIOUS_ACTIONS.clear();
        PREVIOUS_HEALTH.clear();
        POUNCE_SNARL_PLAYED.clear();
        MAUL_LOOPS.clear();
        preyBreath = null;
        ambient = null;
        chase = null;
        ambientTriggered = false;
        ambientInvisibleTicks = 0;
        chaseEntityId = null;
        chaseMissingTicks = 0;
        previousHoldingBreath = false;
    }
}
