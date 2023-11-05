package net.mcreator.scpadditions.procedures;

import net.minecraft.world.IWorld;

import net.mcreator.scpadditions.ScpAdditionsModVariables;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Map;

public class Scp294CoinSlot2Procedure {

	public static void executeProcedure(Map<String, Object> dependencies) {
		if (dependencies.get("world") == null) {
			if (!dependencies.containsKey("world"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency world for procedure Scp294CoinSlot2!");
			return;
		}
		IWorld world = (IWorld) dependencies.get("world");
		ScpAdditionsModVariables.WorldVariables.get(world).coinslot = 0;
		ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
	}
}
