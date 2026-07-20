package net.mcreator.scpadditions.event;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.mcreator.scpadditions.entity.Scp173Entity;
import net.mcreator.scpadditions.entity.Scp173Sounds;
import net.mcreator.scpadditions.init.ScpAdditionsModEntities;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = EventBusSubscriber.Bus.GAME)
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
    private static final Map<UUID, Integer> NEXT_SPAWN_CHECK_TICKS = new HashMap<>();

    private Scp173SpawnEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            scheduleNextCheck(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        NEXT_SPAWN_CHECK_TICKS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()
                || !ScpAdditionsModulesConfig.get().scp173.enabled
                || !ScpAdditionsModulesConfig.get().scp173.naturalSpawnEnabled) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;
        int currentTick = server.getTickCount();
        int nextCheckTick = NEXT_SPAWN_CHECK_TICKS.computeIfAbsent(
                player.getUUID(), ignored -> currentTick + SPAWN_CHECK_INTERVAL_TICKS);
        if (currentTick < nextCheckTick) return;

        // Schedule the following interval before evaluating the current attempt,
        // preserving the original five-minute cadence even when an SCP-173 is
        // already loaded or the random chance fails.
        NEXT_SPAWN_CHECK_TICKS.put(player.getUUID(), currentTick + SPAWN_CHECK_INTERVAL_TICKS);
        if (hasAnyScp173(player)) return;

        RandomSource random = player.getRandom();
        if (random.nextInt(SPAWN_CHANCE_BOUND) != 0) return;
        trySpawnNearPlayer(player, random);
    }

    private static void scheduleNextCheck(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        NEXT_SPAWN_CHECK_TICKS.put(
                player.getUUID(), server.getTickCount() + SPAWN_CHECK_INTERVAL_TICKS);
    }

    private static boolean hasAnyScp173(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return true;
        AABB worldWide = new AABB(-30000000.0D, -2048.0D, -30000000.0D, 30000000.0D, 4096.0D, 30000000.0D);
        for (ServerLevel level : server.getAllLevels()) {
            if (!level.getEntitiesOfClass(Scp173Entity.class, worldWide, Scp173Entity::isAlive).isEmpty()) return true;
        }
        return false;
    }

    private static void trySpawnNearPlayer(ServerPlayer player, RandomSource random) {
        ServerLevel level = player.serverLevel();
        Vec3 forward = horizontal(player.getLookAngle());
        if (forward.lengthSqr() < 0.0001D) forward = new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        int playerY = player.blockPosition().getY();
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            Vec3 candidate = attempt < FRONT_ATTEMPTS
                    ? frontCandidate(player.position(), forward, right, random)
                    : aroundCandidate(player.position(), random);
            int x = Mth.floor(candidate.x), z = Mth.floor(candidate.z);
            BlockPos pos = findLocalSpawnPosition(level, x, playerY, z);
            if (pos == null) pos = findSurfaceSpawnPosition(level, x, z);
            if (pos != null) {
                spawn173(level, pos, player, random);
                return;
            }
        }
    }

    private static Vec3 frontCandidate(Vec3 playerPos, Vec3 forward, Vec3 right, RandomSource random) {
        double distance = MIN_SPAWN_DISTANCE + random.nextDouble() * (MAX_FRONT_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE);
        double side = (random.nextDouble() - 0.5D) * FRONT_SIDE_SPREAD * 2.0D;
        return playerPos.add(forward.scale(distance)).add(right.scale(side));
    }

    private static Vec3 aroundCandidate(Vec3 playerPos, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = MIN_SPAWN_DISTANCE + random.nextDouble() * (MAX_AROUND_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE);
        double side = (random.nextDouble() - 0.5D) * AROUND_SIDE_SPREAD;
        Vec3 radial = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
        Vec3 tangent = new Vec3(-radial.z, 0.0D, radial.x);
        return playerPos.add(radial.scale(distance)).add(tangent.scale(side));
    }

    private static Vec3 horizontal(Vec3 vector) {
        Vec3 horizontal = new Vec3(vector.x, 0.0D, vector.z);
        double length = horizontal.length();
        return length <= 0.0001D ? horizontal : horizontal.scale(1.0D / length);
    }

    private static BlockPos findLocalSpawnPosition(ServerLevel level, int x, int playerY, int z) {
        for (int offset = 0; offset <= Math.max(LOCAL_Y_SCAN_UP, LOCAL_Y_SCAN_DOWN); offset++) {
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

    private static BlockPos findSurfaceSpawnPosition(ServerLevel level, int x, int z) {
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        return isValidSpawnPosition(level, surface) ? surface : null;
    }

    private static boolean isValidSpawnPosition(ServerLevel level, BlockPos pos) {
        BlockState floor = level.getBlockState(pos.below());
        if (floor.getCollisionShape(level, pos.below()).isEmpty()) return false;
        double x = pos.getX() + 0.5D, y = pos.getY(), z = pos.getZ() + 0.5D;
        AABB box = new AABB(x - ENTITY_HALF_WIDTH, y, z - ENTITY_HALF_WIDTH,
                x + ENTITY_HALF_WIDTH, y + ENTITY_HEIGHT, z + ENTITY_HALF_WIDTH);
        return level.noCollision(box);
    }

    private static void spawn173(ServerLevel level, BlockPos pos, ServerPlayer player, RandomSource random) {
        Scp173Entity scp173 = ScpAdditionsModEntities.SCP_173.get().create(level);
        if (scp173 == null) return;
        double x = pos.getX() + 0.5D, y = pos.getY(), z = pos.getZ() + 0.5D;
        Vec3 toPlayer = player.position().subtract(new Vec3(x, y, z));
        float yaw = (float) (Mth.atan2(toPlayer.z, toPlayer.x) * Mth.RAD_TO_DEG) - 90.0F;
        scp173.moveTo(x, y, z, yaw, 0.0F);
        scp173.markRoutineSpawn();
        level.addFreshEntity(scp173);
        level.playSound(null, x, y + 0.6D, z, Scp173Sounds.RATTLE.get(), SoundSource.HOSTILE,
                0.72F, 0.96F + random.nextFloat() * 0.08F);
    }
}
