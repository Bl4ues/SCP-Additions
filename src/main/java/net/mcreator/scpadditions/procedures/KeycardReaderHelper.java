package net.mcreator.scpadditions.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import net.mcreator.scpadditions.init.ScpAdditionsModItems;

import java.util.Map;
import java.util.function.Supplier;

public final class KeycardReaderHelper {
	private KeycardReaderHelper() {
	}

	public static void handleReader(LevelAccessor world, double x, double y, double z, Entity entity, int requiredLevel, Supplier<? extends Block> acceptBlock, Supplier<? extends Block> wrongBlock) {
		if (!(entity instanceof Player player)) {
			return;
		}

		setReaderState(world, BlockPos.containing(x, y, z), highestKeycardLevel(player) >= requiredLevel ? acceptBlock.get().defaultBlockState() : wrongBlock.get().defaultBlockState());
	}

	private static int highestKeycardLevel(Player player) {
		int highest = 0;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.isEmpty()) {
				continue;
			}
			highest = Math.max(highest, keycardLevel(stack.getItem()));
			if (highest >= 6) {
				return highest;
			}
		}
		return highest;
	}

	private static int keycardLevel(Item item) {
		if (item == ScpAdditionsModItems.LEVEL_6_KEYCARD.get()) return 6;
		if (item == ScpAdditionsModItems.LEVEL_5_KEYCARD.get()) return 5;
		if (item == ScpAdditionsModItems.LEVEL_4_KEYCARD.get()) return 4;
		if (item == ScpAdditionsModItems.LEVEL_3_KEYCARD.get()) return 3;
		if (item == ScpAdditionsModItems.LEVEL_2_KEYCARD.get()) return 2;
		if (item == ScpAdditionsModItems.LEVEL_1_KEYCARD.get()) return 1;
		return 0;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void setReaderState(LevelAccessor world, BlockPos pos, BlockState targetState) {
		BlockState currentState = world.getBlockState(pos);
		BlockState result = targetState;
		for (Map.Entry<Property<?>, Comparable<?>> entry : currentState.getValues().entrySet()) {
			Property property = result.getBlock().getStateDefinition().getProperty(entry.getKey().getName());
			if (property != null) {
				try {
					result = result.setValue((Property) property, (Comparable) entry.getValue());
				} catch (Exception ignored) {
				}
			}
		}
		world.setBlock(pos, result, 3);
	}
}
