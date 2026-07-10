package net.mcreator.scpadditions.procedures;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundSource;
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
		return player != null && player.getInventory().contains(new ItemStack(ScpAdditionsModItems.SECURITY_CREDENTIALS.get()));
	}

	public static void enableTeslaGates(LevelAccessor world, double x, double y, double z, Player player) {
		if (!authorize(world, x, y, z, player)) {
			return;
		}
		play(world, x, y, z, "scp_additions:click");
		world.getLevelData().getGameRules().getRule(ScpAdditionsModGameRules.TESLAGATEON).set(true, server(world));
	}

	public static void disableTeslaGates(LevelAccessor world, double x, double y, double z, Player player) {
		if (!authorize(world, x, y, z, player)) {
			return;
		}
		play(world, x, y, z, "scp_additions:click");
		world.getLevelData().getGameRules().getRule(ScpAdditionsModGameRules.TESLAGATEON).set(false, server(world));
		world.getLevelData().getGameRules().getRule(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE).set(false, server(world));
	}

	public static void toggleManualOverride(LevelAccessor world, double x, double y, double z, Player player) {
		if (!authorize(world, x, y, z, player)) {
			return;
		}
		boolean current = world.getLevelData().getGameRules().getBoolean(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE);
		world.getLevelData().getGameRules().getRule(ScpAdditionsModGameRules.TESLAGATEON).set(true, server(world));
		world.getLevelData().getGameRules().getRule(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE).set(!current, server(world));
		play(world, x, y, z, "scp_additions:click");
	}

	public static void logout(Player player) {
		if (player != null) {
			player.closeContainer();
		}
	}

	private static boolean authorize(LevelAccessor world, double x, double y, double z, Player player) {
		if (hasSecurityCredentials(player)) {
			return true;
		}
		play(world, x, y, z, "scp_additions:accessdenied");
		message(player, "Incorrect credentials. Please contact Security Admin.");
		return false;
	}

	private static MinecraftServer server(LevelAccessor world) {
		return world instanceof Level level ? level.getServer() : null;
	}

	private static void message(Player player, String text) {
		if (player != null) {
			player.displayClientMessage(Component.literal(text), true);
		}
	}

	private static void play(LevelAccessor world, double x, double y, double z, String soundId) {
		if (world instanceof Level level) {
			ResourceLocation sound = new ResourceLocation(soundId);
			if (!level.isClientSide()) {
				level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(sound), SoundSource.NEUTRAL, 1, 1);
			} else {
				level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(sound), SoundSource.NEUTRAL, 1, 1, false);
			}
		}
	}
}
