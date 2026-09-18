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
import java.util.UUID;
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

    public static boolean powered(ServerLevel level, TransformGroup group,
            TransformGroup.GridPos consumer) {
        if (level == null || group == null || consumer == null) return false;
        for (net.minecraft.core.Direction direction
                : net.minecraft.core.Direction.values()) {
            TransformGroup.GridPos neighbor = consumer.offset(
                    direction.getStepX(), direction.getStepY(),
                    direction.getStepZ());
            if (source(group.cells().get(neighbor))) return true;
        }
        return powered(level, group.cellCenter(consumer));
    }

    public static boolean powered(ServerLevel level, ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot consumer) {
        if (level == null || surface == null || consumer == null) return false;
        int[][] offsets = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] offset : offsets) {
            ConstructionSurface.SurfaceSlot neighbor =
                    new ConstructionSurface.SurfaceSlot(
                            consumer.column() + offset[0],
                            consumer.row() + offset[1]);
            ConstructionSurface.SurfaceAttachment attachment =
                    surface.attachments().get(neighbor);
            if (attachment != null && source(attachment.state())) return true;
        }
        double u = (consumer.column() + 0.5D) / surface.columns();
        double v = (consumer.row() + 0.5D) / surface.rows();
        return powered(level, surface.gridPoint(u, v)
                .add(surface.gridNormal(u, v).scale(0.5D)));
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
    public static synchronized void refreshGroup(MinecraftServer server,
            TransformGroup group) {
        if (server == null || group == null) return;
        PowerIndex index = INDEXES.get(server);
        if (index != null) index.replaceGroup(group);
    }

    public static synchronized void refreshSurface(MinecraftServer server,
            ConstructionSurface surface) {
        if (server == null || surface == null) return;
        PowerIndex index = INDEXES.get(server);
        if (index != null) index.replaceSurface(surface);
    }

    public static synchronized void removeGroup(MinecraftServer server,
            ResourceLocation dimension, UUID id) {
        if (server == null || dimension == null || id == null) return;
        PowerIndex index = INDEXES.get(server);
        if (index != null) {
            index.replace(new SourceOwner(dimension, id, false), List.of());
        }
    }

    public static synchronized void removeSurface(MinecraftServer server,
            ResourceLocation dimension, UUID id) {
        if (server == null || dimension == null || id == null) return;
        PowerIndex index = INDEXES.get(server);
        if (index != null) {
            index.replace(new SourceOwner(dimension, id, true), List.of());
        }
    }

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
            result.replaceGroup(group);
        }
        for (ConstructionSurface surface : data.surfaces()) {
            result.replaceSurface(surface);
        }
        return result;
    }

    private static boolean source(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (state.is(Blocks.REDSTONE_BLOCK)) return true;
        return state.hasProperty(BlockStateProperties.POWERED)
                && state.getValue(BlockStateProperties.POWERED);
    }

    private record SourceOwner(ResourceLocation dimension, UUID id,
            boolean surface) {
    }

    private record SourcePoint(ResourceLocation dimension, Vec3 world) {
    }

    private static final class PowerIndex {
        private final Map<ResourceLocation, Map<Long, List<Vec3>>> byCell =
                new HashMap<>();
        private final Map<SourceOwner, List<SourcePoint>> byOwner =
                new HashMap<>();

        private void replaceGroup(TransformGroup group) {
            List<SourcePoint> points = new ArrayList<>();
            for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                    : group.cells().entrySet()) {
                if (source(entry.getValue())) {
                    points.add(new SourcePoint(group.dimension(),
                            group.cellCenter(entry.getKey())));
                }
            }
            replace(new SourceOwner(group.dimension(), group.id(), false),
                    points);
        }

        private void replaceSurface(ConstructionSurface surface) {
            List<SourcePoint> points = new ArrayList<>();
            for (Map.Entry<ConstructionSurface.SurfaceSlot,
                    ConstructionSurface.SurfaceAttachment> entry
                    : surface.attachments().entrySet()) {
                if (!source(entry.getValue().state())) continue;
                ConstructionSurface.SurfaceSlot slot = entry.getKey();
                double u = (slot.column() + 0.5D) / surface.columns();
                double v = (slot.row() + 0.5D) / surface.rows();
                points.add(new SourcePoint(surface.dimension(),
                        surface.gridPoint(u, v)
                                .add(surface.gridNormal(u, v).scale(0.5D))));
            }
            replace(new SourceOwner(surface.dimension(), surface.id(), true),
                    points);
        }

        private void replace(SourceOwner owner, List<SourcePoint> next) {
            List<SourcePoint> previous = byOwner.remove(owner);
            if (previous != null) {
                for (SourcePoint point : previous) remove(point);
            }
            if (next == null || next.isEmpty()) return;
            List<SourcePoint> immutable = List.copyOf(next);
            byOwner.put(owner, immutable);
            for (SourcePoint point : immutable) add(point);
        }

        private void add(SourcePoint point) {
            BlockPos cell = BlockPos.containing(point.world());
            byCell.computeIfAbsent(point.dimension(), ignored -> new HashMap<>())
                    .computeIfAbsent(cell.asLong(), ignored -> new ArrayList<>())
                    .add(point.world());
        }

        private void remove(SourcePoint point) {
            Map<Long, List<Vec3>> cells = byCell.get(point.dimension());
            if (cells == null) return;
            BlockPos cell = BlockPos.containing(point.world());
            List<Vec3> values = cells.get(cell.asLong());
            if (values == null) return;
            values.remove(point.world());
            if (values.isEmpty()) cells.remove(cell.asLong());
            if (cells.isEmpty()) byCell.remove(point.dimension());
        }

        private boolean hasSourceNear(ResourceLocation dimension, Vec3 center,
                double range) {
            Map<Long, List<Vec3>> cells = byCell.get(dimension);
            if (cells == null || cells.isEmpty()) return false;
            double maxDistanceSqr = range * range;
            BlockPos base = BlockPos.containing(center);
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
    }}
