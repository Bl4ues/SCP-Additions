package net.mcreator.scpadditions.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.scpadditions.network.ScpAdditionsModVariables;

public class NuclearDisplayOverlayIngameProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if ((entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new ScpAdditionsModVariables.PlayerVariables())).nuclear) {
			return true;
		}
		return false;
	}
}
