package net.mcreator.scpadditions.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Model-derived selection and collision outlines for the decontamination
 * checkpoint. The large model is split into block-local pieces by
 * {@link #localShape(Direction, boolean, int, int, int)} so collision is
 * evaluated in every occupied cell rather than from one oversized block.
 */
public final class DecontaminationShapeHelper {
    private static final VoxelShape OPEN_SOUTH = Shapes.or(
            Block.box(-16, -16, -16, -13, 32, 32),
            Block.box(-16, 19, -16, 16, 32, -13),
            Block.box(13, -16, -16, 16, 32, 32),
            Block.box(-16, 19, 29, 15, 32, 32),
            Block.box(-16, -16, 29, -8, 19, 32),
            Block.box(10, -16, 29, 16, 19, 32),
            Block.box(10, -16, -16, 16, 19, -13),
            Block.box(-16, -16, -16, -8, 19, -13),
            Block.box(10, -16, -16, 16, 19, 0),
            Block.box(-14, 30, -16, 0, 32, 32));

    private static final VoxelShape OPEN_NORTH = Shapes.or(
            Block.box(29, -16, -16, 32, 32, 32),
            Block.box(0, 19, 29, 32, 32, 32),
            Block.box(0, -16, -16, 3, 32, 32),
            Block.box(1, 19, -16, 32, 32, -13),
            Block.box(24, -16, -16, 32, 19, -13),
            Block.box(0, -16, -16, 6, 19, -13),
            Block.box(0, -16, 29, 6, 19, 32),
            Block.box(24, -16, 29, 32, 19, 32),
            Block.box(0, -16, 16, 6, 19, 32),
            Block.box(16, 30, -16, 30, 32, 32));

    private static final VoxelShape OPEN_EAST = Shapes.or(
            Block.box(-16, -16, 29, 32, 32, 32),
            Block.box(-16, 19, 0, -13, 32, 32),
            Block.box(-16, -16, 0, 32, 32, 3),
            Block.box(29, 19, 1, 32, 32, 32),
            Block.box(29, -16, 24, 32, 19, 32),
            Block.box(29, -16, 0, 32, 19, 6),
            Block.box(-16, -16, 0, -13, 19, 6),
            Block.box(-16, -16, 24, -13, 19, 32),
            Block.box(-16, -16, 6, -13, 19, 24),
            Block.box(-16, -16, 0, 0, 19, 6),
            Block.box(-16, 30, 16, 32, 32, 30));

    private static final VoxelShape OPEN_WEST = Shapes.or(
            Block.box(-16, -16, -16, 32, 32, -13),
            Block.box(29, 19, -16, 32, 32, 16),
            Block.box(-16, -16, 13, 32, 32, 16),
            Block.box(-16, 19, -16, -13, 32, 15),
            Block.box(-16, -16, -16, -13, 19, -8),
            Block.box(-16, -16, 10, -13, 19, 16),
            Block.box(29, -16, 10, 32, 19, 16),
            Block.box(29, -16, -16, 32, 19, -8),
            Block.box(16, -16, 10, 32, 19, 16),
            Block.box(-16, 30, -14, 32, 32, 0));

    private static final VoxelShape CLOSED_SOUTH = Shapes.or(
            Block.box(-16, -16, -16, -13, 32, 32),
            Block.box(-16, 19, -16, 16, 32, -13),
            Block.box(13, -16, -16, 16, 32, 32),
            Block.box(-16, 19, 29, 15, 32, 32),
            Block.box(-16, -16, 29, -8, 19, 32),
            Block.box(10, -16, 29, 16, 19, 32),
            Block.box(10, -16, -16, 16, 19, -13),
            Block.box(-16, -16, -16, -8, 19, -13),
            Block.box(-8, -16, -16, 10, 19, -13),
            Block.box(-8, -16, 29, 10, 19, 32),
            Block.box(-14, 30, -16, 0, 32, 32));

    private static final VoxelShape CLOSED_NORTH = Shapes.or(
            Block.box(29, -16, -16, 32, 32, 32),
            Block.box(0, 19, 29, 32, 32, 32),
            Block.box(0, -16, -16, 3, 32, 32),
            Block.box(1, 19, -16, 32, 32, -13),
            Block.box(24, -16, -16, 32, 19, -13),
            Block.box(0, -16, -16, 6, 19, -13),
            Block.box(0, -16, 29, 6, 19, 32),
            Block.box(24, -16, 29, 32, 19, 32),
            Block.box(6, -16, 29, 24, 19, 32),
            Block.box(6, -16, -16, 24, 19, -13),
            Block.box(16, 30, -16, 30, 32, 32));

    private static final VoxelShape CLOSED_EAST = Shapes.or(
            Block.box(-16, -16, 29, 32, 32, 32),
            Block.box(-16, 19, 0, -13, 32, 32),
            Block.box(-16, -16, 0, 32, 32, 3),
            Block.box(29, 19, 1, 32, 32, 32),
            Block.box(29, -16, 24, 32, 19, 32),
            Block.box(29, -16, 0, 32, 19, 6),
            Block.box(-16, -16, 0, -13, 19, 6),
            Block.box(-16, -16, 24, -13, 19, 32),
            Block.box(-16, -16, 6, -13, 19, 24),
            Block.box(29, -16, 6, 32, 19, 24),
            Block.box(-16, 30, 16, 32, 32, 30));

    private static final VoxelShape CLOSED_WEST = Shapes.or(
            Block.box(-16, -16, -16, 32, 32, -13),
            Block.box(29, 19, -16, 32, 32, 16),
            Block.box(-16, -16, 13, 32, 32, 16),
            Block.box(-16, 19, -16, -13, 32, 15),
            Block.box(-16, -16, -16, -13, 19, -8),
            Block.box(-16, -16, 10, -13, 19, 16),
            Block.box(29, -16, 10, 32, 19, 16),
            Block.box(29, -16, -16, 32, 19, -8),
            Block.box(29, -16, -8, 32, 19, 10),
            Block.box(-16, -16, -8, -13, 19, 10),
            Block.box(-16, 30, -14, 32, 32, 0));

    private DecontaminationShapeHelper() {
    }

    public static VoxelShape shape(Direction facing, boolean closed) {
        if (closed) {
            return switch (facing) {
                case NORTH -> CLOSED_NORTH;
                case EAST -> CLOSED_EAST;
                case WEST -> CLOSED_WEST;
                default -> CLOSED_SOUTH;
            };
        }
        return switch (facing) {
            case NORTH -> OPEN_NORTH;
            case EAST -> OPEN_EAST;
            case WEST -> OPEN_WEST;
            default -> OPEN_SOUTH;
        };
    }

    public static VoxelShape localShape(Direction facing, boolean closed,
            int offsetX, int offsetY, int offsetZ) {
        VoxelShape shifted = shape(facing, closed).move(
                -offsetX, -offsetY, -offsetZ);
        return Shapes.join(shifted, Shapes.block(), BooleanOp.AND);
    }

    public static VoxelShape localStructureShape(Direction facing,
            int offsetX, int offsetY, int offsetZ) {
        return Shapes.or(
                localShape(facing, false, offsetX, offsetY, offsetZ),
                localShape(facing, true, offsetX, offsetY, offsetZ));
    }

    public static boolean hasStructurePart(Direction facing,
            int offsetX, int offsetY, int offsetZ) {
        return !localStructureShape(facing, offsetX, offsetY, offsetZ).isEmpty();
    }
}
