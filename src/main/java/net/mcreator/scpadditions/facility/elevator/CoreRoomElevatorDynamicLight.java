package net.mcreator.scpadditions.facility.elevator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
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
 * <p>The invisible vanilla light block exists only in the client level and is
 * restored as soon as the carriage leaves its block. Nothing is written to the
 * server world or saved to disk, while normal block-light propagation remains
 * compatible with the vanilla renderer and shader packs.</p>
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CoreRoomElevatorDynamicLight {
    private static final int LIGHT_LEVEL = 7;
    private static final int UPDATE_INTERVAL_TICKS = 2;
    private static final int REFRESH_INTERVAL_TICKS = 20;
    private static final double TRACKING_RANGE = 64.0D;
    private static final Map<Integer, BlockPos> SOURCES = new HashMap<>();
    private static final Map<BlockPos, BlockState> ORIGINAL_STATES =
            new HashMap<>();
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
                    carriage.getY() + 3.05D, carriage.getZ()).immutable();
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
        BlockState current = level.getBlockState(position);
        if (current.is(Blocks.LIGHT)) {
            return;
        }
        if (!current.isAir()) {
            return;
        }
        ORIGINAL_STATES.putIfAbsent(position, current);
        level.setBlock(position, Blocks.LIGHT.defaultBlockState()
                .setValue(LightBlock.LEVEL, LIGHT_LEVEL), 3);
    }

    private static void removeSource(ClientLevel level, BlockPos position) {
        BlockState original = ORIGINAL_STATES.remove(position);
        if (original == null || !level.hasChunkAt(position)) {
            return;
        }
        if (level.getBlockState(position).is(Blocks.LIGHT)) {
            level.setBlock(position, original, 3);
        }
    }

    private static void clearTrackedSources() {
        if (trackedLevel != null) {
            for (BlockPos position : new HashSet<>(SOURCES.values())) {
                removeSource(trackedLevel, position);
            }
        }
        SOURCES.clear();
        ORIGINAL_STATES.clear();
    }
}
