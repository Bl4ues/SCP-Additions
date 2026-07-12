package net.mcreator.scpadditions.inventory;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

import java.util.ArrayList;
import java.util.List;

public final class ScpInventoryAccess {
    private ScpInventoryAccess() {
    }

    public static LazyOptional<IScpInventory> get(Player player) {
        return player == null
                ? LazyOptional.empty()
                : player.getCapability(ScpInventoryCapability.INSTANCE);
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
                if (!stack.isEmpty() && stack.is(currency)) count[0] += stack.getCount();
            }
        });
        return count[0];
    }

    public static ItemStack extractCurrency(Player player, Item currency) {
        if (player == null || currency == null) return ItemStack.EMPTY;
        final ItemStack[] result = {ItemStack.EMPTY};
        get(player).ifPresent(inventory -> {
            for (int slot = 0; slot < inventory.getMaxMainSlots(); slot++) {
                ItemStack stack = inventory.getInventoryItem(slot);
                if (!stack.isEmpty() && stack.is(currency)) {
                    result[0] = inventory.extractInventoryItem(slot);
                    return;
                }
            }
        });
        return result[0];
    }
}
