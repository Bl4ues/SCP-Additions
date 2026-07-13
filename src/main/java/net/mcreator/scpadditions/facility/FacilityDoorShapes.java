package net.mcreator.scpadditions.facility;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Exact manual-door outline/collision shapes ported from the original
 * SCP Unity Extra Blocks block classes. Shapes are authored in the SOUTH
 * orientation and rotated around the owning block for the other facings.
 */
final class FacilityDoorShapes {
    private FacilityDoorShapes() {
    }

    static VoxelShape shape(String familyId, boolean open, Direction facing) {
        double[][] boxes = switch (familyId) {
            case "normal" -> open ? NORMAL_OPEN : NORMAL_CLOSED;
            case "left_logistics" -> open ? LEFT_LOGISTICS_OPEN : LEFT_LOGISTICS_CLOSED;
            case "right_logistics" -> open ? RIGHT_LOGISTICS_OPEN : RIGHT_LOGISTICS_CLOSED;
            case "office" -> open ? OFFICE_OPEN : OFFICE_CLOSED;
            case "bathroom" -> open ? BATHROOM_OPEN : BATHROOM_CLOSED;
            case "workshop" -> open ? WORKSHOP_OPEN : WORKSHOP_CLOSED;
            default -> null;
        };
        if (boxes == null) {
            return Shapes.empty();
        }

        VoxelShape result = Shapes.empty();
        for (double[] box : boxes) {
            double[] rotated = rotate(box, facing);
            result = Shapes.or(result, Block.box(
                    rotated[0], rotated[1], rotated[2],
                    rotated[3], rotated[4], rotated[5]));
        }
        return result;
    }

    /**
     * Geometry used specifically for sight rays. Both the Normal and Office
     * doors keep the real window openings already present in their closed
     * model-derived shapes; only the surrounding frames block observation.
     */
    static VoxelShape visualOcclusionShape(String familyId, Direction facing) {
        return shape(familyId, false, facing);
    }

    private static double[] rotate(double[] box, Direction facing) {
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

    private static final double[][] NORMAL_CLOSED = {
            {0.25, 26.25, 12.5, 15.75, 31, 13.5},
            {10.75, 17, 12.5, 15.75, 26.25, 13.5},
            {0.25, 17, 12.5, 5.25, 26.25, 13.5},
            {0.25, 0, 12.5, 15.75, 17, 13.5},
            {13.75, 16.5, 13.5, 14.5, 17.25, 14.5},
            {13.75, 16.5, 11.5, 14.5, 17.25, 12.5},
            {11.75, 16.5, 11, 14.5, 17.25, 11.5},
            {11.75, 16.5, 14.5, 14.5, 17.25, 15},
            {15.75, 15.5, 12.75, 16, 16.5, 13.25},
            {15.75, 0, 9.75, 16.75, 32, 16.25},
            {-0.75, 0, 9.75, 0.25, 32, 16.25},
            {0.25, 31, 9.75, 15.75, 32, 16.25}
    };

    private static final double[][] NORMAL_OPEN = {
            {-0.5, 26.25, -1.75, 0.5, 31, 13.75},
            {-0.5, 17, -1.75, 0.5, 26.25, 3.25},
            {-0.5, 17, 8.75, 0.5, 26.25, 13.75},
            {-0.5, 0, -1.75, 0.5, 17, 13.75},
            {-1.475, 16.5, -0.375, -0.475, 17.25, 0.375},
            {-1.975, 16.5, -0.375, -1.475, 17.25, 2.375},
            {1.525, 16.5, -0.375, 2.025, 17.25, 2.375},
            {0.525, 16.5, -0.375, 1.525, 17.25, 0.375},
            {-0.25, 15.5, -2.05, 0.25, 16.5, -1.8},
            {15.75, 0, 9.75, 16.75, 32, 16.25},
            {-0.75, 0, 9.75, 0.25, 32, 16.25},
            {0.25, 31, 9.75, 15.75, 32, 16.25}
    };

    private static final double[][] LEFT_LOGISTICS_CLOSED = {
            {0.25, 0, 12.5, 16, 31, 13.5},
            {14.35, 16.35, 13.5, 15.1, 17.1, 14.5},
            {14.35, 16.35, 11.5, 15.1, 17.1, 12.5},
            {12.35, 16.35, 11, 15.1, 17.1, 11.5},
            {12.35, 16.35, 14.5, 15.1, 17.1, 15},
            {15.9, 15.5, 12.75, 16.15, 16.5, 13.25},
            {-0.75, 0, 9.75, 0.25, 32, 16.25},
            {0.25, 31, 9.75, 16, 32, 16.25}
    };

    private static final double[][] LEFT_LOGISTICS_OPEN = {
            {-0.5, 0, -2.75, 0.5, 31, 12.75},
            {0.425, 16.35, -1.875, 1.425, 17.1, -1.125},
            {-1.475, 16.35, -1.875, -0.475, 17.1, -1.125},
            {-1.975, 16.35, -1.875, -1.475, 17.1, 0.875},
            {1.425, 16.35, -1.875, 1.925, 17.1, 0.875},
            {-0.25, 15.5, -2.95, 0.25, 16.5, -2.7},
            {-0.75, 0, 9.75, 0.25, 32, 16.25},
            {0.25, 31, 9.75, 15.75, 32, 16.25}
    };

    private static final double[][] RIGHT_LOGISTICS_CLOSED = {
            {0, 0, 12.5, 15.75, 31, 13.5},
            {0.9, 16.35, 13.5, 1.65, 17.1, 14.5},
            {0.9, 16.35, 11.5, 1.65, 17.1, 12.5},
            {0.9, 16.35, 11, 3.65, 17.1, 11.5},
            {0.9, 16.35, 14.5, 3.65, 17.1, 15},
            {-0.2, 15.5, 12.75, 0.05, 16.5, 13.25},
            {15.75, 0, 9.75, 16.75, 32, 16.25},
            {0, 31, 9.75, 15.75, 32, 16.25}
    };

    private static final double[][] RIGHT_LOGISTICS_OPEN = {
            {15.5, 0, -2.75, 16.5, 31, 12.75},
            {14.575, 16.35, -1.875, 15.575, 17.1, -1.125},
            {16.475, 16.35, -1.875, 17.475, 17.1, -1.125},
            {17.475, 16.35, -1.875, 17.975, 17.1, 0.875},
            {14.075, 16.35, -1.875, 14.575, 17.1, 0.875},
            {15.75, 15.5, -2.95, 16.25, 16.5, -2.7},
            {15.75, 0, 9.75, 16.75, 32, 16.25},
            {0.25, 31, 9.75, 15.75, 32, 16.25}
    };

    private static final double[][] OFFICE_CLOSED = {
            {0.25, 29.5, 12.5, 15.75, 31, 13.5},
            {14.25, 9.5, 12.5, 15.75, 29.5, 13.5},
            {0.25, 9.5, 12.5, 1.75, 29.5, 13.5},
            {0.25, 0, 12.5, 15.75, 9.5, 13.5},
            {14.65, 16.5, 13.5, 15.4, 17.25, 14.5},
            {14.65, 16.5, 11.5, 15.4, 17.25, 12.5},
            {12.65, 16.5, 11, 15.4, 17.25, 11.5},
            {12.65, 16.5, 14.5, 15.4, 17.25, 15},
            {15.65, 15.5, 12.75, 15.9, 16.5, 13.25},
            {15.75, 0, 9.75, 16.75, 32, 16.25},
            {-0.75, 0, 9.75, 0.25, 32, 16.25},
            {0.25, 31, 9.75, 15.75, 32, 16.25}
    };

    private static final double[][] OFFICE_OPEN = {
            {-0.5, 29.5, -2.75, 0.5, 31, 12.75},
            {-0.5, 9.5, -2.75, 0.5, 29.5, -1.25},
            {-0.5, 9.5, 11.25, 0.5, 29.5, 12.75},
            {-0.5, 0, -2.75, 0.5, 9.5, 12.75},
            {0.425, 16.5, -2.475, 1.425, 17.25, -1.725},
            {-1.475, 16.5, -2.475, -0.475, 17.25, -1.725},
            {-1.975, 16.5, -2.475, -1.475, 17.25, 0.275},
            {1.425, 16.5, -2.475, 1.925, 17.25, 0.275},
            {-0.2, 15.5, -2.95, 0.3, 16.5, -2.7},
            {15.75, 0, 9.75, 16.75, 32, 16.25},
            {-0.75, 0, 9.75, 0.25, 32, 16.25},
            {0.25, 31, 9.75, 15.75, 32, 16.25}
    };

    private static final double[][] BATHROOM_CLOSED = {
            {0.75, 0.5, 12.5, 15.25, 30.5, 13.5},
            {12.15, 18.65, 13.5, 12.9, 19.4, 14.5},
            {12.15, 14.15, 13.5, 12.9, 14.9, 14.5},
            {12.15, 14.15, 14.5, 12.9, 19.4, 15},
            {0.25, 31, 9.75, 15.75, 32, 16.25},
            {0.25, 0, 12.2, 0.75, 32, 13.8},
            {15.25, 0, 12.2, 15.75, 32, 13.8},
            {0.75, 30.45, 12.2, 15.25, 31.05, 13.8},
            {0.75, -0.05, 12.2, 15.25, 0.55, 13.8},
            {15.75, 0, 9.75, 16.75, 32, 16.25},
            {-0.75, 0, 9.75, 0.25, 32, 16.25}
    };

    private static final double[][] BATHROOM_OPEN = {
            {0.25, 0.5, 13, 1.25, 30.5, 27.5},
            {-0.6, 18.65, 24.25, 0.4, 19.4, 25},
            {-0.6, 14.15, 24.25, 0.4, 14.9, 25},
            {-1.1, 14.15, 24.25, -0.6, 19.4, 25},
            {0.25, 31, 9.75, 15.75, 32, 16.25},
            {0.25, 0, 12.2, 0.75, 32, 13.8},
            {15.25, 0, 12.2, 15.75, 32, 13.8},
            {0.75, 30.45, 12.2, 15.25, 31.05, 13.8},
            {0.75, -0.05, 12.2, 15.25, 0.55, 13.8},
            {15.75, 0, 9.75, 16.75, 32, 16.25},
            {-0.75, 0, 9.75, 0.25, 32, 16.25}
    };

    private static final double[][] WORKSHOP_CLOSED = {
            {0.25, 0, 12.5, 16, 31, 13.5},
            {0.7, 16.35, 13.5, 1.45, 17.1, 14.5},
            {0.7, 16.35, 11.5, 1.45, 17.1, 12.5},
            {0.7, 16.35, 11, 3.45, 17.1, 11.5},
            {0.7, 16.35, 14.5, 3.45, 17.1, 15},
            {-0.15, 15.5, 12.75, 0.1, 16.5, 13.25},
            {-0.75, 0, 9.75, 0.25, 32, 16.25},
            {16, 0, 9.75, 17, 32, 16.25},
            {0.25, 31, 9.75, 16, 32, 16.25}
    };

    private static final double[][] WORKSHOP_OPEN = {
            {15.75, 0, -3, 16.75, 31, 12.75},
            {14.55, 16.35, -2.35, 15.55, 17.1, -1.6},
            {16.55, 16.35, -2.35, 17.55, 17.1, -1.6},
            {17.55, 16.35, -2.35, 18.05, 17.1, 0.4},
            {14.05, 16.35, -2.35, 14.55, 17.1, 0.4},
            {16, 15.5, -3.4, 16.5, 16.5, -3.15},
            {-0.75, 0, 9.75, 0.25, 32, 16.25},
            {16, 0, 9.75, 17, 32, 16.25},
            {0.25, 31, 9.75, 16, 32, 16.25}
    };
}
