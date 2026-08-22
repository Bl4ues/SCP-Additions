package com.bl4ues.scpclassifieddirective.procedures;

import net.minecraft.world.level.LevelAccessor;

import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;

public class TeslaActive2BlockAddedProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		TeslaGatePulseHelper.pulseAndTransition(world, x, y, z, ScpClassifiedDirectiveModBlocks.TESLA_ACTIVE_2, ScpClassifiedDirectiveModBlocks.TESLA_ACTIVE_3);
	}
}
