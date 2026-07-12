package net.mcreator.scpadditions.inventory;

import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.item.ScpItemType;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.network.NetworkEvent;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

import java.util.Optional;
import java.util.function.Supplier;

/** First complete server-side action layer for the integrated inventory screen. */
public final class ScpInventoryActionPacket {
    public enum Action {
        DROP_MAIN,
        DROP_KEY,
        USE_MAIN,
        EQUIP_MAIN,
        UNEQUIP
    }

    private final Action action;
    private final int index;
    private final ScpEquipmentSlot equipmentSlot;

    public ScpInventoryActionPacket(Action action, int index) {
        this(action, index, null);
    }

    public ScpInventoryActionPacket(Action action, ScpEquipmentSlot slot) {
        this(action, -1, slot);
    }

    private ScpInventoryActionPacket(Action action, int index,
            ScpEquipmentSlot equipmentSlot) {
        this.action = action == null ? Action.DROP_MAIN : action;
        this.index = index;
        this.equipmentSlot = equipmentSlot;
    }

    public static void encode(ScpInventoryActionPacket message,
            FriendlyByteBuf buffer) {
        buffer.writeEnum(message.action);
        buffer.writeVarInt(message.index + 1);
        buffer.writeBoolean(message.equipmentSlot != null);
        if (message.equipmentSlot != null) buffer.writeEnum(message.equipmentSlot);
    }

    public static ScpInventoryActionPacket decode(FriendlyByteBuf buffer) {
        Action action = buffer.readEnum(Action.class);
        int index = buffer.readVarInt() - 1;
        ScpEquipmentSlot slot = buffer.readBoolean()
                ? buffer.readEnum(ScpEquipmentSlot.class) : null;
        return new ScpInventoryActionPacket(action, index, slot);
    }

    public static void handle(ScpInventoryActionPacket message,
            Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.isCreative() || player.isSpectator()
                    || !ScpAdditionsModulesConfig.get().inventory.enabled) return;

            player.getCapability(ScpInventoryCapability.INSTANCE)
                    .ifPresent(inventory -> {
                        switch (message.action) {
                            case DROP_MAIN -> dropMain(player, inventory, message.index);
                            case DROP_KEY -> dropKey(player, inventory, message.index);
                            case USE_MAIN -> useMain(player, inventory, message.index);
                            case EQUIP_MAIN -> equipMain(player, inventory, message.index);
                            case UNEQUIP -> unequip(player, inventory,
                                    message.equipmentSlot);
                        }
                        ScpInventoryNetwork.sync(player);
                    });
        });
        context.setPacketHandled(true);
    }

    private static void dropMain(ServerPlayer player, IScpInventory inventory,
            int index) {
        if (!inventory.isValidMainSlot(index)) return;
        ItemStack stack = inventory.extractInventoryItem(index);
        ScpPickupRouter.stripInternalMarkers(stack);
        if (!stack.isEmpty()) player.drop(stack, false);
    }

    private static void dropKey(ServerPlayer player, IScpInventory inventory,
            int index) {
        ItemStack stack = inventory.extractKeyItem(index);
        ScpPickupRouter.stripInternalMarkers(stack);
        if (!stack.isEmpty()) player.drop(stack, false);
    }

    private static void useMain(ServerPlayer player, IScpInventory inventory,
            int index) {
        if (!inventory.isValidMainSlot(index)) return;
        ItemStack stored = inventory.getInventoryItem(index);
        if (stored.isEmpty()) return;
        ScpItemType type = ScpItemClassifier.getType(stored);
        if (type != ScpItemType.CONSUMABLE) {
            // USABLE needs the protected hotbar-session lifecycle; do not create
            // a duplicate or eat the source item until that lifecycle owns it.
            return;
        }

        ItemStack one = stored.copy();
        one.setCount(1);
        ScpPickupRouter.stripInternalMarkers(one);
        UseAnim animation = one.getUseAnimation();
        if (!one.isEdible() && animation != UseAnim.EAT
                && animation != UseAnim.DRINK) return;

        ItemStack result = one.finishUsingItem(player.level(), player);
        inventory.removeInventoryItem(index);
        if (!result.isEmpty() && !ItemStack.isSameItemSameTags(result, one)) {
            int accepted = ScpPickupRouter.accept(inventory, player, result);
            if (accepted < result.getCount()) {
                ItemStack remainder = result.copy();
                remainder.shrink(accepted);
                if (!remainder.isEmpty()) player.drop(remainder, false);
            }
        }
        player.swing(InteractionHand.MAIN_HAND, true);
    }

    private static void equipMain(ServerPlayer player, IScpInventory inventory,
            int index) {
        if (!inventory.isValidMainSlot(index)) return;
        ItemStack candidate = inventory.getInventoryItem(index);
        Optional<ScpEquipmentSlot> resolved =
                ScpItemClassifier.getEquipmentSlot(candidate);
        if (candidate.isEmpty() || resolved.isEmpty()) return;

        ScpEquipmentSlot slot = resolved.get();
        ItemStack incoming = inventory.extractInventoryItem(index);
        ItemStack previous = inventory.extractEquipment(slot);
        removeMirror(player, slot);
        inventory.setEquipment(slot, incoming);
        installMirror(player, slot, incoming);
        if (!previous.isEmpty()) inventory.setInventoryItem(index, previous);
    }

    private static void unequip(ServerPlayer player, IScpInventory inventory,
            ScpEquipmentSlot slot) {
        if (slot == null) return;
        ItemStack equipped = inventory.extractEquipment(slot);
        removeMirror(player, slot);
        if (equipped.isEmpty()) return;
        ScpPickupRouter.stripInternalMarkers(equipped);
        int accepted = inventory.addInventoryItems(equipped);
        if (accepted < equipped.getCount()) {
            ItemStack remainder = equipped.copy();
            remainder.shrink(accepted);
            if (!remainder.isEmpty()) player.drop(remainder, false);
        }
    }

    private static void installMirror(ServerPlayer player,
            ScpEquipmentSlot slot, ItemStack source) {
        if (source == null || source.isEmpty()) return;
        ItemStack mirror = source.copy();
        mirror.setCount(1);
        ScpPickupRouter.markEquipmentMirror(mirror, slot);
        EquipmentSlot vanilla = vanillaSlot(slot);
        if (vanilla != null) {
            player.setItemSlot(vanilla, mirror);
        } else if (slot == ScpEquipmentSlot.ACCESSORY) {
            player.setItemInHand(InteractionHand.OFF_HAND, mirror);
        } else if (slot == ScpEquipmentSlot.WEAPON) {
            Inventory vanillaInventory = player.getInventory();
            vanillaInventory.setItem(vanillaInventory.selected, mirror);
            vanillaInventory.setChanged();
        }
    }

    private static void removeMirror(ServerPlayer player,
            ScpEquipmentSlot slot) {
        EquipmentSlot vanilla = vanillaSlot(slot);
        if (vanilla != null
                && ScpPickupRouter.isEquipmentMirror(player.getItemBySlot(vanilla), slot)) {
            player.setItemSlot(vanilla, ItemStack.EMPTY);
        }
        if (slot == ScpEquipmentSlot.ACCESSORY
                && ScpPickupRouter.isEquipmentMirror(player.getOffhandItem(), slot)) {
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.items.size(); i++) {
            if (ScpPickupRouter.isEquipmentMirror(inventory.items.get(i), slot))
                inventory.items.set(i, ItemStack.EMPTY);
        }
        inventory.setChanged();
    }

    private static EquipmentSlot vanillaSlot(ScpEquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> EquipmentSlot.HEAD;
            case CHEST -> EquipmentSlot.CHEST;
            case LEGS -> EquipmentSlot.LEGS;
            case FEET -> EquipmentSlot.FEET;
            default -> null;
        };
    }
}
