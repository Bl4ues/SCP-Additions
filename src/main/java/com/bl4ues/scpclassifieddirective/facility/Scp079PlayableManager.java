package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilityCameraDefinition;
import com.bl4ues.scpclassifieddirective.facility.surveillance.FacilitySurveillanceRegistry;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import com.bl4ues.scpclassifieddirective.network.Scp079PlayableNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server authority for the playable SCP-079 role.
 *
 * The final SCP role selector intentionally lives outside this class. Until that
 * UI exists, operators can sneak-use the physical SCP-079 with an empty hand to
 * assume or release control so the role remains directly testable.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp079PlayableManager {
    private static final double MAX_DEVICE_RANGE_SQR = 24.0D * 24.0D;
    private static final double DOOR_ACTION_COST = 5.0D;
    private static final double DOOR_LOCK_COST = 12.0D;
    private static final int DOOR_LOCK_TICKS = 100;
    private static final Map<MinecraftServer, Session> SESSIONS =
            new WeakHashMap<>();
    private static int followTick;

    private Scp079PlayableManager() {
    }

    public enum ManualAction {
        PRIMARY,
        LOCK
    }

    public static boolean hasController(MinecraftServer server) {
        synchronized (SESSIONS) {
            return server != null && SESSIONS.containsKey(server);
        }
    }

    public static ServerPlayer controller(MinecraftServer server) {
        if (server == null) return null;
        Session session;
        synchronized (SESSIONS) {
            session = SESSIONS.get(server);
        }
        return session == null ? null
                : server.getPlayerList().getPlayer(session.playerId);
    }

    public static boolean isController(ServerPlayer player) {
        if (player == null || player.getServer() == null) return false;
        synchronized (SESSIONS) {
            Session session = SESSIONS.get(player.getServer());
            return session != null && session.playerId.equals(player.getUUID());
        }
    }

    public static boolean isCameraMode(ServerPlayer player) {
        Session session = session(player);
        return session != null && session.cameraId != null;
    }

    public static boolean assume(ServerPlayer player, BlockPos hostPos) {
        if (player == null || hostPos == null || player.getServer() == null) {
            return false;
        }
        MinecraftServer server = player.getServer();
        ServerLevel hostLevel = player.serverLevel();
        if (!isHost(hostLevel.getBlockState(hostPos))) return false;

        synchronized (SESSIONS) {
            Session existing = SESSIONS.get(server);
            if (existing != null) {
                if (existing.playerId.equals(player.getUUID())) return true;
                player.displayClientMessage(Component.literal(
                        "SCP-079 is already under player control."), true);
                return false;
            }

            Session session = new Session(player.getUUID(),
                    hostLevel.dimension(), hostPos.immutable(),
                    player.level().dimension(), player.position(),
                    player.getYRot(), player.getXRot(),
                    player.gameMode.getGameModeForPlayer(),
                    player.isInvulnerable());
            SESSIONS.put(server, session);
            player.setInvulnerable(true);
            if (!player.isSpectator()) player.setGameMode(GameType.SPECTATOR);
            anchorToHost(player, session, true);
            Scp079ScreenState.setLocalActive(hostLevel, hostPos, true);
            sync(player, session);
        }
        return true;
    }

    public static void release(ServerPlayer player) {
        if (player == null || player.getServer() == null) return;
        Session session;
        synchronized (SESSIONS) {
            session = SESSIONS.get(player.getServer());
            if (session == null || !session.playerId.equals(player.getUUID())) {
                return;
            }
            SESSIONS.remove(player.getServer());
        }
        restore(player, session);
    }

    public static void returnToHost(ServerPlayer player) {
        Session session = session(player);
        if (session == null) return;
        session.cameraId = null;
        ServerLevel level = player.server.getLevel(session.hostDimension);
        if (level == null) return;
        anchorToHost(player, session, true);
        Scp079ScreenState.setLocalActive(level, session.hostPos, true);
        sync(player, session);
    }

    public static boolean switchToRoom(ServerPlayer player, UUID roomId) {
        Session session = session(player);
        if (session == null || roomId == null) return false;
        ServerLevel hostLevel = player.server.getLevel(session.hostDimension);
        if (hostLevel == null || !networkAvailable(player, hostLevel)) {
            player.displayClientMessage(Component.literal(
                    "SCP-079 network access is offline."), true);
            return false;
        }
        FacilityCameraDefinition camera = FacilitySurveillanceRegistry
                .nextForRoom(hostLevel, roomId, session.cameraId);
        if (camera == null) {
            player.displayClientMessage(Component.literal(
                    "No surveillance camera is registered in this room."), true);
            sync(player, session);
            return false;
        }
        session.cameraId = camera.id();
        Scp079ScreenState.setLocalActive(hostLevel, session.hostPos, false);
        anchorToCamera(player, camera, true);
        sync(player, session);
        return true;
    }

    public static boolean performAction(ServerPlayer player,
            ManualAction action, BlockPos aimedPos) {
        Session session = session(player);
        if (session == null || session.cameraId == null || aimedPos == null) {
            return false;
        }
        FacilityCameraDefinition camera = currentCamera(player, session);
        ServerLevel level = levelFor(player.server, camera == null
                ? null : camera.dimension());
        if (camera == null || level == null
                || camera.eyePosition().distanceToSqr(
                Vec3.atCenterOf(aimedPos)) > MAX_DEVICE_RANGE_SQR) {
            return false;
        }

        BlockPos door = nearestTracked(level,
                Scp079FacilityAccessSavedData.get(player.server).doors(),
                aimedPos, 3.5D);
        if (door != null && HeavyDoorControlPanelAccess
                .hasControllableInterface(level, door)) {
            return action == ManualAction.LOCK
                    ? lockDoor(level, door) : toggleDoor(level, door);
        }

        BlockPos tesla = nearestTracked(level,
                Scp079FacilityAccessSavedData.get(player.server).teslaGates(),
                aimedPos, 5.0D);
        return action == ManualAction.PRIMARY && tesla != null
                && Scp079TeslaSuppression.tryPlayerSuppress(level, tesla);
    }

    private static boolean toggleDoor(ServerLevel level, BlockPos door) {
        if (!Scp079PlayerPower.trySpend(level, DOOR_ACTION_COST)) return false;
        boolean powered = level.hasNeighborSignal(door)
                || level.hasNeighborSignal(door.above());
        int controls = powered
                ? HeavyDoorControlPanelAccess.closeConnectedControls(level, door)
                : HeavyDoorControlPanelAccess.openConnectedControls(level, door);
        if (controls <= 0) {
            Scp079PlayerPower.refund(level, DOOR_ACTION_COST);
            return false;
        }
        Scp079DecisionLog.record(level,
                powered ? Scp079DecisionLog.DecisionType.CLOSE_DOOR
                        : Scp079DecisionLog.DecisionType.OPEN_DOOR,
                Scp079DecisionLog.DecisionOutcome.EXECUTED, door,
                Scp079ProcessingManager.adjustedActionCost(level,
                        DOOR_ACTION_COST),
                "manual SCP-079 camera control");
        return true;
    }

    private static boolean lockDoor(ServerLevel level, BlockPos door) {
        if (!HeavyDoorControlPanelAccess.hasDeniableInterface(level, door)
                || !Scp079PlayerPower.trySpend(level, DOOR_LOCK_COST)) {
            return false;
        }
        int controls = HeavyDoorControlPanelAccess.temporarilyDenyConnectedControls(
                level, door, DOOR_LOCK_TICKS);
        if (controls <= 0) {
            Scp079PlayerPower.refund(level, DOOR_LOCK_COST);
            return false;
        }
        Scp079DecisionLog.record(level,
                Scp079DecisionLog.DecisionType.DENY_ACCESS,
                Scp079DecisionLog.DecisionOutcome.EXECUTED, door,
                Scp079ProcessingManager.adjustedActionCost(level,
                        DOOR_LOCK_COST),
                "manual SCP-079 lock · " + DOOR_LOCK_TICKS / 20.0D + "s");
        return true;
    }

    public static BlockPos hostPosition(ServerPlayer player) {
        Session session = session(player);
        return session == null ? null : session.hostPos;
    }

    private static Session session(ServerPlayer player) {
        if (player == null || player.getServer() == null) return null;
        synchronized (SESSIONS) {
            Session session = SESSIONS.get(player.getServer());
            return session != null && session.playerId.equals(player.getUUID())
                    ? session : null;
        }
    }

    private static FacilityCameraDefinition currentCamera(ServerPlayer player,
            Session session) {
        if (session == null || session.cameraId == null) return null;
        ServerLevel hostLevel = player.server.getLevel(session.hostDimension);
        return hostLevel == null ? null : FacilitySurveillanceRegistry.camera(
                hostLevel, session.cameraId);
    }

    private static void restore(ServerPlayer player, Session session) {
        ServerLevel hostLevel = player.server.getLevel(session.hostDimension);
        if (hostLevel != null) {
            Scp079ScreenState.setLocalActive(hostLevel, session.hostPos, false);
        }
        ServerLevel origin = player.server.getLevel(session.originDimension);
        if (origin != null) {
            player.teleportTo(origin, session.origin.x, session.origin.y,
                    session.origin.z, session.originYaw, session.originPitch);
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.setInvulnerable(session.originInvulnerable);
        if (player.gameMode.getGameModeForPlayer() != session.originGameMode) {
            player.setGameMode(session.originGameMode);
        }
        Scp079PlayableNetwork.sendInactive(player);
    }

    private static void anchorToHost(ServerPlayer player, Session session,
            boolean force) {
        ServerLevel level = player.server.getLevel(session.hostDimension);
        if (level == null) return;
        Vec3 anchor = Vec3.atCenterOf(session.hostPos).add(0.0D, 0.12D, 0.0D);
        boolean wrongDimension = !player.level().dimension()
                .equals(session.hostDimension);
        if (force || wrongDimension
                || player.position().distanceToSqr(anchor) > 0.16D) {
            player.teleportTo(level, anchor.x, anchor.y, anchor.z,
                    player.getYRot(), player.getXRot());
        }
        player.setDeltaMovement(Vec3.ZERO);
    }

    private static void anchorToCamera(ServerPlayer player,
            FacilityCameraDefinition camera, boolean force) {
        ServerLevel level = levelFor(player.server, camera.dimension());
        if (level == null) return;
        Vec3 eye = camera.eyePosition();
        boolean wrongDimension = !player.level().dimension().equals(
                level.dimension());
        if (force || wrongDimension
                || player.position().distanceToSqr(eye) > 0.16D) {
            player.teleportTo(level, eye.x, eye.y, eye.z,
                    player.getYRot(), player.getXRot());
        }
        player.setDeltaMovement(Vec3.ZERO);
    }

    private static void sync(ServerPlayer player, Session session) {
        ServerLevel level = player.server.getLevel(session.hostDimension);
        if (level == null) {
            Scp079PlayableNetwork.sendInactive(player);
            return;
        }
        int power = Math.round(Scp079ProcessingManager.getPower(level));
        boolean auxiliary = Scp079FacilityAccessManager.auxiliaryPowerOnline(
                player.getServer());
        boolean network = networkAvailable(player, level);
        FacilityCameraDefinition camera = network
                ? currentCamera(player, session) : null;
        if (session.cameraId != null && camera == null) session.cameraId = null;
        Scp079PlayableNetwork.sendState(player, session.hostDimension,
                session.hostPos, power, auxiliary, network, camera);
    }

    private static boolean networkAvailable(ServerPlayer player,
            ServerLevel hostLevel) {
        return Scp079FacilityAccessManager.hasFacilityAccess(hostLevel)
                && Scp079FacilityAccessManager.auxiliaryPowerOnline(
                player.getServer());
    }

    private static BlockPos nearestTracked(ServerLevel level,
            java.util.Set<Scp079FacilityAccessSavedData.TrackedPosition> tracked,
            BlockPos aimedPos, double radius) {
        String dimension = level.dimension().location().toString();
        double radiusSqr = radius * radius;
        return tracked.stream()
                .filter(position -> position.dimension().equals(dimension))
                .map(position -> BlockPos.of(position.packedPos()))
                .filter(pos -> pos.distSqr(aimedPos) <= radiusSqr)
                .min(Comparator.comparingDouble(pos -> pos.distSqr(aimedPos)))
                .orElse(null);
    }

    private static ServerLevel levelFor(MinecraftServer server,
            net.minecraft.resources.ResourceLocation dimension) {
        if (server == null || dimension == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().equals(dimension)) return level;
        }
        return null;
    }

    private static boolean isHost(BlockState state) {
        return state != null && (state.is(ScpClassifiedDirectiveModBlocks.SCP_079ON.get())
                || state.is(ScpClassifiedDirectiveModBlocks.SCP_079OFF.get()));
    }

    @SubscribeEvent
    public static void onHostUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !player.isShiftKeyDown()
                || !player.getMainHandItem().isEmpty()
                || !player.canUseGameMasterBlocks()
                || !isHost(event.getLevel().getBlockState(event.getPos()))) {
            return;
        }
        if (isController(player)) release(player);
        else assume(player, event.getPos());
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Session session;
        synchronized (SESSIONS) {
            session = SESSIONS.get(event.getServer());
        }
        if (session == null) return;

        ServerPlayer player = event.getServer().getPlayerList()
                .getPlayer(session.playerId);
        ServerLevel hostLevel = event.getServer().getLevel(session.hostDimension);
        if (player == null) {
            if (hostLevel != null) {
                Scp079ScreenState.setLocalActive(hostLevel, session.hostPos, false);
            }
            synchronized (SESSIONS) {
                SESSIONS.remove(event.getServer());
            }
            return;
        }
        if (hostLevel == null || !isHost(hostLevel.getBlockState(session.hostPos))) {
            release(player);
            return;
        }

        player.setInvulnerable(true);
        if (!player.isSpectator()) player.setGameMode(GameType.SPECTATOR);
        if (session.cameraId != null) {
            if (!networkAvailable(player, hostLevel)) {
                returnToHost(player);
            } else {
                FacilityCameraDefinition camera = currentCamera(player, session);
                if (camera == null) returnToHost(player);
                else {
                    Scp079ScreenState.setLocalActive(hostLevel,
                            session.hostPos, false);
                    anchorToCamera(player, camera, false);
                }
            }
        } else {
            anchorToHost(player, session, false);
            Scp079ScreenState.setLocalActive(hostLevel, session.hostPos, true);
        }
        if (++followTick % 5 == 0) sync(player, session);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.getServer() == null) return;
        Session session;
        synchronized (SESSIONS) {
            session = SESSIONS.get(player.getServer());
            if (session == null || !session.playerId.equals(player.getUUID())) return;
            SESSIONS.remove(player.getServer());
        }
        ServerLevel level = player.server.getLevel(session.hostDimension);
        if (level != null) {
            Scp079ScreenState.setLocalActive(level, session.hostPos, false);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        synchronized (SESSIONS) {
            SESSIONS.remove(event.getServer());
        }
    }

    private static final class Session {
        private final UUID playerId;
        private final ResourceKey<Level> hostDimension;
        private final BlockPos hostPos;
        private final ResourceKey<Level> originDimension;
        private final Vec3 origin;
        private final float originYaw;
        private final float originPitch;
        private final GameType originGameMode;
        private final boolean originInvulnerable;
        private UUID cameraId;

        private Session(UUID playerId, ResourceKey<Level> hostDimension,
                BlockPos hostPos, ResourceKey<Level> originDimension,
                Vec3 origin, float originYaw, float originPitch,
                GameType originGameMode, boolean originInvulnerable) {
            this.playerId = playerId;
            this.hostDimension = hostDimension;
            this.hostPos = hostPos;
            this.originDimension = originDimension;
            this.origin = origin;
            this.originYaw = originYaw;
            this.originPitch = originPitch;
            this.originGameMode = originGameMode;
            this.originInvulnerable = originInvulnerable;
        }
    }
}
