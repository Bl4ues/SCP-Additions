package com.bl4ues.scpinventory.events;

import net.neoforged.fml.common.EventBusSubscriber;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.item.ScpItemType;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
import com.bl4ues.scpinventory.network.InventoryActionPacket;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import com.bl4ues.scpadditions.compat.TickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = "scp_additions")
public class VanillaMirrorSyncHandler {

    private static final int VANILLA_HOTBAR_START = 0;
    private static final int VANILLA_HOTBAR_END_EXCLUSIVE = 9;
    private static final int VANILLA_MAIN_START = 9;
    private static final int VANILLA_MAIN_END_EXCLUSIVE = 36;
    private static final int SYNC_INTERVAL_TICKS = 1;
    private static final long FULL_MESSAGE_COOLDOWN_MS = 550L;
    private static final Map<UUID, Long> LAST_FULL_MESSAGE = new HashMap<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (!ScpAdditionsModulesConfig.get().inventory.enabled) {
            return;
        }

        if (player.tickCount % SYNC_INTERVAL_TICKS != 0) {
            return;
        }

        ScpInventoryCapability.get(player).ifPresent(inventory -> {
            boolean changed = false;
            changed |= syncEquipmentSlot(player, inventory, ScpEquipmentSlot.HEAD, EquipmentSlot.HEAD);
            changed |= syncEquipmentSlot(player, inventory, ScpEquipmentSlot.CHEST, EquipmentSlot.CHEST);
            changed |= syncEquipmentSlot(player, inventory, ScpEquipmentSlot.LEGS, EquipmentSlot.LEGS);
            changed |= syncEquipmentSlot(player, inventory, ScpEquipmentSlot.FEET, EquipmentSlot.FEET);
            changed |= routeVanillaInventoryToCustom(player, inventory);
            changed |= syncEquippedMainMirror(player, inventory, ScpEquipmentSlot.WEAPON);
            changed |= syncEquippedMainMirror(player, inventory, ScpEquipmentSlot.ACCESSORY);

            if (changed) {
                player.getInventory().setChanged();
                player.containerMenu.broadcastChanges();
                ModNetwork.syncTo(player, inventory);
            }
        });
    }

    private static boolean syncEquipmentSlot(ServerPlayer player, IScpInventory inventory, ScpEquipmentSlot customSlot, EquipmentSlot vanillaSlot) {
        ItemStack vanillaStack = player.getItemBySlot(vanillaSlot);
        ItemStack customStack = inventory.getEquipment(customSlot);

        if (vanillaStack.isEmpty()) {
            if (!customStack.isEmpty()) {
                inventory.clearEquipment(customSlot);
                return true;
            }
            return false;
        }

        if (ScpItemClassifier.getEquipmentSlot(vanillaStack).orElse(null) != customSlot) {
            if (!customStack.isEmpty()) {
                inventory.clearEquipment(customSlot);
                return true;
            }
            return false;
        }

        ItemStack normalized = vanillaStack.copy();
        normalized.setCount(1);

        if (!ItemStack.isSameItemSameComponents(customStack, normalized) || customStack.getCount() != 1) {
            inventory.setEquipment(customSlot, normalized);
            InventoryActionPacket.syncVanillaEquipmentSlot(player, customSlot, normalized);
            return true;
        }

        return false;
    }

    private static boolean routeVanillaInventoryToCustom(ServerPlayer player, IScpInventory inventory) {
        // Creative keeps normal Minecraft inventory behavior. Spectators are also
        // excluded because they should never have survival routing applied.
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }

        boolean changed = false;
        Inventory vanillaInventory = player.getInventory();

        for (int i = VANILLA_HOTBAR_START; i < VANILLA_MAIN_END_EXCLUSIVE && i < vanillaInventory.items.size(); i++) {
            ItemStack stack = vanillaInventory.items.get(i);
            if (stack.isEmpty() || isManagedVanillaMirror(inventory, stack)) {
                continue;
            }

            ScpItemType type = ScpItemClassifier.getType(stack);
            ScpEquipmentSlot preservedSlot = getPreservedMirrorSlot(type);
            if (preservedSlot != null && isPreservedEquipmentMirror(inventory, preservedSlot, stack)) {
                changed |= syncEquipmentFromMirror(inventory, preservedSlot, stack);
                changed |= routeMirrorOverflow(player, inventory, vanillaInventory, i, stack);
                continue;
            }

            int originalCount = stack.getCount();
            ItemStack routingStack = stack.copy();
            int accepted = Math.max(0, Math.min(originalCount,
                    ScpPickupRouter.accept(inventory, player, routingStack)));
            int remaining = originalCount - accepted;

            // Once a survival stack is eligible for SCP Inventory routing it must
            // not remain as an accidental vanilla fallback. Accepted items move to
            // the capability; any remainder is dropped into the world.
            vanillaInventory.items.set(i, ItemStack.EMPTY);
            changed = true;

            if (remaining > 0) {
                ItemStack overflow = stack.copy();
                overflow.setCount(remaining);
                dropOverflowStack(player, overflow);
            }
        }

        return changed;
    }

    private static boolean isManagedVanillaMirror(IScpInventory inventory, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (ScpPickupRouter.isUsableSession(stack)
                || ScpPickupRouter.isCoinMirror(stack)
                || ScpPickupRouter.isHarmfulMirror(stack)) {
            return true;
        }

        ItemStack activeUsable = inventory == null ? ItemStack.EMPTY : inventory.getActiveUsable();
        return stack.getCount() == 1
                && !activeUsable.isEmpty()
                && ItemStack.isSameItemSameComponents(
                        normalizeRouterComparable(stack),
                        normalizeRouterComparable(activeUsable));
    }

    private static ItemStack normalizeRouterComparable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        ScpPickupRouter.stripNoMergeMarker(copy);
        ScpPickupRouter.stripUsableSession(copy);
        ScpPickupRouter.stripCoinMirror(copy);
        ScpPickupRouter.stripHarmfulMirror(copy);
        return copy;
    }

    private static void dropOverflowStack(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return;
        }

        ItemStack dropped = stack.copy();
        ScpPickupRouter.stripNoMergeMarker(dropped);
        ScpPickupRouter.stripUsableSession(dropped);
        ScpPickupRouter.stripCoinMirror(dropped);
        ScpPickupRouter.stripHarmfulMirror(dropped);
        if (dropped.isEmpty()) {
            return;
        }

        player.drop(dropped, false);
        showInventoryFullThrottled(player);
    }

    private static ScpEquipmentSlot getPreservedMirrorSlot(ScpItemType type) {
        return switch (type) {
            case WEAPON -> ScpEquipmentSlot.WEAPON;
            case ACCESSORY -> ScpEquipmentSlot.ACCESSORY;
            default -> null;
        };
    }

    private static boolean isPreservedEquipmentMirror(IScpInventory inventory, ScpEquipmentSlot slot, ItemStack vanillaStack) {
        ItemStack equipped = inventory.getEquipment(slot);
        ItemStack normalized = vanillaStack.copy();
        normalized.setCount(1);
        return !equipped.isEmpty() && ItemStack.isSameItemSameComponents(equipped, normalized);
    }

    private static boolean syncEquipmentFromMirror(IScpInventory inventory, ScpEquipmentSlot slot, ItemStack vanillaStack) {
        ItemStack normalized = vanillaStack.copy();
        normalized.setCount(1);
        ItemStack equipped = inventory.getEquipment(slot);

        if (equipped.getCount() != 1 || !ItemStack.isSameItemSameComponents(equipped, normalized)) {
            inventory.setEquipment(slot, normalized);
            return true;
        }

        return false;
    }

    private static boolean routeMirrorOverflow(ServerPlayer player, IScpInventory inventory,
            Inventory vanillaInventory, int slot, ItemStack stack) {
        if (stack.getCount() <= 1) {
            return false;
        }

        int overflowCount = stack.getCount() - 1;
        ItemStack overflow = stack.copy();
        overflow.setCount(overflowCount);
        int accepted = Math.max(0, Math.min(overflowCount,
                ScpPickupRouter.accept(inventory, player, overflow.copy())));
        int remaining = overflowCount - accepted;

        stack.setCount(1);
        vanillaInventory.items.set(slot, stack);

        if (remaining > 0) {
            ItemStack dropped = overflow.copy();
            dropped.setCount(remaining);
            dropOverflowStack(player, dropped);
        }

        return true;
    }

    private static boolean syncEquippedMainMirror(ServerPlayer player, IScpInventory inventory, ScpEquipmentSlot slot) {
        ItemStack equipped = inventory.getEquipment(slot);
        if (equipped.isEmpty()) {
            return false;
        }

        Inventory vanillaInventory = player.getInventory();
        if (findSameItem(vanillaInventory, equipped) != -1) {
            return false;
        }

        inventory.clearEquipment(slot);
        return true;
    }

    private static int findSameItem(Inventory inventory, ItemStack stack) {
        ItemStack normalizedExpected = stack.copy();
        normalizedExpected.setCount(1);

        for (int i = VANILLA_HOTBAR_START; i < VANILLA_MAIN_END_EXCLUSIVE && i < inventory.items.size(); i++) {
            ItemStack candidate = inventory.items.get(i);
            if (candidate.isEmpty()) {
                continue;
            }

            ItemStack normalizedCandidate = candidate.copy();
            normalizedCandidate.setCount(1);
            if (ItemStack.isSameItemSameComponents(normalizedCandidate, normalizedExpected)) {
                return i;
            }
        }
        return -1;
    }

    private static void showInventoryFullThrottled(ServerPlayer player) {
        long now = System.currentTimeMillis();
        UUID playerId = player.getUUID();
        long lastShown = LAST_FULL_MESSAGE.getOrDefault(playerId, 0L);

        if (now - lastShown >= FULL_MESSAGE_COOLDOWN_MS) {
            LAST_FULL_MESSAGE.put(playerId, now);
            ModNetwork.showInventoryFull(player);
        }
    }
}
