package com.bl4ues.scpinventory.item;

import com.bl4ues.scpinventory.capability.IScpInventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative router for real custom-inventory items and tagged mirrors. */
public final class ScpPickupRouter {
    public static final String NO_MERGE_TAG = "ScpInventoryNoMerge";
    public static final String USABLE_SESSION_TAG = "ScpInventoryUsableSession";
    public static final String USABLE_START_TICK_TAG = "ScpInventoryUsableStartTick";
    public static final String COIN_MIRROR_TAG = "ScpInventoryCoinMirror";
    public static final String HARMFUL_MIRROR_TAG = "ScpInventoryHarmfulMirror";
    public static final String EQUIPMENT_MIRROR_TAG = "ScpInventoryEquipmentMirror";
    public static final String EQUIPMENT_SLOT_TAG = "ScpInventoryEquipmentSlot";

    private ScpPickupRouter() {
    }

    public static int accept(IScpInventory inventory, ServerPlayer player,
            ItemStack stack) {
        if (inventory == null || player == null || stack == null || stack.isEmpty()
                || player.isCreative() || player.isSpectator()
                || isUsableSession(stack) || isInternalMirror(stack)) {
            return 0;
        }

        ItemStack clean = stack.copy();
        stripInternalMarkers(clean);
        ScpItemType type = ScpItemClassifier.getType(clean);

        return switch (type) {
            case KEY -> acceptKeys(inventory, clean);
            case CODEX -> acceptDocuments(inventory, clean);
            default -> inventory.addInventoryItems(clean);
        };
    }

    private static int acceptKeys(IScpInventory inventory, ItemStack stack) {
        int accepted = 0;
        for (int count = 0; count < stack.getCount(); count++) {
            ItemStack single = stack.copy();
            single.setCount(1);
            if (!inventory.addKeyItem(single)) break;
            accepted++;
        }
        return accepted;
    }

    private static int acceptDocuments(IScpInventory inventory, ItemStack stack) {
        int accepted = 0;
        for (int count = 0; count < stack.getCount(); count++) {
            ItemStack single = stack.copy();
            single.setCount(1);
            if (!inventory.addDocumentItem(single)) break;
            accepted++;
        }
        return accepted;
    }

    public static boolean isUsableSession(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.hasTag()
                && stack.getTag().getBoolean(USABLE_SESSION_TAG);
    }

    public static boolean isInternalMirror(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) return false;
        return stack.getTag().getBoolean(COIN_MIRROR_TAG)
                || stack.getTag().getBoolean(HARMFUL_MIRROR_TAG)
                || stack.getTag().getBoolean(EQUIPMENT_MIRROR_TAG);
    }

    public static void markEquipmentMirror(ItemStack stack,
            ScpEquipmentSlot slot) {
        if (stack == null || stack.isEmpty() || slot == null) return;
        stack.getOrCreateTag().putBoolean(EQUIPMENT_MIRROR_TAG, true);
        stack.getOrCreateTag().putString(EQUIPMENT_SLOT_TAG, slot.name());
    }

    public static boolean isEquipmentMirror(ItemStack stack,
            ScpEquipmentSlot slot) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()
                || !stack.getTag().getBoolean(EQUIPMENT_MIRROR_TAG)) return false;
        return slot == null || slot.name().equals(
                stack.getTag().getString(EQUIPMENT_SLOT_TAG));
    }

    public static void stripInternalMarkers(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) return;
        stack.getTag().remove(NO_MERGE_TAG);
        stack.getTag().remove(USABLE_SESSION_TAG);
        stack.getTag().remove(USABLE_START_TICK_TAG);
        stack.getTag().remove(COIN_MIRROR_TAG);
        stack.getTag().remove(HARMFUL_MIRROR_TAG);
        stack.getTag().remove(EQUIPMENT_MIRROR_TAG);
        stack.getTag().remove(EQUIPMENT_SLOT_TAG);
        if (stack.getTag().isEmpty()) stack.setTag(null);
    }
}
