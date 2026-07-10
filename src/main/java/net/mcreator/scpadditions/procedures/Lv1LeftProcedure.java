package net.mcreator.scpadditions.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;

public class Lv1LeftProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		KeycardReaderHelper.handleReader(world, x, y, z, entity, 1, ScpAdditionsModBlocks.LEFT_READER_ACCEPT, ScpAdditionsModBlocks.LEFT_READER_WRONG);
	}
}
