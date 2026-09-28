package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup.GridPos;
import java.util.ArrayList;
import java.util.List;
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

    private static void addCandidate(TransformGroup group, GridPos source,
            GridPos gap, List<AABB> result) {
        if (hasCollision(group, gap)) return;

        int xNeg = distanceToSolid(group, gap, -1, 0);
        int xPos = distanceToSolid(group, gap, 1, 0);
        int zNeg = distanceToSolid(group, gap, 0, -1);
        int zPos = distanceToSolid(group, gap, 0, 1);
        boolean boundedX = xNeg > 0 && xPos > 0;
        boolean boundedZ = zNeg > 0 && zPos > 0;
        if (!boundedX && !boundedZ) return;

        boolean widthAlongX;
        if (gap.x() != source.x() && boundedX) {
            widthAlongX = true;
        } else if (gap.z() != source.z() && boundedZ) {
            widthAlongX = false;
        } else if (boundedX && boundedZ) {
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
            return;
        }
        width = width.normalize();
        through = through.normalize();
        addOrientedPassage(group.cellCenter(gap), width, through, result);
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
