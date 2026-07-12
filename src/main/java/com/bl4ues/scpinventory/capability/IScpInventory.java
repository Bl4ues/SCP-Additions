package com.bl4ues.scpinventory.capability;

import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface IScpInventory {
    int DEFAULT_MAIN_SLOT_COUNT = 12;
    int MIN_MAIN_SLOT_COUNT = 1;
    int MAX_MAIN_SLOT_COUNT = 128;
    int MAX_KEY_COUNT = 12;

    List<ItemStack> getInventory();
    ItemStack getInventoryItem(int index);
    boolean setInventoryItem(int index, ItemStack stack);
    int addInventoryItems(ItemStack stack);
    ItemStack extractInventoryItem(int index);
    boolean removeInventoryItem(int index);
    boolean isInventoryFull();

    int getMaxMainSlots();
    void setMaxMainSlots(int slots);

    int getCoinCount();
    void setCoinCount(int count);

    List<ItemStack> getKeys();
    boolean addKeyItem(ItemStack stack);
    ItemStack extractKeyItem(int index);

    List<ItemStack> getDocuments();
    boolean addDocumentItem(ItemStack stack);
    ItemStack extractDocumentItem(int index);

    ItemStack getEquipment(ScpEquipmentSlot slot);
    void setEquipment(ScpEquipmentSlot slot, ItemStack stack);
    ItemStack extractEquipment(ScpEquipmentSlot slot);

    ItemStack getActiveUsable();
    void setActiveUsable(ItemStack stack);
    ItemStack extractActiveUsable();

    void resetAll();
    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag tag);
}
