package com.bl4ues.scpclassifieddirective.procedures;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModGameRules;
import com.bl4ues.scpclassifieddirective.advancement.ScpAdvancementAwards;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.bl4ues.scpclassifieddirective.integration.PlayerItemAccess;
import com.bl4ues.scpclassifieddirective.facility.Scp079FacilityAccessManager;
import net.minecraft.network.chat.Component;

public final class TeslaTerminalController {
	private TeslaTerminalController() {
	}

	public static boolean hasSecurityCredentials(Player player) {
		return PlayerItemAccess.has(player, stack -> stack.is(ScpClassifiedDirectiveModItems.SECURITY_CREDENTIALS.get()));
	}

	public static void enableTeslaGates(LevelAccessor world, double x, double y, double z, Player player) {
		if (!requireAuxiliaryPower(world, player)) return;
		world.getLevelData().getGameRules().getRule(ScpClassifiedDirectiveModGameRules.TESLAGATEON).set(true, server(world));
		recordConfiguration(world, player);
	}

	public static void disableTeslaGates(LevelAccessor world, double x, double y, double z, Player player) {
		if (!requireAuxiliaryPower(world, player)) return;
		world.getLevelData().getGameRules().getRule(ScpClassifiedDirectiveModGameRules.TESLAGATEON).set(false, server(world));
		world.getLevelData().getGameRules().getRule(ScpClassifiedDirectiveModGameRules.TESLAGATEMANUALOVERRIDE).set(false, server(world));
		recordConfiguration(world, player);
	}

	public static void setManualOverride(LevelAccessor world, double x, double y, double z, Player player, boolean enabled) {
		if (!requireAuxiliaryPower(world, player)) return;
		if (enabled) {
			world.getLevelData().getGameRules().getRule(ScpClassifiedDirectiveModGameRules.TESLAGATEON).set(true, server(world));
		}
		world.getLevelData().getGameRules().getRule(ScpClassifiedDirectiveModGameRules.TESLAGATEMANUALOVERRIDE).set(enabled, server(world));
		recordConfiguration(world, player);
	}

	public static void toggleManualOverride(LevelAccessor world, double x, double y, double z, Player player) {
		boolean current = world.getLevelData().getGameRules().getBoolean(ScpClassifiedDirectiveModGameRules.TESLAGATEMANUALOVERRIDE);
		setManualOverride(world, x, y, z, player, !current);
	}

	public static void logout(Player player) {
		if (player != null) {
			player.closeContainer();
		}
	}

	private static void recordConfiguration(LevelAccessor world, Player player) {
		Scp079FacilityAccessManager.recordActivity(world,
				Scp079FacilityAccessManager.Activity.TESLA_CONFIGURATION);
		if (player instanceof ServerPlayer serverPlayer) {
			ScpAdvancementAwards.award(serverPlayer, ScpAdvancementAwards.TESLA);
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
