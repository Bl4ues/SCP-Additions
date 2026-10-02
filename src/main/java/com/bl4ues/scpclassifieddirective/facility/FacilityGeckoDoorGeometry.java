package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.facility.FacilityGeckoDoorModule.Family;
import com.bl4ues.scpclassifieddirective.facility.FacilityGeckoDoorModule.Phase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Physical handle geometry shared by contextual targeting, direct use and the
 * handle-only outline. Coordinates are derived from the authored Gecko bones,
 * not from a convenient approximation around the whole door.
 */
public final class FacilityGeckoDoorGeometry {
    private static final List<AABB> COMMON_HANDLE = List.of(
            px(14.65, 16.5, 13.5, 15.4, 17.25, 14.5),
            px(14.65, 16.5, 11.5, 15.4, 17.25, 12.5),
            px(12.65, 16.5, 11.0, 15.4, 17.25, 11.5),
            px(12.65, 16.5, 14.5, 15.4, 17.25, 15.0));
    private static final List<AABB> RIGHT_LOGISTICS_HANDLE = List.of(
            px(0.6, 16.5, 13.5, 1.35, 17.25, 14.5),
            px(0.6, 16.5, 11.5, 1.35, 17.25, 12.5),
            px(0.6, 16.5, 11.0, 3.35, 17.25, 11.5),
            px(0.6, 16.5, 14.5, 3.35, 17.25, 15.0));
    private static final List<AABB> BATHROOM_HANDLE = List.of(
            px(12.65, 14.15, 13.5, 13.4, 14.9, 14.5),
            px(12.65, 14.15, 14.5, 13.4, 19.4, 15.0),
            px(12.65, 18.65, 13.5, 13.4, 19.4, 14.5));

    private FacilityGeckoDoorGeometry() {
    }

    private static AABB px(double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        return new AABB(minX / 16.0D, minY / 16.0D, minZ / 16.0D,
                maxX / 16.0D, maxY / 16.0D, maxZ / 16.0D);
    }

    public static List<AABB> closedHandleBoxes(Family family) {
        if (family == Family.LOGISTICS_RIGHT) {
            return RIGHT_LOGISTICS_HANDLE;
        }
        if (family == Family.BATHROOM) {
            return BATHROOM_HANDLE;
        }
        return COMMON_HANDLE;
    }

    public static Vec3 handleAnchor(BlockState state) {
        Family family = FacilityGeckoDoorModule.family(state);
        if (family == null) return new Vec3(0.5D, 1.0D, 0.5D);
        AABB bounds = union(closedHandleBoxes(family));
        Vec3 point = new Vec3((bounds.minX + bounds.maxX) * 0.5D,
                (bounds.minY + bounds.maxY) * 0.5D,
                (bounds.minZ + bounds.maxZ) * 0.5D);
        if (FacilityGeckoDoorModule.phase(state) == Phase.OPEN) {
            point = rotateLeaf(point, family, openAngle(family));
        }
        return rotateFacing(point,
                state.getValue(FacilityGeckoDoorModule.FACING));
    }

    public static boolean isHandleHit(BlockState state, BlockPos pos,
            Vec3 worldHit) {
        if (!FacilityGeckoDoorModule.isInteractable(state)
                || pos == null || worldHit == null) return false;
        Family family = FacilityGeckoDoorModule.family(state);
        Vec3 point = worldHit.subtract(pos.getX(), pos.getY(), pos.getZ());
        point = unrotateFacing(point,
                state.getValue(FacilityGeckoDoorModule.FACING));
        if (FacilityGeckoDoorModule.phase(state) == Phase.OPEN) {
            point = rotateLeaf(point, family, -openAngle(family));
        }
        final double epsilon = 0.035D;
        for (AABB box : closedHandleBoxes(family)) {
            if (point.x >= box.minX - epsilon
                    && point.x <= box.maxX + epsilon
                    && point.y >= box.minY - epsilon
                    && point.y <= box.maxY + epsilon
                    && point.z >= box.minZ - epsilon
                    && point.z <= box.maxZ + epsilon) {
                return true;
            }
        }
        return false;
    }

    public static Vec3 transformHandlePoint(BlockState state, Vec3 point) {
        Family family = FacilityGeckoDoorModule.family(state);
        if (family == null) return point;
        Vec3 transformed = point;
        if (FacilityGeckoDoorModule.phase(state) == Phase.OPEN) {
            transformed = rotateLeaf(transformed, family, openAngle(family));
        }
        return rotateFacing(transformed,
                state.getValue(FacilityGeckoDoorModule.FACING));
    }

    public static Vec3 hingePivot(Family family) {
        return new Vec3(family == Family.LOGISTICS_RIGHT ? 1.0D : 0.0D,
                14.0D / 16.0D, 13.0D / 16.0D);
    }

    public static double openAngle(Family family) {
        // Gecko's authored Z basis is mirrored relative to block-local Z.
        // Therefore +100° in the animation is -100° in block coordinates.
        return Math.toRadians(family == Family.LOGISTICS_RIGHT
                ? 100.0D : -100.0D);
    }

    public static Vec3 rotateLeaf(Vec3 point, Family family, double angle) {
        Vec3 pivot = hingePivot(family);
        double dx = point.x - pivot.x;
        double dz = point.z - pivot.z;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(pivot.x + dx * cos - dz * sin,
                point.y,
                pivot.z + dx * sin + dz * cos);
    }

    public static Vec3 rotateFacing(Vec3 point, Direction facing) {
        return switch (facing) {
            case EAST -> new Vec3(point.z, point.y, 1.0D - point.x);
            case NORTH -> new Vec3(1.0D - point.x, point.y,
                    1.0D - point.z);
            case WEST -> new Vec3(1.0D - point.z, point.y, point.x);
            default -> point;
        };
    }

    public static Vec3 unrotateFacing(Vec3 point, Direction facing) {
        return switch (facing) {
            case EAST -> new Vec3(1.0D - point.z, point.y, point.x);
            case NORTH -> new Vec3(1.0D - point.x, point.y,
                    1.0D - point.z);
            case WEST -> new Vec3(point.z, point.y, 1.0D - point.x);
            default -> point;
        };
    }

    private static AABB union(List<AABB> boxes) {
        AABB result = boxes.get(0);
        for (int i = 1; i < boxes.size(); i++) {
            result = result.minmax(boxes.get(i));
        }
        return result;
    }
}
