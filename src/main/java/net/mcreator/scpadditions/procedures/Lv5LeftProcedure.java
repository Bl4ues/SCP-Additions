package net.mcreator.scpadditions.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;

public class Lv5LeftProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		KeycardReaderHelper.handleReader(world, x, y, z, entity, 5, ScpAdditionsModBlocks.LV_5_LEFT_READER_ACCEPT, ScpAdditionsModBlocks.LV_5_LEFT_READER_WRONG);
	}
}
