package net.mcreator.scpadditions.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;

public class Lv6LeftProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		KeycardReaderHelper.handleReader(world, x, y, z, entity, 6, ScpAdditionsModBlocks.LV_6_LEFT_READER_ACCEPT, ScpAdditionsModBlocks.LV_6_LEFT_READER_WRONG);
	}
}
