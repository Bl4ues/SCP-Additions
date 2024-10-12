package net.mcreator.scpadditions.procedures;

import net.minecraft.world.level.LevelAccessor;

import net.mcreator.scpadditions.network.ScpAdditionsModVariables;

public class Scp914dialRoughBlockDestroyedByPlayerProcedure {
	public static void execute(LevelAccessor world) {
		ScpAdditionsModVariables.MapVariables.get(world).Scp914Rough = false;
		ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
		ScpAdditionsModVariables.MapVariables.get(world).Scp914Coarse = false;
		ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
		ScpAdditionsModVariables.MapVariables.get(world).Scp914OneToOne = true;
		ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
		ScpAdditionsModVariables.MapVariables.get(world).Scp914Fine = false;
		ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
		ScpAdditionsModVariables.MapVariables.get(world).Scp914VeryFine = false;
		ScpAdditionsModVariables.MapVariables.get(world).syncData(world);
	}
}
