package com.bl4ues.scpclassifieddirective.procedures;

import net.minecraft.world.level.LevelAccessor;

import com.bl4ues.scpclassifieddirective.network.ScpClassifiedDirectiveModVariables;

public class Scp914dialRoughBlockDestroyedByPlayerProcedure {
	public static void execute(LevelAccessor world) {
		ScpClassifiedDirectiveModVariables.MapVariables.get(world).Scp914Rough = false;
		ScpClassifiedDirectiveModVariables.MapVariables.get(world).syncData(world);
		ScpClassifiedDirectiveModVariables.MapVariables.get(world).Scp914Coarse = false;
		ScpClassifiedDirectiveModVariables.MapVariables.get(world).syncData(world);
		ScpClassifiedDirectiveModVariables.MapVariables.get(world).Scp914OneToOne = true;
		ScpClassifiedDirectiveModVariables.MapVariables.get(world).syncData(world);
		ScpClassifiedDirectiveModVariables.MapVariables.get(world).Scp914Fine = false;
		ScpClassifiedDirectiveModVariables.MapVariables.get(world).syncData(world);
		ScpClassifiedDirectiveModVariables.MapVariables.get(world).Scp914VeryFine = false;
		ScpClassifiedDirectiveModVariables.MapVariables.get(world).syncData(world);
	}
}
