package net.mcreator.scpadditions.procedures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;

public class TerminalOffProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		TeslaTerminalController.disableTeslaGates(world, x, y, z, null);
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Player player) {
		TeslaTerminalController.disableTeslaGates(world, x, y, z, player);
	}
}
