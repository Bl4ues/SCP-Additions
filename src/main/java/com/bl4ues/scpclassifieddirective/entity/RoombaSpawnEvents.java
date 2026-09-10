package com.bl4ues.scpclassifieddirective.entity;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModEntities;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModGameRules;
import com.bl4ues.scpclassifieddirective.safezone.SafeZoneManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Sparse SCP: Unity-inspired Roomba encounters driven by Facility Mapping.
 *
 * <p>Unnamed mapped rooms are eligible by default, while rooms explicitly named
 * as corridors or hallways remain eligible and receive a small selection bias.
 * Safe Zones, Sublevel 3, Heavy Containment and Super Heavy Containment are
 * always excluded. Standard LCZ labels keep the Unity-style SL1/SL2 split,
 * Entrance Zone uses the same frequency tier as SL1, and other layouts fall
 * back to a height-based preference for upper mapped floors.</p>
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RoombaSpawnEvents {
    private static final int CHECK_INTERVAL_TICKS = 1_800;
    private static final int CHECK_JITTER_TICKS = 600;

    /*
     * The highest-frequency tier keeps the old 1/80 check rate. Lower tiers
     * become progressively rarer instead of making SL1 four times more common
     * than it was before Facility Mapping became the source of truth.
     */
    private static final int PRIMARY_CHANCE_SCALE = 320;
    private static final int MAX_FREQUENCY_WEIGHT = 4;
    private static final int CORRIDOR_SELECTION_MULTIPLIER = 2;
    private static final int PAIR_CHANCE_BOUND = 64;
    private static final int SEARCH_ATTEMPTS = 48;
    private static final int PAIR_SEARCH_ATTEMPTS = 28;
    private static final int MIN_SEARCH_RADIUS = 8;
    private static final int MAX_SEARCH_RADIUS = 22;
    private static final double LOCAL_EXCLUSION_RADIUS = 18.0D;
    private static final double REGIONAL_RADIUS = 72.0D;
    private static final int REGIONAL_CAP = 2;
    private static final double SAFE_ROOM_HEIGHT = 3.0D;

    private static final Map<UUID, Long> NEXT_CHECK = new HashMap<>();

    private RoombaSpawnEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)
                || player.isCreative() || player.isSpectator()
                || !(player.level() instanceof ServerLevel level)
                || !level.getGameRules().getBoolean(
                ScpClassifiedDirectiveModGameRules.ROOMBA_SPAWN)) {
            return;
        }

        long gameTime = level.getGameTime();
        long due = NEXT_CHECK.computeIfAbsent(player.getUUID(), ignored ->
                gameTime + CHECK_INTERVAL_TICKS
                        + player.getRandom().nextInt(CHECK_JITTER_TICKS + 1));
        if (gameTime < due) return;

        NEXT_CHECK.put(player.getUUID(), gameTime + CHECK_INTERVAL_TICKS
                + player.getRandom().nextInt(CHECK_JITTER_TICKS + 1));

        RandomSource random = player.getRandom();
        int regionalCount = countRoombas(level, player.getX(), player.getY(),
                player.getZ(), REGIONAL_RADIUS);
        if (countRoombas(level, player.getX(), player.getY(), player.getZ(),
                LOCAL_EXCLUSION_RADIUS) > 0 || regionalCount >= REGIONAL_CAP) {
            return;
        }

        Optional<MappedFloor> floor = findPrimaryFloor(level, player, random);
        if (floor.isEmpty()) return;

        MappedFloor mapped = floor.get();
        if (!passesFrequencyRoll(random, mapped.frequencyWeight())) return;

        RoombaEntity primary = spawnAt(level, mapped.floor(), random);
        if (primary == null || regionalCount > 0
                || !isUnitySublevelOne(mapped.room())
                || random.nextInt(PAIR_CHANCE_BOUND) != 0) {
            return;
        }

        findPairFloor(level, mapped, random)
                .ifPresent(pairFloor -> spawnAt(level, pairFloor, random));
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        NEXT_CHECK.remove(event.getEntity().getUUID());
    }

    private static Optional<MappedFloor> findPrimaryFloor(ServerLevel level,
            ServerPlayer player, RandomSource random) {
        List<WeightedPatch> patches = nearbyMappedPatches(level, player);
        if (patches.isEmpty()) return Optional.empty();

        for (int attempt = 0; attempt < SEARCH_ATTEMPTS; attempt++) {
            WeightedPatch selected = weightedPatch(patches, random);
            FacilityFloorPatch patch = selected.patch();
            int x = randomBetween(random, patch.minX(), patch.maxX());
            int z = randomBetween(random, patch.minZ(), patch.maxZ());
            BlockPos floor = new BlockPos(x, patch.y(), z);
            double dx = x + 0.5D - player.getX();
            double dz = z + 0.5D - player.getZ();
            double distanceSqr = dx * dx + dz * dz;
            if (distanceSqr < MIN_SEARCH_RADIUS * MIN_SEARCH_RADIUS
                    || distanceSqr > MAX_SEARCH_RADIUS * MAX_SEARCH_RADIUS
                    || !isUsableMappedFloor(level, floor)) {
                continue;
            }
            if (countRoombas(level, x + 0.5D, floor.getY() + 1.0D,
                    z + 0.5D, LOCAL_EXCLUSION_RADIUS) == 0) {
                return Optional.of(new MappedFloor(floor.immutable(),
                        selected.room(), selected.frequencyWeight()));
            }
        }
        return Optional.empty();
    }

    private static List<WeightedPatch> nearbyMappedPatches(ServerLevel level,
            ServerPlayer player) {
        List<FacilityRoomSnapshot> eligibleRooms = new ArrayList<>();
        for (FacilityRoomSnapshot room : FacilityMappingManager.roomSnapshots(level)) {
            if (isEligibleRoom(level, room)) eligibleRooms.add(room);
        }
        if (eligibleRooms.isEmpty()) return List.of();

        Map<Integer, Integer> customElevationWeights =
                customElevationWeights(eligibleRooms);
        List<WeightedPatch> result = new ArrayList<>();
        for (FacilityRoomSnapshot room : eligibleRooms) {
            int roomMultiplier = isCorridorOrHallway(room.name())
                    ? CORRIDOR_SELECTION_MULTIPLIER : 1;
            for (FacilityFloorPatch patch : room.patches()) {
                int frequencyWeight = frequencyWeight(room, patch,
                        customElevationWeights);
                if (frequencyWeight <= 0
                        || Math.abs((patch.y() + 1.0D) - player.getY()) > 5.0D
                        || distanceSqrToPatch(player.getX(), player.getZ(), patch)
                        > MAX_SEARCH_RADIUS * MAX_SEARCH_RADIUS) {
                    continue;
                }
                long area = Math.max(1L, Math.min(96L, patch.area()));
                result.add(new WeightedPatch(room, patch,
                        area * roomMultiplier, frequencyWeight));
            }
        }
        return result;
    }

    private static boolean isEligibleRoom(ServerLevel level,
            FacilityRoomSnapshot room) {
        if (room == null || room.patches().isEmpty()
                || intersectsSafeZone(level, room)) {
            return false;
        }

        String labels = floorLabels(room);
        if (isExcludedLocation(labels)) return false;

        String name = room.name() == null ? "" : room.name().strip();
        return name.isBlank() || isCorridorOrHallway(name);
    }

    /**
     * A Safe Zone anywhere in the room's normal walkable height excludes the
     * whole mapped room. This prevents a room marked safe from still producing a
     * Roomba just outside the exact Safe Zone selection boundary.
     */
    private static boolean intersectsSafeZone(ServerLevel level,
            FacilityRoomSnapshot room) {
        for (FacilityFloorPatch patch : room.patches()) {
            AABB walkableRoom = new AABB(patch.minX(), patch.y(), patch.minZ(),
                    patch.maxX() + 1.0D, patch.y() + SAFE_ROOM_HEIGHT,
                    patch.maxZ() + 1.0D);
            if (SafeZoneManager.intersects(level, walkableRoom)) return true;
        }
        return false;
    }

    private static int frequencyWeight(FacilityRoomSnapshot room,
            FacilityFloorPatch patch, Map<Integer, Integer> customWeights) {
        String labels = floorLabels(room);
        if (isExcludedLocation(labels)) return 0;
        if (isEntranceZone(labels)) return MAX_FREQUENCY_WEIGHT;
        if (isStandardLcz(labels)) {
            if (matchesSublevel(labels, 1)) return MAX_FREQUENCY_WEIGHT;
            if (matchesSublevel(labels, 2)) return 1;
            return 0;
        }
        return customWeights.getOrDefault(patch.y(), 1);
    }

    /**
     * For non-LCZ/non-EZ maps, distinct floor elevations form four frequency
     * tiers: highest = 4, next = 3, next = 2, and every lower floor = 1.
     */
    private static Map<Integer, Integer> customElevationWeights(
            List<FacilityRoomSnapshot> rooms) {
        Set<Integer> elevations = new LinkedHashSet<>();
        rooms.stream()
                .filter(room -> {
                    String labels = floorLabels(room);
                    return !isStandardLcz(labels) && !isEntranceZone(labels);
                })
                .flatMap(room -> room.patches().stream())
                .map(FacilityFloorPatch::y)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .forEach(elevations::add);

        Map<Integer, Integer> weights = new HashMap<>();
        int rank = 0;
        for (int y : elevations) {
            weights.put(y, Math.max(1, MAX_FREQUENCY_WEIGHT - rank));
            rank++;
        }
        return weights;
    }

    private static boolean passesFrequencyRoll(RandomSource random, int weight) {
        int clamped = Math.max(1, Math.min(MAX_FREQUENCY_WEIGHT, weight));
        int bound = Math.max(1,
                (PRIMARY_CHANCE_SCALE + clamped - 1) / clamped);
        return random.nextInt(bound) == 0;
    }

    private static WeightedPatch weightedPatch(List<WeightedPatch> patches,
            RandomSource random) {
        long total = 0L;
        for (WeightedPatch patch : patches) total += patch.selectionWeight();
        long roll = Math.floorMod(random.nextLong(), Math.max(1L, total));
        for (WeightedPatch patch : patches) {
            if (roll < patch.selectionWeight()) return patch;
            roll -= patch.selectionWeight();
        }
        return patches.get(patches.size() - 1);
    }

    private static Optional<BlockPos> findPairFloor(ServerLevel level,
            MappedFloor primary, RandomSource random) {
        List<FacilityFloorPatch> patches = primary.room().patches();
        if (patches.isEmpty()) return Optional.empty();
        BlockPos origin = primary.floor();

        for (int attempt = 0; attempt < PAIR_SEARCH_ATTEMPTS; attempt++) {
            FacilityFloorPatch patch = patches.get(random.nextInt(patches.size()));
            int x = randomBetween(random, patch.minX(), patch.maxX());
            int z = randomBetween(random, patch.minZ(), patch.maxZ());
            BlockPos floor = new BlockPos(x, patch.y(), z);
            double dx = x - origin.getX();
            double dz = z - origin.getZ();
            double distanceSqr = dx * dx + dz * dz;
            if (distanceSqr < 4.0D || distanceSqr > 25.0D
                    || Math.abs(floor.getY() - origin.getY()) > 1
                    || !isUsableMappedFloor(level, floor)) {
                continue;
            }
            if (countRoombas(level, x + 0.5D, floor.getY() + 1.0D,
                    z + 0.5D, 1.5D) == 0) {
                return Optional.of(floor.immutable());
            }
        }
        return Optional.empty();
    }

    private static boolean isUsableMappedFloor(ServerLevel level,
            BlockPos floor) {
        if (!level.hasChunkAt(floor)) return false;
        BlockState state = level.getBlockState(floor);
        if (state.isAir() || !state.getFluidState().isEmpty()
                || !state.isFaceSturdy(level, floor, Direction.UP)) {
            return false;
        }
        BlockPos spawn = floor.above();
        if (SafeZoneManager.contains(level, floor)
                || SafeZoneManager.contains(level, spawn)) {
            return false;
        }
        return level.getFluidState(spawn).isEmpty()
                && level.getBlockState(spawn).getCollisionShape(level, spawn)
                        .isEmpty()
                && level.getBlockState(spawn.above()).getCollisionShape(
                        level, spawn.above()).isEmpty();
    }

    private static boolean isUnitySublevelOne(FacilityRoomSnapshot room) {
        String labels = floorLabels(room);
        return isStandardLcz(labels) && matchesSublevel(labels, 1);
    }

    private static boolean isExcludedLocation(String labels) {
        return matchesSublevel(labels, 3)
                || isHeavyContainmentZone(labels)
                || isSuperHeavyContainmentZone(labels);
    }

    private static boolean isStandardLcz(String labels) {
        return labels.contains("light containment zone")
                || token(labels, "lcz");
    }

    private static boolean isEntranceZone(String labels) {
        return labels.contains("entrance zone") || token(labels, "ez");
    }

    private static boolean isHeavyContainmentZone(String labels) {
        return labels.contains("heavy containment zone")
                || token(labels, "hcz");
    }

    private static boolean isSuperHeavyContainmentZone(String labels) {
        return labels.contains("super heavy containment zone")
                || token(labels, "shcz");
    }

    private static boolean isCorridorOrHallway(String roomName) {
        if (roomName == null || roomName.isBlank()) return false;
        String name = roomName.toLowerCase(Locale.ROOT);
        return name.contains("corridor") || name.contains("hallway");
    }

    private static String floorLabels(FacilityRoomSnapshot room) {
        if (room == null) return "";
        return ((room.floorLongLabel() == null ? "" : room.floorLongLabel())
                + " " + (room.floorShortLabel() == null ? ""
                : room.floorShortLabel())).strip().toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private static boolean matchesSublevel(String text, int number) {
        String n = Integer.toString(number);
        return text.contains("sublevel " + n)
                || text.contains("sublevel-" + n)
                || text.contains("sublevel_" + n)
                || text.contains("sl " + n)
                || text.contains("sl-" + n)
                || text.contains("sl_" + n)
                || text.contains("sl" + n);
    }

    private static boolean token(String value, String token) {
        int index = -1;
        while ((index = value.indexOf(token, index + 1)) >= 0) {
            boolean left = index == 0
                    || !Character.isLetterOrDigit(value.charAt(index - 1));
            int end = index + token.length();
            boolean right = end >= value.length()
                    || !Character.isLetterOrDigit(value.charAt(end));
            if (left && right) return true;
        }
        return false;
    }

    private static double distanceSqrToPatch(double x, double z,
            FacilityFloorPatch patch) {
        double nearestX = Math.max(patch.minX(),
                Math.min(x, patch.maxX() + 1.0D));
        double nearestZ = Math.max(patch.minZ(),
                Math.min(z, patch.maxZ() + 1.0D));
        double dx = nearestX - x;
        double dz = nearestZ - z;
        return dx * dx + dz * dz;
    }

    private static int randomBetween(RandomSource random, int min, int max) {
        if (max <= min) return min;
        return min + random.nextInt(max - min + 1);
    }

    private static RoombaEntity spawnAt(ServerLevel level, BlockPos floor,
            RandomSource random) {
        EntityType<RoombaEntity> type = ScpClassifiedDirectiveModEntities.ROOMBA.get();
        RoombaEntity roomba = type.create(level);
        if (roomba == null) return null;

        double x = floor.getX() + 0.5D;
        double y = floor.getY() + 1.001D;
        double z = floor.getZ() + 0.5D;
        float yaw = random.nextFloat() * 360.0F;
        roomba.moveTo(x, y, z, yaw, 0.0F);
        roomba.setYBodyRot(yaw);
        roomba.setYHeadRot(yaw);
        roomba.getPersistentData().putBoolean("NaturalRoomba", true);
        if (!level.noCollision(roomba, roomba.getBoundingBox())) {
            roomba.discard();
            return null;
        }
        level.addFreshEntity(roomba);
        return roomba;
    }

    private static int countRoombas(ServerLevel level, double x, double y,
            double z, double radius) {
        AABB area = new AABB(x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius);
        return level.getEntitiesOfClass(RoombaEntity.class, area,
                entity -> entity.isAlive() && !entity.isRemoved()).size();
    }

    private record WeightedPatch(FacilityRoomSnapshot room,
            FacilityFloorPatch patch, long selectionWeight,
            int frequencyWeight) {
    }

    private record MappedFloor(BlockPos floor, FacilityRoomSnapshot room,
            int frequencyWeight) {
    }
}
