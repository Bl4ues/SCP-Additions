package com.bl4ues.scpclassifieddirective.inventory.client;

import com.bl4ues.scpclassifieddirective.inventory.capability.ScpInventoryCapability;
import com.bl4ues.scpclassifieddirective.inventory.config.InventoryModuleRuntimeState;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpItemClassifier;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpItemType;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class ClientPacketHandlers {

    private ClientPacketHandlers() {
    }

    public static void showInventoryFullOverlay() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.isSpectator()) {
            InventoryFullOverlay.hide();
            return;
        }
        InventoryFullOverlay.show(minecraft.player != null && minecraft.player.isCreative());
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

    public static void activateUsableItem(int hotbarSlot, boolean continuousUse,
            ItemStack stack) {
        activateUsableItem(hotbarSlot, -1, continuousUse, stack);
    }

    public static void activateUsableItem(int hotbarSlot, int sourceSlot,
            boolean continuousUse, ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isCreative()
                || minecraft.player.isSpectator()) {
            return;
        }

        ItemStack activeStack = stack == null ? ItemStack.EMPTY : stack.copy();
        ScpItemType type = activeStack.isEmpty()
                ? ScpItemType.MISCELLANEOUS
                : ScpItemClassifier.getType(activeStack);
        boolean heldEquipment = !activeStack.isEmpty()
                && ScpItemClassifier.getEquipmentSlot(activeStack).isPresent();
        boolean placeable = type == ScpItemType.PLACEABLE || heldEquipment;

        if (hotbarSlot >= 0 && hotbarSlot < 9 && !activeStack.isEmpty()) {
            activeStack.setCount(1);
            if (placeable) {
                PlaceableHotbarSessionClient.start(hotbarSlot, sourceSlot,
                        activeStack);
            } else {
                UsableHotbarSessionClient.start(hotbarSlot, sourceSlot,
                        activeStack);
            }

            // The server remains authoritative, but the client also applies and
            // selects the mirror immediately. Waiting for a later vanilla slot
            // sync allowed the inventory screen to close while the previously
            // selected PLACEABLE remained visibly in hand.
            applyHotbarItem(hotbarSlot, activeStack);
        }

        minecraft.setScreen(null);
        if (sourceSlot >= 0) {
            minecraft.player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
                if (inventory.isValidMainSlot(sourceSlot)) {
                    inventory.setInventoryItem(sourceSlot, ItemStack.EMPTY);
                }
            });
        }
        if (hotbarSlot >= 0 && hotbarSlot < 9 && !activeStack.isEmpty()) {
            minecraft.execute(() -> applyHotbarItem(hotbarSlot, activeStack));
        }

        Component prompt;
        if (heldEquipment) {
            prompt = Component.literal("Item held in hand");
        } else {
            prompt = Component.literal(placeable
                    ? "Right click to place" : "Right click to use");
        }
        if (InventoryModuleRuntimeState.actionBarsRobotoForClient()) {
            prompt = ScpFonts.roboto(prompt);
        }
        minecraft.player.displayClientMessage(prompt, true);
    }

    private static void applyHotbarItem(int hotbarSlot, ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isCreative()
                || minecraft.player.isSpectator()) {
            return;
        }
        if (hotbarSlot < 0 || hotbarSlot >= 9
                || hotbarSlot >= minecraft.player.getInventory().items.size()
                || stack == null || stack.isEmpty()) {
            return;
        }
        ItemStack activeStack = stack.copy();
        activeStack.setCount(1);
        Inventory inventory = minecraft.player.getInventory();
        inventory.setItem(hotbarSlot, activeStack.copy());
        inventory.selected = hotbarSlot;
        inventory.setChanged();
        if (minecraft.player.connection != null) {
            minecraft.player.connection.send(
                    new ServerboundSetCarriedItemPacket(hotbarSlot));
        }
    }
}
