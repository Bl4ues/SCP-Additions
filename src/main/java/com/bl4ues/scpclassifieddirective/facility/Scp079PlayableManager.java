package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
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

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Server authority for the playable SCP-079 role.
 *
 * The final role selector intentionally lives outside this class. For development,
 * operators can sneak-use the physical SCP-079 with an empty hand to assume or
 * release control, which keeps this system testable before the selector UI exists.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp079PlayableManager {
    private static final Map<MinecraftServer, Session> SESSIONS =
            new WeakHashMap<>();
    private static int followTick;

    private Scp079PlayableManager() {
    }

    public static boolean hasController(MinecraftServer server) {
        synchronized (SESSIONS) {
            return server != null && SESSIONS.containsKey(server);
        }
    }

    public static boolean isController(ServerPlayer player) {
        if (player == null || player.getServer() == null) return false;
        synchronized (SESSIONS) {
            Session session = SESSIONS.get(player.getServer());
            return session != null && session.playerId.equals(player.getUUID());
        }
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

    public static BlockPos hostPosition(ServerPlayer player) {
        Session session = session(player);
        return session == null ? null : session.hostPos;
    }

    public static ServerLevel hostLevel(ServerPlayer player) {
        Session session = session(player);
        return session == null ? null : player.server.getLevel(session.hostDimension);
    }

    private static Session session(ServerPlayer player) {
        if (player == null || player.getServer() == null) return null;
        synchronized (SESSIONS) {
            Session session = SESSIONS.get(player.getServer());
            return session != null && session.playerId.equals(player.getUUID())
                    ? session : null;
        }
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
        boolean wrongDimension = player.level().dimension()
                != session.hostDimension;
        if (force || wrongDimension || player.position().distanceToSqr(anchor) > 0.16D) {
            player.teleportTo(level, anchor.x, anchor.y, anchor.z,
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
        boolean network = Scp079FacilityAccessManager.hasFacilityAccess(level)
                && auxiliary;
        Scp079PlayableNetwork.sendState(player, session.hostDimension,
                session.hostPos, power, auxiliary, network);
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
        ServerLevel level = event.getServer().getLevel(session.hostDimension);
        if (player == null) {
            if (level != null) {
                Scp079ScreenState.setLocalActive(level, session.hostPos, false);
            }
            synchronized (SESSIONS) {
                SESSIONS.remove(event.getServer());
            }
            return;
        }
        if (level == null || !isHost(level.getBlockState(session.hostPos))) {
            release(player);
            return;
        }

        player.setInvulnerable(true);
        if (!player.isSpectator()) player.setGameMode(GameType.SPECTATOR);
        anchorToHost(player, session, false);
        Scp079ScreenState.setLocalActive(level, session.hostPos, true);
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
        private final java.util.UUID playerId;
        private final ResourceKey<Level> hostDimension;
        private final BlockPos hostPos;
        private final ResourceKey<Level> originDimension;
        private final Vec3 origin;
        private final float originYaw;
        private final float originPitch;
        private final GameType originGameMode;
        private final boolean originInvulnerable;

        private Session(java.util.UUID playerId,
                ResourceKey<Level> hostDimension, BlockPos hostPos,
                ResourceKey<Level> originDimension, Vec3 origin,
                float originYaw, float originPitch, GameType originGameMode,
                boolean originInvulnerable) {
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
