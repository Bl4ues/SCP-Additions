package net.mcreator.scpadditions.scp1576;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Single-stack container preserving the exact SCP-1576 instance while placed. */
public final class Scp1576PlacedBlockEntity extends BlockEntity
        implements GeoBlockEntity, Container {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    public Scp1576PlacedBlockEntity(BlockPos pos, BlockState state) {
        super(Scp1576Module.PLACED_BLOCK_ENTITY.get(), pos, state);
    }

    public ItemStack getStoredItem() {
        return items.get(0);
    }

    public void setStoredItem(ItemStack stack) {
        ItemStack value = stack == null ? ItemStack.EMPTY : stack.copy();
        if (!value.isEmpty()) value.setCount(1);
        items.set(0, value);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.clear();
        ContainerHelper.loadAllItems(tag, items);
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return items.get(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? items.get(0) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != 0) return;
        setStoredItem(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.set(0, ItemStack.EMPTY);
        setChanged();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Placed SCP-1576 deliberately remains in its authored default pose.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
