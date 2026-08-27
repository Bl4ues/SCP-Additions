package com.bl4ues.scpclassifieddirective.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Authored collision shell for the rebuilt five-block chamber. */
public final class DecontaminationShapeHelper {
    /**
     * The chamber's authored clear width is roughly 40 model units (2.5
     * blocks). The two helper columns therefore hold only the outer four pixels
     * of each host cell, leaving the centre plus most of both side cells free.
     */
    private static final double WALL_THICKNESS = 4.0D;

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

        // The authored floor is a fully solid 3x5 platform. The roof collision
        // intentionally stays a simple solid rectangle, as requested, instead
        // of reproducing every slope in the visual mesh.
        if (height == -1 || height == 3) return Shapes.block();

        // Side helpers occupy the OUTER edge of their host cells. The previous
        // implementation used the inner edge, collapsing the walkable chamber
        // to about one block wide and putting collision across the windows.
        Direction right = facing.getClockWise();
        Direction outward = side > 0 ? right : right.getOpposite();
        return switch (outward) {
            case EAST -> Block.box(16.0D - WALL_THICKNESS, 0.0D, 0.0D,
                    16.0D, 16.0D, 16.0D);
            case WEST -> Block.box(0.0D, 0.0D, 0.0D,
                    WALL_THICKNESS, 16.0D, 16.0D);
            case SOUTH -> Block.box(0.0D, 0.0D, 16.0D - WALL_THICKNESS,
                    16.0D, 16.0D, 16.0D);
            case NORTH -> Block.box(0.0D, 0.0D, 0.0D,
                    16.0D, 16.0D, WALL_THICKNESS);
            default -> Shapes.empty();
        };
    }
}
