package com.bl4ues.scpclassifieddirective.facility.transform.client;

import com.bl4ues.scpclassifieddirective.facility.transform.ConstructionSurface;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionClientBridge;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformConstructionManager;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformMath;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformSurfaceGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Client mirror plus transient editor selection for transformed construction. */
public final class TransformConstructionClientState {
    private static final int GROUP_SUBDIVISIONS = 3;
    private static final double SURFACE_SELECTION_THICKNESS = 0.055D;

    private static ResourceLocation dimension;
    private static List<TransformGroup> groups = List.of();
    private static List<ConstructionSurface> surfaces = List.of();
    private static Map<Long, TransformConstructionManager.ProxyCell> proxyCells =
            Map.of();
    private static Selection selection;
    private static EditMode mode = EditMode.MOVE;
    private static Axis axis = Axis.X;

    static {
        TransformConstructionClientBridge.install(
                new TransformConstructionClientBridge.Provider() {
                    @Override
                    public VoxelShape selection(BlockPos pos) {
                        return proxySelectionShape(pos);
                    }

                    @Override
                    public VoxelShape collision(BlockPos pos) {
                        return proxyCollisionShape(pos);
                    }
                });
    }

    private TransformConstructionClientState() {
    }

    public static void sync(ResourceLocation nextDimension,
            List<TransformGroup> nextGroups,
            List<ConstructionSurface> nextSurfaces) {
        dimension = nextDimension;
        groups = nextGroups == null ? List.of() : List.copyOf(nextGroups);
        surfaces = nextSurfaces == null ? List.of() : List.copyOf(nextSurfaces);
        rebuildProxyCells();
        if (selection != null && !selectionStillExists()) selection = null;
    }

    public static void clear() {
        dimension = null;
        groups = List.of();
        surfaces = List.of();
        proxyCells = Map.of();
        selection = null;
    }

    public static ResourceLocation dimension() {
        return dimension;
    }

    public static List<TransformGroup> groups(ResourceLocation targetDimension) {
        return targetDimension != null && targetDimension.equals(dimension)
                ? groups : List.of();
    }

    public static List<ConstructionSurface> surfaces(
            ResourceLocation targetDimension) {
        return targetDimension != null && targetDimension.equals(dimension)
                ? surfaces : List.of();
    }

    public static TransformGroup group(UUID id) {
        if (id == null) return null;
        for (TransformGroup group : groups) {
            if (id.equals(group.id())) return group;
        }
        return null;
    }

    public static ConstructionSurface surface(UUID id) {
        if (id == null) return null;
        for (ConstructionSurface surface : surfaces) {
            if (id.equals(surface.id())) return surface;
        }
        return null;
    }

    public static VoxelShape proxySelectionShape(BlockPos pos) {
        TransformConstructionManager.ProxyCell cell = proxyCell(pos);
        return cell == null ? Shapes.empty() : cell.selection();
    }

    public static VoxelShape proxyCollisionShape(BlockPos pos) {
        TransformConstructionManager.ProxyCell cell = proxyCell(pos);
        return cell == null ? Shapes.empty() : cell.collision();
    }

    private static TransformConstructionManager.ProxyCell proxyCell(BlockPos pos) {
        return pos == null ? null : proxyCells.get(pos.asLong());
    }

    public static void upsertGroup(TransformGroup replacement) {
        if (replacement == null || dimension == null
                || !dimension.equals(replacement.dimension())) return;
        ArrayList<TransformGroup> next = new ArrayList<>(groups);
        for (int index = 0; index < next.size(); index++) {
            if (next.get(index).id().equals(replacement.id())) {
                next.set(index, replacement);
                groups = List.copyOf(next);
                rebuildProxyCells();
                return;
            }
        }
        next.add(replacement);
        groups = List.copyOf(next);
        rebuildProxyCells();
    }

    public static void upsertSurface(ConstructionSurface replacement) {
        if (replacement == null || dimension == null
                || !dimension.equals(replacement.dimension())) return;
        ArrayList<ConstructionSurface> next = new ArrayList<>(surfaces);
        for (int index = 0; index < next.size(); index++) {
            if (next.get(index).id().equals(replacement.id())) {
                next.set(index, replacement);
                surfaces = List.copyOf(next);
                rebuildProxyCells();
                return;
            }
        }
        next.add(replacement);
        surfaces = List.copyOf(next);
        rebuildProxyCells();
    }

    public static void remove(UUID id, boolean surface) {
        if (id == null) return;
        if (surface) {
            ArrayList<ConstructionSurface> next = new ArrayList<>(surfaces);
            if (next.removeIf(value -> id.equals(value.id()))) {
                surfaces = List.copyOf(next);
                rebuildProxyCells();
            }
        } else {
            ArrayList<TransformGroup> next = new ArrayList<>(groups);
            if (next.removeIf(value -> id.equals(value.id()))) {
                groups = List.copyOf(next);
                rebuildProxyCells();
            }
        }
        if (selection != null && id.equals(selection.id())) selection = null;
    }

    public static Selection selection() {
        return selection;
    }

    public static void selectGroup(UUID id) {
        selection = id == null ? null
                : new Selection(SelectionType.GROUP, id, SurfaceHandle.CENTER);
        mode = EditMode.MOVE;
    }

    public static void selectSurface(UUID id, SurfaceHandle handle) {
        selection = id == null ? null : new Selection(SelectionType.SURFACE, id,
                handle == null ? SurfaceHandle.CENTER : handle);
        mode = EditMode.MOVE;
    }

    public static void clearSelection() {
        selection = null;
    }

    public static EditMode mode() {
        return mode;
    }

    public static void setMode(EditMode next) {
        if (next != null) mode = next;
    }

    public static Axis axis() {
        return axis;
    }

    public static void setAxis(Axis next) {
        if (next != null) axis = next;
    }

    public static Vec3 axisVector() {
        return switch (axis) {
            case X -> new Vec3(1.0D, 0.0D, 0.0D);
            case Y -> new Vec3(0.0D, 1.0D, 0.0D);
            case Z -> new Vec3(0.0D, 0.0D, 1.0D);
        };
    }

    private static void rebuildProxyCells() {
        if (dimension == null) {
            proxyCells = Map.of();
            return;
        }
        Map<Long, MutableProxyCell> mutable = new LinkedHashMap<>();
        for (TransformGroup group : groups) {
            if (dimension.equals(group.dimension())) addGroup(mutable, group);
        }
        for (ConstructionSurface surface : surfaces) {
            if (dimension.equals(surface.dimension())) addSurface(mutable, surface);
        }
        Map<Long, TransformConstructionManager.ProxyCell> frozen =
                new LinkedHashMap<>();
        mutable.forEach((key, value) -> frozen.put(key, value.freeze()));
        proxyCells = Map.copyOf(frozen);
    }

    private static void addGroup(Map<Long, MutableProxyCell> index,
            TransformGroup group) {
        for (Map.Entry<TransformGroup.GridPos, BlockState> entry
                : group.cells().entrySet()) {
            TransformGroup.GridPos cell = entry.getKey();
            BlockState state = entry.getValue();
            boolean placeholder = state == null || state.isAir();
            AABB selection = new AABB(cell.x() - 0.5D, cell.y() - 0.5D,
                    cell.z() - 0.5D, cell.x() + 0.5D, cell.y() + 0.5D,
                    cell.z() + 0.5D);
            addWorldBox(index, transformedBounds(group, selection), group.id(),
                    null, true, false,
                    placeholder ? 0 : state.getLightEmission());
            if (placeholder) continue;

            VoxelShape collision = state.getCollisionShape(
                    EmptyBlockGetter.INSTANCE, BlockPos.ZERO,
                    CollisionContext.empty());
            if (collision.isEmpty()) continue;
            int subdivisions = nearOrthogonal(group) ? 1 : GROUP_SUBDIVISIONS;
            double inv = 1.0D / subdivisions;
            collision.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
                double boxX = maxX - minX;
                double boxY = maxY - minY;
                double boxZ = maxZ - minZ;
                for (int sx = 0; sx < subdivisions; sx++) {
                    for (int sy = 0; sy < subdivisions; sy++) {
                        for (int sz = 0; sz < subdivisions; sz++) {
                            AABB local = new AABB(
                                    cell.x() - 0.5D + minX + boxX * sx * inv,
                                    cell.y() - 0.5D + minY + boxY * sy * inv,
                                    cell.z() - 0.5D + minZ + boxZ * sz * inv,
                                    cell.x() - 0.5D + minX + boxX * (sx + 1) * inv,
                                    cell.y() - 0.5D + minY + boxY * (sy + 1) * inv,
                                    cell.z() - 0.5D + minZ + boxZ * (sz + 1) * inv);
                            addWorldBox(index, transformedBounds(group, local),
                                    group.id(), null, false, true,
                                    state.getLightEmission());
                        }
                    }
                }
            });
        }
    }

    private static void addSurface(Map<Long, MutableProxyCell> index,
            ConstructionSurface surface) {
        int columns = surface.columns();
        int rows = surface.rows();
        for (int column = 0; column < columns; column++) {
            for (int row = 0; row < rows; row++) {
                ConstructionSurface.SurfaceSlot slot =
                        new ConstructionSurface.SurfaceSlot(column, row);
                ConstructionSurface.SurfaceAttachment attachment =
                        surface.attachments().get(slot);
                addWorldBox(index, surfaceSlotBounds(surface, column, row,
                                SURFACE_SELECTION_THICKNESS),
                        null, surface.id(), true, false, 0);
                if (attachment == null || attachment.state().isAir()) continue;
                for (AABB collision : TransformSurfaceGeometry.collisionBoxes(
                        surface, slot, attachment)) {
                    addWorldBox(index, collision, null, surface.id(), false, true,
                            attachment.state().getLightEmission());
                }
            }
        }
    }

    private static AABB transformedBounds(TransformGroup group, AABB local) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (int xi = 0; xi < 2; xi++) {
            for (int yi = 0; yi < 2; yi++) {
                for (int zi = 0; zi < 2; zi++) {
                    Vec3 localPoint = new Vec3(
                            xi == 0 ? local.minX : local.maxX,
                            yi == 0 ? local.minY : local.maxY,
                            zi == 0 ? local.minZ : local.maxZ);
                    Vec3 world = TransformMath.localToWorld(group.origin(),
                            localPoint, group.rotationX(), group.rotationY(),
                            group.rotationZ());
                    minX = Math.min(minX, world.x);
                    minY = Math.min(minY, world.y);
                    minZ = Math.min(minZ, world.z);
                    maxX = Math.max(maxX, world.x);
                    maxY = Math.max(maxY, world.y);
                    maxZ = Math.max(maxZ, world.z);
                }
            }
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static AABB surfaceSlotBounds(ConstructionSurface surface,
            int column, int row, double thickness) {
        int columns = surface.columns();
        int rows = surface.rows();
        double u0 = column / (double) columns;
        double u1 = (column + 1.0D) / columns;
        double v0 = row / (double) rows;
        double v1 = (row + 1.0D) / rows;
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        double half = thickness * 0.5D;
        for (int ui = 0; ui <= 2; ui++) {
            double u = u0 + (u1 - u0) * ui / 2.0D;
            for (int vi = 0; vi <= 2; vi++) {
                double v = v0 + (v1 - v0) * vi / 2.0D;
                Vec3 point = surface.gridPoint(u, v);
                Vec3 normal = surface.gridNormal(u, v).scale(half);
                for (int sign : new int[]{-1, 1}) {
                    Vec3 world = point.add(normal.scale(sign));
                    minX = Math.min(minX, world.x);
                    minY = Math.min(minY, world.y);
                    minZ = Math.min(minZ, world.z);
                    maxX = Math.max(maxX, world.x);
                    maxY = Math.max(maxY, world.y);
                    maxZ = Math.max(maxZ, world.z);
                }
            }
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void addWorldBox(Map<Long, MutableProxyCell> index,
            AABB worldBox, UUID groupId, UUID surfaceId, boolean selection,
            boolean collision, int light) {
        int minX = (int) Math.floor(worldBox.minX);
        int minY = (int) Math.floor(worldBox.minY);
        int minZ = (int) Math.floor(worldBox.minZ);
        int maxX = (int) Math.floor(Math.nextDown(worldBox.maxX));
        int maxY = (int) Math.floor(Math.nextDown(worldBox.maxY));
        int maxZ = (int) Math.floor(Math.nextDown(worldBox.maxZ));
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    AABB cell = new AABB(x, y, z, x + 1.0D, y + 1.0D,
                            z + 1.0D);
                    AABB clipped = intersect(worldBox, cell);
                    if (clipped == null) continue;
                    AABB local = clipped.move(-x, -y, -z);
                    index.computeIfAbsent(BlockPos.asLong(x, y, z),
                                    ignored -> new MutableProxyCell())
                            .add(local, groupId, surfaceId, selection, collision,
                                    light);
                }
            }
        }
    }

    private static AABB intersect(AABB first, AABB second) {
        double minX = Math.max(first.minX, second.minX);
        double minY = Math.max(first.minY, second.minY);
        double minZ = Math.max(first.minZ, second.minZ);
        double maxX = Math.min(first.maxX, second.maxX);
        double maxY = Math.min(first.maxY, second.maxY);
        double maxZ = Math.min(first.maxZ, second.maxZ);
        return minX < maxX && minY < maxY && minZ < maxZ
                ? new AABB(minX, minY, minZ, maxX, maxY, maxZ) : null;
    }

    private static boolean nearOrthogonal(TransformGroup group) {
        return nearRightAngle(group.rotationX())
                && nearRightAngle(group.rotationY())
                && nearRightAngle(group.rotationZ());
    }

    private static boolean nearRightAngle(float degrees) {
        double normalized = Math.abs(degrees % 90.0F);
        return normalized < 0.01D
                || Math.abs(normalized - 90.0D) < 0.01D;
    }

    private static boolean selectionStillExists() {
        if (selection == null) return false;
        return selection.type == SelectionType.GROUP
                ? group(selection.id) != null : surface(selection.id) != null;
    }

    private static final class MutableProxyCell {
        private VoxelShape selection = Shapes.empty();
        private VoxelShape collision = Shapes.empty();
        private int light;
        private TransformConstructionManager.ProxyCell frozen;
        private final Set<UUID> groupIds = new LinkedHashSet<>();
        private final Set<UUID> surfaceIds = new LinkedHashSet<>();

        private void add(AABB local, UUID groupId, UUID surfaceId,
                boolean hasSelection, boolean hasCollision, int light) {
            VoxelShape shape = Shapes.create(local);
            if (hasSelection) selection = Shapes.or(selection, shape);
            if (hasCollision) collision = Shapes.or(collision, shape);
            this.light = Math.max(this.light, Math.max(0, Math.min(15, light)));
            if (groupId != null) groupIds.add(groupId);
            if (surfaceId != null) surfaceIds.add(surfaceId);
            frozen = null;
        }

        private TransformConstructionManager.ProxyCell freeze() {
            if (frozen == null) {
                frozen = new TransformConstructionManager.ProxyCell(
                        selection.optimize(), collision.optimize(), light,
                        Set.copyOf(groupIds), Set.copyOf(surfaceIds));
            }
            return frozen;
        }
    }

    public enum SelectionType { GROUP, SURFACE }
    public enum EditMode { MOVE, ROTATE }
    public enum Axis { X, Y, Z }
    public enum SurfaceHandle {
        BOTTOM_START, BOTTOM_END, TOP_START, TOP_END, CENTER
    }

    public record Selection(SelectionType type, UUID id,
            SurfaceHandle handle) {
    }
}
