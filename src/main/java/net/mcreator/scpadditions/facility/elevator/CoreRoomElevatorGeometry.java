package net.mcreator.scpadditions.facility.elevator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/** Model-derived collision and coordinate transforms shared by elevator pieces. */
public final class CoreRoomElevatorGeometry {
    private static final double THIN = 1.0E-7D;

    private static final List<AABB> STATION_STATIC = List.of(
            modelBox(13, 0, -15.75, 16, 48, -12.25),
            modelBox(-16, 0, -15.75, -13, 48, -12.25),
            modelBox(13, 0, 13, 14.5, 48, 15),
            modelBox(-14.5, 0, 13, -13, 48, 15),

            modelBox(17, 0, -24, 24, 0.75, 24),
            modelBox(-24, 0, -24, -17, 0.75, 24),
            modelBox(-17, 0, 16.5, 17, 0.75, 24),
            modelBox(-12, 0, -24, 12, 0.75, -18.5),
            modelBox(-12, 0, -29.75, 12, 0.75, -19.5),

            modelBox(12, 0, -16.6, 17, 13.5, -16.4),
            modelBox(-17, 0, -16.6, -12, 13.5, -16.4),
            modelBox(-17, 0, 16.4, 17, 13.5, 16.6),
            modelBox(16.4, 0, -16.5, 16.6, 13.5, 16.5),
            modelBox(-16.6, 0, -16.5, -16.4, 13.5, 16.5),

            modelBox(10, 0, -18.5, 12, 17, -14.25),
            modelBox(-12, 0, -18.5, -10, 17, -14.25)
    );

    private static final AABB STATION_GATE = modelBox(
            -10, 0, -19.65, 10, 9.5, -19.15);

    private static final List<AABB> PULLEY_STATIC = List.of(
            modelBox(-13, 15, -13, 13, 16, 13),
            modelBox(-7, 10, -8.5, 7, 15, 8.5),
            modelBox(-16.5, 0, 13, -13, 16, 16),
            modelBox(12.25, 0, 13, 14.25, 16, 14.5),
            modelBox(12.25, 0, -14.5, 14.25, 16, -13),
            modelBox(-16.5, 0, -16, -13, 16, -13)
    );

    private static final List<AABB> BEAMS = List.of(
            javaModelBox(21, 0, -7.75, 24, 16, -4.25),
            javaModelBox(21, 0, 21, 22.5, 16, 23),
            javaModelBox(-6.5, 0, 21, -5, 16, 23),
            javaModelBox(-8, 0, -7.75, -5, 16, -4.25)
    );

    private CoreRoomElevatorGeometry() {
    }

    private static AABB modelBox(double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        return new AABB(0.5D + minX / 16.0D, minY / 16.0D,
                0.5D + minZ / 16.0D, 0.5D + maxX / 16.0D,
                maxY / 16.0D, 0.5D + maxZ / 16.0D);
    }

    private static AABB javaModelBox(double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        return new AABB(minX / 16.0D, minY / 16.0D,
                minZ / 16.0D, maxX / 16.0D,
                maxY / 16.0D, maxZ / 16.0D);
    }

    public static VoxelShape stationCellShape(Direction facing, int localX,
            int localY, int localZ, boolean gateSolid) {
        List<AABB> boxes = new ArrayList<>(STATION_STATIC);
        if (gateSolid) {
            boxes.add(STATION_GATE);
        }
        return cellShape(boxes, facing, localX, localY, localZ);
    }

    public static VoxelShape pulleyCellShape(Direction facing, int localX,
            int localY, int localZ) {
        return cellShape(PULLEY_STATIC, facing, localX, localY, localZ);
    }

    public static VoxelShape beamShape(Direction facing) {
        return beamCellShape(facing, 0, 0, 0);
    }

    public static VoxelShape beamCellShape(Direction facing, int localX,
            int localY, int localZ) {
        return cellShape(BEAMS, facing, localX, localY, localZ);
    }

    private static VoxelShape cellShape(List<AABB> source, Direction facing,
            int localX, int localY, int localZ) {
        BlockPos rotatedCell = CoreRoomElevatorModule.rotateOffset(facing,
                localX, localY, localZ);
        AABB cell = new AABB(rotatedCell.getX(), rotatedCell.getY(),
                rotatedCell.getZ(), rotatedCell.getX() + 1.0D,
                rotatedCell.getY() + 1.0D, rotatedCell.getZ() + 1.0D);
        VoxelShape result = Shapes.empty();
        for (AABB original : source) {
            AABB rotated = rotateAabb(original, facing, 0.5D, 0.5D);
            AABB clipped = intersect(rotated, cell);
            if (clipped == null) continue;
            result = Shapes.or(result, Shapes.create(clipped.move(
                    -rotatedCell.getX(), -rotatedCell.getY(),
                    -rotatedCell.getZ())));
        }
        return result.optimize();
    }

    private static AABB intersect(AABB first, AABB second) {
        double minX = Math.max(first.minX, second.minX);
        double minY = Math.max(first.minY, second.minY);
        double minZ = Math.max(first.minZ, second.minZ);
        double maxX = Math.min(first.maxX, second.maxX);
        double maxY = Math.min(first.maxY, second.maxY);
        double maxZ = Math.min(first.maxZ, second.maxZ);
        if (maxX - minX <= THIN || maxY - minY <= THIN
                || maxZ - minZ <= THIN) {
            return null;
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static AABB rotateAabb(AABB box, Direction facing,
            double pivotX, double pivotZ) {
        Vec3[] corners = new Vec3[]{
                new Vec3(box.minX, box.minY, box.minZ),
                new Vec3(box.minX, box.minY, box.maxZ),
                new Vec3(box.maxX, box.minY, box.minZ),
                new Vec3(box.maxX, box.minY, box.maxZ),
                new Vec3(box.minX, box.maxY, box.minZ),
                new Vec3(box.minX, box.maxY, box.maxZ),
                new Vec3(box.maxX, box.maxY, box.minZ),
                new Vec3(box.maxX, box.maxY, box.maxZ)
        };
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (Vec3 corner : corners) {
            Vec3 rotated = rotatePoint(corner, facing, pivotX, pivotZ);
            minX = Math.min(minX, rotated.x);
            minY = Math.min(minY, rotated.y);
            minZ = Math.min(minZ, rotated.z);
            maxX = Math.max(maxX, rotated.x);
            maxY = Math.max(maxY, rotated.y);
            maxZ = Math.max(maxZ, rotated.z);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static Vec3 rotatePoint(Vec3 point, Direction facing,
            double pivotX, double pivotZ) {
        double x = point.x - pivotX;
        double z = point.z - pivotZ;
        return switch (facing) {
            case SOUTH -> new Vec3(pivotX - x, point.y, pivotZ - z);
            case EAST -> new Vec3(pivotX - z, point.y, pivotZ + x);
            case WEST -> new Vec3(pivotX + z, point.y, pivotZ - x);
            default -> point;
        };
    }

    public static Vec3 worldToModelLocal(BlockPos master, Direction facing,
            Vec3 worldPoint) {
        double x = worldPoint.x - (master.getX() + 0.5D);
        double y = worldPoint.y - master.getY();
        double z = worldPoint.z - (master.getZ() + 0.5D);
        Vec3 unrotated = switch (facing) {
            case SOUTH -> new Vec3(-x, y, -z);
            case EAST -> new Vec3(z, y, -x);
            case WEST -> new Vec3(-z, y, x);
            default -> new Vec3(x, y, z);
        };
        return unrotated;
    }

    public static Vec3 rotateLocalVector(Direction facing, double x,
            double y, double z) {
        return switch (facing) {
            case SOUTH -> new Vec3(-x, y, -z);
            case EAST -> new Vec3(-z, y, x);
            case WEST -> new Vec3(z, y, -x);
            default -> new Vec3(x, y, z);
        };
    }
}
