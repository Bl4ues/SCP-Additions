package com.bl4ues.scpinventory.events;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Synchronizes the authoritative module state and safely releases custom
 * inventory contents when the module is disabled at runtime.
 */
@Mod.EventBusSubscriber(modid = "scp_additions")
public final class InventoryModuleStateEvents {
    private static final Set<UUID> RELEASED_WHILE_DISABLED = new HashSet<>();

    private InventoryModuleStateEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ModNetwork.syncModuleState(player);
        updateDisabledState(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        RELEASED_WHILE_DISABLED.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide()
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        updateDisabledState(player);
    }

    private static void updateDisabledState(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (ScpAdditionsModulesConfig.get().inventory.enabled) {
            RELEASED_WHILE_DISABLED.remove(playerId);
            return;
        }
        if (!RELEASED_WHILE_DISABLED.add(playerId)) return;
        releaseStoredItems(player);
    }

    private static void releaseStoredItems(ServerPlayer player) {
        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
            Inventory vanilla = player.getInventory();

            // Tagged harmful/usable stacks are mirrors of capability-owned data.
            // Remove those duplicates before restoring the authoritative stacks.
            purgeTransientMirrors(vanilla.items);
            purgeTransientMirrors(vanilla.offhand);
            purgeTransientMirrors(vanilla.armor);

            for (ScpEquipmentSlot slot : ScpEquipmentSlot.values()) {
                ItemStack equipped = inventory.extractEquipment(slot);
                if (!equipped.isEmpty() && !hasEquivalentVanillaMirror(player, slot, equipped)) {
                    restoreOrDrop(player, equipped);
                }
            }

            for (int slot = 0; slot < inventory.getMaxMainSlots(); slot++) {
                restoreOrDrop(player, inventory.extractInventoryItem(slot));
            }
            while (!inventory.getKeys().isEmpty()) {
                restoreOrDrop(player, inventory.extractKeyItem(0));
            }
            while (!inventory.getDocuments().isEmpty()) {
                restoreOrDrop(player, inventory.extractDocumentItem(0));
            }
            restoreOrDrop(player, inventory.extractActiveUsable());

            inventory.resetAll();
            vanilla.setChanged();
            player.inventoryMenu.broadcastChanges();
            if (player.containerMenu != player.inventoryMenu) {
                player.containerMenu.broadcastChanges();
            }
            ModNetwork.syncModuleState(player);
        });
    }

    private static void purgeTransientMirrors(List<ItemStack> stacks) {
        for (int index = 0; index < stacks.size(); index++) {
            ItemStack stack = stacks.get(index);
            if (stack.isEmpty()) continue;
            if (ScpPickupRouter.isCoinMirror(stack)
                    || ScpPickupRouter.isHarmfulMirror(stack)
                    || ScpPickupRouter.isUsableSession(stack)) {
                stacks.set(index, ItemStack.EMPTY);
                continue;
            }
            ScpPickupRouter.stripNoMergeMarker(stack);
        }
    }

    private static boolean hasEquivalentVanillaMirror(ServerPlayer player,
                                                        ScpEquipmentSlot slot,
                                                        ItemStack customStack) {
        ItemStack expected = normalized(customStack);
        if (expected.isEmpty()) return false;

        EquipmentSlot vanillaSlot = switch (slot) {
            case HEAD -> EquipmentSlot.HEAD;
            case CHEST -> EquipmentSlot.CHEST;
            case LEGS -> EquipmentSlot.LEGS;
            case FEET -> EquipmentSlot.FEET;
            default -> null;
        };
        if (vanillaSlot != null) {
            return sameSingle(player.getItemBySlot(vanillaSlot), expected);
        }
        if (slot == ScpEquipmentSlot.ACCESSORY) {
            return sameSingle(player.getOffhandItem(), expected);
        }
        if (slot == ScpEquipmentSlot.WEAPON) {
            for (ItemStack stack : player.getInventory().items) {
                if (sameSingle(stack, expected)) return true;
            }
        }
        return false;
    }

    private static void restoreOrDrop(ServerPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        ItemStack restoring = stack.copy();
        ScpPickupRouter.stripNoMergeMarker(restoring);
        ScpPickupRouter.stripCoinMirror(restoring);
        ScpPickupRouter.stripHarmfulMirror(restoring);
        ScpPickupRouter.stripUsableSession(restoring);
        player.getInventory().add(restoring);
        if (!restoring.isEmpty()) player.drop(restoring, false);
    }

    private static ItemStack normalized(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        ScpPickupRouter.stripNoMergeMarker(copy);
        ScpPickupRouter.stripCoinMirror(copy);
        ScpPickupRouter.stripHarmfulMirror(copy);
        ScpPickupRouter.stripUsableSession(copy);
        return copy;
    }

    private static boolean sameSingle(ItemStack first, ItemStack second) {
        if (first == null || first.isEmpty() || second == null || second.isEmpty()) return false;
        return ItemStack.isSameItemSameTags(normalized(first), normalized(second));
    }
}
