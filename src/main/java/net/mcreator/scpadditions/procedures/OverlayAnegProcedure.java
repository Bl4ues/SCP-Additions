package net.mcreator.scpadditions.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.scpadditions.network.ScpAdditionsModVariables;

public class OverlayAnegProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		return (entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new ScpAdditionsModVariables.PlayerVariables())).Aneg;
	}
}
