package net.mcreator.scpadditions.procedures;

import net.minecraft.world.World;
import net.minecraft.world.IWorld;

import net.mcreator.scpadditions.world.Scp079controlOnGameRule;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Map;

public class SCP079SystemControlOffProcedure {

	public static void executeProcedure(Map<String, Object> dependencies) {
		if (dependencies.get("world") == null) {
			if (!dependencies.containsKey("world"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency world for procedure SCP079SystemControlOff!");
			return;
		}
		IWorld world = (IWorld) dependencies.get("world");
		if (world instanceof World) {
			((World) world).getGameRules().get(Scp079controlOnGameRule.gamerule).set((false), ((World) world).getServer());
		}
	}
}
