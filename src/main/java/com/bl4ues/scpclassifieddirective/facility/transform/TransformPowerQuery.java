package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.keycard.KeycardReaderLevels;

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
        // Parent-world redstone may feed the local grid at the physical cell,
        // but transformed sources do not use Euclidean "nearby" power here.
        // Inside one authored grid, adjacency is discrete just like vanilla.
        return vanillaPowered(level, group.cellCenter(consumer));
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
            if (sourceAtLogicalSlot(surface, neighbor)) return true;
        }
        return vanillaPowered(level, TransformSurfaceGeometry.cellCenter(
                surface, consumer, TransformSurfaceGeometry.MAIN_SIDE,
                false));
    }

    public static boolean powered(ServerLevel level, Vec3 center) {
        if (level == null || center == null) return false;
        return vanillaPowered(level, center)
                || index(level.getServer()).hasSourceNear(
                        level.dimension().location(), center, 1.05D);
    }

    private static boolean vanillaPowered(ServerLevel level, Vec3 center) {
        if (level == null || center == null) return false;
        BlockPos base = BlockPos.containing(center);
        if (level.hasNeighborSignal(base) || level.hasNeighborSignal(base.above())) {
            return true;
        }
        for (net.minecraft.core.Direction direction
                : net.minecraft.core.Direction.values()) {
            if (level.hasNeighborSignal(base.relative(direction))) return true;
        }
        return false;
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

    public static synchronized void refreshGroupCell(MinecraftServer server,
            TransformGroup group, TransformGroup.GridPos cell) {
        if (server == null || group == null || cell == null) return;
        PowerIndex index = INDEXES.get(server);
        if (index != null) index.replaceGroupCell(group, cell);
    }

    public static synchronized void refreshSurfaceSlot(MinecraftServer server,
            ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot) {
        if (server == null || surface == null || slot == null) return;
        PowerIndex index = INDEXES.get(server);
        if (index != null) index.replaceSurfaceSlot(surface, slot);
    }

    public static synchronized void removeGroup(MinecraftServer server,
            ResourceLocation dimension, UUID id) {
        if (server == null || dimension == null || id == null) return;
        PowerIndex index = INDEXES.get(server);
        if (index != null) index.removeOwner(dimension, id, false);
    }

    public static synchronized void removeSurface(MinecraftServer server,
            ResourceLocation dimension, UUID id) {
        if (server == null || dimension == null || id == null) return;
        PowerIndex index = INDEXES.get(server);
        if (index != null) index.removeOwner(dimension, id, true);
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
        TransformWallFixturePlacement.DoorButtonPhase buttonPhase =
                TransformWallFixturePlacement.doorButtonPhase(state);
        if (buttonPhase == TransformWallFixturePlacement.DoorButtonPhase.OPENING
                || buttonPhase == TransformWallFixturePlacement.DoorButtonPhase.OPEN) {
            return true;
        }
        KeycardReaderLevels.ReaderDescriptor reader =
                KeycardReaderLevels.describe(state);
        if (reader != null && state.getBlock()
                == KeycardReaderLevels.acceptedBlock(
                        reader.level(), reader.side())) {
            return true;
        }
        return state.hasProperty(BlockStateProperties.POWERED)
                && state.getValue(BlockStateProperties.POWERED);
    }

    private static boolean sourceAtLogicalSlot(
            ConstructionSurface surface,
            ConstructionSurface.SurfaceSlot slot) {
        if (surface == null || slot == null
                || slot.column() < 0 || slot.column() >= surface.columns()
                || slot.row() < 0 || slot.row() >= surface.rows()) {
            return false;
        }
        ConstructionSurface.SurfaceAttachment main =
                surface.attachments().get(slot);
        if (main != null && source(main.state())) return true;
        ConstructionSurface.SurfaceAttachment positive =
                surface.overlay(slot, 1);
        if (positive != null && source(positive.state())) return true;
        ConstructionSurface.SurfaceAttachment negative =
                surface.overlay(slot, -1);
        return negative != null && source(negative.state());
    }

    private record SourceOwner(ResourceLocation dimension, UUID id,
            boolean surface, int x, int y, int z) {
        private static SourceOwner group(TransformGroup group,
                TransformGroup.GridPos cell) {
            return new SourceOwner(group.dimension(), group.id(), false,
                    cell.x(), cell.y(), cell.z());
        }

        private static SourceOwner surface(ConstructionSurface surface,
                ConstructionSurface.SurfaceSlot slot) {
            return new SourceOwner(surface.dimension(), surface.id(), true,
                    slot.column(), slot.row(), 0);
        }
    }

    private record SourcePoint(ResourceLocation dimension, Vec3 world) {
    }

    private static final class PowerIndex {
        private final Map<ResourceLocation, Map<Long, List<Vec3>>> byCell =
                new HashMap<>();
        private final Map<SourceOwner, List<SourcePoint>> byOwner =
                new HashMap<>();

        private void replaceGroup(TransformGroup group) {
            removeOwner(group.dimension(), group.id(), false);
            for (TransformGroup.GridPos cell : group.cells().keySet()) {
                replaceGroupCell(group, cell);
            }
        }

        private void replaceGroupCell(TransformGroup group,
                TransformGroup.GridPos cell) {
            SourceOwner owner = SourceOwner.group(group, cell);
            BlockState state = group.cells().get(cell);
            if (!source(state)) {
                replace(owner, List.of());
                return;
            }
            replace(owner, List.of(new SourcePoint(group.dimension(),
                    group.cellCenter(cell))));
        }

        private void replaceSurface(ConstructionSurface surface) {
            removeOwner(surface.dimension(), surface.id(), true);
            java.util.LinkedHashSet<ConstructionSurface.SurfaceSlot> slots =
                    new java.util.LinkedHashSet<>(surface.attachments().keySet());
            for (ConstructionSurface.SurfaceOverlaySlot overlay
                    : surface.overlays().keySet()) {
                slots.add(overlay.slot());
            }
            for (ConstructionSurface.SurfaceSlot slot : slots) {
                replaceSurfaceSlot(surface, slot);
            }
        }

        private void replaceSurfaceSlot(ConstructionSurface surface,
                ConstructionSurface.SurfaceSlot slot) {
            SourceOwner owner = SourceOwner.surface(surface, slot);
            List<SourcePoint> points = new ArrayList<>(3);
            ConstructionSurface.SurfaceAttachment main =
                    surface.attachments().get(slot);
            if (main != null && source(main.state())) {
                points.add(new SourcePoint(surface.dimension(),
                        TransformSurfaceGeometry.cellCenter(surface, slot,
                                TransformSurfaceGeometry.MAIN_SIDE, false)));
            }
            ConstructionSurface.SurfaceAttachment positive =
                    surface.overlay(slot, 1);
            if (positive != null && source(positive.state())) {
                points.add(new SourcePoint(surface.dimension(),
                        TransformSurfaceGeometry.cellCenter(surface, slot,
                                1, true)));
            }
            ConstructionSurface.SurfaceAttachment negative =
                    surface.overlay(slot, -1);
            if (negative != null && source(negative.state())) {
                points.add(new SourcePoint(surface.dimension(),
                        TransformSurfaceGeometry.cellCenter(surface, slot,
                                -1, true)));
            }
            replace(owner, points);
        }

        private void removeOwner(ResourceLocation dimension, UUID id,
                boolean surface) {
            List<SourceOwner> matches = byOwner.keySet().stream()
                    .filter(owner -> owner.surface() == surface
                            && owner.id().equals(id)
                            && owner.dimension().equals(dimension))
                    .toList();
            for (SourceOwner owner : matches) replace(owner, List.of());
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
