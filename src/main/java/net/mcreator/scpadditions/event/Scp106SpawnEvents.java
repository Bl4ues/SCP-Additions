package net.mcreator.scpadditions.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.entity.Scp106Entity;
import net.mcreator.scpadditions.init.ScpAdditionsModEntities;
import net.mcreator.scpadditions.roamer.RoamerManager;
import net.mcreator.scpadditions.roamer.RoamerResult;
import net.mcreator.scpadditions.roamer.RoamerType;
import net.mcreator.scpadditions.roamer.Scp106EmergenceLocator;
import net.mcreator.scpadditions.roamer.Scp106SpawnSuppression;

/** Natural SCP-106 encounter checks and aggressive emergence spawning. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp106SpawnEvents {
    private static final int NORMAL_CHANCE_BOUND = 3;
    private static final int OTHER_ROAMER_CHANCE_BOUND = 5;
    private static final AABB WORLD_BOUNDS = new AABB(-30000000.0D,
            -2048.0D, -30000000.0D, 30000000.0D, 4096.0D,
            30000000.0D);

    private Scp106SpawnEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)
                || !RoamerManager.pollSpawnCheck(player,
                RoamerType.SCP_106)) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return;
        if (Scp106SpawnSuppression.isSuppressed(server)) {
            RoamerManager.recordResult(player, RoamerType.SCP_106,
                    RoamerResult.DESPAWNED_TIMER_RESET);
            return;
        }

        Scp106Entity existing = findAnyScp106(server);
        if (existing != null) {
            RoamerManager.markSpawned(server, RoamerType.SCP_106,
                    existing.getUUID());
            RoamerManager.recordResult(player, RoamerType.SCP_106,
                    RoamerResult.BLOCKED_BY_EXISTING);
            return;
        }

        boolean otherRoamerActive = RoamerManager.hasOtherActive(server,
                RoamerType.SCP_106);
        int chanceBound = otherRoamerActive
                ? OTHER_ROAMER_CHANCE_BOUND : NORMAL_CHANCE_BOUND;
        RandomSource random = player.getRandom();
        if (random.nextInt(chanceBound) != 0) {
            RoamerManager.recordResult(player, RoamerType.SCP_106,
                    otherRoamerActive
                            ? RoamerResult.CHANCE_FAILED_OTHER_ROAMER
                            : RoamerResult.CHANCE_FAILED);
            return;
        }

        Scp106EmergenceLocator.Placement placement =
                Scp106EmergenceLocator.findInitial(player.serverLevel(),
                        player, random);
        if (placement == null) {
            RoamerManager.recordResult(player, RoamerType.SCP_106,
                    RoamerResult.NO_VALID_POSITION);
            return;
        }

        Scp106Entity spawned = spawn106(player, placement);
        if (spawned == null) {
            RoamerManager.recordResult(player, RoamerType.SCP_106,
                    RoamerResult.NO_VALID_POSITION);
            return;
        }

        RoamerManager.markSpawned(server, RoamerType.SCP_106,
                spawned.getUUID());
        RoamerManager.recordResult(player, RoamerType.SCP_106,
                RoamerResult.SPAWNED);
    }

    public static RoamerResult forceSpawn(ServerPlayer player) {
        if (player == null) return RoamerResult.NO_VALID_POSITION;
        MinecraftServer server = player.getServer();
        if (server == null) return RoamerResult.NO_VALID_POSITION;

        Scp106Entity existing = findAnyScp106(server);
        if (existing != null) {
            RoamerManager.markSpawned(server, RoamerType.SCP_106,
                    existing.getUUID());
            RoamerManager.recordResult(player, RoamerType.SCP_106,
                    RoamerResult.BLOCKED_BY_EXISTING);
            return RoamerResult.BLOCKED_BY_EXISTING;
        }

        Scp106EmergenceLocator.Placement placement =
                Scp106EmergenceLocator.findInitial(player.serverLevel(),
                        player, player.getRandom());
        if (placement == null) {
            RoamerManager.recordResult(player, RoamerType.SCP_106,
                    RoamerResult.NO_VALID_POSITION);
            return RoamerResult.NO_VALID_POSITION;
        }

        Scp106Entity spawned = spawn106(player, placement);
        if (spawned == null) {
            RoamerManager.recordResult(player, RoamerType.SCP_106,
                    RoamerResult.NO_VALID_POSITION);
            return RoamerResult.NO_VALID_POSITION;
        }

        RoamerManager.markSpawned(server, RoamerType.SCP_106,
                spawned.getUUID());
        RoamerManager.recordResult(player, RoamerType.SCP_106,
                RoamerResult.SPAWNED);
        return RoamerResult.SPAWNED;
    }

    private static Scp106Entity findAnyScp106(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            var entities = level.getEntitiesOfClass(Scp106Entity.class,
                    WORLD_BOUNDS, entity -> entity.isAlive()
                            && !entity.isRemoved());
            if (!entities.isEmpty()) return entities.get(0);
        }
        return null;
    }

    private static Scp106Entity spawn106(ServerPlayer player,
            Scp106EmergenceLocator.Placement placement) {
        ServerLevel level = player.serverLevel();
        Scp106Entity scp106 = ScpAdditionsModEntities.SCP_106.get().create(level);
        if (scp106 == null) return null;

        scp106.moveTo(placement.position().x, placement.position().y,
                placement.position().z, placement.yaw(), 0.0F);
        scp106.beginNaturalEncounter(player, placement.emergence());
        return level.addFreshEntity(scp106) ? scp106 : null;
    }
}
