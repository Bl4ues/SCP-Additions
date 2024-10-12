package net.mcreator.scpadditions.procedures;

import net.minecraftforge.items.ItemHandlerHelper;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;

import net.mcreator.scpadditions.init.ScpAdditionsModItems;

public class Scp330CandyGiveProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (Math.random() < 0.25) {
			if (entity instanceof Player _player) {
				ItemStack _setstack = new ItemStack(ScpAdditionsModItems.SCP_330_RED_CANDY.get());
				_setstack.setCount(1);
				ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
			}
		} else {
			if (Math.random() > 0.25 && Math.random() < 0.5) {
				if (entity instanceof Player _player) {
					ItemStack _setstack = new ItemStack(ScpAdditionsModItems.SCP_330_GREEN_CANDY.get());
					_setstack.setCount(1);
					ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
				}
			} else {
				if (Math.random() > 0.5 && Math.random() < 0.75) {
					if (entity instanceof Player _player) {
						ItemStack _setstack = new ItemStack(ScpAdditionsModItems.SCP_330_YELLOW_CANDY.get());
						_setstack.setCount(1);
						ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
					}
				} else {
					if (entity instanceof Player _player) {
						ItemStack _setstack = new ItemStack(ScpAdditionsModItems.SCP_330_BLUE_CANDY.get());
						_setstack.setCount(1);
						ItemHandlerHelper.giveItemToPlayer(_player, _setstack);
					}
				}
			}
		}
	}
}
