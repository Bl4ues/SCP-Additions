package com.bl4ues.scpclassifieddirective.facility;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Tight selection/collision geometry for the GeckoLib manual doors.
 *
 * <p>The old replacement reused oversized MCreator-era envelopes. These boxes
 * are derived from the current Gecko geometry itself: model X is shifted by
 * eight pixels and model Z by 20.5 pixels into the door's authored SOUTH-local
 * block space. Animated leaves use the exact final 100-degree hinge rotation.
 * Handles are deliberately excluded here and supplied by
 * {@link FacilityGeckoDoorGeometry} so targeting and the hand outline share
 * one source of truth.</p>
 */
final class FacilityDoorShapes {
    private static final double WORKSHOP_CENTER_X = 8.0D;
    private static final double DOOR_CENTER_Z = 13.0D;

    private FacilityDoorShapes() {
    }

    static VoxelShape shape(String familyId, boolean open, Direction facing) {
        Geometry geometry = geometry(familyId);
        if (geometry == null) return Shapes.empty();

        VoxelShape result = Shapes.empty();
        for (double[] source : geometry.frame()) {
            double[] box = geometry.flipModel()
                    ? rotate180(source) : source;
            result = Shapes.or(result, rotatedBox(box, facing));
        }
        for (double[] source : geometry.leaf()) {
            double[] box = geometry.flipModel()
                    ? rotate180(source) : source;
            if (open) {
                box = rotateLeaf(box, geometry.hingeX(),
                        DOOR_CENTER_Z, geometry.openAngleDegrees());
            }
            result = Shapes.or(result, rotatedBox(box, facing));
        }
        return result.optimize();
    }

    /**
     * Optical clipping uses the same tight opaque geometry. Window panes are
     * intentionally absent so Facility and Office door windows remain visible
     * through rather than behaving like invisible solid rectangles.
     */
    static VoxelShape visualOcclusionShape(String familyId, Direction facing) {
        return shape(familyId, false, facing);
    }

    private static Geometry geometry(String familyId) {
        return switch (familyId) {
            case "normal" -> new Geometry(NORMAL_FRAME, NORMAL_LEAF,
                    0.0D, -100.0D, false);
            case "left_logistics" -> new Geometry(LEFT_LOGISTICS_FRAME,
                    LEFT_LOGISTICS_LEAF, 0.0D, -100.0D, false);
            case "right_logistics" -> new Geometry(RIGHT_LOGISTICS_FRAME,
                    RIGHT_LOGISTICS_LEAF, 16.0D, 100.0D, false);
            case "office" -> new Geometry(OFFICE_FRAME, OFFICE_LEAF,
                    0.0D, -100.0D, false);
            case "bathroom" -> new Geometry(BATHROOM_FRAME, BATHROOM_LEAF,
                    0.0D, -100.0D, false);
            case "workshop" -> new Geometry(WORKSHOP_FRAME, WORKSHOP_LEAF,
                    16.0D, 100.0D, true);
            default -> null;
        };
    }

    private static VoxelShape rotatedBox(double[] box, Direction facing) {
        double[] b = rotateFacing(box, facing);
        return Block.box(b[0], b[1], b[2], b[3], b[4], b[5]);
    }

    private static double[] rotateFacing(double[] box, Direction facing) {
        return switch (facing) {
            case NORTH -> new double[] {
                    16.0D - box[3], box[1], 16.0D - box[5],
                    16.0D - box[0], box[4], 16.0D - box[2]
            };
            case EAST -> new double[] {
                    box[2], box[1], 16.0D - box[3],
                    box[5], box[4], 16.0D - box[0]
            };
            case WEST -> new double[] {
                    16.0D - box[5], box[1], box[0],
                    16.0D - box[2], box[4], box[3]
            };
            default -> box;
        };
    }

    private static double[] rotate180(double[] box) {
        return new double[] {
                2.0D * WORKSHOP_CENTER_X - box[3],
                box[1],
                2.0D * DOOR_CENTER_Z - box[5],
                2.0D * WORKSHOP_CENTER_X - box[0],
                box[4],
                2.0D * DOOR_CENTER_Z - box[2]
        };
    }

    private static double[] rotateLeaf(double[] box, double pivotX,
            double pivotZ, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double minX = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (int xi = 0; xi < 2; xi++) {
            for (int zi = 0; zi < 2; zi++) {
                double x = xi == 0 ? box[0] : box[3];
                double z = zi == 0 ? box[2] : box[5];
                double dx = x - pivotX;
                double dz = z - pivotZ;
                double transformedX = pivotX + dx * cos - dz * sin;
                double transformedZ = pivotZ + dx * sin + dz * cos;
                minX = Math.min(minX, transformedX);
                minZ = Math.min(minZ, transformedZ);
                maxX = Math.max(maxX, transformedX);
                maxZ = Math.max(maxZ, transformedZ);
            }
        }
        return new double[] {
                minX, box[1], minZ, maxX, box[4], maxZ
        };
    }

    private record Geometry(double[][] frame, double[][] leaf,
            double hingeX, double openAngleDegrees, boolean flipModel) {
    }

    private static final double[][] NORMAL_FRAME = {
            {15.75D, 0.0D, 12.25D, 16.75D, 32.0D, 13.75D},
            {-0.75D, 0.0D, 12.25D, 0.25D, 32.0D, 13.75D},
            {-0.75D, 32.0D, 12.25D, 16.75D, 33.0D, 13.75D}
    };
    private static final double[][] NORMAL_LEAF = {
            {0.25D, 27.0D, 12.5D, 15.75D, 32.0D, 13.5D},
            {10.75D, 17.5D, 12.5D, 15.75D, 27.0D, 13.5D},
            {0.25D, 17.5D, 12.5D, 5.25D, 27.0D, 13.5D},
            {0.25D, 0.0D, 12.5D, 15.75D, 17.5D, 13.5D},
            {15.65D, 15.5D, 12.75D, 15.9D, 16.5D, 13.25D}
    };

    private static final double[][] LEFT_LOGISTICS_FRAME = {
            {-0.75D, 0.0D, 12.25D, 0.25D, 32.0D, 13.75D},
            {-0.75D, 32.0D, 12.25D, 16.0D, 33.0D, 13.75D}
    };
    private static final double[][] LEFT_LOGISTICS_LEAF = {
            {0.25D, 0.0D, 12.5D, 16.0D, 32.0D, 13.5D},
            {15.9D, 15.5D, 12.75D, 16.15D, 16.5D, 13.25D}
    };

    private static final double[][] RIGHT_LOGISTICS_FRAME = {
            {15.75D, 0.0D, 12.25D, 16.75D, 32.0D, 13.75D},
            {0.0D, 32.0D, 12.25D, 16.75D, 33.0D, 13.75D}
    };
    private static final double[][] RIGHT_LOGISTICS_LEAF = {
            {0.0D, 0.0D, 12.5D, 15.75D, 32.0D, 13.5D},
            {-0.2D, 15.5D, 12.75D, 0.05D, 16.5D, 13.25D}
    };

    private static final double[][] OFFICE_FRAME = {
            {15.75D, 0.0D, 12.25D, 16.75D, 32.0D, 13.75D},
            {-0.75D, 0.0D, 12.25D, 0.25D, 32.0D, 13.75D},
            {-0.75D, 32.0D, 12.25D, 16.75D, 33.0D, 13.75D}
    };
    private static final double[][] OFFICE_LEAF = {
            {0.25D, 30.0D, 12.5D, 15.75D, 32.0D, 13.5D},
            {14.25D, 9.5D, 12.5D, 15.75D, 30.0D, 13.5D},
            {0.25D, 9.5D, 12.5D, 1.75D, 30.0D, 13.5D},
            {0.25D, 0.0D, 12.5D, 15.75D, 9.5D, 13.5D},
            {15.65D, 15.5D, 12.75D, 15.9D, 16.5D, 13.25D}
    };

    private static final double[][] BATHROOM_FRAME = {
            {15.75D, 0.0D, 12.25D, 16.75D, 32.0D, 13.75D},
            {-0.75D, 0.0D, 12.25D, 0.25D, 32.0D, 13.75D},
            {-0.75D, 32.0D, 12.25D, 16.75D, 33.0D, 13.75D},
            {0.25D, 0.0D, 12.2D, 0.75D, 32.0D, 13.8D},
            {15.25D, 0.0D, 12.2D, 15.75D, 32.0D, 13.8D},
            {0.75D, 31.45D, 12.2D, 15.25D, 32.05D, 13.8D},
            {0.75D, -0.05D, 12.2D, 15.25D, 0.55D, 13.8D}
    };
    private static final double[][] BATHROOM_LEAF = {
            {0.25D, 0.0D, 12.5D, 15.75D, 32.0D, 13.5D}
    };

    private static final double[][] WORKSHOP_FRAME = {
            {15.75D, 0.0D, 12.25D, 16.75D, 32.0D, 13.75D},
            {-0.75D, 0.0D, 12.25D, 0.25D, 32.0D, 13.75D},
            {-0.75D, 32.0D, 12.25D, 16.75D, 33.0D, 13.75D}
    };
    private static final double[][] WORKSHOP_LEAF = {
            {0.25D, 0.0D, 12.5D, 15.75D, 32.0D, 13.5D},
            {15.65D, 15.5D, 12.75D, 15.9D, 16.5D, 13.25D}
    };
}
