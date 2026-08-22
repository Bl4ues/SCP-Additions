package com.bl4ues.scpclassifieddirective.event;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Optional spawn-region registry for SCP-939.
 *
 * The ordinary near-player encounter remains the fallback. Future facility
 * regions can register bounded alternatives without replacing the scheduler or
 * duplicating SCP-939's spawn validation and lifecycle code.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Scp939SpawnRegionRegistry {
    private static final Map<MinecraftServer, Map<ResourceLocation, SpawnRegion>>
            REGIONS = new WeakHashMap<>();

    private Scp939SpawnRegionRegistry() {
    }

    public static void register(MinecraftServer server, ResourceLocation id,
            ResourceKey<Level> dimension, AABB bounds, int weight,
            boolean naturalSpawn) {
        if (server == null || id == null || dimension == null || bounds == null) {
            return;
        }
        SpawnRegion region = new SpawnRegion(id, dimension, bounds,
                Math.max(1, weight), naturalSpawn);
        synchronized (REGIONS) {
            REGIONS.computeIfAbsent(server, ignored -> new LinkedHashMap<>())
                    .put(id, region);
        }
    }

    public static void unregister(MinecraftServer server, ResourceLocation id) {
        if (server == null || id == null) return;
        synchronized (REGIONS) {
            Map<ResourceLocation, SpawnRegion> regions = REGIONS.get(server);
            if (regions == null) return;
            regions.remove(id);
            if (regions.isEmpty()) REGIONS.remove(server);
        }
    }

    public static List<SpawnRegion> naturalRegions(ServerPlayer player) {
        if (player == null || player.getServer() == null) return List.of();
        synchronized (REGIONS) {
            Map<ResourceLocation, SpawnRegion> regions =
                    REGIONS.get(player.getServer());
            if (regions == null || regions.isEmpty()) return List.of();
            List<SpawnRegion> matching = new ArrayList<>();
            for (SpawnRegion region : regions.values()) {
                if (region.naturalSpawn()
                        && region.dimension().equals(player.level().dimension())) {
                    matching.add(region);
                }
            }
            return List.copyOf(matching);
        }
    }

    public static SpawnRegion chooseNatural(ServerPlayer player,
            RandomSource random) {
        List<SpawnRegion> regions = naturalRegions(player);
        if (regions.isEmpty() || random == null) return null;
        int totalWeight = 0;
        for (SpawnRegion region : regions) {
            totalWeight = Math.min(Integer.MAX_VALUE - region.weight(),
                    totalWeight) + region.weight();
        }
        int roll = random.nextInt(Math.max(1, totalWeight));
        for (SpawnRegion region : regions) {
            roll -= region.weight();
            if (roll < 0) return region;
        }
        return regions.get(regions.size() - 1);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        synchronized (REGIONS) {
            REGIONS.remove(event.getServer());
        }
    }

    public record SpawnRegion(ResourceLocation id, ResourceKey<Level> dimension,
                              AABB bounds, int weight, boolean naturalSpawn) {
    }
}
