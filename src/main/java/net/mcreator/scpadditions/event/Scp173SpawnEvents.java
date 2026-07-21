package net.mcreator.scpadditions.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.mcreator.scpadditions.entity.Scp173Entity;
import net.mcreator.scpadditions.entity.Scp173Sounds;
import net.mcreator.scpadditions.init.ScpAdditionsModEntities;
import net.mcreator.scpadditions.network.Scp079EnergyPacket.SpawnStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp173SpawnEvents {
    private static final int SPAWN_CHECK_INTERVAL_TICKS = 6000;
    private static final int SPAWN_CHANCE_BOUND = 3;
    private static final int SPAWN_ATTEMPTS = 96;
    private static final int FRONT_ATTEMPTS = 64;
    private static final int LOCAL_Y_SCAN_UP = 4;
    private static final int LOCAL_Y_SCAN_DOWN = 10;
    private static final double MIN_SPAWN_DISTANCE = 10.0D;
    private static final double MAX_FRONT_SPAWN_DISTANCE = 30.0D;
    private static final double MAX_AROUND_SPAWN_DISTANCE = 24.0D;
    private static final double FRONT_SIDE_SPREAD = 7.0D;
    private static final double AROUND_SIDE_SPREAD = 18.0D;
    private static final double ENTITY_HALF_WIDTH = 0.425D;
    private static final double ENTITY_HEIGHT = 1.95D;

    private static final Map<UUID, Integer> NEXT_SPAWN_CHECK_TICKS =
            new HashMap<>();
    private static final Map<UUID, SpawnStatus> LAST_RESULTS = new HashMap<>();

    private Scp173SpawnEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            scheduleNextCheck(player, SpawnStatus.COUNTDOWN);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        NEXT_SPAWN_CHECK_TICKS.remove(playerId);
        LAST_RESULTS.remove(playerId);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)
                || player.isCreative() || player.isSpectator()
                || !ScpAdditionsModulesConfig.get().scp173.enabled
                || !ScpAdditionsModulesConfig.get().scp173
                .naturalSpawnEnabled) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return;
        int currentTick = server.getTickCount();
        int nextCheckTick = NEXT_SPAWN_CHECK_TICKS.computeIfAbsent(
                player.getUUID(), ignored ->
                        currentTick + SPAWN_CHECK_INTERVAL_TICKS);
        LAST_RESULTS.putIfAbsent(player.getUUID(), SpawnStatus.COUNTDOWN);
        if (currentTick < nextCheckTick) return;

        // Preserve the five-minute cadence for every result. A natural despawn
        // is the only event that deliberately resets this interval again.
        NEXT_SPAWN_CHECK_TICKS.put(player.getUUID(),
                currentTick + SPAWN_CHECK_INTERVAL_TICKS);
        if (hasAnyScp173(player)) {
            LAST_RESULTS.put(player.getUUID(),
                    SpawnStatus.BLOCKED_BY_EXISTING);
            return;
        }

        RandomSource random = player.getRandom();
        if (random.nextInt(SPAWN_CHANCE_BOUND) != 0) {
            LAST_RESULTS.put(player.getUUID(), SpawnStatus.CHANCE_FAILED);
            return;
        }

        boolean spawned = trySpawnNearPlayer(player, random);
        LAST_RESULTS.put(player.getUUID(), spawned
                ? SpawnStatus.SPAWNED : SpawnStatus.NO_VALID_POSITION);
    }

    /**
     * A routine SCP-173 despawn resets every online player's scheduler. This
     * respects the global single-instance rule and prevents another player's
     * nearly-complete timer from immediately replacing the despawned entity.
     */
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide
                || !(event.getEntity() instanceof Scp173Entity scp173)
                || !scp173.isRoutineSpawn()
                || scp173.getRemovalReason() != Entity.RemovalReason.DISCARDED) {
            return;
        }

        MinecraftServer server = event.getLevel().getServer();
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            scheduleNextCheck(player, SpawnStatus.DESPAWNED_TIMER_RESET);
        }
    }

    public static DebugSnapshot debugSnapshot(ServerPlayer player) {
        MinecraftServer server = player == null ? null : player.getServer();
        if (server == null) {
            return new DebugSnapshot(-1, SpawnStatus.COUNTDOWN);
        }

        int currentTick = server.getTickCount();
        int nextCheckTick = NEXT_SPAWN_CHECK_TICKS.computeIfAbsent(
                player.getUUID(), ignored ->
                        currentTick + SPAWN_CHECK_INTERVAL_TICKS);
        ScpAdditionsModulesConfig.Root config =
                ScpAdditionsModulesConfig.get();
        SpawnStatus status;
        if (!config.scp173.enabled) {
            status = SpawnStatus.MODULE_DISABLED;
        } else if (!config.scp173.naturalSpawnEnabled) {
            status = SpawnStatus.NATURAL_SPAWN_DISABLED;
        } else if (player.isSpectator()) {
            status = SpawnStatus.PAUSED_SPECTATOR;
        } else if (player.isCreative()) {
            status = SpawnStatus.PAUSED_CREATIVE;
        } else {
            status = LAST_RESULTS.getOrDefault(player.getUUID(),
                    SpawnStatus.COUNTDOWN);
        }
        return new DebugSnapshot(nextCheckTick, status);
    }

    private static void scheduleNextCheck(ServerPlayer player,
            SpawnStatus result) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        NEXT_SPAWN_CHECK_TICKS.put(player.getUUID(),
                server.getTickCount() + SPAWN_CHECK_INTERVAL_TICKS);
        LAST_RESULTS.put(player.getUUID(), result);
    }

    private static boolean hasAnyScp173(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return true;
        AABB worldWide = new AABB(-30000000.0D, -2048.0D, -30000000.0D,
                30000000.0D, 4096.0D, 30000000.0D);
        for (ServerLevel level : server.getAllLevels()) {
            if (!level.getEntitiesOfClass(Scp173Entity.class, worldWide,
                    Scp173Entity::isAlive).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean trySpawnNearPlayer(ServerPlayer player,
            RandomSource random) {
        ServerLevel level = player.serverLevel();
        Vec3 forward = horizontal(player.getLookAngle());
        if (forward.lengthSqr() < 0.0001D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        int playerY = player.blockPosition().getY();
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            Vec3 candidate = attempt < FRONT_ATTEMPTS
                    ? frontCandidate(player.position(), forward, right, random)
                    : aroundCandidate(player.position(), random);
            int x = Mth.floor(candidate.x);
            int z = Mth.floor(candidate.z);
            BlockPos pos = findLocalSpawnPosition(level, x, playerY, z);
            if (pos == null) pos = findSurfaceSpawnPosition(level, x, z);
            if (pos != null && spawn173(level, pos, player, random)) {
                return true;
            }
        }
        return false;
    }

    private static Vec3 frontCandidate(Vec3 playerPos, Vec3 forward,
            Vec3 right, RandomSource random) {
        double distance = MIN_SPAWN_DISTANCE + random.nextDouble()
                * (MAX_FRONT_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE);
        double side = (random.nextDouble() - 0.5D)
                * FRONT_SIDE_SPREAD * 2.0D;
        return playerPos.add(forward.scale(distance)).add(right.scale(side));
    }

    private static Vec3 aroundCandidate(Vec3 playerPos, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = MIN_SPAWN_DISTANCE + random.nextDouble()
                * (MAX_AROUND_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE);
        double side = (random.nextDouble() - 0.5D) * AROUND_SIDE_SPREAD;
        Vec3 radial = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
        Vec3 tangent = new Vec3(-radial.z, 0.0D, radial.x);
        return playerPos.add(radial.scale(distance)).add(tangent.scale(side));
    }

    private static Vec3 horizontal(Vec3 vector) {
        Vec3 horizontal = new Vec3(vector.x, 0.0D, vector.z);
        double length = horizontal.length();
        return length <= 0.0001D ? horizontal
                : horizontal.scale(1.0D / length);
    }

    private static BlockPos findLocalSpawnPosition(ServerLevel level, int x,
            int playerY, int z) {
        for (int offset = 0; offset <= Math.max(LOCAL_Y_SCAN_UP,
                LOCAL_Y_SCAN_DOWN); offset++) {
            if (offset <= LOCAL_Y_SCAN_DOWN) {
                BlockPos down = new BlockPos(x, playerY - offset, z);
                if (isValidSpawnPosition(level, down)) return down;
            }
            if (offset > 0 && offset <= LOCAL_Y_SCAN_UP) {
                BlockPos up = new BlockPos(x, playerY + offset, z);
                if (isValidSpawnPosition(level, up)) return up;
            }
        }
        return null;
    }

    private static BlockPos findSurfaceSpawnPosition(ServerLevel level,
            int x, int z) {
        BlockPos surface = level.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(x, 0, z));
        return isValidSpawnPosition(level, surface) ? surface : null;
    }

    private static boolean isValidSpawnPosition(ServerLevel level,
            BlockPos pos) {
        BlockState floor = level.getBlockState(pos.below());
        if (floor.getCollisionShape(level, pos.below()).isEmpty()) return false;
        double x = pos.getX() + 0.5D;
        double y = pos.getY();
        double z = pos.getZ() + 0.5D;
        AABB box = new AABB(x - ENTITY_HALF_WIDTH, y,
                z - ENTITY_HALF_WIDTH, x + ENTITY_HALF_WIDTH,
                y + ENTITY_HEIGHT, z + ENTITY_HALF_WIDTH);
        return level.noCollision(box);
    }

    private static boolean spawn173(ServerLevel level, BlockPos pos,
            ServerPlayer player, RandomSource random) {
        Scp173Entity scp173 = ScpAdditionsModEntities.SCP_173.get().create(level);
        if (scp173 == null) return false;
        double x = pos.getX() + 0.5D;
        double y = pos.getY();
        double z = pos.getZ() + 0.5D;
        Vec3 toPlayer = player.position().subtract(new Vec3(x, y, z));
        float yaw = (float) (Mth.atan2(toPlayer.z, toPlayer.x)
                * Mth.RAD_TO_DEG) - 90.0F;
        scp173.moveTo(x, y, z, yaw, 0.0F);
        scp173.markRoutineSpawn();
        if (!level.addFreshEntity(scp173)) return false;
        level.playSound(null, x, y + 0.6D, z,
                Scp173Sounds.RATTLE.get(), SoundSource.HOSTILE,
                0.72F, 0.96F + random.nextFloat() * 0.08F);
        return true;
    }

    public record DebugSnapshot(int nextCheckTick, SpawnStatus status) {
    }
}
