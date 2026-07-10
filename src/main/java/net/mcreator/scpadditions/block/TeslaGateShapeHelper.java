package net.mcreator.scpadditions.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class TeslaGateShapeHelper {
	private static final VoxelShape NORTH_SOUTH = Shapes.or(
			Block.box(-8, -16, -2, -5, 32, 18),
			Block.box(21, -16, -2, 24, 32, 18),
			Block.box(-8, 27, -2, 24, 32, 18),
			Block.box(-8, -16, -2, 24, -13, 18));
	private static final VoxelShape EAST_WEST = Shapes.or(
			Block.box(-2, -16, -8, 18, 32, -5),
			Block.box(-2, -16, 21, 18, 32, 24),
			Block.box(-2, 27, -8, 18, 32, 24),
			Block.box(-2, -16, -8, 18, -13, 24));

	private TeslaGateShapeHelper() {
	}

	public static VoxelShape shape(Direction facing) {
		return switch (facing) {
			case EAST, WEST -> EAST_WEST;
			default -> NORTH_SOUTH;
		};
	}
}
