package com.bl4ues.scpclassifieddirective.inventory.network;

import com.bl4ues.scpclassifieddirective.effect.Scp714ExposureManager;
import com.bl4ues.scpclassifieddirective.inventory.capability.IScpInventory;
import com.bl4ues.scpclassifieddirective.inventory.capability.ScpInventoryCapability;
import com.bl4ues.scpclassifieddirective.inventory.event.PlaceableHotbarSessionEvents;
import com.bl4ues.scpclassifieddirective.inventory.event.ScpInventoryMaintenanceEvents;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpItemClassifier;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpItemType;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpPickupRouter;
import com.bl4ues.scpclassifieddirective.inventory.sound.InventoryInteractionSoundFeedback;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;
import com.bl4ues.scpclassifieddirective.equipment.HazmatSuitAccess;
import com.bl4ues.scpclassifieddirective.equipment.HazmatSuitEvents;
import com.bl4ues.scpclassifieddirective.vitals.HungerSystemEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MainUseActionPacket {

    private static final int VANILLA_HOTBAR_SIZE = 9;

    private final int slot;

    public MainUseActionPacket(int slot) {
        this.slot = slot;
    }

    public static void encode(MainUseActionPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.slot);
    }

    public static MainUseActionPacket decode(FriendlyByteBuf buf) {
        return new MainUseActionPacket(buf.readInt());
    }

    public static void handle(MainUseActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (!ScpClassifiedDirectiveModulesConfig.get().inventory.enabled) return;
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (Scp714ExposureManager.isControlsLocked(player)) {
                player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(
                        inventory -> ModNetwork.syncTo(player, inventory));
                return;
            }

            player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
                if (!inventory.isValidMainSlot(msg.slot)) {
                    ModNetwork.syncTo(player, inventory);
                    return;
                }

                ItemStack stack = inventory.getInventoryItem(msg.slot);
                if (stack.isEmpty()) {
                    ModNetwork.syncTo(player, inventory);
                    return;
                }

                ScpItemType type = ScpItemClassifier.getType(stack);
                if (type == ScpItemType.PLACEABLE) {
                    PlaceableHotbarSessionEvents.activatePlaceableSession(
                            player, inventory, msg.slot);
                    return;
                }
                if (type == ScpItemType.USABLE) {
                    activateUsableHotbarSession(player, inventory, msg.slot);
                    return;
                }

                if (type == ScpItemType.CONSUMABLE) {
                    // The primary-use button has its own packet, separate from
                    // InventoryActionPacket. Enforce the sealed mask here before
                    // either direct consumption or a custom usable session can
                    // remove the authoritative SCP Inventory stack.
                    if (HazmatSuitAccess.isFullyEquipped(player)) {
                        HazmatSuitEvents.showSealedMaskMessage(player);
                        ModNetwork.syncTo(player, inventory);
                        return;
                    }

                    if (isVanillaConsumable(stack)) {
                        consume(player, inventory, msg.slot, stack);
                    } else {
                        activateUsableHotbarSession(player, inventory, msg.slot);
                    }
                    ModNetwork.syncTo(player, inventory);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * USABLE has one slot of its own. Replacing that slot must never evict a
     * PLACEABLE entry, because the custom hotbar intentionally allows one item
     * from each transient category to coexist.
     *
     * Package-private so InventoryActionPacket uses the exact same replacement
     * path instead of maintaining a subtly different second implementation.
     */
    static void activateUsableHotbarSession(ServerPlayer player,
            IScpInventory inventory, int sourceSlot) {
        ItemStack activeStack = inventory.getActiveUsable();
        if (!activeStack.isEmpty()) {
            int activeHotbarSlot = findTrackedUsableHotbarSlot(player,
                    activeStack);
            if (activeHotbarSlot >= 0) {
                // Preserve normal source-slot bookkeeping when the in-memory
                // session still exists. Only reconstruct it after reconnect or
                // reload when the original tracking map is actually gone.
                if (!ScpInventoryMaintenanceEvents.returnTrackedUsableSession(
                        player, activeHotbarSlot)) {
                    ScpInventoryMaintenanceEvents.trackUsableSession(player,
                            activeHotbarSlot, activeStack, -1);
                    ScpInventoryMaintenanceEvents.returnTrackedUsableSession(
                            player, activeHotbarSlot);
                }
            } else {
                // No mirror survived. Do not let a stale ActiveUsable block the
                // newly requested item forever; return it to SCP Inventory first.
                ItemStack stale = inventory.extractActiveUsable();
                restoreOrDrop(player, inventory, stale);
            }
        }

        ScpInventoryMaintenanceEvents.activateUsableSession(
                player, inventory, sourceSlot);
    }

    private static int findTrackedUsableHotbarSlot(ServerPlayer player,
            ItemStack expected) {
        if (player == null) return -1;
        int end = Math.min(VANILLA_HOTBAR_SIZE,
                player.getInventory().items.size());
        for (int slot = 0; slot < end; slot++) {
            ItemStack candidate = player.getInventory().items.get(slot);
            if (candidate.isEmpty()
                    || ScpItemClassifier.getType(candidate) == ScpItemType.PLACEABLE) {
                continue;
            }
            if (ScpPickupRouter.isUsableSession(candidate)
                    || isSameSessionItem(candidate, expected)) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isSameSessionItem(ItemStack left, ItemStack right) {
        if (left == null || left.isEmpty() || right == null || right.isEmpty()) {
            return false;
        }
        ItemStack normalizedLeft = left.copy();
        ItemStack normalizedRight = right.copy();
        normalizedLeft.setCount(1);
        normalizedRight.setCount(1);
        ScpPickupRouter.stripUsableSession(normalizedLeft);
        ScpPickupRouter.stripUsableSession(normalizedRight);
        ScpPickupRouter.stripNoMergeMarker(normalizedLeft);
        ScpPickupRouter.stripNoMergeMarker(normalizedRight);
        return ItemStack.isSameItemSameTags(normalizedLeft, normalizedRight);
    }

    private static void restoreOrDrop(ServerPlayer player,
            IScpInventory inventory, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        ItemStack restored = stack.copy();
        restored.setCount(1);
        ScpPickupRouter.stripUsableSession(restored);
        ScpPickupRouter.stripNoMergeMarker(restored);
        if (!inventory.addInventoryItem(restored)) {
            player.drop(restored, false);
        }
    }

    private static boolean isVanillaConsumable(ItemStack stack) {
        UseAnim animation = stack.getUseAnimation();
        return stack.isEdible() || animation == UseAnim.EAT
                || animation == UseAnim.DRINK;
    }

    private static void consume(ServerPlayer player, IScpInventory inventory,
            int slot, ItemStack stack) {
        ItemStack usedStack = stack.copy();
        usedStack.setCount(1);
        ScpPickupRouter.stripNoMergeMarker(usedStack);

        player.swing(InteractionHand.MAIN_HAND, true);
        InventoryInteractionSoundFeedback.consumed(player, usedStack);

        HungerSystemEvents.healFromFood(player, usedStack);
        ItemStack result = usedStack.finishUsingItem(player.level(), player);
        stack.shrink(1);
        inventory.setInventoryItem(slot,
                stack.isEmpty() ? ItemStack.EMPTY : stack);

        if (!result.isEmpty()) routeUseRemainder(player, inventory, result);
    }

    private static void routeUseRemainder(ServerPlayer player,
            IScpInventory inventory, ItemStack remainder) {
        ItemStack leftover = remainder.copy();
        ScpPickupRouter.stripNoMergeMarker(leftover);
        int accepted = ScpPickupRouter.accept(inventory, player, leftover);
        if (accepted > 0) leftover.shrink(accepted);
        if (!leftover.isEmpty()) player.drop(leftover, false);
    }
}
