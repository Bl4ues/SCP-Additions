package net.mcreator.scpadditions.procedures;

import net.minecraft.entity.Entity;

import net.mcreator.scpadditions.ScpAdditionsModVariables;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Map;

public class NuclearDisplayOverlayIngameProcedure {

	public static boolean executeProcedure(Map<String, Object> dependencies) {
		if (dependencies.get("entity") == null) {
			if (!dependencies.containsKey("entity"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency entity for procedure NuclearDisplayOverlayIngame!");
			return false;
		}
		Entity entity = (Entity) dependencies.get("entity");
		if ((entity.getCapability(ScpAdditionsModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new ScpAdditionsModVariables.PlayerVariables())).nuclear) {
			return true;
		}
		return false;
	}
}
