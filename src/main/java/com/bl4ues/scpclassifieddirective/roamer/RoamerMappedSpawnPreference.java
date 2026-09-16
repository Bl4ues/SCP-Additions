package com.bl4ues.scpclassifieddirective.roamer;

import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.safezone.SafeZoneManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Shared mapped-room preference for roamers whose natural placement checks are
 * centered around a player.
 *
 * <p>Facility Mapping is authoritative whenever at least one mapped floor patch
 * is close enough to participate in the encounter. The caller may reject sampled
 * positions for entity-specific collision or visibility rules, but it must not
 * fall back to arbitrary around-player placement while {@link Search#preferred()}
 * is true. This keeps authored facility cells authoritative instead of letting a
 * failed sample spill the encounter into an unmapped wall, exterior, or service
 * void.</p>
 */
public final class RoamerMappedSpawnPreference {
    private RoamerMappedSpawnPreference() {
    }

    public static Search search(ServerPlayer player, RandomSource random,
            double minDistance, double maxDistance,
            int scanUp, int scanDown, int attempts) {
        return search(player, random, minDistance, maxDistance,
                scanUp, scanDown, attempts, room -> true, false);
    }

    public static Search search(ServerPlayer player, RandomSource random,
            double minDistance, double maxDistance,
            int scanUp, int scanDown, int attempts,
            Predicate<FacilityRoomSnapshot> roomFilter,
            boolean requireEligibleMappedRoom) {
        if (player == null || random == null || maxDistance <= 0.0D
                || attempts <= 0) {
            return Search.NONE;
        }

        Predicate<FacilityRoomSnapshot> filter = roomFilter == null
                ? room -> true : roomFilter;
        ServerLevel level = player.serverLevel();
        double playerX = player.getX();
        double playerZ = player.getZ();
        int playerY = player.blockPosition().getY();
        double maxDistanceSqr = maxDistance * maxDistance;
        double minDistanceSqr = Math.max(0.0D,
                minDistance * minDistance);

        int horizontalMinX = Mth.floor(playerX - maxDistance);
        int horizontalMaxX = Mth.floor(playerX + maxDistance);
        int horizontalMinZ = Mth.floor(playerZ - maxDistance);
        int horizontalMaxZ = Mth.floor(playerZ + maxDistance);

        List<PatchWindow> windows = new ArrayList<>();
        boolean anyMappedPatchNearby = false;
        for (FacilityRoomSnapshot room :
                FacilityMappingManager.roomSnapshots(level)) {
            for (FacilityFloorPatch patch : room.patches()) {
                int spawnY = patch.y() + 1;
                int yOffset = spawnY - playerY;
                if (yOffset > scanUp || yOffset < -scanDown) continue;

                int minX = Math.max(patch.minX(), horizontalMinX);
                int maxX = Math.min(patch.maxX(), horizontalMaxX);
                int minZ = Math.max(patch.minZ(), horizontalMinZ);
                int maxZ = Math.min(patch.maxZ(), horizontalMaxZ);
                if (minX > maxX || minZ > maxZ) continue;

                if (distanceSqrToWindow(playerX, playerZ,
                        minX, maxX, minZ, maxZ) > maxDistanceSqr) {
                    continue;
                }

                anyMappedPatchNearby = true;
                if (!filter.test(room)) continue;

                /*
                 * Do not use the minimum encounter distance to decide whether
                 * mapping is authoritative. A small mapped room close to the
                 * player is still a mapped room; if it cannot provide a legal
                 * spawn, the encounter should fail this check rather than leak
                 * into an arbitrary unmapped location.
                 */
                long weight = ((long) maxX - minX + 1L)
                        * ((long) maxZ - minZ + 1L);
                windows.add(new PatchWindow(patch.y(), minX, maxX,
                        minZ, maxZ, Math.max(1L, weight)));
            }
        }

        if (windows.isEmpty()) {
            return requireEligibleMappedRoom && anyMappedPatchNearby
                    ? Search.MAPPED_EMPTY : Search.NONE;
        }

        List<BlockPos> candidates = new ArrayList<>();
        Set<BlockPos> unique = new HashSet<>();
        for (int attempt = 0; attempt < attempts; attempt++) {
            PatchWindow window = chooseWindow(windows, random);
            int x = randomBetween(random, window.minX(), window.maxX());
            int z = randomBetween(random, window.minZ(), window.maxZ());
            double dx = x + 0.5D - playerX;
            double dz = z + 0.5D - playerZ;
            double distanceSqr = dx * dx + dz * dz;
            if (distanceSqr < minDistanceSqr
                    || distanceSqr > maxDistanceSqr) {
                continue;
            }

            BlockPos floor = new BlockPos(x, window.floorY(), z);
            BlockPos spawn = floor.above();
            if (!level.hasChunkAt(floor)
                    || SafeZoneManager.contains(level, floor)
                    || SafeZoneManager.contains(level, spawn)
                    || !unique.add(spawn)) {
                continue;
            }
            candidates.add(spawn.immutable());
        }

        return new Search(true, List.copyOf(candidates));
    }

    public static boolean hasNearbyMapping(ServerPlayer player,
            double maxDistance, int scanUp, int scanDown) {
        if (player == null || maxDistance <= 0.0D) return false;

        ServerLevel level = player.serverLevel();
        double playerX = player.getX();
        double playerZ = player.getZ();
        int playerY = player.blockPosition().getY();
        double maxDistanceSqr = maxDistance * maxDistance;

        int horizontalMinX = Mth.floor(playerX - maxDistance);
        int horizontalMaxX = Mth.floor(playerX + maxDistance);
        int horizontalMinZ = Mth.floor(playerZ - maxDistance);
        int horizontalMaxZ = Mth.floor(playerZ + maxDistance);

        for (FacilityRoomSnapshot room :
                FacilityMappingManager.roomSnapshots(level)) {
            for (FacilityFloorPatch patch : room.patches()) {
                int spawnY = patch.y() + 1;
                int yOffset = spawnY - playerY;
                if (yOffset > scanUp || yOffset < -scanDown) continue;

                int minX = Math.max(patch.minX(), horizontalMinX);
                int maxX = Math.min(patch.maxX(), horizontalMaxX);
                int minZ = Math.max(patch.minZ(), horizontalMinZ);
                int maxZ = Math.min(patch.maxZ(), horizontalMaxZ);
                if (minX > maxX || minZ > maxZ) continue;

                if (distanceSqrToWindow(playerX, playerZ,
                        minX, maxX, minZ, maxZ) <= maxDistanceSqr) {
                    return true;
                }
            }
        }
        return false;
    }

    private static PatchWindow chooseWindow(List<PatchWindow> windows,
            RandomSource random) {
        long total = 0L;
        for (PatchWindow window : windows) {
            total = Math.min(Long.MAX_VALUE - window.weight(),
                    total) + window.weight();
        }
        long roll = Math.floorMod(random.nextLong(), Math.max(1L, total));
        for (PatchWindow window : windows) {
            if (roll < window.weight()) return window;
            roll -= window.weight();
        }
        return windows.get(windows.size() - 1);
    }

    private static double distanceSqrToWindow(double x, double z,
            int minX, int maxX, int minZ, int maxZ) {
        double nearestX = Math.max(minX + 0.5D,
                Math.min(x, maxX + 0.5D));
        double nearestZ = Math.max(minZ + 0.5D,
                Math.min(z, maxZ + 0.5D));
        double dx = nearestX - x;
        double dz = nearestZ - z;
        return dx * dx + dz * dz;
    }

    private static int randomBetween(RandomSource random, int min, int max) {
        if (max <= min) return min;
        return min + random.nextInt(max - min + 1);
    }

    public record Search(boolean preferred, List<BlockPos> candidates) {
        private static final Search NONE = new Search(false, List.of());
        private static final Search MAPPED_EMPTY = new Search(true, List.of());

        public Search {
            candidates = candidates == null ? List.of()
                    : List.copyOf(candidates);
        }
    }

    private record PatchWindow(int floorY, int minX, int maxX,
            int minZ, int maxZ, long weight) {
    }
}
