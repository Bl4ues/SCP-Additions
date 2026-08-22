package com.bl4ues.scpclassifieddirective.procedures;

import net.minecraft.world.level.LevelAccessor;

import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;

public class TeslaActiveProcedureProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		TeslaGatePulseHelper.pulseAndTransition(world, x, y, z, ScpClassifiedDirectiveModBlocks.TESLA_ACTIVE, ScpClassifiedDirectiveModBlocks.TESLA_ACTIVE_2);
	}
}
