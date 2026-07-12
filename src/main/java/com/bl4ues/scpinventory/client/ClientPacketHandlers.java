package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

public final class ClientPacketHandlers {

    private ClientPacketHandlers() {
    }

    public static void showInventoryFullOverlay() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && (minecraft.player.isCreative() || minecraft.player.isSpectator())) {
            InventoryFullOverlay.hide();
            return;
        }
        InventoryFullOverlay.show();
    }




    public static void syncInventory(CompoundTag inventoryTag) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || inventoryTag == null) {
            return;
        }
        minecraft.player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
            inventory.deserializeNBT(inventoryTag.copy());
            UsableHotbarSessionClient.filterActiveSourceSlot(inventory);
        });
    }

    public static void activateUsableItem(int hotbarSlot, boolean continuousUse, ItemStack stack) {
        activateUsableItem(hotbarSlot, -1, continuousUse, stack);
    }

    public static void activateUsableItem(int hotbarSlot, int sourceSlot, boolean continuousUse, ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isCreative() || minecraft.player.isSpectator()) {
            return;
        }
        ItemStack usableStack = stack == null ? ItemStack.EMPTY : stack.copy();
        boolean shouldClientApply = shouldClientApplyUsableHotbarCopy(usableStack);
        if (hotbarSlot >= 0 && hotbarSlot < 9 && !usableStack.isEmpty()) {
            usableStack.setCount(1);
            UsableHotbarSessionClient.start(hotbarSlot, sourceSlot, usableStack);
            if (shouldClientApply) {
                applyUsableHotbarItem(hotbarSlot, usableStack);
            }
        }
        minecraft.setScreen(null);
        if (sourceSlot >= 0) {
            minecraft.player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
                if (inventory.isValidMainSlot(sourceSlot)) {
                    inventory.setInventoryItem(sourceSlot, ItemStack.EMPTY);
                }
            });
        }
        if (shouldClientApply && hotbarSlot >= 0 && hotbarSlot < 9 && !usableStack.isEmpty()) {
            minecraft.execute(() -> applyUsableHotbarItem(hotbarSlot, usableStack));
        }
        minecraft.player.displayClientMessage(Component.literal("Right click to use"), true);
    }

    private static boolean shouldClientApplyUsableHotbarCopy(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getUseAnimation() == UseAnim.SPYGLASS;
    }

    private static void applyUsableHotbarItem(int hotbarSlot, ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isCreative() || minecraft.player.isSpectator()) {
            return;
        }
        if (hotbarSlot < 0 || hotbarSlot >= 9 || hotbarSlot >= minecraft.player.getInventory().items.size() || stack == null || stack.isEmpty()) {
            return;
        }
        ItemStack usableStack = stack.copy();
        usableStack.setCount(1);
        Inventory inventory = minecraft.player.getInventory();
        inventory.setItem(hotbarSlot, usableStack.copy());
        inventory.selected = hotbarSlot;
        inventory.setChanged();
        if (minecraft.player.connection != null) {
            minecraft.player.connection.send(new ServerboundSetCarriedItemPacket(hotbarSlot));
        }
    }
}
