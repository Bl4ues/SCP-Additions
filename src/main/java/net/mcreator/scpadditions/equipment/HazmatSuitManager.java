package net.mcreator.scpadditions.equipment;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;
import net.mcreator.scpadditions.network.ScpEntityNetwork;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Server-authoritative lifecycle for the single-item Hazmat Suit. */
public final class HazmatSuitManager {
    public static final int EQUIP_DURATION_TICKS = 80;
    public static final int UNEQUIP_DURATION_TICKS = 60;

    private static final String RETURN_ITEM_TAG =
            "ScpAdditionsHazmatReturnPublicItem";
    private static final int PROGRESS_SYNC_INTERVAL_TICKS = 10;
    private static final double MOVEMENT_CANCEL_DISTANCE_SQR = 0.0004D;

    private static final Set<UUID> EQUIPPING = new HashSet<>();
    private static final Set<UUID> KNOWN_EQUIPPED = new HashSet<>();
    private static final Set<UUID> INTERNAL_MUTATION = new HashSet<>();
    private static final Set<UUID> MANUAL_UNEQUIP = new HashSet<>();
    private static final Map<UUID, Integer> UNEQUIP_PROGRESS =
            new HashMap<>();
    private static final Map<UUID, Vec3> ACTION_START_POSITIONS =
            new HashMap<>();

    private HazmatSuitManager() {
    }

    public static boolean canBeginEquip(Player player) {
        if (player == null || player.isSpectator()
                || UNEQUIP_PROGRESS.containsKey(player.getUUID())
                || HazmatSuitAccess.isFullyEquipped(player)) {
            return false;
        }

        return player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                && player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                && player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                && player.getItemBySlot(EquipmentSlot.FEET).isEmpty();
    }

    public static void explainBlockedEquip(ServerPlayer player) {
        showActionBar(player,
                "message.scp_additions.hazmat.remove_armor_first");
    }

    public static void beginEquip(ServerPlayer player) {
        if (player == null || !canBeginEquip(player)) {
            return;
        }
        UUID id = player.getUUID();
        EQUIPPING.add(id);
        ACTION_START_POSITIONS.put(id, player.position());
        showActionBar(player,
                "message.scp_additions.hazmat.equip_hold_still");
        ScpEntityNetwork.beginEquipmentProgress(
                player, EQUIP_DURATION_TICKS);
    }

    public static void cancelEquip(ServerPlayer player) {
        cancelEquip(player, null);
    }

    private static void cancelEquip(ServerPlayer player, String messageKey) {
        if (player == null) {
            return;
        }
        UUID id = player.getUUID();
        ACTION_START_POSITIONS.remove(id);
        if (EQUIPPING.remove(id)) {
            player.stopUsingItem();
            ScpEntityNetwork.cancelEquipmentProgress(player);
            showActionBar(player, messageKey);
        }
    }

    public static boolean completeEquip(ServerPlayer player) {
        if (player == null || !EQUIPPING.remove(player.getUUID())) {
            return false;
        }
        UUID id = player.getUUID();
        ACTION_START_POSITIONS.remove(id);
        if (!canBeginEquip(player)) {
            ScpEntityNetwork.cancelEquipmentProgress(player);
            explainBlockedEquip(player);
            return false;
        }

        INTERNAL_MUTATION.add(id);
        try {
            player.setItemSlot(EquipmentSlot.HEAD,
                    new ItemStack(ScpAdditionsModItems.HAZMAT_SUIT_HELMET.get()));
            player.setItemSlot(EquipmentSlot.CHEST,
                    new ItemStack(ScpAdditionsModItems.HAZMAT_SUIT_CHESTPLATE.get()));
            player.setItemSlot(EquipmentSlot.LEGS,
                    new ItemStack(ScpAdditionsModItems.HAZMAT_SUIT_LEGGINGS.get()));
            player.setItemSlot(EquipmentSlot.FEET,
                    new ItemStack(ScpAdditionsModItems.HAZMAT_SUIT_BOOTS.get()));
            net.mcreator.scpadditions.fabric.FabricPersistentData.get(player).putBoolean(
                    RETURN_ITEM_TAG, !player.isCreative());
            KNOWN_EQUIPPED.add(id);
            ScpEntityNetwork.completeEquipmentProgress(player);
            ScpPickupRouter.syncVanillaInventory(player);
            showActionBar(player,
                    "message.scp_additions.hazmat.remove_instruction");
            return true;
        } finally {
            INTERNAL_MUTATION.remove(id);
        }
    }

    /** Starts the removal flow after an armor-slot interaction. */
    public static boolean requestUnequip(ServerPlayer player) {
        return requestUnequip(player, false);
    }

    private static boolean requestUnequip(ServerPlayer player,
            boolean manualHold) {
        if (player == null || player.isSpectator()) {
            return false;
        }

        UUID id = player.getUUID();
        if (UNEQUIP_PROGRESS.containsKey(id)) {
            return true;
        }
        if (!HazmatSuitAccess.isFullyEquipped(player)
                && !KNOWN_EQUIPPED.contains(id)) {
            return false;
        }

        recoverCompleteSet(player);
        KNOWN_EQUIPPED.add(id);
        UNEQUIP_PROGRESS.put(id, 0);
        ACTION_START_POSITIONS.put(id, player.position());
        if (manualHold) {
            MANUAL_UNEQUIP.add(id);
            showActionBar(player,
                    "message.scp_additions.hazmat.unequip_hold_use");
        } else {
            MANUAL_UNEQUIP.remove(id);
            showActionBar(player,
                    "message.scp_additions.hazmat.unequip_hold_still");
        }
        ScpEntityNetwork.beginEquipmentProgress(
                player, UNEQUIP_DURATION_TICKS);
        return true;
    }

    /**
     * Receives the client hold state. Crouching is required only to begin;
     * after that, the player may stand but must keep holding Use.
     */
    public static void setManualRemovalInput(ServerPlayer player,
            boolean held) {
        if (player == null || player.isSpectator()) {
            return;
        }

        UUID id = player.getUUID();
        if (held) {
            if (!UNEQUIP_PROGRESS.containsKey(id)
                    && player.isCrouching()
                    && HazmatSuitAccess.isFullyEquipped(player)) {
                requestUnequip(player, true);
            }
            return;
        }

        if (MANUAL_UNEQUIP.contains(id)) {
            cancelUnequip(player,
                    "message.scp_additions.hazmat.unequip_canceled_release");
        }
    }

    public static boolean isUnequipping(Player player) {
        return player != null
                && UNEQUIP_PROGRESS.containsKey(player.getUUID());
    }

    public static boolean isKnownEquipped(Player player) {
        return player != null
                && KNOWN_EQUIPPED.contains(player.getUUID());
    }

    public static void serverTick(ServerPlayer player) {
        if (player == null || player.isSpectator()) {
            return;
        }

        UUID id = player.getUUID();
        if (INTERNAL_MUTATION.contains(id)) {
            return;
        }

        if (EQUIPPING.contains(id) && movedSinceActionStart(player)) {
            cancelEquip(player,
                    "message.scp_additions.hazmat.equip_canceled_moved");
            return;
        }

        if (HazmatSuitAccess.isFullyEquipped(player)) {
            KNOWN_EQUIPPED.add(id);
        } else if (KNOWN_EQUIPPED.contains(id)) {
            // A vanilla inventory click, shift-click, armor replacement, drop,
            // or another ordinary interaction removed at least one proxy piece.
            recoverCompleteSet(player);
            requestUnequip(player);
        }

        Integer elapsed = UNEQUIP_PROGRESS.get(id);
        if (elapsed == null) {
            return;
        }

        if (movedSinceActionStart(player)) {
            cancelUnequip(player,
                    "message.scp_additions.hazmat.unequip_canceled_moved");
            return;
        }

        if (!HazmatSuitAccess.isFullyEquipped(player)) {
            recoverCompleteSet(player);
        }

        int nextElapsed = elapsed + 1;
        if (nextElapsed >= UNEQUIP_DURATION_TICKS) {
            completeUnequip(player);
            return;
        }

        UNEQUIP_PROGRESS.put(id, nextElapsed);
        if (nextElapsed % PROGRESS_SYNC_INTERVAL_TICKS == 0) {
            ScpEntityNetwork.syncEquipmentProgress(player,
                    nextElapsed, UNEQUIP_DURATION_TICKS);
        }
    }

    public static void clearTransientState(Player player) {
        if (player == null) {
            return;
        }
        UUID id = player.getUUID();
        EQUIPPING.remove(id);
        UNEQUIP_PROGRESS.remove(id);
        KNOWN_EQUIPPED.remove(id);
        INTERNAL_MUTATION.remove(id);
        MANUAL_UNEQUIP.remove(id);
        ACTION_START_POSITIONS.remove(id);
    }

    public static boolean shouldReplaceInternalDeathDrops(Player player) {
        return player != null && (HazmatSuitAccess.isFullyEquipped(player)
                || KNOWN_EQUIPPED.contains(player.getUUID()));
    }

    public static boolean shouldReturnPublicItem(Player player) {
        if (player == null) {
            return false;
        }
        if (net.mcreator.scpadditions.fabric.FabricPersistentData.get(player).contains(RETURN_ITEM_TAG)) {
            return net.mcreator.scpadditions.fabric.FabricPersistentData.get(player).getBoolean(RETURN_ITEM_TAG);
        }
        return !player.isCreative();
    }

    private static void cancelUnequip(ServerPlayer player,
            String messageKey) {
        if (player == null) {
            return;
        }
        UUID id = player.getUUID();
        boolean removed = UNEQUIP_PROGRESS.remove(id) != null;
        MANUAL_UNEQUIP.remove(id);
        ACTION_START_POSITIONS.remove(id);
        if (removed) {
            ScpEntityNetwork.cancelEquipmentProgress(player);
            showActionBar(player, messageKey);
        }
    }

    private static boolean movedSinceActionStart(ServerPlayer player) {
        Vec3 start = ACTION_START_POSITIONS.get(player.getUUID());
        return start != null
                && player.position().distanceToSqr(start)
                > MOVEMENT_CANCEL_DISTANCE_SQR;
    }

    private static void completeUnequip(ServerPlayer player) {
        UUID id = player.getUUID();
        INTERNAL_MUTATION.add(id);
        try {
            boolean returnPublicItem = shouldReturnPublicItem(player);
            sanitizeInternalCopiesOutsideArmor(player);
            clearInternalArmorSlot(player, EquipmentSlot.HEAD);
            clearInternalArmorSlot(player, EquipmentSlot.CHEST);
            clearInternalArmorSlot(player, EquipmentSlot.LEGS);
            clearInternalArmorSlot(player, EquipmentSlot.FEET);
            clearCustomEquipmentMirrors(player);

            EQUIPPING.remove(id);
            UNEQUIP_PROGRESS.remove(id);
            KNOWN_EQUIPPED.remove(id);
            MANUAL_UNEQUIP.remove(id);
            ACTION_START_POSITIONS.remove(id);
            net.mcreator.scpadditions.fabric.FabricPersistentData.get(player).remove(RETURN_ITEM_TAG);

            if (returnPublicItem) {
                routeToAuthoritativeInventoryOrDrop(player,
                        new ItemStack(ScpAdditionsModItems.HAZMAT_SUIT.get()));
            }

            ScpEntityNetwork.completeEquipmentProgress(player);
            ScpPickupRouter.syncVanillaInventory(player);
            showActionBar(player,
                    "message.scp_additions.hazmat.removed");
        } finally {
            INTERNAL_MUTATION.remove(id);
        }
    }

    private static void recoverCompleteSet(ServerPlayer player) {
        UUID id = player.getUUID();
        INTERNAL_MUTATION.add(id);
        try {
            sanitizeInternalCopiesOutsideArmor(player);
            restoreSlot(player, EquipmentSlot.HEAD,
                    ScpAdditionsModItems.HAZMAT_SUIT_HELMET.get());
            restoreSlot(player, EquipmentSlot.CHEST,
                    ScpAdditionsModItems.HAZMAT_SUIT_CHESTPLATE.get());
            restoreSlot(player, EquipmentSlot.LEGS,
                    ScpAdditionsModItems.HAZMAT_SUIT_LEGGINGS.get());
            restoreSlot(player, EquipmentSlot.FEET,
                    ScpAdditionsModItems.HAZMAT_SUIT_BOOTS.get());
            ScpPickupRouter.syncVanillaInventory(player);
        } finally {
            INTERNAL_MUTATION.remove(id);
        }
    }

    private static void restoreSlot(ServerPlayer player, EquipmentSlot slot,
            Item expectedItem) {
        ItemStack current = player.getItemBySlot(slot);
        if (!current.isEmpty() && current.is(expectedItem)) {
            return;
        }

        if (!current.isEmpty() && !HazmatSuitAccess.isInternalPiece(current)) {
            routeToAuthoritativeInventoryOrDrop(player, current.copy());
        }
        player.setItemSlot(slot, new ItemStack(expectedItem));
    }

    private static void clearInternalArmorSlot(ServerPlayer player,
            EquipmentSlot slot) {
        ItemStack current = player.getItemBySlot(slot);
        if (HazmatSuitAccess.isInternalPiece(current)) {
            player.setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    private static void sanitizeInternalCopiesOutsideArmor(
            ServerPlayer player) {
        Inventory vanilla = player.getInventory();
        sanitizeList(vanilla.items);
        sanitizeList(vanilla.offhand);
        if (player.containerMenu != null
                && HazmatSuitAccess.isInternalPiece(
                        player.containerMenu.getCarried())) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }

        ScpInventoryCapability.get(player)
                .ifPresent(inventory -> {
                    for (int i = 0; i < inventory.getMaxMainSlots(); i++) {
                        if (HazmatSuitAccess.isInternalPiece(
                                inventory.getInventoryItem(i))) {
                            inventory.removeInventoryItem(i);
                        }
                    }
                    for (ScpEquipmentSlot slot : ScpEquipmentSlot.values()) {
                        if (HazmatSuitAccess.isInternalPiece(
                                inventory.getEquipment(slot))) {
                            inventory.clearEquipment(slot);
                        }
                    }
                    ModNetwork.syncTo(player, inventory);
                });
        vanilla.setChanged();
    }

    private static void sanitizeList(java.util.List<ItemStack> stacks) {
        for (int i = 0; i < stacks.size(); i++) {
            if (HazmatSuitAccess.isInternalPiece(stacks.get(i))) {
                stacks.set(i, ItemStack.EMPTY);
            }
        }
    }

    private static void clearCustomEquipmentMirrors(ServerPlayer player) {
        ScpInventoryCapability.get(player)
                .ifPresent(inventory -> {
                    inventory.clearEquipment(ScpEquipmentSlot.HEAD);
                    inventory.clearEquipment(ScpEquipmentSlot.CHEST);
                    inventory.clearEquipment(ScpEquipmentSlot.LEGS);
                    inventory.clearEquipment(ScpEquipmentSlot.FEET);
                    ModNetwork.syncTo(player, inventory);
                });
    }

    public static void routeToAuthoritativeInventoryOrDrop(
            ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return;
        }

        ItemStack remaining = stack.copy();
        if (player.isCreative()
                || !ScpAdditionsModulesConfig.get().inventory.enabled) {
            player.getInventory().add(remaining);
        } else {
            AtomicInteger accepted = new AtomicInteger();
            ScpInventoryCapability.get(player)
                    .ifPresent(inventory -> {
                        accepted.set(ScpPickupRouter.accept(
                                inventory, player, remaining.copy()));
                        ModNetwork.syncTo(player, inventory);
                    });
            if (accepted.get() > 0) {
                remaining.shrink(Math.min(remaining.getCount(),
                        accepted.get()));
            }
        }

        if (!remaining.isEmpty()) {
            player.drop(remaining, false);
            ModNetwork.showInventoryFull(player);
        }
    }

    private static void showActionBar(ServerPlayer player, String key) {
        if (player != null && key != null && !key.isBlank()) {
            player.displayClientMessage(Component.translatable(key), true);
        }
    }
}
