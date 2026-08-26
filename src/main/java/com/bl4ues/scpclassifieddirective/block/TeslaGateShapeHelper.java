package com.bl4ues.scpclassifieddirective.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Coarse selectable volume matching the replacement Tesla Gate model. */
final class TeslaGateShapeHelper {
    /*
     * Blockbench/GeckoLib model X is mirrored relative to Minecraft's local
     * VoxelShape X for this asset. These boxes are authored from the visual
     * model and mirrored around the controller block centre before FACING is
     * applied, keeping selection aligned with the cabinet and frame.
     */
    private static final double[][] NORTH_MODEL_BOXES = {
            // Left and right frame uprights.
            {-17, 0, -3, -5, 64, 19},
            {21, 0, -3, 33, 64, 19},
            // Large electronics cabinet on the model's right side.
            {32, 0, 0, 64, 64, 16},
            // Header and floor plates. Fine cameras/bolts are intentionally ignored.
            {-12, 58, 2, 28, 64, 14},
            {-10, 0, -17, 26, 1.5, 33}
    };

    private TeslaGateShapeHelper() {
    }

    static VoxelShape shape(Direction facing) {
        VoxelShape result = Shapes.empty();
        for (double[] modelBox : NORTH_MODEL_BOXES) {
            result = Shapes.or(result, rotatedBox(mirrorModelX(modelBox), facing));
        }
        return result.optimize();
    }

    private static double[] mirrorModelX(double[] box) {
        return new double[] {
                16.0D - box[3], box[1], box[2],
                16.0D - box[0], box[4], box[5]
        };
    }

    private static VoxelShape rotatedBox(double[] b, Direction facing) {
        double minX = b[0], minY = b[1], minZ = b[2];
        double maxX = b[3], maxY = b[4], maxZ = b[5];
        return switch (facing) {
            case SOUTH -> Block.box(16 - maxX, minY, 16 - maxZ,
                    16 - minX, maxY, 16 - minZ);
            case EAST -> Block.box(16 - maxZ, minY, minX,
                    16 - minZ, maxY, maxX);
            case WEST -> Block.box(minZ, minY, 16 - maxX,
                    maxZ, maxY, 16 - minX);
            default -> Block.box(minX, minY, minZ, maxX, maxY, maxZ);
        };
    }
}
