package com.bl4ues.scpclassifieddirective.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;

public class Lv2RightProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		KeycardReaderHelper.handleReader(world, x, y, z, entity, 2, ScpClassifiedDirectiveModBlocks.LV_2_RIGHT_READER_ACCEPT, ScpClassifiedDirectiveModBlocks.LV_2_RIGHT_READER_WRONG);
	}
}
