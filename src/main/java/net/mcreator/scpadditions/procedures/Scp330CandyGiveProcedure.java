package net.mcreator.scpadditions.procedures;

import net.minecraftforge.items.ItemHandlerHelper;

import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.Entity;

import net.mcreator.scpadditions.item.Scp330YellowCandyItem;
import net.mcreator.scpadditions.item.Scp330RedCandyItem;
import net.mcreator.scpadditions.item.Scp330GreenCandyItem;
import net.mcreator.scpadditions.item.Scp330BlueCandyItem;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Map;

public class Scp330CandyGiveProcedure {

	public static void executeProcedure(Map<String, Object> dependencies) {
		if (dependencies.get("entity") == null) {
			if (!dependencies.containsKey("entity"))
				ScpAdditionsMod.LOGGER.warn("Failed to load dependency entity for procedure Scp330CandyGive!");
			return;
		}
		Entity entity = (Entity) dependencies.get("entity");
		if (Math.random() < 0.25) {
			if (entity instanceof PlayerEntity) {
				ItemStack _setstack = new ItemStack(Scp330RedCandyItem.block);
				_setstack.setCount((int) 1);
				ItemHandlerHelper.giveItemToPlayer(((PlayerEntity) entity), _setstack);
			}
		} else {
			if (Math.random() > 0.25 && Math.random() < 0.5) {
				if (entity instanceof PlayerEntity) {
					ItemStack _setstack = new ItemStack(Scp330GreenCandyItem.block);
					_setstack.setCount((int) 1);
					ItemHandlerHelper.giveItemToPlayer(((PlayerEntity) entity), _setstack);
				}
			} else {
				if (Math.random() > 0.5 && Math.random() < 0.75) {
					if (entity instanceof PlayerEntity) {
						ItemStack _setstack = new ItemStack(Scp330YellowCandyItem.block);
						_setstack.setCount((int) 1);
						ItemHandlerHelper.giveItemToPlayer(((PlayerEntity) entity), _setstack);
					}
				} else {
					if (entity instanceof PlayerEntity) {
						ItemStack _setstack = new ItemStack(Scp330BlueCandyItem.block);
						_setstack.setCount((int) 1);
						ItemHandlerHelper.giveItemToPlayer(((PlayerEntity) entity), _setstack);
					}
				}
			}
		}
	}
}
