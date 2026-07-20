package net.neoforged.neoforge.items;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public class ItemStackHandler implements IItemHandlerModifiable {
    protected final List<ItemStack> stacks;
    public ItemStackHandler(int size) {
        stacks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) stacks.add(ItemStack.EMPTY);
    }
    @Override public int getSlots() { return stacks.size(); }
    @Override public ItemStack getStackInSlot(int slot) { return valid(slot) ? stacks.get(slot) : ItemStack.EMPTY; }
    @Override public void setStackInSlot(int slot, ItemStack stack) {
        if (!valid(slot)) return;
        stacks.set(slot, stack == null ? ItemStack.EMPTY : stack);
        onContentsChanged(slot);
    }
    @Override public ItemStack insertItem(int slot, ItemStack incoming, boolean simulate) {
        if (!valid(slot) || incoming == null || incoming.isEmpty() || !isItemValid(slot, incoming)) return incoming;
        ItemStack current = stacks.get(slot);
        int limit = Math.min(getSlotLimit(slot), incoming.getMaxStackSize());
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, incoming)) return incoming;
        int room = limit - current.getCount();
        if (room <= 0) return incoming;
        int moved = Math.min(room, incoming.getCount());
        if (!simulate) {
            if (current.isEmpty()) {
                ItemStack inserted = incoming.copy();
                inserted.setCount(moved);
                stacks.set(slot, inserted);
            } else current.grow(moved);
            onContentsChanged(slot);
        }
        ItemStack remainder = incoming.copy();
        remainder.shrink(moved);
        return remainder;
    }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!valid(slot) || amount <= 0) return ItemStack.EMPTY;
        ItemStack current = stacks.get(slot);
        if (current.isEmpty()) return ItemStack.EMPTY;
        int moved = Math.min(amount, current.getCount());
        ItemStack result = current.copy();
        result.setCount(moved);
        if (!simulate) {
            current.shrink(moved);
            if (current.isEmpty()) stacks.set(slot, ItemStack.EMPTY);
            onContentsChanged(slot);
        }
        return result;
    }
    @Override public int getSlotLimit(int slot) { return 64; }
    @Override public boolean isItemValid(int slot, ItemStack stack) { return true; }
    protected void onContentsChanged(int slot) {}
    private boolean valid(int slot) { return slot >= 0 && slot < stacks.size(); }
}
