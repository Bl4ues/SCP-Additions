package com.bl4ues.scpclassifieddirective.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.entity.Scp939Entity;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModEntities;
import com.bl4ues.scpclassifieddirective.roamer.RoamerManager;
import com.bl4ues.scpclassifieddirective.roamer.RoamerResult;
import com.bl4ues.scpclassifieddirective.roamer.RoamerType;

/** Natural SCP-939 encounter scheduler and hidden placement. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp939SpawnEvents {
    private static final int NORMAL_CHANCE_BOUND = 4;
    private static final int OTHER_ROAMER_CHANCE_BOUND = 7;
    private static final int SPAWN_ATTEMPTS = 112;
    private static final int BEHIND_ATTEMPTS = 76;
    private static final int REGION_SPAWN_ATTEMPTS = 96;
    private static final int LOCAL_Y_SCAN_UP = 4;
    private static final int LOCAL_Y_SCAN_DOWN = 10;
    private static final double MIN_DISTANCE = 12.0D;
    private static final double MAX_DISTANCE = 27.0D;
    private static final double ENTITY_HALF_WIDTH = 0.60D;
    private static final double ENTITY_HEIGHT = 1.48D;

    private Scp939SpawnEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer driver)) {
            return;
        }

        ServerPlayer player = RoamerManager.pollSpawnTarget(driver,
                RoamerType.SCP_939);
        if (player == null) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;

        boolean otherRoamerActive = RoamerManager.hasOtherActive(server,
                RoamerType.SCP_939);
        int existing939 = RoamerManager.activeCount(server, RoamerType.SCP_939);
        int chanceBound = spawnChanceBound(otherRoamerActive, existing939);
        RandomSource random = player.getRandom();
        if (random.nextInt(chanceBound) != 0) {
            RoamerManager.recordResult(player, RoamerType.SCP_939,
                    otherRoamerActive
                            ? RoamerResult.CHANCE_FAILED_OTHER_ROAMER
                            : RoamerResult.CHANCE_FAILED);
            return;
        }

        Scp939Entity spawned = trySpawnNatural(player, random);
        if (spawned == null) {
            RoamerManager.recordResult(player, RoamerType.SCP_939,
                    RoamerResult.NO_VALID_POSITION);
            return;
        }
        RoamerManager.markSpawned(server, RoamerType.SCP_939,
                spawned.getUUID());
        RoamerManager.recordResult(player, RoamerType.SCP_939,
                RoamerResult.SPAWNED);
    }

    public static RoamerResult forceSpawn(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return RoamerResult.NO_VALID_POSITION;
        }
        MinecraftServer server = player.getServer();
        Scp939Entity spawned = trySpawnNatural(player, player.getRandom());
        if (spawned == null) {
            RoamerManager.recordResult(player, RoamerType.SCP_939,
                    RoamerResult.NO_VALID_POSITION);
            return RoamerResult.NO_VALID_POSITION;
        }
        RoamerManager.markSpawned(server, RoamerType.SCP_939,
                spawned.getUUID());
        RoamerManager.recordResult(player, RoamerType.SCP_939,
                RoamerResult.SPAWNED);
        return RoamerResult.SPAWNED;
    }

    /**
     * Existing SCP-939 instances never block their own scheduler. They make an
     * additional 939 progressively less likely instead, so overlapping
     * encounters remain possible without snowballing into a pack every cycle.
     */
    private static int spawnChanceBound(boolean otherRoamerActive,
            int existing939) {
        int base = otherRoamerActive
                ? OTHER_ROAMER_CHANCE_BOUND : NORMAL_CHANCE_BOUND;
        int penalty = Math.min(8, Math.max(0, existing939) * 2);
        return base + penalty;
    }

    private static Scp939Entity trySpawnNatural(ServerPlayer player,
            RandomSource random) {
        Scp939SpawnRegionRegistry.SpawnRegion region =
                Scp939SpawnRegionRegistry.chooseNatural(player, random);
        if (region != null) {
            Scp939Entity regional = trySpawnInRegion(player, random, region);
            if (regional != null) return regional;
        }
        return trySpawnNearPlayer(player, random);
    }

    private static Scp939Entity trySpawnInRegion(ServerPlayer player,
            RandomSource random,
            Scp939SpawnRegionRegistry.SpawnRegion region) {
        ServerLevel level = player.serverLevel();
        if (!region.dimension().equals(level.dimension())) return null;
        AABB bounds = region.bounds();
        int minX = Mth.floor(bounds.minX);
        int maxX = Mth.ceil(bounds.maxX) - 1;
        int minY = Math.max(level.getMinBuildHeight() + 1,
                Mth.floor(bounds.minY));
        int maxY = Math.min(level.getMaxBuildHeight() - 2,
                Mth.ceil(bounds.maxY) - 1);
        int minZ = Mth.floor(bounds.minZ);
        int maxZ = Mth.ceil(bounds.maxZ) - 1;
        if (maxX < minX || maxY < minY || maxZ < minZ) return null;

        for (int attempt = 0; attempt < REGION_SPAWN_ATTEMPTS; attempt++) {
            int x = randomBetween(random, minX, maxX);
            int y = randomBetween(random, minY, maxY);
            int z = randomBetween(random, minZ, maxZ);
            BlockPos pos = new BlockPos(x, y, z);
            if (!isValidPosition(level, pos)) continue;
            Vec3 spawnPos = Vec3.atBottomCenterOf(pos);
            if (isDirectlyVisible(player, spawnPos)) continue;
            Scp939Entity spawned = spawn(level, spawnPos, player);
            if (spawned != null) return spawned;
        }
        return null;
    }

    private static int randomBetween(RandomSource random, int min, int max) {
        if (max <= min) return min;
        return min + random.nextInt(max - min + 1);
    }

    private static Scp939Entity trySpawnNearPlayer(ServerPlayer player,
            RandomSource random) {
        ServerLevel level = player.serverLevel();
        Vec3 look = horizontal(player.getLookAngle());
        if (look.lengthSqr() < 0.0001D) look = new Vec3(0, 0, 1);
        Vec3 behind = look.scale(-1.0D);
        Vec3 right = new Vec3(-behind.z, 0.0D, behind.x);
        int playerY = player.blockPosition().getY();

        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            Vec3 candidate;
            if (attempt < BEHIND_ATTEMPTS) {
                double distance = MIN_DISTANCE + random.nextDouble()
                        * (MAX_DISTANCE - MIN_DISTANCE);
                double side = (random.nextDouble() - 0.5D) * 15.0D;
                candidate = player.position().add(behind.scale(distance))
                        .add(right.scale(side));
            } else {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double distance = MIN_DISTANCE + random.nextDouble()
                        * (MAX_DISTANCE - MIN_DISTANCE);
                candidate = player.position().add(Math.cos(angle) * distance,
                        0.0D, Math.sin(angle) * distance);
            }

            int x = Mth.floor(candidate.x);
            int z = Mth.floor(candidate.z);
            BlockPos pos = findLocalSpawnPosition(level, x, playerY, z);
            if (pos == null) pos = findSurfaceSpawnPosition(level, x, z);
            if (pos == null) continue;

            Vec3 spawnPos = Vec3.atBottomCenterOf(pos);
            if (isDirectlyVisible(player, spawnPos)) continue;
            Scp939Entity spawned = spawn(level, spawnPos, player);
            if (spawned != null) return spawned;
        }
        return null;
    }

    private static BlockPos findLocalSpawnPosition(ServerLevel level, int x,
            int playerY, int z) {
        int scan = Math.max(LOCAL_Y_SCAN_UP, LOCAL_Y_SCAN_DOWN);
        for (int offset = 0; offset <= scan; offset++) {
            if (offset <= LOCAL_Y_SCAN_DOWN) {
                BlockPos down = new BlockPos(x, playerY - offset, z);
                if (isValidPosition(level, down)) return down;
            }
            if (offset > 0 && offset <= LOCAL_Y_SCAN_UP) {
                BlockPos up = new BlockPos(x, playerY + offset, z);
                if (isValidPosition(level, up)) return up;
            }
        }
        return null;
    }

    private static BlockPos findSurfaceSpawnPosition(ServerLevel level,
            int x, int z) {
        BlockPos surface = level.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(x, 0, z));
        return isValidPosition(level, surface) ? surface : null;
    }

    private static boolean isValidPosition(ServerLevel level, BlockPos pos) {
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

    /** Rejects only placements the player can actually see unobstructed. */
    private static boolean isDirectlyVisible(ServerPlayer player,
            Vec3 spawnPos) {
        Vec3 eye = player.getEyePosition();
        Vec3 target = spawnPos.add(0.0D, ENTITY_HEIGHT * 0.58D, 0.0D);
        Vec3 delta = target.subtract(eye);
        if (delta.lengthSqr() < 0.0001D) return true;
        Vec3 direction = delta.normalize();
        if (player.getLookAngle().normalize().dot(direction) < 0.32D) {
            return false;
        }
        BlockHitResult hit = player.level().clip(new ClipContext(eye, target,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS
                || hit.getLocation().distanceToSqr(target) <= 0.30D;
    }

    private static Scp939Entity spawn(ServerLevel level, Vec3 pos,
            ServerPlayer player) {
        Scp939Entity scp939 = ScpClassifiedDirectiveModEntities.SCP_939.get().create(level);
        if (scp939 == null) return null;
        Vec3 towardPlayer = player.position().subtract(pos);
        float yaw = (float) (Mth.atan2(towardPlayer.z, towardPlayer.x)
                * Mth.RAD_TO_DEG) - 90.0F;
        scp939.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        scp939.beginNaturalEncounter(player, pos);
        return level.addFreshEntity(scp939) ? scp939 : null;
    }

    private static Vec3 horizontal(Vec3 value) {
        Vec3 flat = new Vec3(value.x, 0.0D, value.z);
        return flat.lengthSqr() <= 0.0001D ? flat : flat.normalize();
    }
}
