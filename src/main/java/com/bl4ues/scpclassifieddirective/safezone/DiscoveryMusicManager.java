package com.bl4ues.scpclassifieddirective.safezone;

import com.bl4ues.scpclassifieddirective.facility.elevator.CoreRoomElevatorModule;
import com.bl4ues.scpclassifieddirective.safezone.network.SafeZoneNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Detects and distributes persistent, one-time discovery music cues. */
public final class DiscoveryMusicManager {
    private static final int CORE_ROOM_CHECK_INTERVAL = 10;
    private static final double CORE_ROOM_VIEW_DISTANCE = 64.0D;
    private static final double CORE_ROOM_VIEW_DISTANCE_SQR =
            CORE_ROOM_VIEW_DISTANCE * CORE_ROOM_VIEW_DISTANCE;
    private static final double MINIMUM_VIEW_DOT = 0.45D;
    private static final double[] STATION_SAMPLE_HEIGHTS = {0.6D, 1.5D, 2.4D};

    private DiscoveryMusicManager() {
    }

    public static void checkCoreRoomStation(ServerLevel level,
            BlockPos stationPos) {
        if (level == null || stationPos == null) return;
        long offset = Math.floorMod(stationPos.asLong(),
                CORE_ROOM_CHECK_INTERVAL);
        if (Math.floorMod(level.getGameTime() + offset,
                CORE_ROOM_CHECK_INTERVAL) != 0L) {
            return;
        }

        MinecraftServer server = level.getServer();
        DiscoveryMusicSavedData data = DiscoveryMusicSavedData.get(server);
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator()
                    || data.hasSeenCoreRoom(player.getUUID())
                    || !canSeeStation(level, player, stationPos)) {
                continue;
            }
            if (data.markCoreRoomSeen(player.getUUID())) {
                SafeZoneNetwork.sendDiscoveryCue(player,
                        Cue.CORE_ROOM_DISCOVERY);
            }
        }
    }

    public static void onScp131Followed(ServerLevel level) {
        if (level == null) return;
        MinecraftServer server = level.getServer();
        if (!DiscoveryMusicSavedData.get(server).markScp131Found()) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SafeZoneNetwork.sendDiscoveryCue(player, Cue.SCP_131_FOUND);
        }
    }

    private static boolean canSeeStation(ServerLevel level,
            ServerPlayer player, BlockPos stationPos) {
        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getViewVector(1.0F);
        for (double sampleHeight : STATION_SAMPLE_HEIGHTS) {
            Vec3 target = Vec3.atBottomCenterOf(stationPos)
                    .add(0.0D, sampleHeight, 0.0D);
            Vec3 difference = target.subtract(eye);
            double distanceSqr = difference.lengthSqr();
            if (distanceSqr > CORE_ROOM_VIEW_DISTANCE_SQR
                    || distanceSqr < 0.0001D
                    || view.dot(difference.normalize()) < MINIMUM_VIEW_DOT) {
                continue;
            }
            BlockHitResult hit = level.clip(new ClipContext(eye, target,
                    ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.MISS
                    || belongsToStation(level, hit.getBlockPos(),
                            stationPos)) {
                return true;
            }
        }
        return false;
    }

    private static boolean belongsToStation(ServerLevel level,
            BlockPos hitPos, BlockPos stationPos) {
        if (stationPos.equals(hitPos)) return true;
        BlockEntity blockEntity = level.getBlockEntity(hitPos);
        return blockEntity
                instanceof CoreRoomElevatorModule.StructurePartBlockEntity part
                && stationPos.equals(part.masterPos());
    }

    public enum Cue {
        CORE_ROOM_DISCOVERY,
        SCP_131_FOUND;

        public static Cue byNetworkId(int id) {
            Cue[] values = values();
            return id >= 0 && id < values.length ? values[id] : null;
        }
    }
}
