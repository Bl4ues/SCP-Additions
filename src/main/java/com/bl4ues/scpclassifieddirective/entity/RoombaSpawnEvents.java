package com.bl4ues.scpclassifieddirective.entity;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityFloorPatch;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityMappingManager;
import com.bl4ues.scpclassifieddirective.facility.mapping.FacilityRoomSnapshot;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModEntities;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModGameRules;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Sparse SCP Unity-inspired Roomba encounters driven by authored facility rooms.
 *
 * <p>The Facility Mapping Tool is now the source of truth for valid floor cells.
 * Sublevel 1 rooms are strongly preferred, Sublevel 2 remains possible, and
 * corridor-like room names receive an additional bias. This preserves Unity's
 * "mostly SL1, sometimes SL2" distribution without coupling spawning to a
 * particular decorative floor block palette.</p>
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RoombaSpawnEvents {
    private static final int CHECK_INTERVAL_TICKS = 1_800;
    private static final int CHECK_JITTER_TICKS = 600;
    private static final int PRIMARY_CHANCE_BOUND = 80;
    private static final int PAIR_CHANCE_BOUND = 64;
    private static final int SEARCH_ATTEMPTS = 48;
    private static final int PAIR_SEARCH_ATTEMPTS = 28;
    private static final int MIN_SEARCH_RADIUS = 8;
    private static final int MAX_SEARCH_RADIUS = 22;
    private static final double LOCAL_EXCLUSION_RADIUS = 18.0D;
    private static final double REGIONAL_RADIUS = 72.0D;
    private static final int REGIONAL_CAP = 2;

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
        if (random.nextInt(PRIMARY_CHANCE_BOUND) != 0
                || countRoombas(level, player.getX(), player.getY(),
                player.getZ(), LOCAL_EXCLUSION_RADIUS) > 0
                || regionalCount >= REGIONAL_CAP) {
            return;
        }

        Optional<MappedFloor> floor = findPrimaryFloor(level, player, random);
        if (floor.isEmpty()) return;

        MappedFloor mapped = floor.get();
        RoombaEntity primary = spawnAt(level, mapped.floor(), random);
        if (primary == null || regionalCount > 0
                || !isSublevelOne(mapped.room())
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
                        selected.room()));
            }
        }
        return Optional.empty();
    }

    private static List<WeightedPatch> nearbyMappedPatches(ServerLevel level,
            ServerPlayer player) {
        List<WeightedPatch> result = new ArrayList<>();
        for (FacilityRoomSnapshot room : FacilityMappingManager.roomSnapshots(level)) {
            int roomWeight = roomWeight(room);
            if (roomWeight <= 0) continue;
            for (FacilityFloorPatch patch : room.patches()) {
                if (Math.abs((patch.y() + 1.0D) - player.getY()) > 5.0D
                        || distanceSqrToPatch(player.getX(), player.getZ(), patch)
                        > MAX_SEARCH_RADIUS * MAX_SEARCH_RADIUS) {
                    continue;
                }
                long area = Math.max(1L, Math.min(96L, patch.area()));
                result.add(new WeightedPatch(room, patch,
                        Math.max(1L, area * roomWeight)));
            }
        }
        return result;
    }

    private static WeightedPatch weightedPatch(List<WeightedPatch> patches,
            RandomSource random) {
        long total = 0L;
        for (WeightedPatch patch : patches) total += patch.weight();
        long roll = Math.floorMod(random.nextLong(), Math.max(1L, total));
        for (WeightedPatch patch : patches) {
            if (roll < patch.weight()) return patch;
            roll -= patch.weight();
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
        return level.getFluidState(spawn).isEmpty()
                && level.getBlockState(spawn).getCollisionShape(level, spawn)
                        .isEmpty()
                && level.getBlockState(spawn.above()).getCollisionShape(
                        level, spawn.above()).isEmpty();
    }

    private static int roomWeight(FacilityRoomSnapshot room) {
        if (room == null) return 0;
        String floor = (room.floorLongLabel() + " " + room.floorShortLabel())
                .toLowerCase(Locale.ROOT);
        if (floor.contains("heavy containment") || floor.contains("hcz")
                || floor.contains("super heavy") || floor.contains("surface")
                || floor.contains("exterior")) {
            return 0;
        }

        int base;
        if (matchesSublevel(floor, 1)) base = 4;
        else if (matchesSublevel(floor, 2)) base = 1;
        else return 0;

        String name = room.name().toLowerCase(Locale.ROOT);
        if (name.contains("corridor") || name.contains("hallway")
                || name.contains("hall ") || name.endsWith(" hall")) {
            base *= 2;
        }
        return base;
    }

    private static boolean isSublevelOne(FacilityRoomSnapshot room) {
        if (room == null) return false;
        String floor = (room.floorLongLabel() + " " + room.floorShortLabel())
                .toLowerCase(Locale.ROOT);
        return matchesSublevel(floor, 1);
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

    private static double distanceSqrToPatch(double x, double z,
            FacilityFloorPatch patch) {
        double nearestX = Math.max(patch.minX(), Math.min(x, patch.maxX() + 1.0D));
        double nearestZ = Math.max(patch.minZ(), Math.min(z, patch.maxZ() + 1.0D));
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
            FacilityFloorPatch patch, long weight) {
    }

    private record MappedFloor(BlockPos floor, FacilityRoomSnapshot room) {
    }
}
