package net.mcreator.scpadditions.procedures;

import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.data.Scp294DrinkManager;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;
import net.mcreator.scpadditions.network.ScpAdditionsModVariables;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class Scp294drinkGiveProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, String input) {
		if (!(entity instanceof Player player)) {
			return;
		}

		playSound(world, x, y, z, new ResourceLocation("scp_additions:scp294enter"));

		Slot coinSlot = getCoinSlot(player);
		if (coinSlot == null || coinSlot.getItem().getItem() != ScpAdditionsModItems.COIN.get()) {
			return;
		}

		Optional<Scp294DrinkManager.DrinkDefinition> optionalDrink = Scp294DrinkManager.findByAlias(input);
		if (optionalDrink.isEmpty()) {
			playSound(world, x, y, z, new ResourceLocation("scp_additions:scp294outofrange"));
			return;
		}

		Scp294DrinkManager.DrinkDefinition drink = optionalDrink.get();
		ItemStack result = Scp294DrinkManager.createResult(drink);
		if (result.isEmpty()) {
			return;
		}

		if (drink.consumesCoin()) {
			coinSlot.remove(1);
			player.containerMenu.broadcastChanges();
		}

		playSound(world, x, y, z, drink.sound());
		ScpAdditionsMod.queueServerWork(drink.delayTicks(), () -> ItemHandlerHelper.giveItemToPlayer(player, result.copy()));

		ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
		ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
	}

	private static Slot getCoinSlot(Player player) {
		if (player.containerMenu instanceof Supplier<?> supplier && supplier.get() instanceof Map<?, ?> slots) {
			Object slot = slots.get(0);
			if (slot instanceof Slot coinSlot) {
				return coinSlot;
			}
		}
		return null;
	}

	private static void playSound(LevelAccessor world, double x, double y, double z, ResourceLocation sound) {
		if (world instanceof Level level) {
			if (!level.isClientSide()) {
				level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(sound), SoundSource.NEUTRAL, 1, 1);
			} else {
				level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(sound), SoundSource.NEUTRAL, 1, 1, false);
			}
		}
	}
}