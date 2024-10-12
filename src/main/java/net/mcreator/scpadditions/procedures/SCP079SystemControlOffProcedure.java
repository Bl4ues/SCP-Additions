package net.mcreator.scpadditions.procedures;

import net.minecraft.world.level.LevelAccessor;

import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;

public class SCP079SystemControlOffProcedure {
	public static void execute(LevelAccessor world) {
		world.getLevelData().getGameRules().getRule(ScpAdditionsModGameRules.SCP079CONTROLON).set(false, world.getServer());
	}
}
