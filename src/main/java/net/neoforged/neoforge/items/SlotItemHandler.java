package net.neoforged.neoforge.items;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SlotItemHandler extends Slot {
    private final IItemHandler handler;
    private final int index;
    public SlotItemHandler(IItemHandler handler, int index, int x, int y) {
        super(new SimpleContainer(Math.max(1, handler.getSlots())), index, x, y);
        this.handler = handler;
        this.index = index;
    }
    @Override public ItemStack getItem() { return handler.getStackInSlot(index); }
    @Override public boolean hasItem() { return !getItem().isEmpty(); }
    @Override public void set(ItemStack stack) {
        if (handler instanceof IItemHandlerModifiable modifiable) modifiable.setStackInSlot(index, stack);
        setChanged();
    }
    @Override public ItemStack remove(int amount) { return handler.extractItem(index, amount, false); }
    @Override public boolean mayPlace(ItemStack stack) { return handler.isItemValid(index, stack); }
    @Override public int getMaxStackSize() { return handler.getSlotLimit(index); }
    @Override public void onTake(Player player, ItemStack stack) { setChanged(); }
}
