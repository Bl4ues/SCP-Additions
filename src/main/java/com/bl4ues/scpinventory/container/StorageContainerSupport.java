package com.bl4ues.scpinventory.container;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.items.SlotItemHandler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Detects menus that are safe to present as plain two-sided storage.
 *
 * <p>The SCP storage screen deliberately avoids recipe, processing, equipment,
 * payment, result and other semantic menus. Vanilla storage menus are accepted
 * explicitly. Unknown modded menus are accepted only when they follow the
 * conventional "storage slots first, player inventory last" shape and all
 * storage slots behave like ordinary {@link Slot} or {@link SlotItemHandler}
 * instances.</p>
 */
public final class StorageContainerSupport {
    private static final Set<String> SEMANTIC_SLOT_METHODS = Set.of(
            "mayPlace",
            "mayPickup",
            "onTake",
            "getMaxStackSize",
            "isActive"
    );

    private StorageContainerSupport() {
    }

    public static boolean isSupported(AbstractContainerMenu menu,
                                      Inventory playerInventory) {
        return !storageSlotIds(menu, playerInventory).isEmpty();
    }

    public static List<Integer> storageSlotIds(AbstractContainerMenu menu,
                                                Inventory playerInventory) {
        if (menu == null || playerInventory == null || menu.slots.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> storageSlots = new ArrayList<>();
        int playerSlots = 0;
        boolean seenPlayerSlot = false;
        boolean storageAfterPlayer = false;

        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot.container == playerInventory) {
                playerSlots++;
                seenPlayerSlot = true;
            } else {
                if (seenPlayerSlot) {
                    storageAfterPlayer = true;
                }
                storageSlots.add(i);
            }
        }

        if (storageSlots.isEmpty()) {
            return Collections.emptyList();
        }

        if (isKnownVanillaStorage(menu)) {
            return List.copyOf(storageSlots);
        }

        // Every other vanilla menu has semantics beyond plain storage.
        if (menu.getClass().getName().startsWith("net.minecraft.")) {
            return Collections.emptyList();
        }

        // Modded storage menus normally append the player's inventory after the
        // external slots. Being conservative here is intentional: a menu with a
        // crafting grid or side input/output slots should keep its native screen.
        if (playerSlots < 27 || storageAfterPlayer) {
            return Collections.emptyList();
        }

        for (int slotId : storageSlots) {
            if (!isPlainStorageSlot(menu.slots.get(slotId))) {
                return Collections.emptyList();
            }
        }

        return List.copyOf(storageSlots);
    }

    public static boolean containsStorageSlot(AbstractContainerMenu menu,
                                              Inventory playerInventory,
                                              int menuSlotId) {
        if (menuSlotId < 0 || menuSlotId >= menu.slots.size()) {
            return false;
        }
        return storageSlotIds(menu, playerInventory).contains(menuSlotId);
    }

    private static boolean isKnownVanillaStorage(AbstractContainerMenu menu) {
        return menu instanceof ChestMenu
                || menu instanceof ShulkerBoxMenu
                || menu instanceof HopperMenu
                || menu instanceof DispenserMenu;
    }

    private static boolean isPlainStorageSlot(Slot slot) {
        if (slot == null) {
            return false;
        }

        Class<?> type = slot.getClass();
        if (type == Slot.class || type == SlotItemHandler.class) {
            return true;
        }

        if (SlotItemHandler.class.isAssignableFrom(type)) {
            return !declaresSemanticBehavior(type, SlotItemHandler.class);
        }

        if (Slot.class.isAssignableFrom(type)) {
            return !declaresSemanticBehavior(type, Slot.class);
        }

        return false;
    }

    private static boolean declaresSemanticBehavior(Class<?> type,
                                                    Class<?> safeBase) {
        Class<?> current = type;
        while (current != null && current != safeBase) {
            for (Method method : current.getDeclaredMethods()) {
                if (SEMANTIC_SLOT_METHODS.contains(method.getName())) {
                    return true;
                }
            }
            current = current.getSuperclass();
        }
        return false;
    }
}
