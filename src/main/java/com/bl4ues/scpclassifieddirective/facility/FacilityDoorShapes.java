package com.bl4ues.scpclassifieddirective.facility;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Collision/selection geometry derived from the current Gecko door assets.
 *
 * <p>GeckoLib 4 mirrors Blockbench X, keeps Y/Z, translates the model by
 * (0.5, 0, 0.5), then applies FACING around the block centre. The previous
 * implementation used a guessed +8/+20.5 conversion and therefore put the
 * physical door almost a block behind the rendered one. All coordinates here
 * start in the authored .geo.json coordinate system and use Gecko's actual
 * conversion.</p>
 */
final class FacilityDoorShapes {
    private FacilityDoorShapes() {
    }

    static VoxelShape shape(String familyId, boolean open, Direction facing) {
        Geometry geometry = geometry(familyId);
        if (geometry == null) return Shapes.empty();

        VoxelShape result = Shapes.empty();
        for (double[] raw : geometry.frameRaw()) {
            result = Shapes.or(result, rotatedBox(fromRaw(raw), facing));
        }

        double[] leaf = fromRaw(geometry.leafRaw());
        if (open) {
            leaf = rotateYBounds(leaf, geometry.hingeX(),
                    geometry.hingeZ(), geometry.openAngleDegrees());
        }
        result = Shapes.or(result, rotatedBox(leaf, facing));
        return result.optimize();
    }

    /**
     * Sight clipping keeps real window holes while collision is intentionally
     * allowed to use one simple slab for the leaf.
     */
    static VoxelShape visualOcclusionShape(String familyId, Direction facing) {
        Geometry geometry = geometry(familyId);
        if (geometry == null) return Shapes.empty();

        VoxelShape result = Shapes.empty();
        for (double[] raw : geometry.frameRaw()) {
            result = Shapes.or(result, rotatedBox(fromRaw(raw), facing));
        }

        double[][] opaque = geometry.opaqueLeafRaw();
        if (opaque == null) {
            result = Shapes.or(result,
                    rotatedBox(fromRaw(geometry.leafRaw()), facing));
        } else {
            for (double[] raw : opaque) {
                result = Shapes.or(result, rotatedBox(fromRaw(raw), facing));
            }
        }
        return result.optimize();
    }

    private static Geometry geometry(String familyId) {
        return switch (familyId) {
            case "normal" -> new Geometry(NORMAL_FRAME, NORMAL_LEAF,
                    16.0D, 0.5D, -100.0D, NORMAL_OPAQUE);
            case "left_logistics" -> new Geometry(LEFT_LOGISTICS_FRAME,
                    LEFT_LOGISTICS_LEAF, 16.0D, 0.5D, -100.0D, null);
            case "right_logistics" -> new Geometry(RIGHT_LOGISTICS_FRAME,
                    RIGHT_LOGISTICS_LEAF, 0.0D, 0.5D, 100.0D, null);
            case "office" -> new Geometry(OFFICE_FRAME, OFFICE_LEAF,
                    16.0D, 0.5D, -100.0D, OFFICE_OPAQUE);
            case "bathroom" -> new Geometry(BATHROOM_FRAME, BATHROOM_LEAF,
                    16.0D, 0.5D, -100.0D, null);
            /*
             * Workshop has a 180-degree authored model_root. Its symmetric
             * closed slab/frame keep the same envelope, while the door hinge
             * moves from model-right to model-left. The inverted animation is
             * +100 degrees in Gecko space.
             */
            case "workshop" -> new Geometry(WORKSHOP_FRAME, WORKSHOP_LEAF,
                    0.0D, 0.5D, 100.0D, null);
            default -> null;
        };
    }

    /**
     * Convert one Blockbench cube {origin,size} into the exact north-facing
     * block-local box GeckoLib renders.
     */
    private static double[] fromRaw(double[] raw) {
        double ox = raw[0];
        double oy = raw[1];
        double oz = raw[2];
        double sx = raw[3];
        double sy = raw[4];
        double sz = raw[5];

        return new double[] {
                8.0D - (ox + sx), oy, 8.0D + oz,
                8.0D - ox, oy + sy, 8.0D + oz + sz
        };
    }

    private static VoxelShape rotatedBox(double[] box, Direction facing) {
        double[] b = rotateFacing(box, facing);
        return Block.box(b[0], b[1], b[2], b[3], b[4], b[5]);
    }

    /**
     * Match GeoBlockRenderer.rotateBlock: NORTH=0, SOUTH=180, WEST=+90,
     * EAST=-90.
     */
    private static double[] rotateFacing(double[] b, Direction facing) {
        return switch (facing) {
            case SOUTH -> new double[] {
                    16.0D - b[3], b[1], 16.0D - b[5],
                    16.0D - b[0], b[4], 16.0D - b[2]
            };
            case WEST -> new double[] {
                    b[2], b[1], 16.0D - b[3],
                    b[5], b[4], 16.0D - b[0]
            };
            case EAST -> new double[] {
                    16.0D - b[5], b[1], b[0],
                    16.0D - b[2], b[4], b[3]
            };
            default -> b;
        };
    }

    /**
     * Axis-aligned envelope of the leaf after Gecko's real Y rotation. At
     * 100 degrees this stays narrow while following the correct swing side.
     */
    private static double[] rotateYBounds(double[] box, double pivotX,
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

                // JOML/Mojang positive Y rotation:
                // x' = x*cos + z*sin, z' = -x*sin + z*cos.
                double tx = pivotX + dx * cos + dz * sin;
                double tz = pivotZ - dx * sin + dz * cos;
                minX = Math.min(minX, tx);
                minZ = Math.min(minZ, tz);
                maxX = Math.max(maxX, tx);
                maxZ = Math.max(maxZ, tz);
            }
        }

        return new double[] {
                minX, box[1], minZ, maxX, box[4], maxZ
        };
    }

    private record Geometry(double[][] frameRaw, double[] leafRaw,
            double hingeX, double hingeZ, double openAngleDegrees,
            double[][] opaqueLeafRaw) {
    }

    // Raw cube format: originX, originY, originZ, sizeX, sizeY, sizeZ.
    private static final double[][] NORMAL_FRAME = {
            {7.75D, 0.0D, -8.25D, 1.0D, 32.0D, 1.5D},
            {-8.75D, 0.0D, -8.25D, 1.0D, 32.0D, 1.5D},
            {-8.75D, 32.0D, -8.25D, 17.5D, 1.0D, 1.5D}
    };
    private static final double[] NORMAL_LEAF =
            {-7.75D, 0.0D, -8.0D, 15.5D, 32.0D, 1.0D};
    private static final double[][] NORMAL_OPAQUE = {
            {-7.75D, 27.0D, -8.0D, 15.5D, 5.0D, 1.0D},
            {2.75D, 17.5D, -8.0D, 5.0D, 9.5D, 1.0D},
            {-7.75D, 17.5D, -8.0D, 5.0D, 9.5D, 1.0D},
            {-7.75D, 0.0D, -8.0D, 15.5D, 17.5D, 1.0D}
    };

    private static final double[][] LEFT_LOGISTICS_FRAME = {
            {-8.75D, 0.0D, -8.25D, 1.0D, 32.0D, 1.5D},
            {-8.75D, 32.0D, -8.25D, 16.75D, 1.0D, 1.5D}
    };
    private static final double[] LEFT_LOGISTICS_LEAF =
            {-7.75D, 0.0D, -8.0D, 15.75D, 32.0D, 1.0D};

    private static final double[][] RIGHT_LOGISTICS_FRAME = {
            {7.75D, 0.0D, -8.25D, 1.0D, 32.0D, 1.5D},
            {-8.0D, 32.0D, -8.25D, 16.75D, 1.0D, 1.5D}
    };
    private static final double[] RIGHT_LOGISTICS_LEAF =
            {-8.0D, 0.0D, -8.0D, 15.75D, 32.0D, 1.0D};

    private static final double[][] OFFICE_FRAME = NORMAL_FRAME;
    private static final double[] OFFICE_LEAF =
            {-7.75D, 0.0D, -8.0D, 15.5D, 32.0D, 1.0D};
    private static final double[][] OFFICE_OPAQUE = {
            {-7.75D, 30.0D, -8.0D, 15.5D, 2.0D, 1.0D},
            {6.25D, 9.5D, -8.0D, 1.5D, 20.5D, 1.0D},
            {-7.75D, 9.5D, -8.0D, 1.5D, 20.5D, 1.0D},
            {-7.75D, 0.0D, -8.0D, 15.5D, 9.5D, 1.0D}
    };

    private static final double[][] BATHROOM_FRAME = NORMAL_FRAME;
    private static final double[] BATHROOM_LEAF =
            {-7.75D, 0.0D, -8.0D, 15.5D, 32.0D, 1.0D};

    private static final double[][] WORKSHOP_FRAME = NORMAL_FRAME;
    private static final double[] WORKSHOP_LEAF =
            {-7.75D, 0.0D, -8.0D, 15.5D, 32.0D, 1.0D};
}
