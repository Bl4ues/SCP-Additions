package com.bl4ues.scpinventory.event;

import net.neoforged.fml.common.EventBusSubscriber;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.item.ScpItemType;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import com.bl4ues.scpadditions.compat.TickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = "scp_additions")
public final class ScpInventoryUsableOutputGuardEvents {
    private static final int VANILLA_HOTBAR_START = 0;
    private static final int VANILLA_HOTBAR_END_EXCLUSIVE = 9;

    private static Map<UUID, Integer> activeSlots;
    private static Map<UUID, Integer> activeSourceSlots;
    private static Map<UUID, Integer> activeStartTicks;
    private static Map<UUID, ItemStack> activeStacks;
    private static boolean reflectionFailed;

    private ScpInventoryUsableOutputGuardEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!ScpAdditionsModulesConfig.get().inventory.enabled) return;
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)
                || player.isCreative()
                || player.isSpectator()) {
            return;
        }

        if (!loadSessionMaps()) {
            return;
        }

        UUID id = player.getUUID();
        Integer activeSlot = activeSlots.get(id);
        if (activeSlot == null || activeSlot < VANILLA_HOTBAR_START || activeSlot >= VANILLA_HOTBAR_END_EXCLUSIVE) {
            return;
        }

        Inventory vanillaInventory = player.getInventory();
        if (activeSlot >= vanillaInventory.items.size()) {
            clearSession(id);
            return;
        }

        ItemStack expected = activeStacks.getOrDefault(id, ItemStack.EMPTY);
        ItemStack current = vanillaInventory.items.get(activeSlot);
        if (expected.isEmpty()) {
            clearSession(id);
            return;
        }
        if (current.isEmpty()) {
            // The main maintenance pass owns consumed/vanished usable cleanup.
            // Clearing only these reflected maps here left the capability copy
            // alive, causing consumed items such as spawn eggs to be mirrored
            // back into the player's hand on the next pass.
            return;
        }

        if (ItemStack.isSameItem(current, expected)) {
            ItemStack normalizedCurrent = normalizeSingle(current);
            if (!ItemStack.isSameItemSameTags(normalizedCurrent, expected)) {
                activeStacks.put(id, normalizedCurrent);
            }
            return;
        }

        if (ScpItemClassifier.getType(current) == ScpItemType.MISCELLANEOUS) {
            return;
        }

        ScpInventoryCapability.get(player).ifPresent(inventory -> {
            ItemStack routing = current.copy();
            ScpPickupRouter.stripUsableSession(routing);
            ScpPickupRouter.stripNoMergeMarker(routing);
            int accepted = ScpPickupRouter.accept(inventory, player, routing);
            if (accepted <= 0) {
                return;
            }

            current.shrink(accepted);
            if (current.isEmpty()) {
                vanillaInventory.items.set(activeSlot, ItemStack.EMPTY);
            }
            vanillaInventory.setChanged();
            ScpPickupRouter.syncVanillaInventory(player);
            clearSession(id);
            ModNetwork.syncTo(player, inventory);
        });
    }

    @SuppressWarnings("unchecked")
    private static boolean loadSessionMaps() {
        if (reflectionFailed) {
            return false;
        }
        if (activeSlots != null && activeStacks != null && activeSourceSlots != null && activeStartTicks != null) {
            return true;
        }

        try {
            activeSlots = (Map<UUID, Integer>) getPrivateStaticField("ACTIVE_USABLE_SLOTS");
            activeSourceSlots = (Map<UUID, Integer>) getPrivateStaticField("ACTIVE_USABLE_SOURCE_SLOTS");
            activeStartTicks = (Map<UUID, Integer>) getPrivateStaticField("ACTIVE_USABLE_START_TICKS");
            activeStacks = (Map<UUID, ItemStack>) getPrivateStaticField("ACTIVE_USABLE_STACKS");
            return true;
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            reflectionFailed = true;
            return false;
        }
    }

    private static Object getPrivateStaticField(String fieldName) throws ReflectiveOperationException {
        Field field = ScpInventoryMaintenanceEvents.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(null);
    }

    private static void clearSession(UUID id) {
        activeSlots.remove(id);
        activeSourceSlots.remove(id);
        activeStartTicks.remove(id);
        activeStacks.remove(id);
    }

    private static ItemStack normalizeSingle(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        ScpPickupRouter.stripUsableSession(copy);
        ScpPickupRouter.stripNoMergeMarker(copy);
        return copy;
    }
}
