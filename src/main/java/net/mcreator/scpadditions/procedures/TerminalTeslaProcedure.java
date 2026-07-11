package net.mcreator.scpadditions.procedures;

import net.minecraftforge.network.NetworkHooks;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;

import net.mcreator.scpadditions.init.ScpAdditionsModGameRules;
import net.mcreator.scpadditions.world.inventory.TeslaTerminalMenu;

import io.netty.buffer.Unpooled;

public class TerminalTeslaProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (!(entity instanceof ServerPlayer player)) {
			return;
		}
		BlockPos pos = BlockPos.containing(x, y, z);
		boolean teslaOn = world.getLevelData().getGameRules().getBoolean(ScpAdditionsModGameRules.TESLAGATEON);
		boolean manualOverride = world.getLevelData().getGameRules().getBoolean(ScpAdditionsModGameRules.TESLAGATEMANUALOVERRIDE);
		NetworkHooks.openScreen(player, new MenuProvider() {
			@Override
			public Component getDisplayName() {
				return Component.literal("Tesla Terminal");
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
				FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos);
				data.writeBoolean(teslaOn);
				data.writeBoolean(manualOverride);
				return new TeslaTerminalMenu(id, inventory, data);
			}
		}, data -> {
			data.writeBlockPos(pos);
			data.writeBoolean(teslaOn);
			data.writeBoolean(manualOverride);
		});
	}
}