package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.event.ScpInventoryMaintenanceEvents;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.item.ScpItemType;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.mcreator.scpadditions.equipment.HazmatSuitAccess;
import net.mcreator.scpadditions.equipment.HazmatSuitEvents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import com.bl4ues.scpadditions.compat.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class InventoryActionPacket {

    public static final String ACTION_DROP = "DROP";
    public static final String ACTION_USE = "USE";
    public static final String ACTION_EQUIP = "EQUIP";

    private static final int VANILLA_HOTBAR_START = 0;
    private static final int VANILLA_HOTBAR_END_EXCLUSIVE = 9;
    private static final int VANILLA_MAIN_START = 9;
    private static final int VANILLA_MAIN_END_EXCLUSIVE = 36;

    private final int slot;
    private final String action;

    public InventoryActionPacket(int slot, String action) {
        this.slot = slot;
        this.action = action == null ? "" : action;
    }

    public static void encode(InventoryActionPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.slot);
        buf.writeUtf(msg.action);
    }

    public static InventoryActionPacket decode(FriendlyByteBuf buf) {
        return new InventoryActionPacket(buf.readInt(), buf.readUtf());
    }

    public static void handle(InventoryActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (!ScpAdditionsModulesConfig.get().inventory.enabled) return;
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
                if (!inventory.isValidMainSlot(msg.slot)) {
                    ModNetwork.syncTo(player, inventory);
                    return;
                }

                switch (msg.action) {
                    case ACTION_DROP -> moveSlotToWorld(player, inventory, msg.slot);
                    case ACTION_USE -> useSlot(player, inventory, msg.slot);
                    case ACTION_EQUIP -> equipSlot(player, inventory, msg.slot);
                    default -> {
                    }
                }

                ModNetwork.syncTo(player, inventory);
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private static void moveSlotToWorld(ServerPlayer player, IScpInventory inventory, int slot) {
        ItemStack stack = inventory.getInventoryItem(slot).copy();
        ScpInventoryMaintenanceEvents.discardActiveUsableFromSourceSlot(player, slot, stack);
        stack = inventory.extractInventoryItem(slot);
        if (!stack.isEmpty()) player.drop(stack, false);
    }

    private static void useSlot(ServerPlayer player, IScpInventory inventory, int slot) {
        ItemStack stack = inventory.getInventoryItem(slot);
        if (stack.isEmpty()) return;

        ScpItemType type = ScpItemClassifier.getType(stack);
        if (type == ScpItemType.CONSUMABLE) {
            consumeSlot(player, inventory, slot, stack);
            return;
        }

        if (type == ScpItemType.USABLE || type == ScpItemType.PLACEABLE) {
            useUsableSlot(player, inventory, slot);
        }
    }

    private static void consumeSlot(ServerPlayer player, IScpInventory inventory, int slot, ItemStack stack) {
        // This method is only reached after the authoritative classifier has
        // returned CONSUMABLE, including explicit JSON rules.
        if (HazmatSuitAccess.isFullyEquipped(player)) {
            HazmatSuitEvents.showSealedMaskMessage(player);
            return;
        }

        UseAnim animation = stack.getUseAnimation();
        boolean hasVanillaUseResult = stack.isEdible() || animation == UseAnim.EAT || animation == UseAnim.DRINK;
        if (!hasVanillaUseResult) {
            inventory.removeInventoryItem(slot);
            return;
        }

        ItemStack usedStack = stack.copy();
        usedStack.setCount(1);
        ScpPickupRouter.stripNoMergeMarker(usedStack);

        player.swing(InteractionHand.MAIN_HAND, true);
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                animation == UseAnim.DRINK ? SoundEvents.GENERIC_DRINK : SoundEvents.GENERIC_EAT,
                SoundSource.PLAYERS,
                0.8F,
                0.9F + player.getRandom().nextFloat() * 0.2F
        );

        ItemStack result = usedStack.finishUsingItem(player.level(), player);
        stack.shrink(1);
        inventory.setInventoryItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);

        if (!result.isEmpty()) routeUseRemainder(player, inventory, result);
    }

    private static void useUsableSlot(ServerPlayer player, IScpInventory inventory, int slot) {
        ScpInventoryMaintenanceEvents.activateUsableSession(player, inventory, slot);
    }

    private static void routeUseRemainder(ServerPlayer player, IScpInventory inventory, ItemStack remainder) {
        ItemStack leftover = remainder.copy();
        ScpPickupRouter.stripNoMergeMarker(leftover);
        int accepted = ScpPickupRouter.accept(inventory, player, leftover);
        if (accepted > 0) leftover.shrink(accepted);
        if (!leftover.isEmpty()) player.drop(leftover, false);
    }

    private static void equipSlot(ServerPlayer player, IScpInventory inventory, int slot) {
        ItemStack stack = inventory.getInventoryItem(slot);
        if (stack.isEmpty()) return;

        Optional<ScpEquipmentSlot> equipmentSlot = ScpItemClassifier.getEquipmentSlot(stack);
        if (equipmentSlot.isEmpty()) return;

        ScpEquipmentSlot targetSlot = equipmentSlot.get();
        ItemStack newEquipment = inventory.extractInventoryItem(slot);
        ItemStack previousEquipment = getPreviousEquipmentForReplacement(player, inventory, targetSlot, newEquipment);

        inventory.setEquipment(targetSlot, newEquipment);
        syncVanillaEquipmentSlot(player, targetSlot, newEquipment);

        if (!previousEquipment.isEmpty()) inventory.setInventoryItem(slot, previousEquipment);
    }

    private static ItemStack getPreviousEquipmentForReplacement(ServerPlayer player, IScpInventory inventory, ScpEquipmentSlot targetSlot, ItemStack incomingStack) {
        ItemStack previousEquipment = inventory.getEquipment(targetSlot);
        if (targetSlot != ScpEquipmentSlot.ACCESSORY || !previousEquipment.isEmpty() || ScpItemClassifier.isAccessoryHand(incomingStack)) {
            return previousEquipment;
        }

        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty() && ScpItemClassifier.isAccessoryHand(offhand)) {
            ItemStack copy = offhand.copy();
            copy.setCount(1);
            return copy;
        }

        return previousEquipment;
    }

    public static void syncVanillaEquipmentSlot(ServerPlayer player, ScpEquipmentSlot slot, ItemStack stack) {
        if (slot == ScpEquipmentSlot.ACCESSORY) {
            syncAccessorySlot(player, stack);
            return;
        }

        EquipmentSlot vanillaSlot = getVanillaEquipmentSlot(slot);
        if (vanillaSlot != null) {
            player.setItemSlot(vanillaSlot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            return;
        }

        if (slot == ScpEquipmentSlot.WEAPON) syncMainInventoryMirror(player, slot, stack);
    }

    public static EquipmentSlot getVanillaEquipmentSlot(ScpEquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> EquipmentSlot.HEAD;
            case CHEST -> EquipmentSlot.CHEST;
            case LEGS -> EquipmentSlot.LEGS;
            case FEET -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    private static void syncAccessorySlot(ServerPlayer player, ItemStack stack) {
        if (player == null) return;

        Inventory inventory = player.getInventory();
        removeAllMirrorsForSlot(inventory, ScpEquipmentSlot.ACCESSORY);

        if (stack == null || stack.isEmpty()) {
            clearOffhandAccessory(player);
            ScpPickupRouter.syncVanillaInventory(player);
            return;
        }

        ItemStack copy = stack.copy();
        copy.setCount(1);
        player.setItemInHand(InteractionHand.OFF_HAND, copy);
        ScpPickupRouter.syncVanillaInventory(player);
    }

    private static void clearOffhandAccessory(ServerPlayer player) {
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
    }

    private static void syncMainInventoryMirror(ServerPlayer player, ScpEquipmentSlot slot, ItemStack stack) {
        if (player == null) return;
        Inventory inventory = player.getInventory();
        removeAllMirrorsForSlot(inventory, slot);
        if (stack == null || stack.isEmpty()) {
            ScpPickupRouter.syncVanillaInventory(player);
            return;
        }
        int preferred = inventory.selected;
        int mirrorSlot = findFirstEmpty(inventory, VANILLA_HOTBAR_START, VANILLA_HOTBAR_END_EXCLUSIVE);
        if (mirrorSlot < 0 && preferred >= VANILLA_HOTBAR_START && preferred < VANILLA_HOTBAR_END_EXCLUSIVE) mirrorSlot = preferred;
        if (mirrorSlot < 0) mirrorSlot = findFirstEmpty(inventory, VANILLA_MAIN_START, VANILLA_MAIN_END_EXCLUSIVE);
        if (mirrorSlot < 0) return;

        ItemStack copy = stack.copy();
        copy.setCount(1);
        inventory.setItem(mirrorSlot, copy);
        if (mirrorSlot < VANILLA_HOTBAR_END_EXCLUSIVE) inventory.selected = mirrorSlot;
        ScpPickupRouter.syncVanillaInventory(player);
    }

    private static void removeAllMirrorsForSlot(Inventory inventory, ScpEquipmentSlot slot) {
        for (int i = 0; i < inventory.items.size(); i++) {
            ItemStack stack = inventory.items.get(i);
            if (!stack.isEmpty() && ScpItemClassifier.getEquipmentSlot(stack).orElse(null) == slot) inventory.items.set(i, ItemStack.EMPTY);
        }
        inventory.setChanged();
    }

    private static int findFirstEmpty(Inventory inventory, int startInclusive, int endExclusive) {
        int end = Math.min(endExclusive, inventory.items.size());
        for (int i = startInclusive; i < end; i++) if (inventory.items.get(i).isEmpty()) return i;
        return -1;
    }
}
