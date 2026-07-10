package net.mcreator.scpadditions.procedures;

import net.minecraft.world.level.LevelAccessor;

import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;

public class TeslaActive4BProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		TeslaGatePulseHelper.pulseAndTransition(world, x, y, z, ScpAdditionsModBlocks.TESLA_ACTIVE_4, ScpAdditionsModBlocks.TESLA_RECHARGE);
	}
}
