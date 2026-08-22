package com.bl4ues.scpclassifieddirective.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.config.RoombaSpawnConfig;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModEntities;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModGameRules;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Sparse room-like Roomba encounters on configured facility flooring.
 *
 * <p>The scheduler is deliberately player-local and infrequent. Existing
 * cleaners suppress nearby attempts, while a separate and much smaller roll
 * may place one companion beside a newly spawned unit.</p>
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RoombaSpawnEvents {
    private static final int CHECK_INTERVAL_TICKS = 1_800;
    private static final int CHECK_JITTER_TICKS = 600;
    private static final int PRIMARY_CHANCE_BOUND = 80;
    private static final int PAIR_CHANCE_BOUND = 64;
    private static final int SEARCH_ATTEMPTS = 40;
    private static final int PAIR_SEARCH_ATTEMPTS = 20;
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
        if (gameTime < due) {
            return;
        }
        NEXT_CHECK.put(player.getUUID(), gameTime + CHECK_INTERVAL_TICKS
                + player.getRandom().nextInt(CHECK_JITTER_TICKS + 1));

        RoombaSpawnConfig.reloadIfChanged();
        RandomSource random = player.getRandom();
        int regionalCount = countRoombas(level, player.getX(), player.getY(),
                player.getZ(), REGIONAL_RADIUS);
        if (random.nextInt(PRIMARY_CHANCE_BOUND) != 0
                || countRoombas(level, player.getX(), player.getY(),
                player.getZ(), LOCAL_EXCLUSION_RADIUS) > 0
                || regionalCount >= REGIONAL_CAP) {
            return;
        }

        Optional<BlockPos> floor = findPrimaryFloor(level, player, random);
        if (floor.isEmpty()) {
            return;
        }

        RoombaEntity primary = spawnAt(level, floor.get(), random);
        if (primary == null || regionalCount > 0
                || random.nextInt(PAIR_CHANCE_BOUND) != 0) {
            return;
        }

        findPairFloor(level, floor.get(), random)
                .ifPresent(pairFloor -> spawnAt(level, pairFloor, random));
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        NEXT_CHECK.remove(event.getEntity().getUUID());
    }

    private static Optional<BlockPos> findPrimaryFloor(ServerLevel level,
            ServerPlayer player, RandomSource random) {
        BlockPos origin = player.blockPosition();
        for (int attempt = 0; attempt < SEARCH_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int radius = Mth.nextInt(random, MIN_SEARCH_RADIUS,
                    MAX_SEARCH_RADIUS);
            int x = origin.getX() + Mth.floor(Math.cos(angle) * radius);
            int z = origin.getZ() + Mth.floor(Math.sin(angle) * radius);
            Optional<BlockPos> floor = findFloorInColumn(level, x, z,
                    origin.getY() + 3, origin.getY() - 4);
            if (floor.isEmpty()) {
                continue;
            }
            BlockPos candidate = floor.get();
            if (countRoombas(level, candidate.getX() + 0.5D,
                    candidate.getY() + 1.0D, candidate.getZ() + 0.5D,
                    LOCAL_EXCLUSION_RADIUS) == 0) {
                return floor;
            }
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> findPairFloor(ServerLevel level,
            BlockPos primaryFloor, RandomSource random) {
        for (int attempt = 0; attempt < PAIR_SEARCH_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int radius = Mth.nextInt(random, 2, 5);
            int x = primaryFloor.getX()
                    + Mth.floor(Math.cos(angle) * radius);
            int z = primaryFloor.getZ()
                    + Mth.floor(Math.sin(angle) * radius);
            Optional<BlockPos> floor = findFloorInColumn(level, x, z,
                    primaryFloor.getY() + 1, primaryFloor.getY() - 1);
            if (floor.isEmpty()) {
                continue;
            }
            BlockPos candidate = floor.get();
            if (countRoombas(level, candidate.getX() + 0.5D,
                    candidate.getY() + 1.0D, candidate.getZ() + 0.5D,
                    1.5D) == 0) {
                return floor;
            }
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> findFloorInColumn(ServerLevel level,
            int x, int z, int topY, int bottomY) {
        for (int y = topY; y >= bottomY; y--) {
            BlockPos floor = new BlockPos(x, y, z);
            if (!level.hasChunkAt(floor)
                    || !RoombaSpawnConfig.isValidFloor(
                    level.getBlockState(floor))) {
                continue;
            }
            BlockPos spawn = floor.above();
            if (level.getBlockState(spawn).isAir()
                    && level.getBlockState(spawn.above()).getCollisionShape(
                    level, spawn.above()).isEmpty()) {
                return Optional.of(floor.immutable());
            }
        }
        return Optional.empty();
    }

    private static RoombaEntity spawnAt(ServerLevel level, BlockPos floor,
            RandomSource random) {
        EntityType<RoombaEntity> type = ScpClassifiedDirectiveModEntities.ROOMBA.get();
        RoombaEntity roomba = type.create(level);
        if (roomba == null) {
            return null;
        }

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
}
