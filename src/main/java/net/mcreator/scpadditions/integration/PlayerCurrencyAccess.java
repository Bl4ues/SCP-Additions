package net.mcreator.scpadditions.integration;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

import java.util.Objects;

/**
 * Server-authoritative currency access for systems such as SCP-294.
 *
 * The storage source is selected strictly from the inventory module state:
 * - inventory enabled: only the SCP Inventory capability backend is used;
 * - inventory disabled: only the vanilla player inventory is used.
 *
 * There is deliberately no cross-source fallback. Legacy coin migration must be
 * handled explicitly so that the same currency cannot be counted twice.
 */
public final class PlayerCurrencyAccess {
	private static final CurrencyBackend EMPTY_BACKEND = new CurrencyBackend() {
		@Override
		public boolean accepts(ItemStack stack, Item currency) {
			return false;
		}

		@Override
		public int count(Player player, Item currency) {
			return 0;
		}

		@Override
		public ItemStack extractOne(Player player, Item currency) {
			return ItemStack.EMPTY;
		}

		@Override
		public int insert(Player player, ItemStack stack) {
			return 0;
		}
	};

	private static volatile CurrencyBackend customInventoryBackend = EMPTY_BACKEND;

	private PlayerCurrencyAccess() {
	}

	public static void registerCustomInventoryBackend(CurrencyBackend backend) {
		customInventoryBackend = Objects.requireNonNull(backend, "backend");
	}

	public static void unregisterCustomInventoryBackend(CurrencyBackend backend) {
		if (customInventoryBackend == backend) {
			customInventoryBackend = EMPTY_BACKEND;
		}
	}

	public static boolean usesCustomInventory() {
		return ScpAdditionsModulesConfig.get().inventory.enabled;
	}

	public static int count(Player player, Item currency) {
		if (player == null || currency == null) {
			return 0;
		}
		return usesCustomInventory()
				? Math.max(0, customInventoryBackend.count(player, currency))
				: countVanilla(player, currency);
	}

	public static boolean has(Player player, Item currency) {
		return count(player, currency) > 0;
	}

	public static boolean isCurrency(Player player, ItemStack stack, Item currency) {
		if (player == null || stack == null || stack.isEmpty() || currency == null) {
			return false;
		}
		return stack.is(currency)
				|| (usesCustomInventory() && customInventoryBackend.accepts(stack, currency));
	}

	public static ItemStack extractOne(Player player, Item currency) {
		if (player == null || currency == null) {
			return ItemStack.EMPTY;
		}

		ItemStack extracted = usesCustomInventory()
				? customInventoryBackend.extractOne(player, currency)
				: extractOneVanilla(player, currency);

		if (extracted == null || extracted.isEmpty()) {
			return ItemStack.EMPTY;
		}

		ItemStack single = extracted.copy();
		single.setCount(1);
		return single;
	}

	/**
	 * Returns escrowed currency to the currently authoritative inventory. Any
	 * remainder is dropped once rather than mirrored into the other backend.
	 */
	public static void refund(Player player, ItemStack stack) {
		if (player == null || stack == null || stack.isEmpty()) {
			return;
		}

		ItemStack remainder = stack.copy();
		int requested = remainder.getCount();
		int inserted = usesCustomInventory()
				? customInventoryBackend.insert(player, remainder)
				: insertVanilla(player, remainder);
		remainder.shrink(Math.max(0, Math.min(requested, inserted)));
		if (!remainder.isEmpty()) {
			player.drop(remainder, false);
		}
	}

	private static int countVanilla(Player player, Item currency) {
		int count = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (!stack.isEmpty() && stack.is(currency)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static ItemStack extractOneVanilla(Player player, Item currency) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (!stack.isEmpty() && stack.is(currency)) {
				return stack.split(1);
			}
		}
		return ItemStack.EMPTY;
	}

	private static int insertVanilla(Player player, ItemStack stack) {
		ItemStack inserting = stack.copy();
		int before = inserting.getCount();
		player.getInventory().add(inserting);
		player.getInventory().setChanged();
		return before - inserting.getCount();
	}

	public interface CurrencyBackend {
		boolean accepts(ItemStack stack, Item currency);

		int count(Player player, Item currency);

		ItemStack extractOne(Player player, Item currency);

		int insert(Player player, ItemStack stack);
	}
}
