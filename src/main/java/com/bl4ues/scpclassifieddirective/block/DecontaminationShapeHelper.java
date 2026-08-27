package com.bl4ues.scpclassifieddirective.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Simple, authored collision shell for the rebuilt five-block chamber. */
public final class DecontaminationShapeHelper {
    private DecontaminationShapeHelper() {
    }

    public static VoxelShape controllerSelectionShape() {
        return Shapes.block();
    }

    public static VoxelShape localShape(Direction facing, int side,
            int height, int forward) {
        if (!DecontaminationStructure.isCollisionPart(side, height, forward)) {
            return Shapes.empty();
        }

        // Floor and ceiling are deliberately simple full-cell rectangles.
        if (height == -1 || height == 3) return Shapes.block();

        // Side walls are a thin strip on the inner edge of their host cells.
        Direction right = facing.getClockWise();
        Direction hostDirection = side > 0 ? right : right.getOpposite();
        return switch (hostDirection) {
            case EAST -> Block.box(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D);
            case WEST -> Block.box(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
            case SOUTH -> Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D);
            case NORTH -> Block.box(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D);
            default -> Shapes.empty();
        };
    }
}
