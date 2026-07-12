package com.bl4ues.scpinventory.item;

import com.bl4ues.scpinventory.capability.IScpInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ScpPickupRouter {

    public static final String NO_MERGE_TAG = "ScpInventoryNoMerge";
    public static final String USABLE_SESSION_TAG = "ScpInventoryUsableSession";
    public static final String USABLE_START_TICK_TAG = "ScpInventoryUsableStartTick";
    public static final String COIN_MIRROR_TAG = "ScpInventoryCoinMirror";
    public static final String HARMFUL_MIRROR_TAG = "ScpInventoryHarmfulMirror";
    public static final int MAX_COIN_COUNT = 999;

    private static final int VANILLA_MAIN_START = 9;
    private static final int VANILLA_MAIN_END_EXCLUSIVE = 36;

    private ScpPickupRouter() {
    }

    public static int accept(IScpInventory inventory, ServerPlayer player, ItemStack stack) {
        if (isUsableSession(stack)) {
            return 0;
        }

        if (inventory == null || stack == null || stack.isEmpty() || (player != null && player.isCreative())) {
            return 0;
        }

        if (isCoinMirror(stack) || isHarmfulMirror(stack)) {
            return 0;
        }

        stripNoMergeMarker(stack);

        if (ScpItemClassifier.isCoin(stack)) {
            return acceptCoin(inventory, player, stack);
        }

        ScpItemType type = ScpItemClassifier.getType(stack);

        if (type == ScpItemType.KEY) {
            return acceptKey(inventory, player, stack);
        }

        if (type == ScpItemType.HARMFUL) {
            return acceptHarmful(inventory, player, stack);
        }

        if (type == ScpItemType.CODEX) {
            return inventory.addDocumentItem(stack) ? stack.getCount() : 0;
        }

        return inventory.addInventoryItems(stack);
    }

    public static boolean reconcileCoinMirrors(ServerPlayer player, IScpInventory inventory) {
        if (player == null || inventory == null || player.isCreative() || player.isSpectator()) {
            return false;
        }

        if (isCarryingConfiguredCoin(player)) {
            return false;
        }

        boolean changed = sanitizeStoredCoinMirrors(inventory);
        changed |= removePlainConfiguredCoins(player.getInventory());

        int customCoins = countCustomCoins(inventory);
        int mirrorCoins = countCoinMirrors(player.getInventory());

        if (mirrorCoins < customCoins) {
            changed |= removeCustomCoins(inventory, customCoins - mirrorCoins) > 0;
        } else if (mirrorCoins > customCoins) {
            changed |= removeCoinMirrors(player.getInventory(), mirrorCoins - customCoins) > 0;
        }

        if (changed) {
            syncVanillaInventory(player);
        }
        return changed;
    }

    public static boolean reconcileHarmfulMirrors(ServerPlayer player, IScpInventory inventory) {
        if (player == null || inventory == null || player.isCreative() || player.isSpectator()) {
            return false;
        }

        boolean changed = sanitizeStoredHarmfulMirrors(inventory);
        Inventory vanillaInventory = player.getInventory();
        List<ItemStack> seen = new ArrayList<>();
        List<Integer> seenCounts = new ArrayList<>();

        changed |= removeStaleHarmfulMirrorsFromList(vanillaInventory.items, inventory, seen, seenCounts);
        changed |= removeStaleHarmfulMirrorsFromList(vanillaInventory.offhand, inventory, seen, seenCounts);
        changed |= removeStaleHarmfulMirrorsFromList(vanillaInventory.armor, inventory, seen, seenCounts);
        changed |= addMissingHarmfulMirrors(player, inventory);

        if (changed) {
            syncVanillaInventory(player);
        }
        return changed;
    }

    public static boolean sanitizeStoredCoinMirrors(IScpInventory inventory) {
        if (inventory == null) {
            return false;
        }

        boolean changed = false;
        for (int i = 0; i < inventory.getMaxMainSlots(); i++) {
            ItemStack stack = inventory.getInventoryItem(i);
            if (!stack.isEmpty() && isCoinMirror(stack)) {
                inventory.removeInventoryItem(i);
                changed = true;
            }
        }
        return changed;
    }

    public static boolean sanitizeStoredHarmfulMirrors(IScpInventory inventory) {
        if (inventory == null) {
            return false;
        }

        boolean changed = false;
        for (int i = 0; i < inventory.getMaxMainSlots(); i++) {
            ItemStack stack = inventory.getInventoryItem(i);
            if (!stack.isEmpty() && isHarmfulMirror(stack)) {
                stripHarmfulMirror(stack);
                inventory.setInventoryItem(i, stack);
                changed = true;
            }
        }
        return changed;
    }

    public static boolean syncCoinMirrors(ServerPlayer player, IScpInventory inventory) {
        if (player == null || inventory == null || player.isCreative() || player.isSpectator()) {
            return false;
        }

        if (isCarryingConfiguredCoin(player)) {
            return false;
        }

        Inventory vanillaInventory = player.getInventory();
        boolean changed = removeAllCoinMirrors(vanillaInventory);
        changed |= removePlainConfiguredCoins(vanillaInventory);

        int amount = countCustomCoins(inventory);
        int end = Math.min(VANILLA_MAIN_END_EXCLUSIVE, vanillaInventory.items.size());
        ItemStack template = ScpItemClassifier.getConfiguredCoinStack();

        for (int i = end - 1; i >= VANILLA_MAIN_START && amount > 0 && !template.isEmpty(); i--) {
            if (!vanillaInventory.items.get(i).isEmpty()) {
                continue;
            }

            ItemStack mirror = template.copy();
            mirror.setCount(1);
            markCoinMirror(mirror);
            vanillaInventory.items.set(i, mirror);
            vanillaInventory.setChanged();
            amount--;
            changed = true;
        }

        if (changed) {
            syncVanillaInventory(player);
        }
        return changed;
    }

    public static void resetCoinMirrorTracking(ServerPlayer player) {
    }

    private static int acceptCoin(IScpInventory inventory, ServerPlayer player, ItemStack stack) {
        if (inventory == null || stack == null || stack.isEmpty()) {
            return 0;
        }
        ItemStack stored = stack.copy();
        stripNoMergeMarker(stored);
        stripCoinMirror(stored);
        return inventory.addInventoryItems(stored);
    }

    private static int acceptHarmful(IScpInventory inventory, ServerPlayer player, ItemStack stack) {
        if (player == null || getFreeHarmfulMirrorSlots(player) <= 0) {
            return 0;
        }

        ItemStack stored = stack.copy();
        stored.setCount(Math.min(stack.getCount(), stored.getMaxStackSize()));
        stripNoMergeMarker(stored);
        stripHarmfulMirror(stored);

        int accepted = inventory.addInventoryItems(stored);
        if (accepted <= 0) {
            return 0;
        }

        ItemStack mirror = stored.copy();
        mirror.setCount(accepted);
        if (!addHarmfulMirror(player, mirror)) {
            removeCustomHarmful(inventory, mirror, accepted);
            return 0;
        }

        return accepted;
    }

    private static int getFreeCoinMirrorSlots(ServerPlayer player) {
        if (player == null) {
            return 0;
        }

        int free = 0;
        Inventory inventory = player.getInventory();
        for (int i = VANILLA_MAIN_START; i < VANILLA_MAIN_END_EXCLUSIVE && i < inventory.items.size(); i++) {
            if (inventory.items.get(i).isEmpty()) {
                free++;
            }
        }
        return free;
    }

    private static int getFreeHarmfulMirrorSlots(ServerPlayer player) {
        return getFreeCoinMirrorSlots(player);
    }

    private static boolean addMirroredCoin(ServerPlayer player, ItemStack coin) {
        if (player == null || coin == null || coin.isEmpty()) {
            return false;
        }

        Inventory inventory = player.getInventory();
        for (int i = VANILLA_MAIN_START; i < VANILLA_MAIN_END_EXCLUSIVE && i < inventory.items.size(); i++) {
            if (inventory.items.get(i).isEmpty()) {
                ItemStack copy = coin.copy();
                copy.setCount(1);
                markCoinMirror(copy);
                inventory.items.set(i, copy);
                inventory.setChanged();
                syncVanillaInventory(player);
                return true;
            }
        }
        return false;
    }

    private static boolean addHarmfulMirror(ServerPlayer player, ItemStack harmful) {
        if (player == null || harmful == null || harmful.isEmpty()) {
            return false;
        }

        Inventory inventory = player.getInventory();
        for (int i = VANILLA_MAIN_START; i < VANILLA_MAIN_END_EXCLUSIVE && i < inventory.items.size(); i++) {
            if (inventory.items.get(i).isEmpty()) {
                ItemStack copy = harmful.copy();
                stripNoMergeMarker(copy);
                stripUsableSession(copy);
                stripHarmfulMirror(copy);
                markHarmfulMirror(copy);
                inventory.items.set(i, copy);
                inventory.setChanged();
                syncVanillaInventory(player);
                return true;
            }
        }
        return false;
    }

    private static boolean removeMirroredCoin(ServerPlayer player, ItemStack coin) {
        if (player == null || coin == null || coin.isEmpty()) {
            return false;
        }

        Inventory inventory = player.getInventory();
        for (int i = VANILLA_MAIN_START; i < VANILLA_MAIN_END_EXCLUSIVE && i < inventory.items.size(); i++) {
            ItemStack candidate = inventory.items.get(i);
            if (sameCoinMirror(candidate, coin)) {
                candidate.shrink(1);
                if (candidate.isEmpty()) {
                    inventory.items.set(i, ItemStack.EMPTY);
                }
                inventory.setChanged();
                syncVanillaInventory(player);
                return true;
            }
        }
        return false;
    }

    private static boolean sameCoinMirror(ItemStack candidate, ItemStack coin) {
        if (!isCoinMirror(candidate) || coin == null || coin.isEmpty()) {
            return false;
        }
        ItemStack normalizedCandidate = candidate.copy();
        normalizedCandidate.setCount(1);
        stripCoinMirror(normalizedCandidate);

        ItemStack normalizedCoin = coin.copy();
        normalizedCoin.setCount(1);
        stripCoinMirror(normalizedCoin);

        return ItemStack.isSameItemSameTags(normalizedCandidate, normalizedCoin);
    }

    private static boolean sameHarmfulMirror(ItemStack candidate, ItemStack harmful) {
        if (!isHarmfulMirror(candidate) || harmful == null || harmful.isEmpty()) {
            return false;
        }
        return ItemStack.isSameItemSameTags(normalizeHarmfulComparable(candidate), normalizeHarmfulComparable(harmful));
    }

    private static int countCustomCoins(IScpInventory inventory) {
        int count = 0;
        for (int i = 0; i < inventory.getMaxMainSlots(); i++) {
            ItemStack stack = inventory.getInventoryItem(i);
            if (!stack.isEmpty() && ScpItemClassifier.isCoin(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int countStoredHarmful(IScpInventory inventory, ItemStack harmful) {
        int count = 0;
        if (inventory == null || harmful == null || harmful.isEmpty()) {
            return 0;
        }
        ItemStack normalized = normalizeHarmfulComparable(harmful);
        for (int i = 0; i < inventory.getMaxMainSlots(); i++) {
            ItemStack stack = inventory.getInventoryItem(i);
            if (!stack.isEmpty()
                    && ScpItemClassifier.isHarmful(stack)
                    && ItemStack.isSameItemSameTags(normalizeHarmfulComparable(stack), normalized)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int removeCustomCoins(IScpInventory inventory, int amount) {
        int remaining = amount;
        for (int i = inventory.getMaxMainSlots() - 1; i >= 0 && remaining > 0; i--) {
            ItemStack stack = inventory.getInventoryItem(i);
            if (stack.isEmpty() || !ScpItemClassifier.isCoin(stack)) {
                continue;
            }

            inventory.removeInventoryItem(i);
            remaining--;
        }
        return amount - remaining;
    }

    private static int removeCustomHarmful(IScpInventory inventory, ItemStack harmful, int amount) {
        int remaining = amount;
        if (inventory == null || harmful == null || harmful.isEmpty()) {
            return 0;
        }
        ItemStack normalized = normalizeHarmfulComparable(harmful);
        for (int i = inventory.getMaxMainSlots() - 1; i >= 0 && remaining > 0; i--) {
            ItemStack stack = inventory.getInventoryItem(i);
            if (stack.isEmpty()
                    || !ScpItemClassifier.isHarmful(stack)
                    || !ItemStack.isSameItemSameTags(normalizeHarmfulComparable(stack), normalized)) {
                continue;
            }
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            if (stack.isEmpty()) {
                inventory.removeInventoryItem(i);
            } else {
                inventory.setInventoryItem(i, stack);
            }
            remaining -= removed;
        }
        return amount - remaining;
    }

    private static int countCoinMirrors(Inventory inventory) {
        int count = 0;
        if (inventory == null) return 0;
        for (ItemStack stack : inventory.items) if (isCoinMirror(stack)) count += stack.getCount();
        for (ItemStack stack : inventory.offhand) if (isCoinMirror(stack)) count += stack.getCount();
        for (ItemStack stack : inventory.armor) if (isCoinMirror(stack)) count += stack.getCount();
        return count;
    }

    private static int countHarmfulMirrors(Inventory inventory, ItemStack harmful) {
        int count = 0;
        if (inventory == null || harmful == null || harmful.isEmpty()) return 0;
        for (ItemStack stack : inventory.items) if (sameHarmfulMirror(stack, harmful)) count += stack.getCount();
        for (ItemStack stack : inventory.offhand) if (sameHarmfulMirror(stack, harmful)) count += stack.getCount();
        for (ItemStack stack : inventory.armor) if (sameHarmfulMirror(stack, harmful)) count += stack.getCount();
        return count;
    }

    private static int removeCoinMirrors(Inventory inventory, int amount) {
        int remaining = amount;
        remaining -= removeCoinMirrorsFromList(inventory.items, remaining);
        remaining -= removeCoinMirrorsFromList(inventory.offhand, remaining);
        remaining -= removeCoinMirrorsFromList(inventory.armor, remaining);
        if (remaining != amount) {
            inventory.setChanged();
        }
        return amount - remaining;
    }

    private static int removeCoinMirrorsFromList(java.util.List<ItemStack> stacks, int amount) {
        int removed = 0;
        for (int i = stacks.size() - 1; i >= 0 && removed < amount; i--) {
            ItemStack stack = stacks.get(i);
            if (!isCoinMirror(stack)) {
                continue;
            }

            stacks.set(i, ItemStack.EMPTY);
            removed += stack.getCount();
        }
        return removed;
    }

    private static boolean removeStaleHarmfulMirrorsFromList(java.util.List<ItemStack> stacks, IScpInventory inventory,
                                                             List<ItemStack> seen, List<Integer> seenCounts) {
        boolean changed = false;
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (!isHarmfulMirror(stack)) {
                continue;
            }

            ItemStack normalized = normalizeHarmfulComparable(stack);
            int desired = countStoredHarmful(inventory, normalized);
            int alreadySeen = countSeenHarmful(seen, seenCounts, normalized);
            int allowedHere = desired - alreadySeen;

            if (allowedHere <= 0) {
                stacks.set(i, ItemStack.EMPTY);
                changed = true;
                continue;
            }

            int kept = Math.min(stack.getCount(), allowedHere);
            if (stack.getCount() != kept) {
                stack.setCount(kept);
                changed = true;
            }
            addSeenHarmful(seen, seenCounts, normalized, kept);
        }
        if (changed && stacks == null) {
            return true;
        }
        return changed;
    }

    private static int countSeenHarmful(List<ItemStack> seen, List<Integer> seenCounts, ItemStack harmful) {
        int count = 0;
        for (int i = 0; i < seen.size(); i++) {
            if (ItemStack.isSameItemSameTags(seen.get(i), harmful)) {
                count += seenCounts.get(i);
            }
        }
        return count;
    }

    private static void addSeenHarmful(List<ItemStack> seen, List<Integer> seenCounts, ItemStack harmful, int count) {
        if (count <= 0) {
            return;
        }
        for (int i = 0; i < seen.size(); i++) {
            if (ItemStack.isSameItemSameTags(seen.get(i), harmful)) {
                seenCounts.set(i, seenCounts.get(i) + count);
                return;
            }
        }
        seen.add(harmful.copy());
        seenCounts.add(count);
    }

    private static boolean addMissingHarmfulMirrors(ServerPlayer player, IScpInventory inventory) {
        boolean changed = false;
        for (int i = 0; i < inventory.getMaxMainSlots(); i++) {
            ItemStack stack = inventory.getInventoryItem(i);
            if (stack.isEmpty() || !ScpItemClassifier.isHarmful(stack)) {
                continue;
            }

            int desired = stack.getCount();
            int current = countHarmfulMirrors(player.getInventory(), stack);
            int missing = desired - current;
            while (missing > 0 && getFreeHarmfulMirrorSlots(player) > 0) {
                ItemStack mirror = stack.copy();
                mirror.setCount(Math.min(missing, mirror.getMaxStackSize()));
                if (!addHarmfulMirror(player, mirror)) {
                    break;
                }
                missing -= mirror.getCount();
                changed = true;
            }
        }
        return changed;
    }

    private static boolean removeAllCoinMirrors(Inventory inventory) {
        boolean changed = false;
        changed |= removeAllCoinMirrorsFromList(inventory.items);
        changed |= removeAllCoinMirrorsFromList(inventory.offhand);
        changed |= removeAllCoinMirrorsFromList(inventory.armor);
        if (changed) {
            inventory.setChanged();
        }
        return changed;
    }

    private static boolean removeAllCoinMirrorsFromList(java.util.List<ItemStack> stacks) {
        boolean changed = false;
        for (int i = 0; i < stacks.size(); i++) {
            if (isCoinMirror(stacks.get(i))) {
                stacks.set(i, ItemStack.EMPTY);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean removePlainConfiguredCoins(Inventory inventory) {
        boolean changed = false;
        changed |= removePlainConfiguredCoinsFromList(inventory.items);
        changed |= removePlainConfiguredCoinsFromList(inventory.offhand);
        changed |= removePlainConfiguredCoinsFromList(inventory.armor);
        if (changed) {
            inventory.setChanged();
        }
        return changed;
    }

    private static boolean removePlainConfiguredCoinsFromList(java.util.List<ItemStack> stacks) {
        boolean changed = false;
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (!isCoinMirror(stack) && isConfiguredCoinStack(stack)) {
                stacks.set(i, ItemStack.EMPTY);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean isCarryingConfiguredCoin(ServerPlayer player) {
        if (player == null || player.containerMenu == null) {
            return false;
        }
        ItemStack carried = player.containerMenu.getCarried();
        return isCoinMirror(carried) || isConfiguredCoinStack(carried);
    }

    private static boolean isConfiguredCoinStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ItemStack template = ScpItemClassifier.getConfiguredCoinStack();
        if (template.isEmpty()) {
            return false;
        }
        ItemStack normalizedStack = stack.copy();
        normalizedStack.setCount(1);
        stripCoinMirror(normalizedStack);

        ItemStack normalizedTemplate = template.copy();
        normalizedTemplate.setCount(1);
        stripCoinMirror(normalizedTemplate);

        return ItemStack.isSameItemSameTags(normalizedStack, normalizedTemplate);
    }

    public static void markCoinMirror(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.getOrCreateTag().putBoolean(COIN_MIRROR_TAG, true);
    }

    public static boolean isCoinMirror(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.hasTag() && stack.getTag() != null && stack.getTag().getBoolean(COIN_MIRROR_TAG);
    }

    public static void stripCoinMirror(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }
        tag.remove(COIN_MIRROR_TAG);
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    public static void markHarmfulMirror(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        stack.getOrCreateTag().putBoolean(HARMFUL_MIRROR_TAG, true);
    }

    public static boolean isHarmfulMirror(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.hasTag() && stack.getTag() != null && stack.getTag().getBoolean(HARMFUL_MIRROR_TAG);
    }

    public static void stripHarmfulMirror(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }
        tag.remove(HARMFUL_MIRROR_TAG);
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    public static int countCoins(Inventory inventory) {
        return countCoinMirrors(inventory);
    }

    public static void syncVanillaInventory(ServerPlayer player) {
        if (player == null) {
            return;
        }

        Inventory inventory = player.getInventory();
        inventory.setChanged();
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) {
            player.containerMenu.broadcastChanges();
        }

        for (int i = 0; i < inventory.items.size(); i++) {
            ItemStack copy = inventory.items.get(i).copy();
            player.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, i, copy));
            player.connection.send(new ClientboundContainerSetSlotPacket(player.inventoryMenu.containerId, 0, toInventoryMenuSlot(i), copy));
        }
    }

    private static int toInventoryMenuSlot(int inventoryIndex) {
        return inventoryIndex >= 0 && inventoryIndex < 9 ? inventoryIndex + 36 : inventoryIndex;
    }

    public static void markUsableSession(ItemStack stack, int startTick) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(USABLE_SESSION_TAG, true);
        tag.putInt(USABLE_START_TICK_TAG, startTick);
    }

    public static boolean isUsableSession(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.hasTag() && stack.getTag() != null && stack.getTag().getBoolean(USABLE_SESSION_TAG);
    }

    public static int getUsableSessionStartTick(ItemStack stack) {
        if (!isUsableSession(stack) || stack.getTag() == null) {
            return 0;
        }
        return stack.getTag().getInt(USABLE_START_TICK_TAG);
    }

    public static void stripUsableSession(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }

        tag.remove(USABLE_SESSION_TAG);
        tag.remove(USABLE_START_TICK_TAG);
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    public static void addNoMergeMarker(ItemStack stack, String marker) {
        if (stack == null || stack.isEmpty() || marker == null || marker.isEmpty()) {
            return;
        }
        stack.getOrCreateTag().putString(NO_MERGE_TAG, marker);
    }

    public static void stripNoMergeMarker(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NO_MERGE_TAG)) {
            return;
        }

        tag.remove(NO_MERGE_TAG);
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    private static ItemStack normalizeHarmfulComparable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        stripHarmfulMirror(copy);
        stripNoMergeMarker(copy);
        stripUsableSession(copy);
        return copy;
    }

    private static int acceptKey(IScpInventory inventory, ServerPlayer player, ItemStack stack) {
        if (player == null) {
            return 0;
        }

        int acceptedLimit = Math.min(stack.getCount(), inventory.getFreeKeySlots());
        acceptedLimit = Math.min(acceptedLimit, ScpKeyringMirror.getFreeMirrorSlots(player));

        int accepted = 0;
        for (int i = 0; i < acceptedLimit; i++) {
            ItemStack singleKey = stack.copy();
            singleKey.setCount(1);
            stripNoMergeMarker(singleKey);

            if (!ScpKeyringMirror.addMirroredKey(player, singleKey)) {
                break;
            }

            if (!inventory.addKeyItem(singleKey)) {
                ScpKeyringMirror.removeMirroredKey(player, singleKey);
                break;
            }

            accepted++;
        }

        return accepted;
    }
}
