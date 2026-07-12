package com.bl4ues.scpinventory.capability;

import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ScpInventory implements IScpInventory {
    private final List<ItemStack> inventory = new ArrayList<>();
    private final List<ItemStack> keys = new ArrayList<>();
    private final List<ItemStack> documents = new ArrayList<>();
    private final Map<ScpEquipmentSlot, ItemStack> equipment =
            new EnumMap<>(ScpEquipmentSlot.class);

    private int maxMainSlots = DEFAULT_MAIN_SLOT_COUNT;
    private int coinCount;
    private ItemStack activeUsable = ItemStack.EMPTY;

    public ScpInventory() {
        normalizeInventory();
        for (ScpEquipmentSlot slot : ScpEquipmentSlot.values()) {
            equipment.put(slot, ItemStack.EMPTY);
        }
    }

    @Override
    public List<ItemStack> getInventory() {
        normalizeInventory();
        return inventory;
    }

    @Override
    public ItemStack getInventoryItem(int index) {
        normalizeInventory();
        return index >= 0 && index < inventory.size()
                ? inventory.get(index) : ItemStack.EMPTY;
    }

    @Override
    public boolean setInventoryItem(int index, ItemStack stack) {
        normalizeInventory();
        if (index < 0 || index >= inventory.size()) return false;
        inventory.set(index, single(stack));
        return true;
    }

    @Override
    public int addInventoryItems(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        normalizeInventory();
        int inserted = 0;
        for (int amount = 0; amount < stack.getCount(); amount++) {
            int empty = firstEmptySlot();
            if (empty < 0) break;
            inventory.set(empty, single(stack));
            inserted++;
        }
        return inserted;
    }

    @Override
    public ItemStack extractInventoryItem(int index) {
        normalizeInventory();
        if (index < 0 || index >= inventory.size()) return ItemStack.EMPTY;
        ItemStack stack = inventory.get(index);
        inventory.set(index, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public boolean removeInventoryItem(int index) {
        return !extractInventoryItem(index).isEmpty();
    }

    @Override
    public boolean isInventoryFull() {
        return firstEmptySlot() < 0;
    }

    @Override
    public int getMaxMainSlots() {
        return maxMainSlots;
    }

    @Override
    public void setMaxMainSlots(int slots) {
        maxMainSlots = Math.max(MIN_MAIN_SLOT_COUNT,
                Math.min(MAX_MAIN_SLOT_COUNT, slots));
        normalizeInventory();
    }

    @Override
    public int getCoinCount() {
        return coinCount;
    }

    @Override
    public void setCoinCount(int count) {
        coinCount = Math.max(0, count);
    }

    @Override
    public List<ItemStack> getKeys() {
        return keys;
    }

    @Override
    public boolean addKeyItem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || keys.size() >= MAX_KEY_COUNT) return false;
        keys.add(single(stack));
        return true;
    }

    @Override
    public ItemStack extractKeyItem(int index) {
        return index >= 0 && index < keys.size() ? keys.remove(index) : ItemStack.EMPTY;
    }

    @Override
    public List<ItemStack> getDocuments() {
        return documents;
    }

    @Override
    public boolean addDocumentItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        documents.add(single(stack));
        return true;
    }

    @Override
    public ItemStack extractDocumentItem(int index) {
        return index >= 0 && index < documents.size()
                ? documents.remove(index) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack getEquipment(ScpEquipmentSlot slot) {
        return slot == null ? ItemStack.EMPTY
                : equipment.getOrDefault(slot, ItemStack.EMPTY);
    }

    @Override
    public void setEquipment(ScpEquipmentSlot slot, ItemStack stack) {
        if (slot != null) equipment.put(slot, single(stack));
    }

    @Override
    public ItemStack extractEquipment(ScpEquipmentSlot slot) {
        if (slot == null) return ItemStack.EMPTY;
        ItemStack stack = equipment.getOrDefault(slot, ItemStack.EMPTY);
        equipment.put(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public ItemStack getActiveUsable() {
        return activeUsable;
    }

    @Override
    public void setActiveUsable(ItemStack stack) {
        activeUsable = single(stack);
    }

    @Override
    public ItemStack extractActiveUsable() {
        ItemStack stack = activeUsable;
        activeUsable = ItemStack.EMPTY;
        return stack;
    }

    @Override
    public void resetAll() {
        maxMainSlots = DEFAULT_MAIN_SLOT_COUNT;
        coinCount = 0;
        activeUsable = ItemStack.EMPTY;
        inventory.clear();
        keys.clear();
        documents.clear();
        for (ScpEquipmentSlot slot : ScpEquipmentSlot.values()) {
            equipment.put(slot, ItemStack.EMPTY);
        }
        normalizeInventory();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("MaxMainSlots", maxMainSlots);
        tag.putInt("CoinCount", coinCount);
        tag.put("Inventory", saveList(inventory));
        tag.put("Keys", saveList(keys));
        tag.put("Documents", saveList(documents));
        tag.put("ActiveUsable", activeUsable.save(new CompoundTag()));

        CompoundTag equipmentTag = new CompoundTag();
        for (ScpEquipmentSlot slot : ScpEquipmentSlot.values()) {
            equipmentTag.put(slot.getTagName(),
                    getEquipment(slot).save(new CompoundTag()));
        }
        tag.put("Equipment", equipmentTag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        maxMainSlots = Math.max(MIN_MAIN_SLOT_COUNT,
                Math.min(MAX_MAIN_SLOT_COUNT,
                        tag.contains("MaxMainSlots")
                                ? tag.getInt("MaxMainSlots")
                                : DEFAULT_MAIN_SLOT_COUNT));
        coinCount = Math.max(0, tag.getInt("CoinCount"));
        activeUsable = ItemStack.of(tag.getCompound("ActiveUsable"));

        inventory.clear();
        loadList(inventory, tag.getList("Inventory", 10));
        keys.clear();
        loadList(keys, tag.getList("Keys", 10));
        while (keys.size() > MAX_KEY_COUNT) keys.remove(keys.size() - 1);
        documents.clear();
        loadList(documents, tag.getList("Documents", 10));

        CompoundTag equipmentTag = tag.getCompound("Equipment");
        for (ScpEquipmentSlot slot : ScpEquipmentSlot.values()) {
            equipment.put(slot, ItemStack.of(
                    equipmentTag.getCompound(slot.getTagName())));
        }
        normalizeInventory();
    }

    private static ItemStack single(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private void normalizeInventory() {
        while (inventory.size() < maxMainSlots) inventory.add(ItemStack.EMPTY);
        while (inventory.size() > maxMainSlots) inventory.remove(inventory.size() - 1);
    }

    private int firstEmptySlot() {
        normalizeInventory();
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).isEmpty()) return i;
        }
        return -1;
    }

    private static ListTag saveList(List<ItemStack> stacks) {
        ListTag list = new ListTag();
        for (ItemStack stack : stacks) {
            list.add(stack.save(new CompoundTag()));
        }
        return list;
    }

    private static void loadList(List<ItemStack> target, ListTag list) {
        for (int i = 0; i < list.size(); i++) {
            target.add(ItemStack.of(list.getCompound(i)));
        }
    }
}
