package net.mcreator.scpadditions.procedures;

import net.minecraft.world.level.LevelAccessor;

import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;

public class Scp079controlsystemonProcedure {
	public static void execute(LevelAccessor world) {
		world.getLevelData().getGameRules().getRule(ScpAdditionsModGameRules.SCP079CONTROLON).set(true, world.getServer());
	}
}
