package net.mcreator.scpadditions.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp106Entity;
import net.mcreator.scpadditions.init.Scp106Sounds;
import net.mcreator.scpadditions.network.ScpEntityNetwork;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Coordinates positional SCP-106 sounds and regional chase music. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp106AudioEvents {
    private static final byte EMERGING_GROUND = 1;
    private static final byte EMERGING_WALL = 2;
    // The authored walk loop plants its feet at 0.72 s and 1.62 s. GeckoLib
    // plays that 1.8 s loop at 1.38x while SCP-106 is walking.
    private static final double WALK_SECONDS_PER_TICK = 1.38D / 20.0D;
    private static final double FIRST_FOOTSTEP_SECONDS = 0.72D;
    private static final double FOOTSTEP_INTERVAL_SECONDS = 0.90D;
    // Additional 30% reduction from the previous 0.63 volume.
    private static final float FOOTSTEP_VOLUME = 0.441F;
    private static final double CHASE_MUSIC_RADIUS = 64.0D;
    private static final double CHASE_MUSIC_RADIUS_SQR =
            CHASE_MUSIC_RADIUS * CHASE_MUSIC_RADIUS;
    private static final Map<UUID, Tracked> TRACKED = new HashMap<>();
    private static final Set<UUID> CHASE_LISTENERS = new HashSet<>();

    private Scp106AudioEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide
                && event.getEntity() instanceof Scp106Entity scp106) {
            TRACKED.put(scp106.getUUID(), new Tracked(scp106));
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide
                || !(event.getEntity() instanceof Scp106Entity scp106)) {
            return;
        }
        TRACKED.remove(scp106.getUUID());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        Set<UUID> desiredListeners = new HashSet<>();
        Iterator<Tracked> iterator = TRACKED.values().iterator();
        while (iterator.hasNext()) {
            Tracked tracked = iterator.next();
            Scp106Entity scp106 = tracked.entity;
            if (scp106.isRemoved() || !scp106.isAlive()) {
                iterator.remove();
                continue;
            }

            byte state = scp106.getEncounterState();
            if (state != tracked.previousState
                    && (state == EMERGING_GROUND
                    || state == EMERGING_WALL)) {
                playPhase(scp106, 1.0F);
            }
            tracked.previousState = state;

            tickFootsteps(tracked);

            boolean ranged = scp106.isRangedAttacking();
            if (ranged && !tracked.rangedPreviously) {
                playPhase(scp106, 0.38F);
            }
            tracked.rangedPreviously = ranged;

            if (tracked.musicSuppressed) {
                tracked.musicSuppressed = false;
                continue;
            }
            ServerPlayer target = validTarget(scp106);
            if (target != null && scp106.isHuntingPlayer(target)) {
                collectChaseListeners(scp106, target, desiredListeners);
            }
        }
        synchronizeChaseMusic(server, desiredListeners);
    }

    private static void collectChaseListeners(Scp106Entity scp106,
            ServerPlayer target, Set<UUID> listeners) {
        if (!(scp106.level() instanceof ServerLevel level)) return;
        for (ServerPlayer player : level.players()) {
            if (!isEligibleListener(player)) continue;
            if (player == target
                    || player.distanceToSqr(scp106) <= CHASE_MUSIC_RADIUS_SQR
                    || player.distanceToSqr(target) <= CHASE_MUSIC_RADIUS_SQR) {
                listeners.add(player.getUUID());
            }
        }
    }

    private static boolean isEligibleListener(ServerPlayer player) {
        return player != null && player.isAlive() && !player.isRemoved()
                && !player.isCreative() && !player.isSpectator();
    }

    private static void synchronizeChaseMusic(MinecraftServer server,
            Set<UUID> desiredListeners) {
        if (server == null) {
            CHASE_LISTENERS.clear();
            return;
        }

        for (UUID playerId : new HashSet<>(CHASE_LISTENERS)) {
            if (desiredListeners.contains(playerId)) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) ScpEntityNetwork.setScp106Chase(player, false);
            CHASE_LISTENERS.remove(playerId);
        }
        for (UUID playerId : desiredListeners) {
            if (CHASE_LISTENERS.contains(playerId)) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                ScpEntityNetwork.setScp106Chase(player, true);
                CHASE_LISTENERS.add(playerId);
            }
        }
    }

    private static void tickFootsteps(Tracked tracked) {
        Scp106Entity scp106 = tracked.entity;
        if (!scp106.isWalkingForFootsteps()) {
            tracked.walkingPreviously = false;
            tracked.walkAnimationSeconds = 0.0D;
            tracked.nextFootstepSeconds = FIRST_FOOTSTEP_SECONDS;
            return;
        }

        if (!tracked.walkingPreviously) {
            tracked.walkingPreviously = true;
            tracked.walkAnimationSeconds = 0.0D;
            tracked.nextFootstepSeconds = FIRST_FOOTSTEP_SECONDS;
        }

        tracked.walkAnimationSeconds += WALK_SECONDS_PER_TICK;
        if (tracked.walkAnimationSeconds + 1.0E-6D
                < tracked.nextFootstepSeconds) {
            return;
        }

        playFootstep(scp106);
        tracked.nextFootstepSeconds += FOOTSTEP_INTERVAL_SECONDS;
    }

    private static void playFootstep(Scp106Entity scp106) {
        if (!(scp106.level() instanceof ServerLevel level)) return;
        level.playSound(null, scp106.getX(), scp106.getY() + 0.05D,
                scp106.getZ(), Scp106Sounds.STEP.get(),
                SoundSource.HOSTILE, FOOTSTEP_VOLUME,
                0.96F + scp106.getRandom().nextFloat() * 0.08F);
    }

    public static void stopChaseFor(Scp106Entity scp106) {
        if (scp106 == null || scp106.level().isClientSide) return;
        Tracked tracked = TRACKED.get(scp106.getUUID());
        if (tracked != null) tracked.musicSuppressed = true;
    }

    private static ServerPlayer validTarget(Scp106Entity scp106) {
        if (!(scp106.getTarget() instanceof ServerPlayer player)
                || !player.isAlive() || player.isCreative()
                || player.isSpectator() || player.isRemoved()) {
            return null;
        }
        return player;
    }

    public static void playPhase(Scp106Entity scp106, float volume) {
        if (!(scp106.level() instanceof ServerLevel level)) return;
        level.playSound(null, scp106.getX(), scp106.getY() + 0.8D,
                scp106.getZ(), Scp106Sounds.PHASE.get(),
                SoundSource.HOSTILE, volume,
                0.94F + scp106.getRandom().nextFloat() * 0.12F);
    }

    private static final class Tracked {
        private final Scp106Entity entity;
        private byte previousState = -1;
        private boolean rangedPreviously;
        private boolean walkingPreviously;
        private boolean musicSuppressed;
        private double walkAnimationSeconds;
        private double nextFootstepSeconds = FIRST_FOOTSTEP_SECONDS;

        private Tracked(Scp106Entity entity) {
            this.entity = entity;
        }
    }
}
