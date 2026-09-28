package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup.GridPos;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Walking-space relief for empty cells inside rotated Off-Grid walls.
 *
 * Rotated block collision is represented by small world-axis AABBs. Those
 * boxes are deliberately conservative, but at diagonal angles their corners
 * can protrude into a neighbouring authored empty cell and make a visually
 * clear 1x2 opening too narrow for the player's axis-aligned hitbox. Detect
 * bounded horizontal gaps and carve only the collision contributions of the
 * same source group against an oriented passage through that empty cell.
 */
public final class TransformOpenCellCollision {
    private static final double CLEAR_HALF_WIDTH = 0.55D;
    private static final double CLEAR_HALF_DEPTH = 0.58D;
    private static final double CLEAR_HALF_HEIGHT = 0.51D;
    private static final int WIDTH_TILES = 7;
    private static final int DEPTH_TILES = 5;
    private static final int MAX_GAP_SPAN = 4;
    private static final double EPSILON = 1.0E-6D;

    private TransformOpenCellCollision() {
    }

    /**
     * Only horizontal neighbours can receive spill from an upright wall cell.
     * Wider authored openings are still detected by looking for the opposite
     * solid edge up to MAX_GAP_SPAN cells away.
     */
    public static List<AABB> nearbyPassages(TransformGroup group,
            GridPos source) {
        if (group == null || source == null) return List.of();
        List<AABB> result = new ArrayList<>();
        addCandidate(group, source, source.offset(-1, 0, 0), result);
        addCandidate(group, source, source.offset(1, 0, 0), result);
        addCandidate(group, source, source.offset(0, 0, -1), result);
        addCandidate(group, source, source.offset(0, 0, 1), result);
        return result.isEmpty() ? List.of() : List.copyOf(result);
    }

    /**
     * World-cell indexed passage masks for all authored empty cells that form
     * bounded wall openings. This mirrors TransformDoorwayCollision's final
     * aggregate clipping so incremental edits cannot leave stale neighbour
     * AABBs inside a newly-created opening.
     */
    public static Map<Long, List<AABB>> indexPassages(
            Collection<TransformGroup> groups) {
        if (groups == null || groups.isEmpty()) return Map.of();

        Map<Long, List<AABB>> indexed = new HashMap<>();
        for (TransformGroup group : groups) {
            if (group == null || group.cells().isEmpty()) continue;
            Set<GridPos> gaps = new LinkedHashSet<>();
            for (Map.Entry<GridPos, BlockState> entry
                    : group.cells().entrySet()) {
                if (!hasCollision(group, entry.getKey())) continue;
                GridPos source = entry.getKey();
                for (GridPos gap : List.of(
                        source.offset(-1, 0, 0),
                        source.offset(1, 0, 0),
                        source.offset(0, 0, -1),
                        source.offset(0, 0, 1))) {
                    if (!hasCollision(group, gap)
                            && boundedOpening(group, gap)) {
                        gaps.add(gap);
                    }
                }
            }

            for (GridPos gap : gaps) {
                List<AABB> passages = passagesForGap(group, gap);
                for (AABB passage : passages) {
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
                                        ignored -> new ArrayList<>())
                                        .add(passage);
                            }
                        }
                    }
                }
            }
        }

        Map<Long, List<AABB>> result = new HashMap<>();
        indexed.forEach((key, value) ->
                result.put(key, List.copyOf(value)));
        return Map.copyOf(result);
    }

    private static boolean boundedOpening(TransformGroup group, GridPos gap) {
        boolean boundedX = distanceToSolid(group, gap, -1, 0) > 0
                && distanceToSolid(group, gap, 1, 0) > 0;
        boolean boundedZ = distanceToSolid(group, gap, 0, -1) > 0
                && distanceToSolid(group, gap, 0, 1) > 0;
        return boundedX || boundedZ;
    }

    private static List<AABB> passagesForGap(TransformGroup group,
            GridPos gap) {
        int xNeg = distanceToSolid(group, gap, -1, 0);
        int xPos = distanceToSolid(group, gap, 1, 0);
        int zNeg = distanceToSolid(group, gap, 0, -1);
        int zPos = distanceToSolid(group, gap, 0, 1);
        boolean boundedX = xNeg > 0 && xPos > 0;
        boolean boundedZ = zNeg > 0 && zPos > 0;
        if (!boundedX && !boundedZ) return List.of();

        boolean widthAlongX;
        if (boundedX && boundedZ) {
            widthAlongX = xNeg + xPos <= zNeg + zPos;
        } else {
            widthAlongX = boundedX;
        }

        Vec3 localWidth = widthAlongX
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 localThrough = widthAlongX
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 width = TransformMath.rotate(localWidth,
                group.rotationX(), group.rotationY(), group.rotationZ());
        Vec3 through = TransformMath.rotate(localThrough,
                group.rotationX(), group.rotationY(), group.rotationZ());
        width = new Vec3(width.x, 0.0D, width.z);
        through = new Vec3(through.x, 0.0D, through.z);
        if (width.lengthSqr() < EPSILON || through.lengthSqr() < EPSILON) {
            return List.of();
        }

        List<AABB> result = new ArrayList<>(WIDTH_TILES * DEPTH_TILES);
        addOrientedPassage(group.cellCenter(gap), width.normalize(),
                through.normalize(), result);
        return List.copyOf(result);
    }

    private static void addCandidate(TransformGroup group, GridPos source,
            GridPos gap, List<AABB> result) {
        if (hasCollision(group, gap)) return;

        result.addAll(passagesForGap(group, gap));
    }

    private static int distanceToSolid(TransformGroup group, GridPos start,
            int stepX, int stepZ) {
        for (int distance = 1; distance <= MAX_GAP_SPAN; distance++) {
            GridPos candidate = start.offset(stepX * distance, 0,
                    stepZ * distance);
            if (hasCollision(group, candidate)) return distance;
        }
        return -1;
    }

    private static boolean hasCollision(TransformGroup group, GridPos cell) {
        BlockState state = group.cells().get(cell);
        if (state == null || state.isAir()) return false;
        if (FacilityModule.isFacilityDoor(state)
                && FacilityModule.isDoorPassable(state)) {
            return false;
        }
        return !state.getCollisionShape(EmptyBlockGetter.INSTANCE,
                BlockPos.ZERO, CollisionContext.empty()).isEmpty();
    }

    private static void addOrientedPassage(Vec3 center, Vec3 width,
            Vec3 through, List<AABB> result) {
        double widthStep = CLEAR_HALF_WIDTH * 2.0D / WIDTH_TILES;
        double depthStep = CLEAR_HALF_DEPTH * 2.0D / DEPTH_TILES;
        double tileHalfX = Math.abs(width.x)
                * (widthStep * 0.5D + 0.006D)
                + Math.abs(through.x)
                * (depthStep * 0.5D + 0.006D);
        double tileHalfZ = Math.abs(width.z)
                * (widthStep * 0.5D + 0.006D)
                + Math.abs(through.z)
                * (depthStep * 0.5D + 0.006D);

        for (int w = 0; w < WIDTH_TILES; w++) {
            double sideways = (w + 0.5D) * widthStep - CLEAR_HALF_WIDTH;
            for (int d = 0; d < DEPTH_TILES; d++) {
                double along = (d + 0.5D) * depthStep - CLEAR_HALF_DEPTH;
                Vec3 at = center.add(width.scale(sideways))
                        .add(through.scale(along));
                result.add(new AABB(at.x - tileHalfX,
                        center.y - CLEAR_HALF_HEIGHT,
                        at.z - tileHalfZ,
                        at.x + tileHalfX,
                        center.y + CLEAR_HALF_HEIGHT,
                        at.z + tileHalfZ));
            }
        }
    }
}
