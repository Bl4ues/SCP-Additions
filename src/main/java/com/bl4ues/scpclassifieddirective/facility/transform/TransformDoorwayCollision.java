package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup.GridPos;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Clips only overhanging world-axis AABB approximations around an open
 * transformed door. Both physical indices call this shared implementation.
 */
public final class TransformDoorwayCollision {
    // One rotated doorway must not carve the entire world-axis bounding
    // rectangle surrounding its width and depth: the corners of that rectangle
    // are the adjacent WALL. Use a small tiled mask in the door's local axes.
    private static final double CLEAR_HALF_WIDTH = 0.64D;
    private static final double CLEAR_BELOW = 0.48D;
    private static final double CLEAR_ABOVE = 1.55D;
    private static final double EPSILON = 1.0E-6D;
    private static final int WIDTH_TILES = 7;
    private static final int DEPTH_TILES = 5;
    private static final double CLEAR_HALF_DEPTH = 0.53D;

    private TransformDoorwayCollision() {
    }

    /** Only the authored neighbouring doors are looked up. The individual
     * opening geometry is shared with indexPassages so a doorway is generated
     * exactly once for its owner, not once for every nearby door. */
    public static List<AABB> nearbyPassages(TransformGroup group,
            GridPos source) {
        if (group == null || source == null) return List.of();
        List<AABB> result = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -2; dy <= 0; dy++) {
                    GridPos candidate = source.offset(dx, dy, dz);
                    result.addAll(passagesForDoor(group, candidate,
                            group.cells().get(candidate)));
                }
            }
        }
        return result;
    }

    private static List<AABB> passagesForDoor(TransformGroup group,
            GridPos cell, BlockState state) {
        if (group == null || cell == null
                || !FacilityModule.isFacilityDoor(state)
                || !FacilityModule.isDoorPassable(state)
                || !state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return List.of();
        }
        Vec3 center = group.cellCenter(cell);
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        Vec3 localWidth = facing.getAxis() == Direction.Axis.Z
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 width = TransformMath.rotate(localWidth,
                group.rotationX(), group.rotationY(), group.rotationZ());
        Vec3 through = TransformMath.rotate(
                Vec3.atLowerCornerOf(facing.getNormal()),
                group.rotationX(), group.rotationY(), group.rotationZ());
        width = new Vec3(width.x, 0.0D, width.z).normalize();
        through = new Vec3(through.x, 0.0D, through.z).normalize();
        if (width.lengthSqr() < EPSILON || through.lengthSqr() < EPSILON)
            return List.of();
        // VoxelShapes cannot encode a single arbitrarily rotated prism. Tile
        // its horizontal footprint instead, in BOTH door-local directions.
        // The old wide AABB union extended into the neighbouring wall by a
        // full block; this tiled mask stays within the actual door opening.
        double widthStep = CLEAR_HALF_WIDTH * 2.0D / WIDTH_TILES;
        double depthStep = CLEAR_HALF_DEPTH * 2.0D / DEPTH_TILES;
        double tileHalfX = Math.abs(width.x) * (widthStep * 0.5D + 0.004D)
                + Math.abs(through.x) * (depthStep * 0.5D + 0.004D);
        double tileHalfZ = Math.abs(width.z) * (widthStep * 0.5D + 0.004D)
                + Math.abs(through.z) * (depthStep * 0.5D + 0.004D);
        List<AABB> result = new ArrayList<>(WIDTH_TILES * DEPTH_TILES);
        for (int w = 0; w < WIDTH_TILES; w++) {
            double sideways = (w + 0.5D) * widthStep - CLEAR_HALF_WIDTH;
            for (int d = 0; d < DEPTH_TILES; d++) {
                double along = (d + 0.5D) * depthStep - CLEAR_HALF_DEPTH;
                Vec3 at = center.add(width.scale(sideways))
                        .add(through.scale(along));
                result.add(new AABB(at.x - tileHalfX,
                        center.y - CLEAR_BELOW, at.z - tileHalfZ,
                        at.x + tileHalfX, center.y + CLEAR_ABOVE,
                        at.z + tileHalfZ));
            }
        }
        return result;
    }

    /** Immutable, world-cell keyed openings for every active transformed door.
     * Both physical indices use this cache to clip the FINAL combined collision
     * rather than only the group which owns the door. */
    public static Map<Long, List<AABB>> indexPassages(
            Collection<TransformGroup> groups) {
        if (groups == null || groups.isEmpty()) return Map.of();
        Map<Long, List<AABB>> indexed = new HashMap<>();
        for (TransformGroup group : groups) {
            for (Map.Entry<GridPos, BlockState> entry : group.cells().entrySet()) {
                BlockState state = entry.getValue();
                if (!FacilityModule.isFacilityDoor(state)
                        || !FacilityModule.isDoorPassable(state)
                        || !state.hasProperty(HorizontalDirectionalBlock.FACING)) {
                    continue;
                }
                for (AABB passage : passagesForDoor(group,
                        entry.getKey(), state)) {
                    int x0 = (int) Math.floor(passage.minX);
                    int x1 = (int) Math.floor(passage.maxX - EPSILON);
                    int y0 = (int) Math.floor(passage.minY);
                    int y1 = (int) Math.floor(passage.maxY - EPSILON);
                    int z0 = (int) Math.floor(passage.minZ);
                    int z1 = (int) Math.floor(passage.maxZ - EPSILON);
                    for (int x = x0; x <= x1; x++) {
                        for (int y = y0; y <= y1; y++) {
                            for (int z = z0; z <= z1; z++) {
                                indexed.computeIfAbsent(
                                        BlockPos.asLong(x, y, z),
                                        ignored -> new ArrayList<>()).add(passage);
                            }
                        }
                    }
                }
            }
        }
        Map<Long, List<AABB>> result = new HashMap<>();
        indexed.forEach((key,value) -> result.put(key, List.copyOf(value)));
        return Map.copyOf(result);
    }

    /** World-space clip of a logical world-cell VoxelShape. Call only when the
     * passage cache contains this cell and memoize its result at index level. */
    public static VoxelShape clipShape(VoxelShape original, BlockPos cell,
            Map<Long, List<AABB>> indexed) {
        if (original == null || original.isEmpty() || cell == null
                || indexed == null || indexed.isEmpty()) return original;
        List<AABB> passages = indexed.get(cell.asLong());
        if (passages == null || passages.isEmpty()) return original;
        VoxelShape result = Shapes.empty();
        for (AABB local : original.toAabbs()) {
            for (AABB remaining : clip(local.move(cell), passages)) {
                result = Shapes.or(result, Shapes.create(
                        remaining.move(-cell.getX(), -cell.getY(),
                                -cell.getZ())));
            }
        }
        return result.optimize();
    }

    public static List<AABB> clip(AABB box, List<AABB> passages) {
        if (box == null) return List.of();
        if (passages == null || passages.isEmpty()) return List.of(box);
        List<AABB> remaining = List.of(box);
        for (AABB passage : passages) {
            List<AABB> next = new ArrayList<>();
            for (AABB part : remaining) subtract(part, passage, next);
            if (next.isEmpty()) return List.of();
            remaining = next;
        }
        return remaining;
    }

    private static void subtract(AABB box, AABB passage,
            List<AABB> result) {
        if (!box.intersects(passage)) {
            result.add(box);
            return;
        }
        double x0 = Math.max(box.minX, passage.minX);
        double x1 = Math.min(box.maxX, passage.maxX);
        double y0 = Math.max(box.minY, passage.minY);
        double y1 = Math.min(box.maxY, passage.maxY);
        double z0 = Math.max(box.minZ, passage.minZ);
        double z1 = Math.min(box.maxZ, passage.maxZ);

        add(result, box.minX, box.minY, box.minZ,
                x0, box.maxY, box.maxZ);
        add(result, x1, box.minY, box.minZ,
                box.maxX, box.maxY, box.maxZ);
        add(result, x0, box.minY, box.minZ,
                x1, y0, box.maxZ);
        add(result, x0, y1, box.minZ,
                x1, box.maxY, box.maxZ);
        add(result, x0, y0, box.minZ,
                x1, y1, z0);
        add(result, x0, y0, z1,
                x1, y1, box.maxZ);
    }

    private static void add(List<AABB> result, double x0, double y0,
            double z0, double x1, double y1, double z1) {
        if (x1 - x0 > EPSILON && y1 - y0 > EPSILON
                && z1 - z0 > EPSILON) {
            result.add(new AABB(x0, y0, z0, x1, y1, z1));
        }
    }
}
