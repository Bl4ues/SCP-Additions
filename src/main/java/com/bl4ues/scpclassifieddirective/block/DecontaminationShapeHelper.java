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
        // The hidden controller lives directly above the entrance BLACK_DOOR.
        // Giving that invisible anchor a full-block selection shape exposed an
        // annoying cube over the doorway even though it has no physical
        // collision. The authored floor/wall/ceiling helpers and both doors are
        // already valid break targets for the complete multiblock, so the
        // controller itself does not need a visible/selectable voxel.
        return Shapes.empty();
    }

    public static VoxelShape localShape(Direction facing, int side,
            int height, int forward) {
        if (!DecontaminationStructure.isCollisionPart(side, height, forward)) {
            return Shapes.empty();
        }

        // The authored floor is a fully solid 3x5 platform.
        if (height == -1) return Shapes.block();

        // Ceiling helpers live in the block row at Y + 3 for clean ownership,
        // while the visible roof begins around Y + 2.5. Extend the collision
        // half a block downward instead of leaving a jump-sized void between
        // the walkable chamber and the physical ceiling.
        if (height == 3) {
            return Block.box(0.0D, -8.0D, 0.0D,
                    16.0D, 0.0D, 16.0D);
        }

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
