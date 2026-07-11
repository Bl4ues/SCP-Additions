package net.mcreator.scpadditions.procedures;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;

public final class TeslaTerminalController {
	private TeslaTerminalController() {
	}

	public static boolean hasSecurityCredentials(Player player) {
		if (player == null) {
			return false;
		}
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(ScpAdditionsModItems.SECURITY_CREDENTIALS.get())) {
				return true;
			}
		}
		return false;
	}

	public static void enableTeslaGates(LevelAccessor world, double x, double y, double z, Player player) {
		world.getLevelData().getGameRules().getRule(ScpAdditionsModGameRules.TESLAGATEON).set(true, server(world));
	}

	public static void disableTeslaGates(LevelAccessor world, double x, double y, double z, Player player) {
		world.getLevelData().getGameRules().getRule(ScpAdditionsModGameRules.TESLAGATEON).set(false, server(world));
		world.getLevelData().getGameRules().getRule(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE).set(false, server(world));
	}

	public static void setManualOverride(LevelAccessor world, double x, double y, double z, Player player, boolean enabled) {
		if (enabled) {
			world.getLevelData().getGameRules().getRule(ScpAdditionsModGameRules.TESLAGATEON).set(true, server(world));
		}
		world.getLevelData().getGameRules().getRule(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE).set(enabled, server(world));
	}

	public static void toggleManualOverride(LevelAccessor world, double x, double y, double z, Player player) {
		boolean current = world.getLevelData().getGameRules().getBoolean(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE);
		setManualOverride(world, x, y, z, player, !current);
	}

	public static void logout(Player player) {
		if (player != null) {
			player.closeContainer();
		}
	}

	private static MinecraftServer server(LevelAccessor world) {
		return world instanceof Level level ? level.getServer() : null;
	}
}