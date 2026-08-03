package net.mcreator.scpadditions.facility.elevator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Adds a restrained client-side light source near the carriage ceiling.
 *
 * <p>No blocks are placed in the world. The source is injected into the client
 * light engine and moved only when the carriage crosses into another block,
 * keeping the effect inexpensive and avoiding persistent invisible light
 * blocks in saves.</p>
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CoreRoomElevatorDynamicLight {
    private static final int LIGHT_LEVEL = 7;
    private static final int UPDATE_INTERVAL_TICKS = 2;
    private static final int REFRESH_INTERVAL_TICKS = 20;
    private static final double TRACKING_RANGE = 64.0D;
    private static final Map<Integer, BlockPos> SOURCES = new HashMap<>();
    private static ClientLevel trackedLevel;

    private CoreRoomElevatorDynamicLight() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            clearTrackedSources();
            trackedLevel = null;
            return;
        }

        if (trackedLevel != level) {
            clearTrackedSources();
            trackedLevel = level;
        }

        int tick = minecraft.player.tickCount;
        if (tick % UPDATE_INTERVAL_TICKS != 0) {
            return;
        }

        Map<Integer, BlockPos> nextSources = new HashMap<>();
        AABB searchArea = minecraft.player.getBoundingBox()
                .inflate(TRACKING_RANGE);
        for (CoreRoomElevatorCarriageEntity carriage :
                level.getEntitiesOfClass(CoreRoomElevatorCarriageEntity.class,
                        searchArea, entity -> !entity.isRemoved())) {
            BlockPos source = BlockPos.containing(carriage.getX(),
                    carriage.getY() + 2.35D, carriage.getZ()).immutable();
            nextSources.put(carriage.getId(), source);
        }

        Set<BlockPos> oldPositions = new HashSet<>(SOURCES.values());
        Set<BlockPos> nextPositions = new HashSet<>(nextSources.values());
        for (BlockPos oldPosition : oldPositions) {
            if (!nextPositions.contains(oldPosition)) {
                removeSource(level, oldPosition);
            }
        }

        boolean refresh = tick % REFRESH_INTERVAL_TICKS == 0;
        for (BlockPos nextPosition : nextPositions) {
            if (refresh || !oldPositions.contains(nextPosition)) {
                addSource(level, nextPosition);
            }
        }

        SOURCES.clear();
        SOURCES.putAll(nextSources);
    }

    private static void addSource(ClientLevel level, BlockPos position) {
        if (!level.hasChunkAt(position)) {
            return;
        }
        LevelLightEngine lightEngine = level.getChunkSource().getLightEngine();
        lightEngine.onBlockEmissionIncrease(position, LIGHT_LEVEL);
    }

    private static void removeSource(ClientLevel level, BlockPos position) {
        if (!level.hasChunkAt(position)) {
            return;
        }
        level.getChunkSource().getLightEngine().checkBlock(position);
    }

    private static void clearTrackedSources() {
        if (trackedLevel != null) {
            for (BlockPos position : new HashSet<>(SOURCES.values())) {
                removeSource(trackedLevel, position);
            }
        }
        SOURCES.clear();
    }
}
