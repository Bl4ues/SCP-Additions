package net.mcreator.scpadditions.procedures;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;
import net.mcreator.scpadditions.integration.PlayerItemAccess;
import net.mcreator.scpadditions.facility.Scp079FacilityAccessManager;
import net.minecraft.network.chat.Component;

public final class TeslaTerminalController {
	private TeslaTerminalController() {
	}

	public static boolean hasSecurityCredentials(Player player) {
		return PlayerItemAccess.has(player, stack -> stack.is(ScpAdditionsModItems.SECURITY_CREDENTIALS.get()));
	}

	public static void enableTeslaGates(LevelAccessor world, double x, double y, double z, Player player) {
		if (!requireAuxiliaryPower(world, player)) return;
		world.getLevelData().getGameRules().getRule(ScpAdditionsModGameRules.TESLAGATEON).set(true, server(world));
		Scp079FacilityAccessManager.recordActivity(world,
				Scp079FacilityAccessManager.Activity.TESLA_CONFIGURATION);
	}

	public static void disableTeslaGates(LevelAccessor world, double x, double y, double z, Player player) {
		if (!requireAuxiliaryPower(world, player)) return;
		world.getLevelData().getGameRules().getRule(ScpAdditionsModGameRules.TESLAGATEON).set(false, server(world));
		world.getLevelData().getGameRules().getRule(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE).set(false, server(world));
		Scp079FacilityAccessManager.recordActivity(world,
				Scp079FacilityAccessManager.Activity.TESLA_CONFIGURATION);
	}

	public static void setManualOverride(LevelAccessor world, double x, double y, double z, Player player, boolean enabled) {
		if (!requireAuxiliaryPower(world, player)) return;
		if (enabled) {
			world.getLevelData().getGameRules().getRule(ScpAdditionsModGameRules.TESLAGATEON).set(true, server(world));
		}
		world.getLevelData().getGameRules().getRule(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE).set(enabled, server(world));
		Scp079FacilityAccessManager.recordActivity(world,
				Scp079FacilityAccessManager.Activity.TESLA_CONFIGURATION);
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

	private static boolean requireAuxiliaryPower(LevelAccessor world, Player player) {
		if (Scp079FacilityAccessManager.isAuxiliaryPowerOnline(world)) return true;
		if (player != null) {
			player.displayClientMessage(Component.literal(
					"TESLA GRID UNAVAILABLE: AUXILIARY POWER ISOLATED"), true);
		}
		return false;
	}

	private static MinecraftServer server(LevelAccessor world) {
		return world instanceof Level level ? level.getServer() : null;
	}
}
