package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import com.bl4ues.scpclassifieddirective.network.Scp079PlayableNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side modal signal interruptions for playable SCP-079.
 *
 * Future camera shutdowns, EMP-like room effects and anomalous interference can
 * call this class rather than teaching each source how the 079 UI works. While
 * an interruption is active all remote-control packets are rejected server-side.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp079SignalInterruptionManager {
    public static final int KIND_TEMPORARY = 0;
    public static final int KIND_CAMERA_DISABLED = 1;
    public static final int KIND_HOST_DESTROYED = 2;
    public static final int CAMERA_FAILURE_TICKS = 40;
    public static final int HOST_FAILURE_TICKS = 40;

    private static final Map<UUID, Interruption> INTERRUPTIONS =
            new ConcurrentHashMap<>();
    private static final Map<UUID, ResourceKey<Level>> HOST_DIMENSIONS =
            new ConcurrentHashMap<>();

    private Scp079SignalInterruptionManager() { }

    /** Temporary no-signal state. The same camera resumes when the timer ends. */
    public static boolean temporarilyDisableSignal(ServerPlayer player,
            int durationTicks) {
        return begin(player, KIND_TEMPORARY, Math.max(1, durationTicks));
    }

    /** Camera feed was physically disabled. After two seconds 079 returns to map. */
    public static boolean disableCurrentCamera(ServerPlayer player) {
        if (player == null || !Scp079PlayableManager.isCameraMode(player)) {
            return false;
        }
        return begin(player, KIND_CAMERA_DISABLED, CAMERA_FAILURE_TICKS);
    }

    public static boolean controlsBlocked(ServerPlayer player) {
        if (player == null) return false;
        Interruption interruption = INTERRUPTIONS.get(player.getUUID());
        return interruption != null && player.server != null
                && player.server.getTickCount() < interruption.endTick;
    }

    private static boolean begin(ServerPlayer player, int kind,
            int durationTicks) {
        if (player == null || player.server == null
                || !Scp079PlayableManager.isController(player)) return false;
        int duration = Math.max(1, durationTicks);
        int now = player.server.getTickCount();
        INTERRUPTIONS.put(player.getUUID(), new Interruption(kind,
                now + duration));
        Scp079PlayableNetwork.sendSignalInterruption(player, kind, duration);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        ServerPlayer controller = Scp079PlayableManager.controller(server);

        if (controller == null) {
            HOST_DIMENSIONS.clear();
        } else {
            UUID controllerId = controller.getUUID();
            BlockPos host = Scp079PlayableManager.hostPosition(controller);
            ResourceKey<Level> hostDimension = HOST_DIMENSIONS.get(controllerId);

            if (host != null && hostDimension == null) {
                for (ServerLevel level : server.getAllLevels()) {
                    if (is079Host(level.getBlockState(host))) {
                        hostDimension = level.dimension();
                        HOST_DIMENSIONS.put(controllerId, hostDimension);
                        break;
                    }
                }
            }

            // Detect destruction before Scp079PlayableManager's normal END-tick
            // cleanup releases the role. The pending fatal transition deliberately
            // survives that release for its full two-second no-signal sequence.
            if (host != null && hostDimension != null
                    && !INTERRUPTIONS.containsKey(controllerId)) {
                ServerLevel hostLevel = server.getLevel(hostDimension);
                if (hostLevel == null || !is079Host(hostLevel.getBlockState(host))) {
                    Interruption failure = new Interruption(KIND_HOST_DESTROYED,
                            server.getTickCount() + HOST_FAILURE_TICKS);
                    INTERRUPTIONS.put(controllerId, failure);
                    Scp079PlayableNetwork.sendSignalInterruption(controller,
                            KIND_HOST_DESTROYED, HOST_FAILURE_TICKS);
                }
            }
        }

        int now = server.getTickCount();
        for (Map.Entry<UUID, Interruption> entry : INTERRUPTIONS.entrySet()) {
            Interruption interruption = entry.getValue();
            if (now < interruption.endTick) continue;
            UUID playerId = entry.getKey();
            if (!INTERRUPTIONS.remove(playerId, interruption)) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) continue;

            if (interruption.kind == KIND_CAMERA_DISABLED) {
                if (Scp079PlayableManager.isController(player)) {
                    Scp079PlayableManager.returnToHost(player);
                }
            } else if (interruption.kind == KIND_HOST_DESTROYED) {
                // The normal role manager may already have restored the player's
                // original mode after noticing the missing host. Kill only after
                // the authored no-signal window so vanilla death/spectate flow
                // takes over naturally.
                player.setInvulnerable(false);
                player.kill();
                HOST_DIMENSIONS.remove(playerId);
            }
        }
    }

    private static boolean is079Host(BlockState state) {
        return state != null && (state.is(ScpClassifiedDirectiveModBlocks.SCP_079ON.get())
                || state.is(ScpClassifiedDirectiveModBlocks.SCP_079OFF.get()));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        INTERRUPTIONS.remove(id);
        HOST_DIMENSIONS.remove(id);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        INTERRUPTIONS.clear();
        HOST_DIMENSIONS.clear();
    }

    private record Interruption(int kind, int endTick) { }
}
