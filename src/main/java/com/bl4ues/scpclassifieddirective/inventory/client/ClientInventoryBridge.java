package com.bl4ues.scpclassifieddirective.inventory.client;

import com.bl4ues.scpclassifieddirective.inventory.item.ScpEquipmentSlot;
import com.bl4ues.scpclassifieddirective.inventory.network.DocumentActionPacket;
import com.bl4ues.scpclassifieddirective.inventory.network.EquipmentActionPacket;
import com.bl4ues.scpclassifieddirective.inventory.network.InventoryActionPacket;
import com.bl4ues.scpclassifieddirective.inventory.network.InventoryMovePacket;
import com.bl4ues.scpclassifieddirective.inventory.network.KeyActionPacket;
import com.bl4ues.scpclassifieddirective.inventory.network.MainUseActionPacket;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;

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