package net.mcreator.scpadditions.death;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.compat.MineZeroDeathCoordinator;
import net.mcreator.scpadditions.network.DeathSpectateStatePacket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server authority for the death-screen live feed.
 *
 * A dead observer is temporarily placed in spectator mode and moved close to
 * the selected living player. That makes the normal Minecraft connection stream
 * the target's dimension, chunks and entities even when the target is hundreds
 * of blocks away or in another dimension. The custom client camera is still
 * responsible for the orbiting third-person presentation inside the death UI.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DeathSpectateCoordinator {
    public static final int ACTION_QUERY = 0;
    public static final int ACTION_START = 1;
    public static final int ACTION_CYCLE_PREVIOUS = 2;
    public static final int ACTION_CYCLE_NEXT = 3;
    public static final int ACTION_STOP = 4;

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static int followTick;

    private DeathSpectateCoordinator() {
    }

    public static void handleRequest(ServerPlayer observer, int action) {
        if (observer == null) return;
        switch (action) {
            case ACTION_START -> start(observer);
            case ACTION_CYCLE_PREVIOUS -> cycle(observer, -1);
            case ACTION_CYCLE_NEXT -> cycle(observer, 1);
            case ACTION_STOP -> stop(observer, true);
            default -> query(observer);
        }
    }

    private static void query(ServerPlayer observer) {
        Session session = SESSIONS.get(observer.getUUID());
        if (session != null) {
            ServerPlayer target = player(observer.server, session.targetId);
            if (isEligibleTarget(observer, target)) {
                sendState(observer, true, target, eligibleTargets(observer).size());
                return;
            }
        }
        sendState(observer, false, null, eligibleTargets(observer).size());
    }

    private static void start(ServerPlayer observer) {
        if (!canObserve(observer)) {
            sendState(observer, false, null, eligibleTargets(observer).size());
            return;
        }

        List<ServerPlayer> targets = eligibleTargets(observer);
        if (targets.isEmpty()) {
            sendState(observer, false, null, 0);
            return;
        }

        Session session = SESSIONS.computeIfAbsent(observer.getUUID(), ignored ->
                new Session(observer.getUUID(), observer.level().dimension(),
                        observer.position(), observer.getYRot(), observer.getXRot(),
                        observer.gameMode.getGameModeForPlayer(),
                        observer.isInvulnerable()));
        ServerPlayer target = targets.get(0);
        session.targetId = target.getUUID();
        observer.setInvulnerable(true);
        if (!observer.isSpectator()) observer.setGameMode(GameType.SPECTATOR);
        follow(observer, target, true);
        sendState(observer, true, target, targets.size());
    }

    private static void cycle(ServerPlayer observer, int direction) {
        Session session = SESSIONS.get(observer.getUUID());
        if (session == null) {
            start(observer);
            return;
        }

        List<ServerPlayer> targets = eligibleTargets(observer);
        if (targets.isEmpty()) {
            stop(observer, true);
            return;
        }

        int current = -1;
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i).getUUID().equals(session.targetId)) {
                current = i;
                break;
            }
        }
        int next = Math.floorMod((current < 0 ? 0 : current) + direction,
                targets.size());
        ServerPlayer target = targets.get(next);
        session.targetId = target.getUUID();
        follow(observer, target, true);
        sendState(observer, true, target, targets.size());
    }

    private static void stop(ServerPlayer observer, boolean restoreOrigin) {
        Session session = SESSIONS.remove(observer.getUUID());
        if (session == null) {
            sendState(observer, false, null, eligibleTargets(observer).size());
            return;
        }

        // A normal death-screen observer is still actually dead. Returning them
        // to the corpse location keeps the later vanilla Respawn button honest.
        // MineZero logical deaths are also returned before its eventual rewind.
        if (restoreOrigin && canObserve(observer)) {
            ServerLevel origin = observer.server.getLevel(session.originDimension);
            if (origin != null) {
                observer.teleportTo(origin, session.origin.x, session.origin.y,
                        session.origin.z, session.originYaw, session.originPitch);
            }
        }
        observer.setDeltaMovement(Vec3.ZERO);
        observer.setInvulnerable(session.originInvulnerable);
        if (canObserve(observer)
                && observer.gameMode.getGameModeForPlayer() != session.originGameMode) {
            observer.setGameMode(session.originGameMode);
        }
        sendState(observer, false, null, eligibleTargets(observer).size());
    }

    private static boolean canObserve(ServerPlayer player) {
        return player != null && (!player.isAlive()
                || MineZeroDeathCoordinator.isLogicallyDead(player)
                || SESSIONS.containsKey(player.getUUID()));
    }

    private static List<ServerPlayer> eligibleTargets(ServerPlayer observer) {
        if (observer == null || observer.server == null) return List.of();
        List<ServerPlayer> result = new ArrayList<>();
        for (ServerPlayer candidate : observer.server.getPlayerList().getPlayers()) {
            if (isEligibleTarget(observer, candidate)) result.add(candidate);
        }
        result.sort(Comparator.comparing(player ->
                player.getGameProfile().getName().toLowerCase()));
        return result;
    }

    private static boolean isEligibleTarget(ServerPlayer observer,
            ServerPlayer candidate) {
        return candidate != null
                && candidate != observer
                && candidate.isAlive()
                && !candidate.isSpectator()
                && !MineZeroDeathCoordinator.isLogicallyDead(candidate);
    }

    private static ServerPlayer player(MinecraftServer server, UUID id) {
        return server == null || id == null ? null
                : server.getPlayerList().getPlayer(id);
    }

    /** Keep the dead observer close enough that vanilla keeps streaming the feed. */
    private static void follow(ServerPlayer observer, ServerPlayer target,
            boolean force) {
        if (observer == null || target == null) return;
        boolean otherDimension = !observer.level().dimension()
                .equals(target.level().dimension());
        double distance = otherDimension ? Double.POSITIVE_INFINITY
                : observer.distanceToSqr(target);
        if (!force && !otherDimension && distance <= 64.0D) return;

        observer.teleportTo(target.serverLevel(), target.getX(), target.getY() + 0.25D,
                target.getZ(), target.getYRot(), target.getXRot());
        observer.setDeltaMovement(Vec3.ZERO);
    }

    private static void sendState(ServerPlayer observer, boolean active,
            ServerPlayer target, int availableTargets) {
        UUID id = target == null ? new UUID(0L, 0L) : target.getUUID();
        String name = target == null ? "" : target.getGameProfile().getName();
        ScpAdditionsMod.PACKET_HANDLER.send(
                PacketDistributor.PLAYER.with(() -> observer),
                new DeathSpectateStatePacket(active, id, name,
                        Math.max(0, availableTargets)));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || SESSIONS.isEmpty()) return;
        if (++followTick % 4 != 0) return;

        MinecraftServer server = event.getServer();
        for (Session session : List.copyOf(SESSIONS.values())) {
            ServerPlayer observer = player(server, session.observerId);
            if (observer == null) continue;

            // If vanilla already respawned the player, never drag the new living
            // player back to their corpse just because a stale spectate session survived.
            if (observer.isAlive()
                    && !MineZeroDeathCoordinator.isLogicallyDead(observer)) {
                cleanupAfterRespawn(observer);
                continue;
            }

            ServerPlayer target = player(server, session.targetId);
            if (!isEligibleTarget(observer, target)) {
                List<ServerPlayer> targets = eligibleTargets(observer);
                if (targets.isEmpty()) {
                    stop(observer, true);
                    continue;
                }
                target = targets.get(0);
                session.targetId = target.getUUID();
                follow(observer, target, true);
                sendState(observer, true, target, targets.size());
                continue;
            }
            follow(observer, target, false);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cleanupAfterRespawn(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }

    private static void cleanupAfterRespawn(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null) return;
        player.setInvulnerable(session.originInvulnerable);
        if (player.gameMode.getGameModeForPlayer() != session.originGameMode) {
            player.setGameMode(session.originGameMode);
        }
        sendState(player, false, null, eligibleTargets(player).size());
    }

    private static final class Session {
        private final UUID observerId;
        private final ResourceKey<Level> originDimension;
        private final Vec3 origin;
        private final float originYaw;
        private final float originPitch;
        private final GameType originGameMode;
        private final boolean originInvulnerable;
        private UUID targetId;

        private Session(UUID observerId, ResourceKey<Level> originDimension,
                Vec3 origin, float originYaw, float originPitch,
                GameType originGameMode, boolean originInvulnerable) {
            this.observerId = observerId;
            this.originDimension = originDimension;
            this.origin = origin;
            this.originYaw = originYaw;
            this.originPitch = originPitch;
            this.originGameMode = originGameMode;
            this.originInvulnerable = originInvulnerable;
        }
    }
}
