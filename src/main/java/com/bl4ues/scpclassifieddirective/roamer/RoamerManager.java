package com.bl4ues.scpclassifieddirective.roamer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;
import com.bl4ues.scpclassifieddirective.entity.Scp106Entity;
import com.bl4ues.scpclassifieddirective.entity.Scp173Entity;
import com.bl4ues.scpclassifieddirective.entity.Scp939Entity;
import com.bl4ues.scpclassifieddirective.safezone.SafeZoneManager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Shared lifecycle and global scheduling state for SCP entities with their own
 * natural encounter cycle. Each roamer owns one server-wide timer. When a
 * check becomes due, one valid survival player is selected at random as the
 * encounter target.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RoamerManager {
    private static final AABB WORLD_BOUNDS = new AABB(-30000000.0D,
            -2048.0D, -30000000.0D, 30000000.0D, 4096.0D,
            30000000.0D);
    private static final java.util.Map<MinecraftServer, ServerState> STATES =
            new WeakHashMap<>();

    private RoamerManager() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        synchronized (STATES) {
            for (RoamerType type : RoamerType.values()) {
                normalizeScheduler(server, type, data(server, type));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        MinecraftServer server = event.getEntity().getServer();
        if (server == null) return;
        UUID leavingPlayer = event.getEntity().getUUID();
        synchronized (STATES) {
            ServerState state = STATES.get(server);
            if (state == null) return;
            int remainingPlayers = validPlayers(server, leavingPlayer).size();
            if (remainingPlayers > 0) {
                for (RoamerData data : state.data.values()) {
                    rescaleScheduledDelayForPlayers(server, data,
                            remainingPlayers);
                }
                return;
            }
            for (RoamerData data : state.data.values()) {
                pauseCountdown(server, data);
                if (data.activeEntityIds.isEmpty()) {
                    data.lastResult = RoamerResult.PAUSED_NO_VALID_PLAYERS;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        RoamerType type = RoamerType.fromEntity(event.getEntity());
        if (type == null) return;
        MinecraftServer server = event.getLevel().getServer();
        if (server != null) {
            markSpawned(server, type, event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        Entity entity = event.getEntity();
        RoamerType type = RoamerType.fromEntity(entity);
        Entity.RemovalReason reason = entity.getRemovalReason();
        if (type == null || (reason != Entity.RemovalReason.KILLED
                && reason != Entity.RemovalReason.DISCARDED)) {
            return;
        }
        MinecraftServer server = event.getLevel().getServer();
        if (server != null) markRemoved(server, type, entity.getUUID());
    }

    /**
     * Returns one random valid encounter target exactly once when the global
     * scheduler reaches its next check. Any player tick may drive the poll; the
     * selected target does not need to be that player.
     */
    public static ServerPlayer pollSpawnTarget(ServerPlayer driver,
            RoamerType type) {
        MinecraftServer server = driver == null ? null : driver.getServer();
        if (server == null || type == null) return null;
        synchronized (STATES) {
            RoamerData data = data(server, type);
            if (normalizeScheduler(server, type, data)
                    != RoamerState.COUNTDOWN) {
                return null;
            }

            int currentTick = server.getTickCount();
            if (currentTick < data.nextCheckTick) return null;

            List<ServerPlayer> candidates = validPlayers(server, null);
            if (candidates.isEmpty()) {
                pauseCountdown(server, data);
                data.lastResult = RoamerResult.PAUSED_NO_VALID_PLAYERS;
                return null;
            }

            Difficulty difficulty = currentDifficulty(server);
            int recurringDelay = RoamerDifficultyPolicy.recurringDelayTicks(
                    type, difficulty);
            if (recurringDelay < 0) {
                data.nextCheckTick = -1;
                data.scheduledPlayerCount = 0;
                data.pausedRemainingTicks = -1;
                data.lastResult = RoamerResult.DIFFICULTY_DISABLED;
                return null;
            }
            int scaledDelay = RoamerDifficultyPolicy.scaleDelayForPlayers(
                    recurringDelay, candidates.size());
            data.nextCheckTick = currentTick + Math.max(1, scaledDelay);
            data.scheduledPlayerCount = candidates.size();
            data.pausedRemainingTicks = -1;

            ServerPlayer randomSource = candidates.get(0);
            return candidates.get(randomSource.getRandom()
                    .nextInt(candidates.size()));
        }
    }

    public static void recordResult(ServerPlayer player, RoamerType type,
            RoamerResult result) {
        MinecraftServer server = player == null ? null : player.getServer();
        if (server == null || type == null) return;
        synchronized (STATES) {
            data(server, type).lastResult = result == null
                    ? RoamerResult.NONE : result;
        }
    }

    public static void markSpawned(MinecraftServer server, RoamerType type,
            UUID entityId) {
        if (server == null || type == null || entityId == null) return;
        synchronized (STATES) {
            RoamerData data = data(server, type);
            data.activeEntityIds.add(entityId);
            data.lastResult = RoamerResult.SPAWNED;
            if (!allowsConcurrentInstances(type)) {
                data.nextCheckTick = -1;
                data.scheduledPlayerCount = 0;
                data.pausedRemainingTicks = -1;
                return;
            }

            if (data.nextCheckTick < 0 && canSchedule(server, type, data)) {
                schedule(server, type, data,
                        RoamerDifficultyPolicy.recurringDelayTicks(type,
                                currentDifficulty(server)),
                        RoamerResult.SPAWNED);
            }
        }
    }

    public static void markRemoved(MinecraftServer server, RoamerType type,
            UUID entityId) {
        if (server == null || type == null || entityId == null) return;
        synchronized (STATES) {
            RoamerData data = data(server, type);
            data.activeEntityIds.remove(entityId);
            if (data.activeEntityIds.isEmpty()
                    && !allowsConcurrentInstances(type)) {
                restartSchedule(server, type, data,
                        RoamerResult.DESPAWNED_TIMER_RESET);
            }
        }
    }

    public static boolean hasActive(MinecraftServer server, RoamerType type) {
        return activeCount(server, type) > 0;
    }

    public static int activeCount(MinecraftServer server, RoamerType type) {
        if (server == null || type == null) return 0;
        synchronized (STATES) {
            return data(server, type).activeEntityIds.size();
        }
    }

    public static boolean hasOtherActive(MinecraftServer server,
            RoamerType excludedType) {
        if (server == null || excludedType == null) return false;
        synchronized (STATES) {
            for (RoamerType type : RoamerType.values()) {
                if (type != excludedType
                        && !data(server, type).activeEntityIds.isEmpty()) {
                    return true;
                }
            }
            return false;
        }
    }

    public static RoamerDebugSnapshot debugSnapshot(ServerPlayer player,
            RoamerType type) {
        MinecraftServer server = player == null ? null : player.getServer();
        if (server == null || type == null) {
            return new RoamerDebugSnapshot(type, RoamerState.DISABLED,
                    RoamerResult.NONE, -1);
        }
        synchronized (STATES) {
            RoamerData data = data(server, type);
            RoamerState state = normalizeScheduler(server, type, data);
            int nextCheckTick = state == RoamerState.COUNTDOWN
                    ? data.nextCheckTick : -1;
            return new RoamerDebugSnapshot(type, state,
                    resultFor(type, data, state), nextCheckTick);
        }
    }

    public static List<RoamerDebugSnapshot> debugSnapshots(ServerPlayer player) {
        List<RoamerDebugSnapshot> snapshots = new ArrayList<>();
        for (RoamerType type : RoamerType.values()) {
            snapshots.add(debugSnapshot(player, type));
        }
        return List.copyOf(snapshots);
    }

    public static boolean isSpawnRuleEnabled(MinecraftServer server,
            RoamerType type) {
        if (server == null || type == null) return false;
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        return overworld != null && overworld.getGameRules()
                .getBoolean(type.spawnRule());
    }

    public static void setSpawnRule(MinecraftServer server, RoamerType type,
            boolean enabled) {
        if (server == null || type == null) return;
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        overworld.getGameRules().getRule(type.spawnRule()).set(enabled, server);
        synchronized (STATES) {
            RoamerData data = data(server, type);
            if (!enabled || !type.spawnImplemented() || !moduleEnabled(type)) {
                data.nextCheckTick = -1;
                data.scheduledPlayerCount = 0;
                data.pausedRemainingTicks = -1;
                data.lastResult = !type.spawnImplemented()
                        ? RoamerResult.NOT_IMPLEMENTED
                        : !moduleEnabled(type)
                                ? RoamerResult.MODULE_DISABLED
                                : RoamerResult.RULE_DISABLED;
                return;
            }
            if (!RoamerDifficultyPolicy.schedulesEnabled(
                    currentDifficulty(server))) {
                data.nextCheckTick = -1;
                data.scheduledPlayerCount = 0;
                data.pausedRemainingTicks = -1;
                data.lastResult = RoamerResult.DIFFICULTY_DISABLED;
                return;
            }
            if (!data.contained && (data.activeEntityIds.isEmpty()
                    || allowsConcurrentInstances(type))) {
                scheduleInitial(server, type, data, RoamerResult.TIMER_STARTED);
            }
        }
    }

    /** Future containment systems can hold a roamer without changing gamerules. */
    public static void setContained(MinecraftServer server, RoamerType type,
            boolean contained) {
        if (server == null || type == null) return;
        synchronized (STATES) {
            RoamerData data = data(server, type);
            data.contained = contained;
            if (contained) {
                data.nextCheckTick = -1;
                data.scheduledPlayerCount = 0;
                data.pausedRemainingTicks = -1;
            } else if ((data.activeEntityIds.isEmpty()
                    || allowsConcurrentInstances(type))
                    && isSpawnRuleEnabled(server, type)
                    && type.spawnImplemented() && moduleEnabled(type)) {
                scheduleInitial(server, type, data, RoamerResult.TIMER_STARTED);
            }
        }
    }

    public static boolean isContained(MinecraftServer server,
            RoamerType type) {
        if (server == null || type == null) return false;
        synchronized (STATES) {
            return data(server, type).contained;
        }
    }

    public static boolean isOperationallyUncontained(MinecraftServer server,
            RoamerType type) {
        if (server == null || type == null || !type.spawnImplemented()
                || !moduleEnabled(type) || !isSpawnRuleEnabled(server, type)) {
            return false;
        }
        synchronized (STATES) {
            return !data(server, type).contained;
        }
    }

    public static int despawn(MinecraftServer server, RoamerType type) {
        if (server == null || type == null) return 0;
        List<Entity> loaded = findLoaded(server, type);
        for (Entity entity : loaded) entity.discard();
        synchronized (STATES) {
            RoamerData data = data(server, type);
            for (Entity entity : loaded) {
                data.activeEntityIds.remove(entity.getUUID());
            }
            if (!loaded.isEmpty() && data.activeEntityIds.isEmpty()
                    && !allowsConcurrentInstances(type)) {
                restartSchedule(server, type, data,
                        RoamerResult.DESPAWNED_TIMER_RESET);
            }
        }
        return loaded.size();
    }

    public static int despawnAll(MinecraftServer server) {
        int removed = 0;
        for (RoamerType type : RoamerType.values()) {
            removed += despawn(server, type);
        }
        return removed;
    }

    private static RoamerState normalizeScheduler(MinecraftServer server,
            RoamerType type, RoamerData data) {
        Difficulty difficulty = currentDifficulty(server);
        if (data.scheduledDifficulty != difficulty) {
            data.scheduledDifficulty = difficulty;
            data.nextCheckTick = -1;
            data.scheduledPlayerCount = 0;
            data.pausedRemainingTicks = -1;
        }

        if (!type.spawnImplemented()) {
            data.nextCheckTick = -1;
            data.scheduledPlayerCount = 0;
            data.pausedRemainingTicks = -1;
            data.lastResult = RoamerResult.NOT_IMPLEMENTED;
            return RoamerState.DISABLED;
        }
        if (!moduleEnabled(type)) {
            data.nextCheckTick = -1;
            data.scheduledPlayerCount = 0;
            data.pausedRemainingTicks = -1;
            data.lastResult = RoamerResult.MODULE_DISABLED;
            return RoamerState.DISABLED;
        }
        if (!RoamerDifficultyPolicy.schedulesEnabled(difficulty)) {
            data.nextCheckTick = -1;
            data.scheduledPlayerCount = 0;
            data.pausedRemainingTicks = -1;
            data.lastResult = RoamerResult.DIFFICULTY_DISABLED;
            return RoamerState.DISABLED;
        }
        if (data.contained) {
            data.nextCheckTick = -1;
            data.scheduledPlayerCount = 0;
            data.pausedRemainingTicks = -1;
            return RoamerState.CONTAINED;
        }
        if (!isSpawnRuleEnabled(server, type)) {
            data.nextCheckTick = -1;
            data.scheduledPlayerCount = 0;
            data.pausedRemainingTicks = -1;
            data.lastResult = RoamerResult.RULE_DISABLED;
            return RoamerState.DISABLED;
        }
        if (!data.activeEntityIds.isEmpty()
                && !allowsConcurrentInstances(type)) {
            data.nextCheckTick = -1;
            data.scheduledPlayerCount = 0;
            data.lastResult = RoamerResult.SPAWNED;
            return RoamerState.SPAWNED;
        }

        int validPlayerCount = validPlayers(server, null).size();
        if (validPlayerCount <= 0) {
            pauseCountdown(server, data);
            data.lastResult = RoamerResult.PAUSED_NO_VALID_PLAYERS;
            return RoamerState.PAUSED;
        }

        if (data.nextCheckTick >= 0
                && data.scheduledPlayerCount != validPlayerCount) {
            rescaleScheduledDelayForPlayers(server, data, validPlayerCount);
        }
        if (data.nextCheckTick < 0) {
            if (data.pausedRemainingTicks >= 0) {
                data.nextCheckTick = server.getTickCount()
                        + Math.max(1, data.pausedRemainingTicks);
                data.scheduledPlayerCount = validPlayerCount;
                data.pausedRemainingTicks = -1;
            } else {
                scheduleInitial(server, type, data,
                        RoamerResult.TIMER_STARTED);
            }
        }
        return data.nextCheckTick >= 0
                ? RoamerState.COUNTDOWN : RoamerState.DISABLED;
    }

    private static RoamerResult resultFor(RoamerType type, RoamerData data,
            RoamerState state) {
        if (!type.spawnImplemented()) return RoamerResult.NOT_IMPLEMENTED;
        if (state == RoamerState.SPAWNED) return RoamerResult.SPAWNED;
        return data.lastResult;
    }

    private static void restartSchedule(MinecraftServer server,
            RoamerType type, RoamerData data, RoamerResult result) {
        schedule(server, type, data,
                RoamerDifficultyPolicy.recurringDelayTicks(type,
                        currentDifficulty(server)), result);
    }

    private static void scheduleInitial(MinecraftServer server,
            RoamerType type, RoamerData data, RoamerResult result) {
        schedule(server, type, data,
                RoamerDifficultyPolicy.initialDelayTicks(type,
                        currentDifficulty(server)), result);
    }

    private static void schedule(MinecraftServer server, RoamerType type,
            RoamerData data, int delayTicks, RoamerResult result) {
        if (delayTicks < 0 || !canScheduleWithoutPlayers(server, type, data)) {
            data.nextCheckTick = -1;
            data.scheduledPlayerCount = 0;
            data.pausedRemainingTicks = -1;
            if (!RoamerDifficultyPolicy.schedulesEnabled(
                    currentDifficulty(server))) {
                data.lastResult = RoamerResult.DIFFICULTY_DISABLED;
            }
            return;
        }
        int validPlayerCount = validPlayers(server, null).size();
        if (validPlayerCount <= 0) {
            data.nextCheckTick = -1;
            data.scheduledPlayerCount = 0;
            data.pausedRemainingTicks = Math.max(1, delayTicks);
            data.lastResult = result == null ? RoamerResult.NONE : result;
            return;
        }
        int scaledDelay = RoamerDifficultyPolicy.scaleDelayForPlayers(
                delayTicks, validPlayerCount);
        data.nextCheckTick = server.getTickCount() + Math.max(1, scaledDelay);
        data.scheduledPlayerCount = validPlayerCount;
        data.pausedRemainingTicks = -1;
        data.lastResult = result == null ? RoamerResult.NONE : result;
    }

    private static void rescaleScheduledDelayForPlayers(MinecraftServer server,
            RoamerData data, int newPlayerCount) {
        if (server == null || data == null) return;
        int players = Math.max(0, newPlayerCount);
        if (data.nextCheckTick < 0) {
            data.scheduledPlayerCount = players;
            return;
        }
        if (players <= 0) {
            data.nextCheckTick = -1;
            data.scheduledPlayerCount = 0;
            return;
        }
        int oldPlayerCount = data.scheduledPlayerCount;
        if (oldPlayerCount <= 0 || oldPlayerCount == players) {
            data.scheduledPlayerCount = players;
            return;
        }

        int currentTick = server.getTickCount();
        int remaining = Math.max(1, data.nextCheckTick - currentTick);
        double oldMultiplier = RoamerDifficultyPolicy
                .playerIntervalMultiplier(oldPlayerCount);
        double newMultiplier = RoamerDifficultyPolicy
                .playerIntervalMultiplier(players);
        int rescaledRemaining = Math.max(1, (int) Math.round(remaining
                * newMultiplier / oldMultiplier));
        data.nextCheckTick = currentTick + rescaledRemaining;
        data.scheduledPlayerCount = players;
    }

    /** Preserves the exact countdown while no player is eligible outside zones. */
    private static void pauseCountdown(MinecraftServer server,
            RoamerData data) {
        if (server == null || data == null) return;
        if (data.nextCheckTick >= 0) {
            data.pausedRemainingTicks = Math.max(1,
                    data.nextCheckTick - server.getTickCount());
        }
        data.nextCheckTick = -1;
        data.scheduledPlayerCount = 0;
    }

    private static boolean canSchedule(MinecraftServer server, RoamerType type,
            RoamerData data) {
        return canScheduleWithoutPlayers(server, type, data)
                && !validPlayers(server, null).isEmpty();
    }

    private static boolean canScheduleWithoutPlayers(MinecraftServer server,
            RoamerType type, RoamerData data) {
        return type.spawnImplemented() && moduleEnabled(type)
                && !data.contained && isSpawnRuleEnabled(server, type)
                && RoamerDifficultyPolicy.schedulesEnabled(
                        currentDifficulty(server))
                && (data.activeEntityIds.isEmpty()
                        || allowsConcurrentInstances(type));
    }

    private static List<ServerPlayer> validPlayers(MinecraftServer server,
            UUID excludedPlayer) {
        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (excludedPlayer != null
                    && excludedPlayer.equals(player.getUUID())) continue;
            if (!player.isAlive() || player.isCreative()
                    || player.isSpectator()
                    || SafeZoneManager.isInside(player)) continue;
            players.add(player);
        }
        return players;
    }

    private static Difficulty currentDifficulty(MinecraftServer server) {
        return server.getWorldData().getDifficulty();
    }

    /** SCP-939 can have overlapping encounters; other roamers remain exclusive. */
    private static boolean allowsConcurrentInstances(RoamerType type) {
        return type == RoamerType.SCP_939;
    }

    private static boolean moduleEnabled(RoamerType type) {
        return switch (type) {
            case SCP_173 -> ScpClassifiedDirectiveModulesConfig.get().scp173.enabled;
            case SCP_106, SCP_939 -> true;
        };
    }

    private static List<Entity> findLoaded(MinecraftServer server,
            RoamerType type) {
        List<Entity> entities = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            switch (type) {
                case SCP_173 -> entities.addAll(level.getEntitiesOfClass(
                        Scp173Entity.class, WORLD_BOUNDS,
                        entity -> entity.isAlive() && !entity.isRemoved()));
                case SCP_106 -> entities.addAll(level.getEntitiesOfClass(
                        Scp106Entity.class, WORLD_BOUNDS,
                        entity -> entity.isAlive() && !entity.isRemoved()));
                case SCP_939 -> entities.addAll(level.getEntitiesOfClass(
                        Scp939Entity.class, WORLD_BOUNDS,
                        entity -> entity.isAlive() && !entity.isRemoved()));
            }
        }
        return entities;
    }

    private static RoamerData data(MinecraftServer server, RoamerType type) {
        ServerState state = STATES.computeIfAbsent(server,
                ignored -> new ServerState());
        return state.data.computeIfAbsent(type, ignored -> new RoamerData());
    }

    private static final class ServerState {
        private final EnumMap<RoamerType, RoamerData> data =
                new EnumMap<>(RoamerType.class);
    }

    private static final class RoamerData {
        private final Set<UUID> activeEntityIds = new HashSet<>();
        private int nextCheckTick = -1;
        private int scheduledPlayerCount;
        private int pausedRemainingTicks = -1;
        private RoamerResult lastResult = RoamerResult.NONE;
        private Difficulty scheduledDifficulty;
        private boolean contained;
    }
}
