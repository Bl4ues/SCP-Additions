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
            modelBox(17, 0, -24, 24, 1, 24),
            modelBox(-24, 0, -24, -17, 1, 24),
            modelBox(-17, 0, 16.5, 17, 1, 24),
            modelBox(-12, 0, -29.75, 12, 1, -18.5),
            modelBox(10, 0, -18.5, 12, 17, -14.25),
            modelBox(-12, 0, -18.5, -10, 17, -14.25)
    );

    private static final AABB STATION_GATE = modelBox(
            -10, 0, -19.65, 10, 9.5, -19.15);

    // Post-root (-90 degree) pulley coordinates. Guide bars include the
    // requested one-unit rearward correction.
    private static final List<AABB> PULLEY_STATIC = List.of(
            modelBox(-13, 15, -13, 13, 16, 13),
            modelBox(-8.5, 10, -7, 8.5, 15, 7),
            modelBox(-16, 0, -15.75, -13, 16, -12.25),
            modelBox(-14.5, 0, 13, -13, 16, 15),
            modelBox(13, 0, 13, 14.5, 16, 15),
            modelBox(13, 0, -15.75, 16, 16, -12.25)
    );

    private static final List<AABB> BEAMS = List.of(
            javaModelBox(21, 0, -7.75, 24, 16, -4.25),
            javaModelBox(21, 0, 21, 22.5, 16, 23),
            javaModelBox(-6.5, 0, 21, -5, 16, 23),
            javaModelBox(-8, 0, -7.75, -5, 16, -4.25)
    );

    private CoreRoomElevatorGeometry() {}

    private static final double STATION_BUTTON_X = 14.64492D / 16.0D;
    private static final double STATION_BUTTON_Z = -16.69749D / 16.0D;

    public static Vec3 stationButtonWorld(BlockPos master,
            Direction facing, boolean up) {
        Vec3 local = new Vec3(STATION_BUTTON_X,
                (up ? 21.25D : 19.25D) / 16.0D,
                STATION_BUTTON_Z);
        Vec3 rotated = rotateLocalVector(facing,
                local.x, local.y, local.z);
        return Vec3.atLowerCornerOf(master)
                .add(0.5D, 0.0D, 0.5D)
                .add(rotated);
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
        if (gateSolid) boxes.add(STATION_GATE);
        return cellShape(boxes, facing, localX, localY, localZ, false);
    }

    public static VoxelShape stationSelectionCellShape() {
        return Shapes.block();
    }

    public static VoxelShape pulleyCellShape(Direction facing, int localX,
            int localY, int localZ) {
        return cellShape(PULLEY_STATIC, facing, localX, localY, localZ, false);
    }

    public static VoxelShape pulleySelectionCellShape() {
        return Shapes.block();
    }

    public static VoxelShape beamShape(Direction facing) {
        return beamCellShape(facing, 0, 0, 0);
    }

    public static VoxelShape beamCellShape(Direction facing, int localX,
            int localY, int localZ) {
        return cellShape(BEAMS, facing, localX, localY, localZ, false);
    }

    private static VoxelShape cellShape(List<AABB> source, Direction facing,
            int localX, int localY, int localZ, boolean modelRootRotated) {
        BlockPos rotatedCell = CoreRoomElevatorModule.rotateOffset(facing,
                localX, localY, localZ);
        AABB cell = new AABB(rotatedCell.getX(), rotatedCell.getY(),
                rotatedCell.getZ(), rotatedCell.getX() + 1.0D,
                rotatedCell.getY() + 1.0D, rotatedCell.getZ() + 1.0D);
        VoxelShape result = Shapes.empty();
        for (AABB original : source) {
            AABB aligned = modelRootRotated
                    ? rotateAabb(original, Direction.EAST, 0.5D, 0.5D)
                    : original;
            AABB rotated = rotateAabb(aligned, facing, 0.5D, 0.5D);
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
                || maxZ - minZ <= THIN) return null;
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
        return switch (facing) {
            case SOUTH -> new Vec3(-x, y, -z);
            case EAST -> new Vec3(z, y, -x);
            case WEST -> new Vec3(-z, y, x);
            default -> new Vec3(x, y, z);
        };
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
