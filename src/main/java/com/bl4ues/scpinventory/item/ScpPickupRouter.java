package com.bl4ues.scpinventory.item;

import com.bl4ues.scpinventory.capability.IScpInventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * First integration checkpoint of the standalone pickup router.
 *
 * Real items are moved into capability storage. No vanilla mirror or USABLE
 * session is created here; those sensitive systems are layered on later. COIN
 * deliberately remains capability-only, matching SCP-294's exclusive source
 * policy when the custom inventory toggle is enabled.
 */
public final class ScpPickupRouter {
    public static final String NO_MERGE_TAG = "ScpInventoryNoMerge";
    public static final String USABLE_SESSION_TAG = "ScpInventoryUsableSession";
    public static final String USABLE_START_TICK_TAG = "ScpInventoryUsableStartTick";
    public static final String COIN_MIRROR_TAG = "ScpInventoryCoinMirror";
    public static final String HARMFUL_MIRROR_TAG = "ScpInventoryHarmfulMirror";

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
                || stack.getTag().getBoolean(HARMFUL_MIRROR_TAG);
    }

    public static void stripInternalMarkers(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) return;
        stack.getTag().remove(NO_MERGE_TAG);
        stack.getTag().remove(USABLE_SESSION_TAG);
        stack.getTag().remove(USABLE_START_TICK_TAG);
        stack.getTag().remove(COIN_MIRROR_TAG);
        stack.getTag().remove(HARMFUL_MIRROR_TAG);
        if (stack.getTag().isEmpty()) stack.setTag(null);
    }
}
