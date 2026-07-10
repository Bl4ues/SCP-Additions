package net.mcreator.scpadditions.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;

public class Lv2LeftProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		KeycardReaderHelper.handleReader(world, x, y, z, entity, 2, ScpAdditionsModBlocks.LV_2_LEFT_READER_ACCEPT, ScpAdditionsModBlocks.LV_2_LEFT_READER_WRONG);
	}
}
