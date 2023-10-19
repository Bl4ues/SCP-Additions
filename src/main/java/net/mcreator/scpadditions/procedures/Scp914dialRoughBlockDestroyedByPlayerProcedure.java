package net.mcreator.scpadditions.procedures;

import net.minecraft.world.IWorld;

import net.mcreator.scpadditions.ScpAdditionsModVariables;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Map;

public class Scp914dialRoughBlockDestroyedByPlayerProcedure {

	public static void executeProcedure(Map<String, Object> dependencies) {
		if (dependencies.get("world") == null) {
			if (!dependencies.containsKey("world"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency world for procedure Scp914dialRoughBlockDestroyedByPlayer!");
			return;
		}
		IWorld world = (IWorld) dependencies.get("world");
		ScpAdditionsModVariables.MapVariables.get(world).Scp914Rough = (false);
		ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
		ScpAdditionsModVariables.MapVariables.get(world).Scp914Coarse = (false);
		ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
		ScpAdditionsModVariables.MapVariables.get(world).Scp914OneToOne = (true);
		ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
		ScpAdditionsModVariables.MapVariables.get(world).Scp914Fine = (false);
		ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
		ScpAdditionsModVariables.MapVariables.get(world).Scp914VeryFine = (false);
		ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
	}
}
