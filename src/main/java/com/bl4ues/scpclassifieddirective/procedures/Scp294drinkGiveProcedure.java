package com.bl4ues.scpclassifieddirective.procedures;

import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.block.entity.Scp294BlockEntity;
import com.bl4ues.scpclassifieddirective.data.Scp294ActionExecutor;
import com.bl4ues.scpclassifieddirective.data.Scp294DrinkManager;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.bl4ues.scpclassifieddirective.integration.PlayerCurrencyAccess;
import com.bl4ues.scpclassifieddirective.network.ScpClassifiedDirectiveModVariables;

import java.util.Map;
import java.util.function.Supplier;

public class Scp294drinkGiveProcedure {
	private static final int OUT_OF_RANGE_TICKS = 20;

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, String input) {
		if (!(entity instanceof Player player)) {
			return;
		}

		playSound(world, x, y, z, new ResourceLocation("scp_classified_directive:scp294enter"));

		Slot coinSlot = getCoinSlot(player);
		if (coinSlot == null || !PlayerCurrencyAccess.isCurrency(
				player, coinSlot.getItem(), ScpClassifiedDirectiveModItems.COIN.get())) {
			return;
		}

		Scp294DrinkManager.MatchResult match = Scp294DrinkManager.findByInput(input);
		if (!match.found()) {
			BlockEntity blockEntity = world.getBlockEntity(BlockPos.containing(x, y, z));
			if (blockEntity instanceof Scp294BlockEntity machine) {
				machine.showOutOfRange(OUT_OF_RANGE_TICKS);
			}
			playSound(world, x, y, z, new ResourceLocation("scp_classified_directive:scp294outofrange"));
			return;
		}

		Scp294DrinkManager.DrinkDefinition drink = match.drink();
		ItemStack result = ItemStack.EMPTY;
		if (drink.giveResult()) {
			result = Scp294DrinkManager.createResult(drink);
			if (result.isEmpty()) {
				return;
			}
		}

		if (drink.consumesCoin()) {
			coinSlot.remove(1);
			player.containerMenu.broadcastChanges();
		}

		int dispenseDelay = drink.giveResult() ? Math.max(0, drink.delayTicks()) : 0;
		BlockEntity machineEntity = world.getBlockEntity(BlockPos.containing(x, y, z));
		if (machineEntity instanceof Scp294BlockEntity machine && dispenseDelay > 0) {
			machine.startPouring(dispenseDelay);
		}

		player.closeContainer();
		playSound(world, x, y, z, drink.sound());
		ListTag dispenseActions = Scp294DrinkManager.actionsToTag(drink.dispenseActions());
		Scp294ActionExecutor.executeActions(world, x, y, z, player, dispenseActions);

		if (drink.giveResult()) {
			ItemStack resultCopy = result.copy();
			ScpClassifiedDirectiveMod.queueServerWork(dispenseDelay,
					() -> ItemHandlerHelper.giveItemToPlayer(player, resultCopy));
		}

		ScpClassifiedDirectiveModVariables.WorldVariables.get(world).Scp294stock = ScpClassifiedDirectiveModVariables.WorldVariables.get(world).Scp294stock + 1;
		ScpClassifiedDirectiveModVariables.WorldVariables.get(world).syncData(world);
	}

	public static void insertCoinFromInventory(Player player) {
		Slot coinSlot = getCoinSlot(player);
		if (coinSlot == null || coinSlot.hasItem()) {
			return;
		}

		ItemStack coin = PlayerCurrencyAccess.extractOne(player, ScpClassifiedDirectiveModItems.COIN.get());
		if (coin.isEmpty()) {
			return;
		}
		coinSlot.set(coin);
		coinSlot.setChanged();
		player.containerMenu.broadcastChanges();
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
