package com.bl4ues.scpclassifieddirective.inventory.network;

import com.bl4ues.scpclassifieddirective.effect.Scp714ExposureManager;
import com.bl4ues.scpclassifieddirective.inventory.capability.IScpInventory;
import com.bl4ues.scpclassifieddirective.inventory.capability.ScpInventoryCapability;
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
                if (type == ScpItemType.USABLE || type == ScpItemType.PLACEABLE) {
                    activateHotbarSession(player, inventory, msg.slot, stack);
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

                    if (isVanillaConsumable(stack)) consume(player, inventory, msg.slot, stack);
                    else activateHotbarSession(player, inventory, msg.slot, stack);
                    ModNetwork.syncTo(player, inventory);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * A hotbar category is intentionally singular. Selecting another stack of
     * the same category therefore means replacement, not "re-select whatever was
     * already active". Return the old authoritative stack first, then start the
     * requested session so the incoming item owns the hotbar mirror.
     */
    private static void activateHotbarSession(ServerPlayer player,
            IScpInventory inventory, int sourceSlot, ItemStack requestedStack) {
        ItemStack activeStack = inventory.getActiveUsable();
        if (!activeStack.isEmpty()
                && ScpItemClassifier.getType(activeStack)
                == ScpItemClassifier.getType(requestedStack)) {
            int activeHotbarSlot = findTrackedHotbarSlot(player);
            if (activeHotbarSlot >= 0) {
                ScpInventoryMaintenanceEvents.returnTrackedUsableSession(
                        player, activeHotbarSlot);
            }
        }

        ScpInventoryMaintenanceEvents.activateUsableSession(
                player, inventory, sourceSlot);
    }

    private static int findTrackedHotbarSlot(ServerPlayer player) {
        if (player == null) return -1;
        int end = Math.min(VANILLA_HOTBAR_SIZE,
                player.getInventory().items.size());
        for (int slot = 0; slot < end; slot++) {
            if (ScpPickupRouter.isUsableSession(
                    player.getInventory().items.get(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isVanillaConsumable(ItemStack stack) {
        UseAnim animation = stack.getUseAnimation();
        return stack.isEdible() || animation == UseAnim.EAT || animation == UseAnim.DRINK;
    }

    private static void consume(ServerPlayer player, IScpInventory inventory, int slot, ItemStack stack) {
        UseAnim animation = stack.getUseAnimation();
        ItemStack usedStack = stack.copy();
        usedStack.setCount(1);
        ScpPickupRouter.stripNoMergeMarker(usedStack);

        player.swing(InteractionHand.MAIN_HAND, true);
        InventoryInteractionSoundFeedback.consumed(player, usedStack);

        HungerSystemEvents.healFromFood(player, usedStack);
        ItemStack result = usedStack.finishUsingItem(player.level(), player);
        stack.shrink(1);
        inventory.setInventoryItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);

        if (!result.isEmpty()) routeUseRemainder(player, inventory, result);
    }

    private static void routeUseRemainder(ServerPlayer player, IScpInventory inventory, ItemStack remainder) {
        ItemStack leftover = remainder.copy();
        ScpPickupRouter.stripNoMergeMarker(leftover);
        int accepted = ScpPickupRouter.accept(inventory, player, leftover);
        if (accepted > 0) leftover.shrink(accepted);
        if (!leftover.isEmpty()) player.drop(leftover, false);
    }
}