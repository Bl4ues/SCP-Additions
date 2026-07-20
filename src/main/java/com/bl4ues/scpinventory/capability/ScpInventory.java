package com.bl4ues.scpinventory.capability;

import com.bl4ues.scpinventory.debug.ScpInventoryDebug;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ScpInventory implements IScpInventory {

    private final List<ItemStack> inventory = new ArrayList<>();
    private final List<ItemStack> keys = new ArrayList<>();
    private final List<ItemStack> documents = new ArrayList<>();
    private final Map<ScpEquipmentSlot, ItemStack> equipment = new EnumMap<>(ScpEquipmentSlot.class);

    private int maxMainSlots = DEFAULT_MAIN_SLOT_COUNT;
    private int coinCount = 0;
    private ItemStack activeUsable = ItemStack.EMPTY;

    public ScpInventory() {
        resetEquipmentSlots();
        normalizeMainInventorySize();
    }

    @Override
    public List<ItemStack> getInventory() { return inventory; }

    @Override
    public void setInventory(List<ItemStack> list) {
        int before = countMainInventoryItems();
        inventory.clear();
        for (int i = 0; i < maxMainSlots; i++) {
            if (list != null && i < list.size()) inventory.add(toMainInventoryStack(list.get(i)));
            else inventory.add(ItemStack.EMPTY);
        }
        normalizeMainInventorySize();
        purgeActiveUsableCopiesFromMainInventory();
        debugUsable("setInventory before={} after={} incomingSize={} active={} caller={}",
                before,
                countMainInventoryItems(),
                list == null ? -1 : list.size(),
                ScpInventoryDebug.stack(activeUsable),
                ScpInventoryDebug.caller());
    }

    @Override
    public ItemStack getInventoryItem(int index) {
        if (index < 0 || index >= maxMainSlots) return ItemStack.EMPTY;
        normalizeMainInventorySize();
        return inventory.get(index);
    }

    @Override
    public boolean setInventoryItem(int index, ItemStack stack) {
        if (index < 0 || index >= maxMainSlots) return false;
        normalizeMainInventorySize();
        ItemStack stored = toMainInventoryStack(stack);
        inventory.set(index, stored);
        debugUsable("setInventoryItem slot={} incoming={} stored={} active={} caller={}",
                index,
                ScpInventoryDebug.stack(stack),
                ScpInventoryDebug.stack(stored),
                ScpInventoryDebug.stack(activeUsable),
                ScpInventoryDebug.caller());
        return true;
    }

    @Override
    public boolean addInventoryItem(ItemStack stack) {
        return addInventoryItems(stack) == getStackCount(stack);
    }

    @Override
    public int addInventoryItems(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            debugUsable("addInventoryItems ignored empty/null stack={} active={} caller={}",
                    ScpInventoryDebug.stack(stack),
                    ScpInventoryDebug.stack(activeUsable),
                    ScpInventoryDebug.caller());
            return 0;
        }
        if (isActiveUsableCopy(stack)) {
            debugUsable("addInventoryItems REJECT_ACTIVE incoming={} active={} caller={}",
                    ScpInventoryDebug.stack(stack),
                    ScpInventoryDebug.stack(activeUsable),
                    ScpInventoryDebug.caller());
            return 0;
        }

        normalizeMainInventorySize();

        int before = countMainInventoryItems();
        int amountToInsert = Math.min(stack.getCount(), getFreeMainSlots());
        int inserted = 0;

        for (int i = 0; i < amountToInsert; i++) {
            int emptySlot = firstEmptyMainSlot();
            if (emptySlot == -1) break;

            ItemStack singleItem = toMainInventoryStack(stack);
            if (singleItem.isEmpty()) break;
            inventory.set(emptySlot, singleItem);
            inserted++;
        }

        debugUsable("addInventoryItems incoming={} inserted={} before={} after={} active={} caller={}",
                ScpInventoryDebug.stack(stack),
                inserted,
                before,
                countMainInventoryItems(),
                ScpInventoryDebug.stack(activeUsable),
                ScpInventoryDebug.caller());
        return inserted;
    }

    @Override
    public boolean isInventoryFull() {
        normalizeMainInventorySize();
        for (ItemStack stack : inventory) if (stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack extractInventoryItem(int index) {
        if (index < 0 || index >= maxMainSlots) return ItemStack.EMPTY;
        normalizeMainInventorySize();
        ItemStack stack = inventory.get(index);
        inventory.set(index, ItemStack.EMPTY);
        debugUsable("extractInventoryItem slot={} extracted={} active={} caller={}",
                index,
                ScpInventoryDebug.stack(stack),
                ScpInventoryDebug.stack(activeUsable),
                ScpInventoryDebug.caller());
        return stack;
    }

    @Override
    public boolean removeInventoryItem(int index) {
        if (index < 0 || index >= maxMainSlots) return false;
        normalizeMainInventorySize();
        ItemStack previous = inventory.get(index);
        inventory.set(index, ItemStack.EMPTY);
        debugUsable("removeInventoryItem slot={} previous={} active={} caller={}",
                index,
                ScpInventoryDebug.stack(previous),
                ScpInventoryDebug.stack(activeUsable),
                ScpInventoryDebug.caller());
        return true;
    }

    @Override
    public int getMaxMainSlots() {
        return maxMainSlots;
    }

    @Override
    public void setMaxMainSlots(int slots) {
        maxMainSlots = clampSlots(slots);
        normalizeMainInventorySize();
        purgeActiveUsableCopiesFromMainInventory();
    }

    @Override
    public void resetMainInventory() {
        inventory.clear();
        coinCount = 0;
        activeUsable = ItemStack.EMPTY;
        normalizeMainInventorySize();
    }

    @Override
    public void resetAll() {
        maxMainSlots = DEFAULT_MAIN_SLOT_COUNT;
        coinCount = 0;
        activeUsable = ItemStack.EMPTY;
        resetMainInventory();
        keys.clear();
        documents.clear();
        resetEquipmentSlots();
    }

    @Override
    public int getCoinCount() {
        return coinCount;
    }

    @Override
    public void setCoinCount(int count) {
        coinCount = Math.max(0, Math.min(ScpPickupRouter.MAX_COIN_COUNT, count));
    }

    @Override
    public List<ItemStack> getKeys() { return keys; }

    @Override
    public void setKeys(List<ItemStack> list) {
        keys.clear();
        if (list == null) return;

        for (ItemStack stack : list) {
            if (getKeyCount() >= MAX_KEY_COUNT) break;
            if (stack != null && !stack.isEmpty()) keys.add(toSingleItemOrEmpty(stack));
        }
    }

    @Override
    public boolean addKeyItem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || getKeyCount() >= MAX_KEY_COUNT) return false;
        keys.add(toSingleItemOrEmpty(stack));
        return true;
    }

    @Override
    public ItemStack extractKeyItem(int index) {
        if (index < 0 || index >= keys.size()) return ItemStack.EMPTY;
        return keys.remove(index);
    }

    @Override
    public boolean removeKeyItem(int index) {
        if (index < 0 || index >= keys.size()) return false;
        keys.remove(index);
        return true;
    }

    @Override
    public List<ItemStack> getDocuments() { return documents; }

    @Override
    public void setDocuments(List<ItemStack> list) {
        documents.clear();
        addAllStacks(documents, list);
    }

    @Override
    public boolean addDocumentItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        documents.add(stack.copy());
        return true;
    }

    @Override
    public ItemStack getDocumentItem(int index) {
        if (index < 0 || index >= documents.size()) return ItemStack.EMPTY;
        return documents.get(index);
    }

    @Override
    public ItemStack extractDocumentItem(int index) {
        if (index < 0 || index >= documents.size()) return ItemStack.EMPTY;
        return documents.remove(index);
    }

    @Override
    public boolean removeDocumentItem(int index) {
        if (index < 0 || index >= documents.size()) return false;
        documents.remove(index);
        return true;
    }

    @Override
    public ItemStack getEquipment(ScpEquipmentSlot slot) {
        if (slot == null) return ItemStack.EMPTY;
        return equipment.getOrDefault(slot, ItemStack.EMPTY);
    }

    @Override
    public void setEquipment(ScpEquipmentSlot slot, ItemStack stack) {
        if (slot == null) return;
        equipment.put(slot, copyOrEmpty(stack));
    }

    @Override
    public ItemStack extractEquipment(ScpEquipmentSlot slot) {
        if (slot == null) return ItemStack.EMPTY;
        ItemStack stack = equipment.getOrDefault(slot, ItemStack.EMPTY);
        equipment.put(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public boolean clearEquipment(ScpEquipmentSlot slot) {
        if (slot == null) return false;
        equipment.put(slot, ItemStack.EMPTY);
        return true;
    }

    @Override
    public ItemStack getActiveUsable() {
        return activeUsable;
    }

    @Override
    public void setActiveUsable(ItemStack stack) {
        ItemStack previous = activeUsable;
        activeUsable = toSingleItemOrEmpty(stack);
        debugUsable("setActiveUsable previous={} incoming={} stored={} caller={}",
                ScpInventoryDebug.stack(previous),
                ScpInventoryDebug.stack(stack),
                ScpInventoryDebug.stack(activeUsable),
                ScpInventoryDebug.caller());
        purgeActiveUsableCopiesFromMainInventory();
    }

    @Override
    public ItemStack extractActiveUsable() {
        ItemStack stack = activeUsable;
        activeUsable = ItemStack.EMPTY;
        debugUsable("extractActiveUsable extracted={} caller={}",
                ScpInventoryDebug.stack(stack),
                ScpInventoryDebug.caller());
        return stack;
    }

    @Override
    public boolean clearActiveUsable() {
        if (activeUsable.isEmpty()) {
            debugUsable("clearActiveUsable ignored empty caller={}", ScpInventoryDebug.caller());
            return false;
        }
        debugUsable("clearActiveUsable clearing={} caller={}",
                ScpInventoryDebug.stack(activeUsable),
                ScpInventoryDebug.caller());
        activeUsable = ItemStack.EMPTY;
        return true;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        purgeActiveUsableCopiesFromMainInventory();
        debugUsable("serializeNBT mainCount={} active={} caller={}",
                countMainInventoryItems(),
                ScpInventoryDebug.stack(activeUsable),
                ScpInventoryDebug.caller());
        CompoundTag tag = new CompoundTag();

        tag.putInt("MaxMainSlots", maxMainSlots);
        tag.putInt("CoinCount", coinCount);
        tag.put("Inventory", saveStackList(inventory, true, registries));
        tag.put("Keys", saveStackList(keys, false, registries));
        tag.put("Documents", saveStackList(documents, false, registries));
        saveEquipment(tag, "ActiveUsable", activeUsable, registries);

        CompoundTag equipTag = new CompoundTag();
        for (ScpEquipmentSlot slot : ScpEquipmentSlot.values()) {
            saveEquipment(equipTag, slot.getTagName(), getEquipment(slot), registries);
        }
        tag.put("Equipment", equipTag);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag, HolderLookup.Provider registries) {
        maxMainSlots = tag.contains("MaxMainSlots")
                ? clampSlots(tag.getInt("MaxMainSlots"))
                : DEFAULT_MAIN_SLOT_COUNT;
        coinCount = tag.contains("CoinCount")
                ? Math.max(0, Math.min(ScpPickupRouter.MAX_COIN_COUNT, tag.getInt("CoinCount")))
                : 0;
        activeUsable = toSingleItemOrEmpty(loadEquipment(tag, "ActiveUsable", registries));

        inventory.clear();
        ListTag invList = tag.getList("Inventory", 10);
        for (int i = 0; i < maxMainSlots; i++) {
            if (i < invList.size()) inventory.add(toMainInventoryStack(ItemStack.parseOptional(registries, invList.getCompound(i))));
            else inventory.add(ItemStack.EMPTY);
        }
        normalizeMainInventorySize();
        purgeActiveUsableCopiesFromMainInventory();
        debugUsable("deserializeNBT invListSize={} mainCount={} active={} caller={}",
                invList.size(),
                countMainInventoryItems(),
                ScpInventoryDebug.stack(activeUsable),
                ScpInventoryDebug.caller());

        keys.clear();
        loadKeyList(keys, tag.getList("Keys", 10), registries);

        documents.clear();
        loadStackList(documents, tag.getList("Documents", 10), registries);

        resetEquipmentSlots();
        CompoundTag equipTag = tag.getCompound("Equipment");
        for (ScpEquipmentSlot slot : ScpEquipmentSlot.values()) {
            equipment.put(slot, loadEquipment(equipTag, slot.getTagName(), registries));
        }

        migrateLegacyEquipment(equipTag, registries);
    }

    private int firstEmptyMainSlot() {
        normalizeMainInventorySize();
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).isEmpty()) return i;
        }
        return -1;
    }

    private void normalizeMainInventorySize() {
        while (inventory.size() < maxMainSlots) {
            inventory.add(ItemStack.EMPTY);
        }

        while (inventory.size() > maxMainSlots) {
            inventory.remove(inventory.size() - 1);
        }
    }

    private void resetEquipmentSlots() {
        equipment.clear();
        for (ScpEquipmentSlot slot : ScpEquipmentSlot.values()) {
            equipment.put(slot, ItemStack.EMPTY);
        }
    }

    private void migrateLegacyEquipment(CompoundTag equipTag, HolderLookup.Provider registries) {
        if (getEquipment(ScpEquipmentSlot.HEAD).isEmpty()) {
            equipment.put(ScpEquipmentSlot.HEAD, loadEquipment(equipTag, "Head", registries));
        }

        if (getEquipment(ScpEquipmentSlot.BODY).isEmpty()) {
            equipment.put(ScpEquipmentSlot.BODY, loadEquipment(equipTag, "Chest", registries));
        }

        if (getEquipment(ScpEquipmentSlot.ACCESSORY).isEmpty()) {
            ItemStack legacyAccessory = loadEquipment(equipTag, "Accessory", registries);
            if (legacyAccessory.isEmpty()) {
                legacyAccessory = loadEquipment(equipTag, "Trinket", registries);
            }
            equipment.put(ScpEquipmentSlot.ACCESSORY, legacyAccessory);
        }

        if (getEquipment(ScpEquipmentSlot.WEAPON).isEmpty()) {
            equipment.put(ScpEquipmentSlot.WEAPON, loadEquipment(equipTag, "Weapon", registries));
        }
    }

    private boolean isActiveUsableCopy(ItemStack stack) {
        ItemStack active = normalizeForActiveUsableComparison(activeUsable);
        ItemStack incoming = normalizeForActiveUsableComparison(stack);
        return !active.isEmpty() && !incoming.isEmpty() && ItemStack.isSameItemSameComponents(incoming, active);
    }

    private ItemStack normalizeForActiveUsableComparison(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        ScpPickupRouter.stripUsableSession(copy);
        ScpPickupRouter.stripNoMergeMarker(copy);
        ScpPickupRouter.stripHarmfulMirror(copy);
        ScpPickupRouter.stripCoinMirror(copy);
        copy.setCount(1);
        return copy;
    }

    private ItemStack toMainInventoryStack(ItemStack stack) {
        if (isActiveUsableCopy(stack)) return ItemStack.EMPTY;
        return toSingleItemOrEmpty(stack);
    }

    private void purgeActiveUsableCopiesFromMainInventory() {
        if (activeUsable == null || activeUsable.isEmpty()) return;
        normalizeMainInventorySize();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.get(i);
            if (isActiveUsableCopy(stack)) {
                debugUsable("purgeActiveUsableCopies slot={} purged={} active={} caller={}",
                        i,
                        ScpInventoryDebug.stack(stack),
                        ScpInventoryDebug.stack(activeUsable),
                        ScpInventoryDebug.caller());
                inventory.set(i, ItemStack.EMPTY);
            }
        }
    }

    private int countMainInventoryItems() {
        int count = 0;
        for (ItemStack stack : inventory) {
            if (stack != null && !stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static void debugUsable(String message, Object... args) {
        if (ScpInventoryDebug.USABLE_DEBUG) {
            ScpInventoryDebug.usable(message, args);
        }
    }

    private static int clampSlots(int slots) {
        return Math.max(MIN_MAIN_SLOT_COUNT, Math.min(MAX_MAIN_SLOT_COUNT, slots));
    }

    private static int getStackCount(ItemStack stack) {
        return stack == null || stack.isEmpty() ? 0 : stack.getCount();
    }

    private static ItemStack copyOrEmpty(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    private static ItemStack toSingleItemOrEmpty(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private static void addAllStacks(List<ItemStack> target, List<ItemStack> source) {
        if (source == null) return;
        for (ItemStack stack : source) {
            if (stack != null && !stack.isEmpty()) target.add(stack.copy());
        }
    }

    private static ListTag saveStackList(
            List<ItemStack> stacks,
            boolean keepEmptySlots,
            HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty() && !keepEmptySlots) continue;
            list.add(saveStack(stack, registries));
        }
        return list;
    }

    private static void loadStackList(
            List<ItemStack> target,
            ListTag list,
            HolderLookup.Provider registries) {
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(registries, list.getCompound(i));
            if (!stack.isEmpty()) target.add(stack);
        }
    }

    private static void loadKeyList(
            List<ItemStack> target,
            ListTag list,
            HolderLookup.Provider registries) {
        for (int i = 0; i < list.size() && target.size() < MAX_KEY_COUNT; i++) {
            ItemStack stack = toSingleItemOrEmpty(
                    ItemStack.parseOptional(registries, list.getCompound(i)));
            if (!stack.isEmpty()) target.add(stack);
        }
    }

    private static void saveEquipment(
            CompoundTag parent,
            String key,
            ItemStack stack,
            HolderLookup.Provider registries) {
        if (!stack.isEmpty()) {
            parent.put(key, saveStack(stack, registries));
        }
    }

    private static ItemStack loadEquipment(
            CompoundTag parent,
            String key,
            HolderLookup.Provider registries) {
        return parent.contains(key)
                ? ItemStack.parseOptional(registries, parent.getCompound(key))
                : ItemStack.EMPTY;
    }

    private static CompoundTag saveStack(
            ItemStack stack,
            HolderLookup.Provider registries) {
        if (stack == null || stack.isEmpty()) return new CompoundTag();
        Tag saved = stack.saveOptional(registries);
        if (saved instanceof CompoundTag compound) return compound;
        throw new IllegalStateException("ItemStack did not serialize to a CompoundTag: " + stack);
    }

}
