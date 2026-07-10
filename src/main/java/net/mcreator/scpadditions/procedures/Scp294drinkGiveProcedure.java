package net.mcreator.scpadditions.procedures;

import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.data.Scp294ActionExecutor;
import net.mcreator.scpadditions.data.Scp294DrinkManager;
import net.mcreator.scpadditions.init.ScpAdditionsModBlocks;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;
import net.mcreator.scpadditions.network.ScpAdditionsModVariables;

import java.util.Map;
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

		Scp294DrinkManager.MatchResult match = Scp294DrinkManager.findByInput(input);
		if (!match.found()) {
			player.closeContainer();
			showOutOfRangeScreen(world, x, y, z);
			playSound(world, x, y, z, new ResourceLocation("scp_additions:scp294outofrange"));
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

		player.closeContainer();
		playSound(world, x, y, z, drink.sound());
		ListTag dispenseActions = Scp294DrinkManager.actionsToTag(drink.dispenseActions());
		Scp294ActionExecutor.executeActions(world, x, y, z, player, dispenseActions);

		if (drink.giveResult()) {
			ItemStack resultCopy = result.copy();
			ScpAdditionsMod.queueServerWork(drink.delayTicks(), () -> ItemHandlerHelper.giveItemToPlayer(player, resultCopy));
		}

		ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock = ScpAdditionsModVariables.WorldVariables.get(world).Scp294stock + 1;
		ScpAdditionsModVariables.WorldVariables.get(world).syncData(world);
	}

	public static void insertCoinFromInventory(Player player) {
		Slot coinSlot = getCoinSlot(player);
		if (coinSlot == null || coinSlot.hasItem()) {
			return;
		}

		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.getItem() == ScpAdditionsModItems.COIN.get()) {
				ItemStack coin = stack.split(1);
				coinSlot.set(coin);
				coinSlot.setChanged();
				player.containerMenu.broadcastChanges();
				return;
			}
		}
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

	private static void showOutOfRangeScreen(LevelAccessor world, double x, double y, double z) {
		BlockPos pos = BlockPos.containing(x, y, z);
		BlockState oldState = world.getBlockState(pos);
		BlockState newState = copyProperties(oldState, ScpAdditionsModBlocks.SCP_294_OUT_OF_RANGE.get().defaultBlockState());
		BlockEntity blockEntity = world.getBlockEntity(pos);
		CompoundTag blockEntityTag = null;
		if (blockEntity != null) {
			blockEntityTag = blockEntity.saveWithFullMetadata();
			blockEntity.setRemoved();
		}
		world.setBlock(pos, newState, 3);
		if (blockEntityTag != null) {
			BlockEntity newBlockEntity = world.getBlockEntity(pos);
			if (newBlockEntity != null) {
				try {
					newBlockEntity.load(blockEntityTag);
				} catch (Exception ignored) {
				}
			}
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static BlockState copyProperties(BlockState from, BlockState to) {
		BlockState result = to;
		for (Map.Entry<Property<?>, Comparable<?>> entry : from.getValues().entrySet()) {
			Property property = result.getBlock().getStateDefinition().getProperty(entry.getKey().getName());
			if (property != null) {
				try {
					result = result.setValue((Property) property, (Comparable) entry.getValue());
				} catch (Exception ignored) {
				}
			}
		}
		return result;
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