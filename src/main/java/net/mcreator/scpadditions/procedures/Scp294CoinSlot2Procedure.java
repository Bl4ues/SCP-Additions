package net.mcreator.scpadditions.procedures;

import net.minecraft.world.level.LevelAccessor;

import net.mcreator.scpadditions.network.ScpAdditionsModVariables;

public class Scp294CoinSlot2Procedure {
	public static void execute(LevelAccessor world) {
		ScpAdditionsModVariables.WorldVariables.get(world).coinslot = 0;
		ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
	}
}
