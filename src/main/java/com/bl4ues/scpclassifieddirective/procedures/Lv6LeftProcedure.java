package com.bl4ues.scpclassifieddirective.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;

public class Lv6LeftProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		KeycardReaderHelper.handleReader(world, x, y, z, entity, 6, ScpClassifiedDirectiveModBlocks.LV_6_LEFT_READER_ACCEPT, ScpClassifiedDirectiveModBlocks.LV_6_LEFT_READER_WRONG);
	}
}
