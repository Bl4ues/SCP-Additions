package net.mcreator.scpadditions.inventory;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.bl4ues.scpadditions.compat.LazyOptional;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

import java.util.ArrayList;
import java.util.List;

public final class ScpInventoryAccess {
    private static final ResourceLocation CANONICAL_SCP_294_COIN =
            ResourceLocation.fromNamespaceAndPath("scp_additions", "coin");

    private ScpInventoryAccess() {
    }

    public static LazyOptional<IScpInventory> get(Player player) {
        return player == null
                ? LazyOptional.empty()
                : ScpInventoryCapability.get(player);
    }

    public static Iterable<ItemStack> visibleStacks(Player player) {
        if (!ScpAdditionsModulesConfig.get().inventory.enabled) {
            return List.of();
        }
        List<ItemStack> result = new ArrayList<>();
        get(player).ifPresent(inventory -> {
            result.addAll(inventory.getInventory());
            result.addAll(inventory.getKeys());
            result.addAll(inventory.getDocuments());
            for (ScpEquipmentSlot slot : ScpEquipmentSlot.values()) {
                ItemStack stack = inventory.getEquipment(slot);
                if (!stack.isEmpty()) result.add(stack);
            }
            ItemStack active = inventory.getActiveUsable();
            if (!active.isEmpty()) result.add(active);
        });
        return result;
    }

    public static int countCurrency(Player player, Item currency) {
        if (player == null || currency == null) return 0;
        final int[] count = {0};
        get(player).ifPresent(inventory -> {
            for (ItemStack stack : inventory.getInventory()) {
                if (acceptsCurrency(stack, currency)) count[0] += stack.getCount();
            }
        });
        return count[0];
    }

    public static boolean acceptsCurrency(ItemStack stack, Item currency) {
        if (stack == null || stack.isEmpty() || currency == null) return false;
        if (stack.is(currency)) return true;

        ResourceLocation requestedId = BuiltInRegistries.ITEM.getKey(currency);
        return CANONICAL_SCP_294_COIN.equals(requestedId)
                && ScpItemClassifier.isCoin(stack);
    }

    public static ItemStack extractCurrency(Player player, Item currency) {
        if (player == null || currency == null) return ItemStack.EMPTY;
        final ItemStack[] result = {ItemStack.EMPTY};
        get(player).ifPresent(inventory -> {
            for (int slot = 0; slot < inventory.getMaxMainSlots(); slot++) {
                ItemStack stack = inventory.getInventoryItem(slot);
                if (acceptsCurrency(stack, currency)) {
                    ItemStack extracted = stack.copy();
                    extracted.setCount(1);
                    if (stack.getCount() <= 1) {
                        inventory.removeInventoryItem(slot);
                    } else {
                        ItemStack remainder = stack.copy();
                        remainder.shrink(1);
                        inventory.setInventoryItem(slot, remainder);
                    }
                    result[0] = extracted;
                    if (player instanceof ServerPlayer serverPlayer) {
                        ModNetwork.syncTo(serverPlayer, inventory);
                    }
                    return;
                }
            }
        });
        return result[0];
    }

    public static int insertCurrency(Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()
                || !ScpItemClassifier.isCoin(stack)) return 0;

        final int[] inserted = {0};
        get(player).ifPresent(inventory -> {
            inserted[0] = inventory.addInventoryItems(stack.copy());
            if (inserted[0] > 0 && player instanceof ServerPlayer serverPlayer) {
                ModNetwork.syncTo(serverPlayer, inventory);
            }
        });
        return inserted[0];
    }
}
