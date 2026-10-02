package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.facility.FacilityGeckoDoorModule.Family;
import com.bl4ues.scpclassifieddirective.facility.FacilityGeckoDoorModule.Phase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Physical handle geometry shared by contextual targeting, direct use and the
 * handle-only outline.
 *
 * <p>Coordinates are derived from the current Gecko bones. The Workshop Door
 * has an additional authored 180-degree model rotation, which is applied here
 * before the animated hinge rotation so gameplay and rendering remain in the
 * same coordinate space.</p>
 */
public final class FacilityGeckoDoorGeometry {
    private static final double WORKSHOP_CENTER_X = 0.5D;
    private static final double DOOR_CENTER_Z = 13.0D / 16.0D;

    private static final List<AABB> FACILITY_HANDLE = List.of(
            px(13.65, 16.5, 11.5, 14.4, 17.25, 12.5),
            px(13.65, 16.5, 13.5, 14.4, 17.25, 14.5),
            px(11.65, 16.5, 14.5, 14.4, 17.25, 15.0),
            px(11.65, 16.5, 11.0, 14.4, 17.25, 11.5));

    private static final List<AABB> OFFICE_HANDLE = List.of(
            px(14.65, 16.5, 11.5, 15.4, 17.25, 12.5),
            px(14.65, 16.5, 13.5, 15.4, 17.25, 14.5),
            px(12.65, 16.5, 14.5, 15.4, 17.25, 15.0),
            px(12.65, 16.5, 11.0, 15.4, 17.25, 11.5));

    private static final List<AABB> LEFT_LOGISTICS_HANDLE = List.of(
            px(13.65, 17.25, 11.5, 14.4, 18.0, 12.5),
            px(13.65, 17.25, 13.5, 14.4, 18.0, 14.5),
            px(11.65, 17.25, 14.5, 14.4, 18.0, 15.0),
            px(11.65, 17.25, 11.0, 14.4, 18.0, 11.5));

    private static final List<AABB> RIGHT_LOGISTICS_HANDLE = List.of(
            px(1.6, 17.25, 11.5, 2.35, 18.0, 12.5),
            px(1.6, 17.25, 13.5, 2.35, 18.0, 14.5),
            px(1.6, 17.25, 14.5, 4.35, 18.0, 15.0),
            px(1.6, 17.25, 11.0, 4.35, 18.0, 11.5));

    private static final List<AABB> BATHROOM_HANDLE = List.of(
            px(11.65, 14.9, 11.5, 12.4, 15.65, 12.5),
            px(11.65, 14.9, 11.0, 12.4, 20.15, 11.5),
            px(11.65, 19.4, 11.5, 12.4, 20.15, 12.5));

    private static final List<AABB> WORKSHOP_HANDLE = List.of(
            px(14.65, 17.25, 11.5, 15.4, 18.0, 12.5),
            px(14.65, 17.25, 13.5, 15.4, 18.0, 14.5),
            px(12.65, 17.25, 14.5, 15.4, 18.0, 15.0),
            px(12.65, 17.25, 11.0, 15.4, 18.0, 11.5));

    private FacilityGeckoDoorGeometry() {
    }

    private static AABB px(double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        return new AABB(minX / 16.0D, minY / 16.0D, minZ / 16.0D,
                maxX / 16.0D, maxY / 16.0D, maxZ / 16.0D);
    }

    public static List<AABB> closedHandleBoxes(Family family) {
        return switch (family) {
            case FACILITY -> FACILITY_HANDLE;
            case LOGISTICS_LEFT -> LEFT_LOGISTICS_HANDLE;
            case LOGISTICS_RIGHT -> RIGHT_LOGISTICS_HANDLE;
            case OFFICE -> OFFICE_HANDLE;
            case BATHROOM -> BATHROOM_HANDLE;
            case WORKSHOP -> WORKSHOP_HANDLE;
        };
    }

    public static Vec3 handleAnchor(BlockState state) {
        Family family = FacilityGeckoDoorModule.family(state);
        if (family == null) return new Vec3(0.5D, 1.0D, 0.5D);
        AABB bounds = union(closedHandleBoxes(family));
        Vec3 point = new Vec3((bounds.minX + bounds.maxX) * 0.5D,
                (bounds.minY + bounds.maxY) * 0.5D,
                (bounds.minZ + bounds.maxZ) * 0.5D);
        point = applyStaticModelTransform(point, family);
        if (FacilityGeckoDoorModule.phase(state) == Phase.OPEN) {
            point = rotateLeaf(point, family, openAngle(family));
        }
        return rotateFacing(point,
                state.getValue(FacilityGeckoDoorModule.FACING));
    }

    public static boolean rayHitsHandle(BlockState state, Vec3 localStart,
            Vec3 localEnd) {
        if (!FacilityGeckoDoorModule.isInteractable(state)
                || localStart == null || localEnd == null) return false;
        Family family = FacilityGeckoDoorModule.family(state);
        if (family == null) return false;

        Direction facing = state.getValue(FacilityGeckoDoorModule.FACING);
        Vec3 start = unrotateFacing(localStart, facing);
        Vec3 end = unrotateFacing(localEnd, facing);
        if (FacilityGeckoDoorModule.phase(state) == Phase.OPEN) {
            double inverse = -openAngle(family);
            start = rotateLeaf(start, family, inverse);
            end = rotateLeaf(end, family, inverse);
        }
        start = undoStaticModelTransform(start, family);
        end = undoStaticModelTransform(end, family);

        for (AABB box : closedHandleBoxes(family)) {
            if (box.contains(start) || box.contains(end)
                    || box.clip(start, end).isPresent()) {
                return true;
            }
        }
        return false;
    }

    public static boolean rayHitsHandle(BlockState state, BlockPos pos,
            Vec3 worldStart, Vec3 worldEnd) {
        if (pos == null || worldStart == null || worldEnd == null) {
            return false;
        }
        Vec3 origin = Vec3.atLowerCornerOf(pos);
        return rayHitsHandle(state, worldStart.subtract(origin),
                worldEnd.subtract(origin));
    }

    public static VoxelShape handleSelectionShape(BlockState state) {
        if (!FacilityGeckoDoorModule.isInteractable(state)) {
            return Shapes.empty();
        }
        Family family = FacilityGeckoDoorModule.family(state);
        if (family == null) return Shapes.empty();

        VoxelShape result = Shapes.empty();
        for (AABB box : closedHandleBoxes(family)) {
            AABB transformed = transformedBounds(state, box);
            result = Shapes.or(result, Shapes.box(
                    transformed.minX, transformed.minY, transformed.minZ,
                    transformed.maxX, transformed.maxY, transformed.maxZ));
        }
        return result.optimize();
    }

    private static AABB transformedBounds(BlockState state, AABB box) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (int xi = 0; xi < 2; xi++) {
            for (int yi = 0; yi < 2; yi++) {
                for (int zi = 0; zi < 2; zi++) {
                    Vec3 point = transformHandlePoint(state, new Vec3(
                            xi == 0 ? box.minX : box.maxX,
                            yi == 0 ? box.minY : box.maxY,
                            zi == 0 ? box.minZ : box.maxZ));
                    minX = Math.min(minX, point.x);
                    minY = Math.min(minY, point.y);
                    minZ = Math.min(minZ, point.z);
                    maxX = Math.max(maxX, point.x);
                    maxY = Math.max(maxY, point.y);
                    maxZ = Math.max(maxZ, point.z);
                }
            }
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static boolean isHandleHit(BlockState state, BlockPos pos,
            Vec3 worldHit) {
        if (!FacilityGeckoDoorModule.isInteractable(state)
                || pos == null || worldHit == null) return false;
        Family family = FacilityGeckoDoorModule.family(state);
        if (family == null) return false;

        Vec3 point = worldHit.subtract(pos.getX(), pos.getY(), pos.getZ());
        point = unrotateFacing(point,
                state.getValue(FacilityGeckoDoorModule.FACING));
        if (FacilityGeckoDoorModule.phase(state) == Phase.OPEN) {
            point = rotateLeaf(point, family, -openAngle(family));
        }
        point = undoStaticModelTransform(point, family);

        final double epsilon = 0.025D;
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
        Vec3 transformed = applyStaticModelTransform(point, family);
        if (FacilityGeckoDoorModule.phase(state) == Phase.OPEN) {
            transformed = rotateLeaf(transformed, family, openAngle(family));
        }
        return rotateFacing(transformed,
                state.getValue(FacilityGeckoDoorModule.FACING));
    }

    public static Vec3 hingePivot(Family family) {
        boolean rightHinge = family == Family.LOGISTICS_RIGHT
                || family == Family.WORKSHOP;
        return new Vec3(rightHinge ? 1.0D : 0.0D,
                14.0D / 16.0D, DOOR_CENTER_Z);
    }

    public static double openAngle(Family family) {
        return Math.toRadians(family == Family.LOGISTICS_RIGHT
                || family == Family.WORKSHOP ? 100.0D : -100.0D);
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

    private static Vec3 applyStaticModelTransform(Vec3 point, Family family) {
        if (family != Family.WORKSHOP) return point;
        return new Vec3(2.0D * WORKSHOP_CENTER_X - point.x,
                point.y, 2.0D * DOOR_CENTER_Z - point.z);
    }

    private static Vec3 undoStaticModelTransform(Vec3 point, Family family) {
        return applyStaticModelTransform(point, family);
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
