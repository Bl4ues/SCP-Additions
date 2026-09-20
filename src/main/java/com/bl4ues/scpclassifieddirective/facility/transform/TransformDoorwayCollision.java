package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.transform.TransformGroup.GridPos;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Retains accurate physical collision around an open transformed door.
 *
 * A rotated solid cell is represented by small world-axis-aligned boxes. The
 * boxes can protrude across the authored boundary into the neighbouring empty
 * door cell even when the real rotated solid does not. Clip ONLY those nearby
 * approximation boxes against an inscribed central walking passage; preserve
 * the door's frame, the rest of each wall, and unrelated construction owners.
 * Client and server use this same geometry so movement prediction agrees.
 */
public final class TransformDoorwayCollision {
    private static final double CLEAR_HALF_WIDTH = 0.42D;
    private static final double CLEAR_BELOW = 0.48D;
    private static final double CLEAR_ABOVE = 1.55D;
    private static final double EPSILON = 1.0E-6D;

    private TransformDoorwayCollision() {
    }

    public static List<AABB> nearbyPassages(TransformGroup group,
            GridPos source) {
        if (group == null || source == null) return List.of();
        List<AABB> result = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -2; dy <= 0; dy++) {
                    GridPos candidate = source.offset(dx, dy, dz);
                    BlockState state = group.cells().get(candidate);
                    if (!FacilityModule.isFacilityDoor(state)
                            || !FacilityModule.isDoorPassable(state)) continue;
                    Vec3 center = group.cellCenter(candidate);
                    result.add(new AABB(
                            center.x - CLEAR_HALF_WIDTH,
                            center.y - CLEAR_BELOW,
                            center.z - CLEAR_HALF_WIDTH,
                            center.x + CLEAR_HALF_WIDTH,
                            center.y + CLEAR_ABOVE,
                            center.z + CLEAR_HALF_WIDTH));
                }
            }
        }
        return result;
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
