package net.mcreator.scpadditions.integration;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Shared item lookup used by SCP Additions systems.
 *
 * Vanilla inventory contents are always scanned first. Migrated modules may
 * register additional item sources, such as the SCP Inventory capability,
 * without teaching every reader, machine or terminal about that capability.
 */
public final class PlayerItemAccess {
	private static final List<AdditionalItemSource> ADDITIONAL_SOURCES = new CopyOnWriteArrayList<>();

	private PlayerItemAccess() {
	}

	public static void registerAdditionalSource(AdditionalItemSource source) {
		Objects.requireNonNull(source, "source");
		if (!ADDITIONAL_SOURCES.contains(source)) {
			ADDITIONAL_SOURCES.add(source);
		}
	}

	public static void unregisterAdditionalSource(AdditionalItemSource source) {
		ADDITIONAL_SOURCES.remove(source);
	}

	public static boolean has(Player player, Predicate<ItemStack> predicate) {
		if (player == null || predicate == null) {
			return false;
		}

		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (!stack.isEmpty() && predicate.test(stack)) {
				return true;
			}
		}

		for (AdditionalItemSource source : ADDITIONAL_SOURCES) {
			Iterable<ItemStack> stacks = source.getStacks(player);
			if (stacks == null) {
				continue;
			}
			for (ItemStack stack : stacks) {
				if (stack != null && !stack.isEmpty() && predicate.test(stack)) {
					return true;
				}
			}
		}

		return false;
	}

	public static int highestLevel(Player player, ToIntFunction<ItemStack> levelResolver) {
		if (player == null || levelResolver == null) {
			return 0;
		}

		int highest = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (!stack.isEmpty()) {
				highest = Math.max(highest, levelResolver.applyAsInt(stack));
			}
		}

		for (AdditionalItemSource source : ADDITIONAL_SOURCES) {
			Iterable<ItemStack> stacks = source.getStacks(player);
			if (stacks == null) {
				continue;
			}
			for (ItemStack stack : stacks) {
				if (stack != null && !stack.isEmpty()) {
					highest = Math.max(highest, levelResolver.applyAsInt(stack));
				}
			}
		}

		return highest;
	}

	@FunctionalInterface
	public interface AdditionalItemSource {
		Iterable<ItemStack> getStacks(Player player);
	}
}
