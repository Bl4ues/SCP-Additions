package com.bl4ues.scpclassifieddirective.world.inventory;

import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import com.bl4ues.scpclassifieddirective.block.entity.Scp294BlockEntity;
import com.bl4ues.scpclassifieddirective.network.Scp294GuiSlotMessage;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModMenus;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModItems;
import com.bl4ues.scpclassifieddirective.integration.PlayerCurrencyAccess;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;

public class Scp294GuiMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {
	public final static HashMap<String, Object> guistate = new HashMap<>();
	public final Level world;
	public final Player entity;
	public int x, y, z;
	private ContainerLevelAccess access = ContainerLevelAccess.NULL;
	private IItemHandler internal;
	private final Map<Integer, Slot> customSlots = new HashMap<>();
	private boolean bound = false;
	private Supplier<Boolean> boundItemMatcher = null;
	private Entity boundEntity = null;
	private BlockEntity boundBlockEntity = null;

	public Scp294GuiMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
		super(ScpClassifiedDirectiveModMenus.SCP_294_GUI.get(), id);
		this.entity = inv.player;
		this.world = inv.player.level();
		this.internal = new ItemStackHandler(1);
		BlockPos pos = null;
		if (extraData != null) {
			pos = extraData.readBlockPos();
			this.x = pos.getX();
			this.y = pos.getY();
			this.z = pos.getZ();
			access = ContainerLevelAccess.create(world, pos);
		}
		if (pos != null) {
			if (extraData.readableBytes() == 1) {
				byte hand = extraData.readByte();
				ItemStack itemstack = hand == 0 ? this.entity.getMainHandItem() : this.entity.getOffhandItem();
				this.boundItemMatcher = () -> itemstack == (hand == 0 ? this.entity.getMainHandItem() : this.entity.getOffhandItem());
				itemstack.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(capability -> {
					this.internal = capability;
					this.bound = true;
				});
			} else if (extraData.readableBytes() > 1) {
				extraData.readByte();
				boundEntity = world.getEntity(extraData.readVarInt());
				if (boundEntity != null)
					boundEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(capability -> {
						this.internal = capability;
						this.bound = true;
					});
			} else {
				boundBlockEntity = this.world.getBlockEntity(pos);
				if (boundBlockEntity != null)
					boundBlockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(capability -> {
						this.internal = capability;
						this.bound = true;
					});
			}
		}
		this.customSlots.put(0, this.addSlot(new SlotItemHandler(internal, 0, 142, 27) {
			@Override
			public void setChanged() {
				super.setChanged();
				slotChanged(0, 0, 0);
			}

			@Override
			public void onTake(Player entity, ItemStack stack) {
				super.onTake(entity, stack);
				slotChanged(0, 1, 0);
			}

			@Override
			public void onQuickCraft(ItemStack a, ItemStack b) {
				super.onQuickCraft(a, b);
				slotChanged(0, 2, b.getCount() - a.getCount());
			}

			@Override
			public boolean mayPlace(ItemStack stack) {
				return ScpClassifiedDirectiveModItems.COIN.get() == stack.getItem();
			}
		}));
		for (int si = 0; si < 3; ++si)
			for (int sj = 0; sj < 9; ++sj)
				this.addSlot(new Slot(inv, sj + (si + 1) * 9, 8 + sj * 18, 84 + si * 18));
		for (int si = 0; si < 9; ++si)
			this.addSlot(new Slot(inv, si, 8 + si * 18, 142));
	}

	@Override
	public boolean stillValid(Player player) {
		if (this.bound) {
			if (this.boundItemMatcher != null)
				return this.boundItemMatcher.get();
			else if (this.boundBlockEntity != null)
				return AbstractContainerMenu.stillValid(this.access, player, this.boundBlockEntity.getBlockState().getBlock());
			else if (this.boundEntity != null)
				return this.boundEntity.isAlive();
		}
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player playerIn, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasItem()) {
			ItemStack itemstack1 = slot.getItem();
			itemstack = itemstack1.copy();
			if (index < 1) {
				if (!this.moveItemStackTo(itemstack1, 1, this.slots.size(), true))
					return ItemStack.EMPTY;
				slot.onQuickCraft(itemstack1, itemstack);
			} else if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
				if (index < 28) {
					if (!this.moveItemStackTo(itemstack1, 28, this.slots.size(), true))
						return ItemStack.EMPTY;
				} else if (!this.moveItemStackTo(itemstack1, 1, 28, false)) {
					return ItemStack.EMPTY;
				}
				return ItemStack.EMPTY;
			}
			if (itemstack1.getCount() == 0)
				slot.set(ItemStack.EMPTY);
			else
				slot.setChanged();
			if (itemstack1.getCount() == itemstack.getCount())
				return ItemStack.EMPTY;
			slot.onTake(playerIn, itemstack1);
		}
		return itemstack;
	}

	@Override
	protected boolean moveItemStackTo(ItemStack stack, int start, int end, boolean reverse) {
		boolean moved = false;
		int i = reverse ? end - 1 : start;
		if (stack.isStackable()) {
			while (!stack.isEmpty() && (reverse ? i >= start : i < end)) {
				Slot slot = this.slots.get(i);
				ItemStack existing = slot.getItem();
				if (slot.mayPlace(stack) && !existing.isEmpty() && ItemStack.isSameItemSameTags(stack, existing)) {
					int combined = existing.getCount() + stack.getCount();
					int maxSize = Math.min(slot.getMaxStackSize(), stack.getMaxStackSize());
					if (combined <= maxSize) {
						stack.setCount(0);
						existing.setCount(combined);
						slot.set(existing);
						moved = true;
					} else if (existing.getCount() < maxSize) {
						stack.shrink(maxSize - existing.getCount());
						existing.setCount(maxSize);
						slot.set(existing);
						moved = true;
					}
				}
				i += reverse ? -1 : 1;
			}
		}
		if (!stack.isEmpty()) {
			i = reverse ? end - 1 : start;
			while (reverse ? i >= start : i < end) {
				Slot slot = this.slots.get(i);
				if (slot.getItem().isEmpty() && slot.mayPlace(stack)) {
					if (stack.getCount() > slot.getMaxStackSize())
						slot.setByPlayer(stack.split(slot.getMaxStackSize()));
					else
						slot.setByPlayer(stack.split(stack.getCount()));
					slot.setChanged();
					moved = true;
					break;
				}
				i += reverse ? -1 : 1;
			}
		}
		return moved;
	}

	@Override
	public void removed(Player playerIn) {
		super.removed(playerIn);
		// Physical payment belongs to the machine, not to the temporary keyboard
		// screen. Leaving the typing view must not eject/refund the coin.
		if (!playerIn.level().isClientSide
				&& !(boundBlockEntity instanceof Scp294BlockEntity)) {
			ItemStack escrowedCurrency = internal.extractItem(
					0, internal.getStackInSlot(0).getCount(), false);
			PlayerCurrencyAccess.refund(playerIn, escrowedCurrency);
		}
		if (!bound && playerIn instanceof ServerPlayer serverPlayer) {
			if (!serverPlayer.isAlive() || serverPlayer.hasDisconnected()) {
				for (int j = 1; j < internal.getSlots(); ++j)
					playerIn.drop(internal.extractItem(j, internal.getStackInSlot(j).getCount(), false), false);
			} else {
				for (int i = 1; i < internal.getSlots(); ++i)
					playerIn.getInventory().placeItemBackInInventory(
							internal.extractItem(i, internal.getStackInSlot(i).getCount(), false));
			}
		}
	}

	private void slotChanged(int slotid, int ctype, int meta) {
		if (this.world != null && this.world.isClientSide()) {
			ScpClassifiedDirectiveMod.PACKET_HANDLER.sendToServer(
					new Scp294GuiSlotMessage(slotid, x, y, z, ctype, meta));
			Scp294GuiSlotMessage.handleSlotAction(entity, slotid, ctype, meta, x, y, z);
		}
	}

	@Override
	public Map<Integer, Slot> get() {
		return customSlots;
	}
}
