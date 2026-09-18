package com.bl4ues.scpclassifieddirective.facility.transform;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Shared logical/vanilla redstone query for transformed construction.
 *
 * Transformed power sources are spatially indexed. A door asking whether it is
 * powered must not rescan every authored block in the facility; that turned a
 * handful of angled rooms into an accidental benchmark.
 */
public final class TransformPowerQuery {
    private static final Map<MinecraftServer, PowerIndex> INDEXES =
            new WeakHashMap<>();

    private TransformPowerQuery() {
    }

    public static boolean powered(ServerLevel level, Vec3 center) {
        if (level == null || center == null) return false;
        BlockPos base = BlockPos.containing(center);
        if (level.hasNeighborSignal(base) || level.hasNeighborSignal(base.above())) {
            return true;
        }
        for (net.minecraft.core.Direction direction
                : net.minecraft.core.Direction.values()) {
            if (level.hasNeighborSignal(base.relative(direction))) return true;
        }
        return index(level.getServer()).hasSourceNear(
                level.dimension().location(), center, 1.35D);
    }

    /**
     * Structural edits and transformed button/lever state changes call this
     * explicitly. Door animation states do not invalidate the index because
     * they are consumers, not power sources.
     */
    public static synchronized void invalidate(MinecraftServer server) {
        if (server != null) INDEXES.remove(server);
    }

    private static synchronized PowerIndex index(MinecraftServer server) {
        return INDEXES.computeIfAbsent(server, TransformPowerQuery::buildIndex);
    }

    private static PowerIndex buildIndex(MinecraftServer server) {
        PowerIndex result = new PowerIndex();
        TransformConstructionSavedData data =
                TransformConstructionSavedData.get(server);
        for (TransformGroup group : data.groups()) {
            for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                    : group.cells().entrySet()) {
                if (!source(entry.getValue())) continue;
                result.add(group.dimension(), group.cellCenter(entry.getKey()));
            }
        }
        for (ConstructionSurface surface : data.surfaces()) {
            for (Map.Entry<ConstructionSurface.SurfaceSlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.attachments().entrySet()) {
                if (!source(entry.getValue().state())) continue;
                ConstructionSurface.SurfaceSlot slot = entry.getKey();
                double u = (slot.column() + 0.5D) / surface.columns();
                double v = (slot.row() + 0.5D) / surface.rows();
                result.add(surface.dimension(), surface.gridPoint(u, v)
                        .add(surface.gridNormal(u, v).scale(0.5D)));
            }
        }
        return result;
    }

    private static boolean source(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (state.is(Blocks.REDSTONE_BLOCK)) return true;
        return state.hasProperty(BlockStateProperties.POWERED)
                && state.getValue(BlockStateProperties.POWERED);
    }

    private static final class PowerIndex {
        private final Map<ResourceLocation, Map<Long, List<Vec3>>> byCell =
                new HashMap<>();

        private void add(ResourceLocation dimension, Vec3 world) {
            BlockPos cell = BlockPos.containing(world);
            byCell.computeIfAbsent(dimension, ignored -> new HashMap<>())
                    .computeIfAbsent(cell.asLong(), ignored -> new ArrayList<>())
                    .add(world);
        }

        private boolean hasSourceNear(ResourceLocation dimension, Vec3 center,
                double range) {
            Map<Long, List<Vec3>> cells = byCell.get(dimension);
            if (cells == null || cells.isEmpty()) return false;
            double maxDistanceSqr = range * range;
            BlockPos base = BlockPos.containing(center);
            // range is currently 1.35, but two cells of search margin keeps the
            // index correct for sources close to opposite cell boundaries.
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        List<Vec3> sources = cells.get(BlockPos.asLong(
                                base.getX() + dx, base.getY() + dy,
                                base.getZ() + dz));
                        if (sources == null) continue;
                        for (Vec3 source : sources) {
                            double distance = source.distanceToSqr(center);
                            if (distance > 1.0E-5D
                                    && distance <= maxDistanceSqr) return true;
                        }
                    }
                }
            }
            return false;
        }
    }
}
