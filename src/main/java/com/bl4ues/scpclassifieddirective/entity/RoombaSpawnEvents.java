package com.bl4ues.scpclassifieddirective.entity;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorStationIndex;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoom;
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
import java.util.HashSet;
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
 * <p>Unnamed mapped rooms are eligible by default, while Unity-style transit
 * rooms (corridors, hallways, corners and threeways) remain eligible and receive
 * a small selection bias. Rooms occupied by players and directly adjacent rooms
 * are excluded. Existing natural Roombas reserve only their authored spawn cell,
 * so connected/consecutive mapped rooms remain valid. Safe Zones, Sublevel 3,
 * Heavy Containment and Super Heavy Containment are
 * always excluded. Standard LCZ labels keep the Unity-style SL1/SL2 split,
 * Entrance Zone uses the same frequency tier as SL1, and other layouts fall
 * back to a height-based preference for upper mapped floors.</p>
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RoombaSpawnEvents {
    private static final int CHECK_INTERVAL_TICKS = 1_200;
    private static final int CHECK_JITTER_TICKS = 600;

    /*
     * Unity lets Roombas appear in several SL1 hallway/corner/three-way rooms,
     * while SL2 has only the fan hallway. Preserve that strong SL1 bias without
     * making a single Roomba a multi-hour event: the highest tier is 1/16 per
     * 60-90 second check (~20 minutes for one eligible player), while mapped
     * room occupancy and the cell-density budget keep them sparse.
     */
    private static final int PRIMARY_CHANCE_SCALE = 64;
    private static final int MAX_FREQUENCY_WEIGHT = 4;
    private static final int CORRIDOR_SELECTION_MULTIPLIER = 2;
    private static final int PAIR_CHANCE_BOUND = 64;
    private static final int SEARCH_ATTEMPTS = 48;
    private static final int PAIR_SEARCH_ATTEMPTS = 28;
    private static final int MIN_SEARCH_RADIUS = 8;
    private static final int MAX_SEARCH_RADIUS = 64;
    private static final double SAFE_ROOM_HEIGHT = 3.0D;
    private static final int ADJACENT_ROOM_GAP = 2;

    /*
     * Natural Roomba density is defined by authored Facility Mapping floors,
     * never an arbitrary block radius. Consecutive/adjacent eligible rooms may
     * each contain a Roomba; only the room itself is reserved.
     *
     * At the commonest tier (weight 4), one natural slot is granted per five
     * eligible rooms. Rarer floors scale that budget down with the same weight
     * used by the encounter roll: weight 3 ~= one per 6.7 rooms, weight 2 one
     * per 10, and weight 1 one per 20. Very large authored floors therefore
     * gain proportionally more slots while preserving the same sparse
     * room-density target instead of hitting an arbitrary absolute ceiling.
     */
    private static final int ROOMS_PER_NATURAL_ROOMBA = 5;
    private static final String NATURAL_ROOMBA_ROOM_TAG =
            "NaturalRoombaRoom";
    private static final String NATURAL_ROOMBA_FLOOR_Y_TAG =
            "NaturalRoombaFloorY";

    private static final Map<UUID, Long> NEXT_CHECK = new HashMap<>();

    private RoombaSpawnEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)
                || player.isSpectator()
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
        SpawnPool pool = nearbyMappedPatches(level, player);
        if (pool.patches().isEmpty()) return;

        Optional<MappedFloor> floor =
                findPrimaryFloor(level, player, random, pool);
        if (floor.isEmpty()) return;

        MappedFloor mapped = floor.get();
        FloorBudget budget = pool.floorBudgets().get(mapped.floorKey());
        if (budget == null || budget.full()
                || !passesFrequencyRoll(random, mapped.frequencyWeight())) {
            return;
        }

        RoombaEntity primary = spawnAt(level, mapped.floor(), random,
                mapped.room().id(), mapped.floor().getY());
        if (primary == null
                || budget.naturalRoombaCount() > 0
                || budget.capacity() < 2
                || budget.naturalRoombaCount() + 1 >= budget.capacity()
                || !isUnitySublevelOne(mapped.room())
                || random.nextInt(PAIR_CHANCE_BOUND) != 0) {
            return;
        }

        findPairFloor(level, mapped, random)
                .ifPresent(pairFloor -> spawnAt(level, pairFloor, random,
                        mapped.room().id(), pairFloor.getY()));
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        NEXT_CHECK.remove(event.getEntity().getUUID());
    }

    private static Optional<MappedFloor> findPrimaryFloor(ServerLevel level,
            ServerPlayer player, RandomSource random, SpawnPool pool) {
        List<WeightedPatch> patches = pool.patches();
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
            FloorBudget budget = pool.floorBudgets()
                    .get(selected.floorKey());
            if (distanceSqr < MIN_SEARCH_RADIUS * MIN_SEARCH_RADIUS
                    || distanceSqr > MAX_SEARCH_RADIUS * MAX_SEARCH_RADIUS
                    || budget == null || budget.full()
                    || pool.occupiedNaturalRooms()
                            .contains(selected.room().id())
                    || !isUsableMappedFloor(level, floor)) {
                continue;
            }
            return Optional.of(new MappedFloor(floor.immutable(),
                    selected.room(), selected.frequencyWeight(),
                    selected.floorKey()));
        }
        return Optional.empty();
    }

    private static SpawnPool nearbyMappedPatches(ServerLevel level,
            ServerPlayer player) {
        List<FacilityRoomSnapshot> rooms =
                FacilityMappingManager.roomSnapshots(level);
        if (rooms.isEmpty()) return SpawnPool.EMPTY;

        /*
         * The density budget is based on the authored floor itself, not the
         * player's current search radius. Temporary player occupancy can make a
         * room unavailable for this check, but it must never shrink the floor's
         * configured Roomba capacity.
         */
        List<FacilityRoomSnapshot> globallyEligibleRooms = new ArrayList<>();
        for (FacilityRoomSnapshot room : rooms) {
            if (isEligibleRoom(level, room)) globallyEligibleRooms.add(room);
        }
        if (globallyEligibleRooms.isEmpty()) return SpawnPool.EMPTY;

        Map<Integer, Integer> customElevationWeights =
                customElevationWeights(globallyEligibleRooms);
        FloorCatalog catalog = floorCatalog(level, globallyEligibleRooms,
                customElevationWeights);
        if (catalog.budgets().isEmpty()) return SpawnPool.EMPTY;

        /*
         * Roombas are ambient facility life, not jumpscare spawns. Never create
         * one in a room currently occupied by any non-spectating player or in a
         * room directly adjacent to one. This applies equally to Survival and
         * Creative players, so builders/testers do not watch a cleaner pop into
         * existence in the room beside them.
         */
        Set<UUID> excludedRooms = occupiedAndAdjacentRooms(level, rooms);

        List<WeightedPatch> result = new ArrayList<>();
        for (FacilityRoomSnapshot room : globallyEligibleRooms) {
            if (excludedRooms.contains(room.id())
                    || catalog.occupiedNaturalRooms().contains(room.id())) {
                continue;
            }

            int roomMultiplier = isUnityTransitRoom(room.name())
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

                FloorKey floorKey = floorKey(room, patch);
                FloorBudget budget = catalog.budgets().get(floorKey);
                if (budget == null || budget.full()) continue;

                long area = Math.max(1L, Math.min(96L, patch.area()));
                result.add(new WeightedPatch(room, patch,
                        area * roomMultiplier, frequencyWeight, floorKey));
            }
        }
        if (result.isEmpty()) {
            return new SpawnPool(List.of(),
                    catalog.occupiedNaturalRooms(), catalog.budgets());
        }

        return new SpawnPool(List.copyOf(result),
                catalog.occupiedNaturalRooms(), catalog.budgets());
    }

    /**
     * Builds one installation-wide budget per mapped floor.
     *
     * <p>Capacity uses distinct eligible rooms, not block area and not a radius
     * around the checking player. Thus two consecutive mapped rooms can both
     * host cleaners whenever the floor has budget for them, while a large floor
     * can never asymptotically fill every room.</p>
     */
    private static FloorCatalog floorCatalog(ServerLevel level,
            List<FacilityRoomSnapshot> eligibleRooms,
            Map<Integer, Integer> customElevationWeights) {
        Map<FloorKey, FloorAccumulator> accumulators = new HashMap<>();
        Map<UUID, FacilityRoomSnapshot> roomsById = new HashMap<>();

        for (FacilityRoomSnapshot room : eligibleRooms) {
            roomsById.put(room.id(), room);
            for (FacilityFloorPatch patch : room.patches()) {
                int weight = frequencyWeight(room, patch,
                        customElevationWeights);
                if (weight <= 0) continue;
                FloorKey key = floorKey(room, patch);
                FloorAccumulator accumulator = accumulators.computeIfAbsent(
                        key, ignored -> new FloorAccumulator());
                accumulator.roomIds.add(room.id());
                accumulator.frequencyWeight = Math.max(
                        accumulator.frequencyWeight, weight);
            }
        }

        Map<FloorKey, Integer> naturalCounts = new HashMap<>();
        Set<UUID> occupiedRooms = new HashSet<>();
        for (var entity : level.getAllEntities()) {
            if (!(entity instanceof RoombaEntity roomba)
                    || !roomba.isAlive() || roomba.isRemoved()
                    || !roomba.getPersistentData()
                            .getBoolean("NaturalRoomba")) {
                continue;
            }

            UUID roomId = naturalSpawnRoom(level, roomba);
            FacilityRoomSnapshot room = roomId == null
                    ? null : roomsById.get(roomId);
            if (room == null) continue;

            FloorKey key = naturalSpawnFloorKey(roomba, room);
            if (key == null || !accumulators.containsKey(key)) continue;
            naturalCounts.merge(key, 1, Integer::sum);
            occupiedRooms.add(roomId);
        }

        Map<FloorKey, FloorBudget> budgets = new HashMap<>();
        for (Map.Entry<FloorKey, FloorAccumulator> entry
                : accumulators.entrySet()) {
            FloorAccumulator accumulator = entry.getValue();
            int roomCount = accumulator.roomIds.size();
            int weight = Math.max(1, Math.min(MAX_FREQUENCY_WEIGHT,
                    accumulator.frequencyWeight));
            int capacity = floorCapacity(roomCount, weight);
            budgets.put(entry.getKey(), new FloorBudget(
                    roomCount, weight,
                    naturalCounts.getOrDefault(entry.getKey(), 0),
                    capacity));
        }

        return new FloorCatalog(Map.copyOf(budgets),
                Set.copyOf(occupiedRooms));
    }

    private static int floorCapacity(int eligibleRoomCount,
            int frequencyWeight) {
        if (eligibleRoomCount <= 0 || frequencyWeight <= 0) return 0;

        int weight = Math.max(1, Math.min(MAX_FREQUENCY_WEIGHT,
                frequencyWeight));
        long numerator = (long) eligibleRoomCount * weight;
        long denominator = (long) ROOMS_PER_NATURAL_ROOMBA
                * MAX_FREQUENCY_WEIGHT;
        return (int) Math.max(1L,
                (numerator + denominator - 1L) / denominator);
    }

    private static FloorKey naturalSpawnFloorKey(RoombaEntity roomba,
            FacilityRoomSnapshot room) {
        if (room == null || room.patches().isEmpty()) return null;

        int authoredY = roomba.getPersistentData().contains(
                NATURAL_ROOMBA_FLOOR_Y_TAG)
                ? roomba.getPersistentData().getInt(
                        NATURAL_ROOMBA_FLOOR_Y_TAG)
                : roomba.blockPosition().getY() - 1;

        FacilityFloorPatch nearest = null;
        int bestDistance = Integer.MAX_VALUE;
        for (FacilityFloorPatch patch : room.patches()) {
            int distance = Math.abs(patch.y() - authoredY);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = patch;
            }
        }
        return nearest == null ? null : floorKey(room, nearest);
    }

    private static FloorKey floorKey(FacilityRoomSnapshot room,
            FacilityFloorPatch patch) {
        if (room.floorStation() != null) {
            return new FloorKey("station:"
                    + room.floorStation().asLong());
        }

        String labels = floorLabels(room);
        if (!labels.isBlank()) {
            return new FloorKey("label:" + labels);
        }
        return new FloorKey("y:" + patch.y());
    }

    private static UUID naturalSpawnRoom(ServerLevel level,
            RoombaEntity roomba) {
        String stored = roomba.getPersistentData()
                .getString(NATURAL_ROOMBA_ROOM_TAG);
        if (!stored.isBlank()) {
            try {
                return UUID.fromString(stored);
            } catch (IllegalArgumentException ignored) {
                // Legacy/corrupt metadata falls through to current mapping.
            }
        }

        FacilityRoom room = FacilityMappingManager.roomForPosition(
                level, roomba.blockPosition());
        return room == null ? null : room.id();
    }

    private static Set<UUID> occupiedAndAdjacentRooms(ServerLevel level,
            List<FacilityRoomSnapshot> rooms) {
        Set<UUID> occupied = new HashSet<>();
        for (ServerPlayer other : level.players()) {
            if (other.isSpectator()) continue;
            BlockPos pos = other.blockPosition();
            for (FacilityRoomSnapshot room : rooms) {
                if (room.containsColumn(pos)) occupied.add(room.id());
            }
        }
        if (occupied.isEmpty()) return Set.of();

        Set<UUID> excluded = new HashSet<>(occupied);
        for (FacilityRoomSnapshot room : rooms) {
            if (excluded.contains(room.id())) continue;
            for (FacilityRoomSnapshot occupiedRoom : rooms) {
                if (!occupied.contains(occupiedRoom.id())) continue;
                if (adjacent(room, occupiedRoom)) {
                    excluded.add(room.id());
                    break;
                }
            }
        }
        return Set.copyOf(excluded);
    }

    private static boolean adjacent(FacilityRoomSnapshot a,
            FacilityRoomSnapshot b) {
        if (a == null || b == null || a.id().equals(b.id())) return false;

        /*
         * Adjacency is physical, not semantic. Two touching mapped rooms may
         * legitimately resolve to different Floor Station labels (or one may
         * still be unassigned). Using labels here allowed Roombas to spawn in a
         * literally adjacent room just because its metadata differed.
         */
        for (FacilityFloorPatch pa : a.patches()) {
            for (FacilityFloorPatch pb : b.patches()) {
                if (Math.abs(pa.y() - pb.y()) > 3) continue;
                int gapX = intervalGap(pa.minX(), pa.maxX(),
                        pb.minX(), pb.maxX());
                int gapZ = intervalGap(pa.minZ(), pa.maxZ(),
                        pb.minZ(), pb.maxZ());
                if (Math.max(gapX, gapZ) <= ADJACENT_ROOM_GAP) return true;
            }
        }
        return false;
    }

    private static int intervalGap(int aMin, int aMax,
            int bMin, int bMax) {
        if (aMax < bMin) return bMin - aMax - 1;
        if (bMax < aMin) return aMin - bMax - 1;
        return 0;
    }

    private static boolean isEligibleRoom(ServerLevel level,
            FacilityRoomSnapshot room) {
        if (room == null || room.patches().isEmpty()
                || intersectsSafeZone(level, room)
                || containsElevatorFloorStation(level, room)) {
            return false;
        }

        String labels = floorLabels(room);
        if (isExcludedLocation(labels)) return false;

        String name = room.name() == null ? "" : room.name().strip();
        return name.isBlank() || isUnityTransitRoom(name);
    }

    private static boolean containsElevatorFloorStation(
            ServerLevel level, FacilityRoomSnapshot room) {
        for (BlockPos station : FacilityFloorStationIndex.positions(level)) {
            if (room.containsColumn(station)) return true;
        }
        return false;
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
            if (countNearbyRoombas(level, x + 0.5D, floor.getY() + 1.0D,
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

    private static boolean isUnityTransitRoom(String roomName) {
        if (roomName == null || roomName.isBlank()) return false;
        String name = roomName.toLowerCase(Locale.ROOT);
        return name.contains("corridor")
                || name.contains("hallway")
                || name.contains("corner")
                || name.contains("threeway")
                || name.contains("three-way")
                || name.contains("three way");
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
            RandomSource random, UUID roomId, int authoredFloorY) {
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
        if (roomId != null) {
            roomba.getPersistentData().putString(
                    NATURAL_ROOMBA_ROOM_TAG, roomId.toString());
        }
        roomba.getPersistentData().putInt(
                NATURAL_ROOMBA_FLOOR_Y_TAG, authoredFloorY);
        if (!level.noCollision(roomba, roomba.getBoundingBox())) {
            roomba.discard();
            return null;
        }
        level.addFreshEntity(roomba);
        return roomba;
    }

    private static int countNearbyRoombas(ServerLevel level, double x,
            double y, double z, double radius) {
        AABB area = new AABB(x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius);
        return level.getEntitiesOfClass(RoombaEntity.class, area,
                entity -> entity.isAlive() && !entity.isRemoved()).size();
    }

    private record SpawnPool(List<WeightedPatch> patches,
            Set<UUID> occupiedNaturalRooms,
            Map<FloorKey, FloorBudget> floorBudgets) {
        private static final SpawnPool EMPTY =
                new SpawnPool(List.of(), Set.of(), Map.of());

        private SpawnPool {
            patches = patches == null ? List.of() : List.copyOf(patches);
            occupiedNaturalRooms = occupiedNaturalRooms == null
                    ? Set.of() : Set.copyOf(occupiedNaturalRooms);
            floorBudgets = floorBudgets == null
                    ? Map.of() : Map.copyOf(floorBudgets);
        }
    }

    private record FloorCatalog(Map<FloorKey, FloorBudget> budgets,
            Set<UUID> occupiedNaturalRooms) {
        private FloorCatalog {
            budgets = budgets == null ? Map.of() : Map.copyOf(budgets);
            occupiedNaturalRooms = occupiedNaturalRooms == null
                    ? Set.of() : Set.copyOf(occupiedNaturalRooms);
        }
    }

    private record FloorBudget(int eligibleRoomCount,
            int frequencyWeight, int naturalRoombaCount, int capacity) {
        private boolean full() {
            return naturalRoombaCount >= capacity;
        }
    }

    private static final class FloorAccumulator {
        private final Set<UUID> roomIds = new HashSet<>();
        private int frequencyWeight;
    }

    private record FloorKey(String value) {
    }

    private record WeightedPatch(FacilityRoomSnapshot room,
            FacilityFloorPatch patch, long selectionWeight,
            int frequencyWeight, FloorKey floorKey) {
    }

    private record MappedFloor(BlockPos floor, FacilityRoomSnapshot room,
            int frequencyWeight, FloorKey floorKey) {
    }

}
