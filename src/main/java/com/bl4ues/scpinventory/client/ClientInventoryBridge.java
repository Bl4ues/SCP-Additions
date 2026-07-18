package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.network.CraftingActionPacket;
import com.bl4ues.scpinventory.network.DocumentActionPacket;
import com.bl4ues.scpinventory.network.EquipmentActionPacket;
import com.bl4ues.scpinventory.network.InventoryActionPacket;
import com.bl4ues.scpinventory.network.InventoryMovePacket;
import com.bl4ues.scpinventory.network.KeyActionPacket;
import com.bl4ues.scpinventory.network.MainUseActionPacket;
import com.bl4ues.scpinventory.network.ModNetwork;
import com.bl4ues.scpinventory.network.RequestCraftingStatePacket;
import net.minecraft.resources.ResourceLocation;

public final class ClientInventoryBridge {

    private ClientInventoryBridge() {
    }

    public static void perform(int slot, String name) {
        if (InventoryActionPacket.ACTION_USE.equals(name)) {
            ModNetwork.CHANNEL.sendToServer(new MainUseActionPacket(slot));
            return;
        }
        if (InventoryActionPacket.ACTION_DROP.equals(name)) {
            UsableHotbarSessionClient.discardIfSourceSlot(slot);
        }
        ModNetwork.CHANNEL.sendToServer(new InventoryActionPacket(slot, name));
    }

    public static void performKey(int index, String name) {
        ModNetwork.CHANNEL.sendToServer(new KeyActionPacket(index, name));
    }

    public static void performDocument(int index, String name) {
        ModNetwork.CHANNEL.sendToServer(new DocumentActionPacket(index, name));
    }

    public static void performEquipment(ScpEquipmentSlot slot, String name) {
        if (slot == null) return;
        ModNetwork.CHANNEL.sendToServer(new EquipmentActionPacket(slot.name(), name));
    }

    public static void requestCraftingState() {
        ModNetwork.CHANNEL.sendToServer(new RequestCraftingStatePacket());
    }

    public static void moveMainToCraftingGrid(int sourceIndex, int gridSlot) {
        sendCrafting(CraftingActionPacket.MOVE_MAIN_TO_GRID,
                sourceIndex, gridSlot, null);
    }

    public static void moveCraftingGridToMain(int gridSlot, int targetIndex) {
        sendCrafting(CraftingActionPacket.MOVE_GRID_TO_MAIN,
                gridSlot, targetIndex, null);
    }

    public static void moveCraftingGridToGrid(int sourceSlot, int targetSlot) {
        sendCrafting(CraftingActionPacket.MOVE_GRID_TO_GRID,
                sourceSlot, targetSlot, null);
    }

    public static void autoFillCraftingRecipe(ResourceLocation recipeId) {
        sendCrafting(CraftingActionPacket.AUTO_FILL, -1, -1, recipeId);
    }

    public static void craftPortableGrid() {
        sendCrafting(CraftingActionPacket.CRAFT, -1, -1, null);
    }

    public static void togglePinnedCraftingRecipe(ResourceLocation recipeId) {
        sendCrafting(CraftingActionPacket.TOGGLE_PIN, -1, -1, recipeId);
    }

    private static void sendCrafting(int action, int source, int target,
                                     ResourceLocation recipeId) {
        ModNetwork.CHANNEL.sendToServer(new CraftingActionPacket(
                action, source, target, recipeId));
    }

    public static void moveMainToMain(int sourceIndex, int targetIndex) {
        ModNetwork.CHANNEL.sendToServer(new InventoryMovePacket(
                InventoryMovePacket.PLACE_MAIN, sourceIndex, "",
                InventoryMovePacket.PLACE_MAIN, targetIndex, ""));
    }

    public static void moveMainToEquipment(int sourceIndex,
                                           ScpEquipmentSlot targetSlot) {
        if (targetSlot == null) return;
        ModNetwork.CHANNEL.sendToServer(new InventoryMovePacket(
                InventoryMovePacket.PLACE_MAIN, sourceIndex, "",
                InventoryMovePacket.PLACE_EQUIPMENT, -1, targetSlot.name()));
    }

    public static void moveMainToWorld(int sourceIndex) {
        ModNetwork.CHANNEL.sendToServer(new InventoryMovePacket(
                InventoryMovePacket.PLACE_MAIN, sourceIndex, "",
                InventoryMovePacket.PLACE_WORLD, -1, ""));
    }

    public static void moveEquipmentToMain(ScpEquipmentSlot sourceSlot,
                                           int targetIndex) {
        if (sourceSlot == null) return;
        ModNetwork.CHANNEL.sendToServer(new InventoryMovePacket(
                InventoryMovePacket.PLACE_EQUIPMENT, -1, sourceSlot.name(),
                InventoryMovePacket.PLACE_MAIN, targetIndex, ""));
    }

    public static void moveEquipmentToEquipment(ScpEquipmentSlot sourceSlot,
                                                ScpEquipmentSlot targetSlot) {
        if (sourceSlot == null || targetSlot == null) return;
        ModNetwork.CHANNEL.sendToServer(new InventoryMovePacket(
                InventoryMovePacket.PLACE_EQUIPMENT, -1, sourceSlot.name(),
                InventoryMovePacket.PLACE_EQUIPMENT, -1, targetSlot.name()));
    }

    public static void moveEquipmentToWorld(ScpEquipmentSlot sourceSlot) {
        if (sourceSlot == null) return;
        ModNetwork.CHANNEL.sendToServer(new InventoryMovePacket(
                InventoryMovePacket.PLACE_EQUIPMENT, -1, sourceSlot.name(),
                InventoryMovePacket.PLACE_WORLD, -1, ""));
    }
}
