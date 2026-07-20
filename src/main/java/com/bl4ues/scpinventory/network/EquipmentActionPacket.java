package com.bl4ues.scpinventory.network;

import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.bl4ues.scpadditions.compat.network.NetworkEvent;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;
import net.mcreator.scpadditions.equipment.HazmatSuitAccess;
import net.mcreator.scpadditions.equipment.HazmatSuitManager;

import java.util.Optional;
import java.util.function.Supplier;

public class EquipmentActionPacket {

    public static final String ACTION_UNEQUIP = "UNEQUIP";
    public static final String ACTION_DROP = "DROP";

    private final String slotName;
    private final String action;

    public EquipmentActionPacket(String slotName, String action) {
        this.slotName = slotName == null ? "" : slotName;
        this.action = action == null ? "" : action;
    }

    public static void encode(EquipmentActionPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.slotName);
        buf.writeUtf(msg.action);
    }

    public static EquipmentActionPacket decode(FriendlyByteBuf buf) {
        return new EquipmentActionPacket(buf.readUtf(), buf.readUtf());
    }

    public static void handle(EquipmentActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (!ScpAdditionsModulesConfig.get().inventory.enabled) return;
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            Optional<ScpEquipmentSlot> slot = ScpEquipmentSlot.fromName(msg.slotName);
            if (slot.isEmpty()) {
                return;
            }

            player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
                ItemStack equipped = inventory.getEquipment(slot.get());
                if (HazmatSuitAccess.isInternalPiece(equipped)
                        || (HazmatSuitAccess.isFullyEquipped(player)
                                && isHazmatArmorSlot(slot.get()))) {
                    HazmatSuitManager.requestUnequip(player);
                    ModNetwork.syncTo(player, inventory);
                    return;
                }

                switch (msg.action) {
                    case ACTION_UNEQUIP -> {
                        equipped = inventory.extractEquipment(slot.get());
                        if (equipped.isEmpty()) {
                            return;
                        }

                        InventoryActionPacket.syncVanillaEquipmentSlot(
                                player, slot.get(), ItemStack.EMPTY);
                        if (!inventory.addInventoryItem(equipped)) {
                            // Survival never uses vanilla storage as overflow for
                            // SCP Inventory equipment. A full custom inventory
                            // turns the unequipped stack into a world drop.
                            player.drop(equipped, false);
                            ModNetwork.showInventoryFull(player);
                        }
                    }
                    case ACTION_DROP -> {
                        equipped = inventory.extractEquipment(slot.get());
                        if (!equipped.isEmpty()) {
                            InventoryActionPacket.syncVanillaEquipmentSlot(player, slot.get(), ItemStack.EMPTY);
                            player.drop(equipped, false);
                        }
                    }
                    default -> {
                    }
                }

                ModNetwork.syncTo(player, inventory);
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private static boolean isHazmatArmorSlot(ScpEquipmentSlot slot) {
        return slot == ScpEquipmentSlot.HEAD
                || slot == ScpEquipmentSlot.CHEST
                || slot == ScpEquipmentSlot.LEGS
                || slot == ScpEquipmentSlot.FEET;
    }
}
