package net.mcreator.scpadditions.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;

public class TeslaTerminalOffPProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity instanceof Player player) {
			TeslaTerminalController.logout(player);
		}
	}
}
