package net.neoforged.neoforge.items.wrapper;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public final class SidedInvWrapper implements IItemHandlerModifiable {
    private final Container container;
    private final Direction side;
    public SidedInvWrapper(Container container, Direction side) {
        this.container = container;
        this.side = side;
    }
    private int[] slots() {
        if (container instanceof WorldlyContainer worldly && side != null) return worldly.getSlotsForFace(side);
        int[] result = new int[container.getContainerSize()];
        for (int i = 0; i < result.length; i++) result[i] = i;
        return result;
    }
    private int resolve(int slot) { int[] slots = slots(); return slot >= 0 && slot < slots.length ? slots[slot] : -1; }
    @Override public int getSlots() { return slots().length; }
    @Override public ItemStack getStackInSlot(int slot) { int actual = resolve(slot); return actual < 0 ? ItemStack.EMPTY : container.getItem(actual); }
    @Override public void setStackInSlot(int slot, ItemStack stack) { int actual = resolve(slot); if (actual >= 0) { container.setItem(actual, stack); container.setChanged(); } }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        int actual = resolve(slot);
        if (actual < 0 || stack.isEmpty() || !isItemValid(slot, stack)) return stack;
        ItemStack current = container.getItem(actual);
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, stack)) return stack;
        int limit = Math.min(container.getMaxStackSize(), stack.getMaxStackSize());
        int room = limit - current.getCount();
        int moved = Math.min(room, stack.getCount());
        if (moved <= 0) return stack;
        if (!simulate) {
            if (current.isEmpty()) { ItemStack copy = stack.copy(); copy.setCount(moved); container.setItem(actual, copy); }
            else current.grow(moved);
            container.setChanged();
        }
        ItemStack remainder = stack.copy(); remainder.shrink(moved); return remainder;
    }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        int actual = resolve(slot);
        if (actual < 0 || amount <= 0) return ItemStack.EMPTY;
        ItemStack current = container.getItem(actual);
        if (current.isEmpty()) return ItemStack.EMPTY;
        if (container instanceof WorldlyContainer worldly && side != null && !worldly.canTakeItemThroughFace(actual, current, side)) return ItemStack.EMPTY;
        int moved = Math.min(amount, current.getCount());
        ItemStack result = current.copy(); result.setCount(moved);
        if (!simulate) { current.shrink(moved); if (current.isEmpty()) container.setItem(actual, ItemStack.EMPTY); container.setChanged(); }
        return result;
    }
    @Override public int getSlotLimit(int slot) { return container.getMaxStackSize(); }
    @Override public boolean isItemValid(int slot, ItemStack stack) {
        int actual = resolve(slot);
        if (actual < 0 || !container.canPlaceItem(actual, stack)) return false;
        return !(container instanceof WorldlyContainer worldly) || side == null || worldly.canPlaceItemThroughFace(actual, stack, side);
    }
}
