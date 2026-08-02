package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Full-frame shapes shared by controller and side cells. */
public final class FramedSignShapes {
    private FramedSignShapes() {
    }

    public static VoxelShape controllerShape(Direction facing,
            FramedSignPosition position) {
        if (facing == null || facing.getAxis() == Direction.Axis.Y) {
            return Shapes.empty();
        }
        VoxelShape north = Block.box(
                0.2D + position.modelOffsetBlocks() * 16.0D,
                2.65D,
                13.5D,
                15.7D + position.modelOffsetBlocks() * 16.0D,
                13.35D,
                16.0D);
        return rotateY(north, quarterTurns(facing));
    }

    public static VoxelShape partShape(FacilityPropPartBlock.Part part,
            Direction facing) {
        FramedSignPosition position = part.position();
        if (position == null || facing == null
                || facing.getAxis() == Direction.Axis.Y) {
            return Shapes.empty();
        }

        BlockPos relative = BlockPos.ZERO
                .relative(facing.getClockWise(), part.sideOffset())
                .relative(Direction.UP, part.rowOffset());
        return controllerShape(facing, position).move(
                -relative.getX(), -relative.getY(), -relative.getZ());
    }

    private static int quarterTurns(Direction facing) {
        return switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    private static VoxelShape rotateY(VoxelShape source, int quarterTurns) {
        VoxelShape current = source;
        for (int turn = 0; turn < quarterTurns; turn++) {
            VoxelShape[] rotated = {Shapes.empty()};
            current.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                    rotated[0] = Shapes.or(rotated[0], Shapes.box(
                            1.0D - maxZ, minY, minX,
                            1.0D - minZ, maxY, maxX)));
            current = rotated[0].optimize();
        }
        return current;
    }
}
