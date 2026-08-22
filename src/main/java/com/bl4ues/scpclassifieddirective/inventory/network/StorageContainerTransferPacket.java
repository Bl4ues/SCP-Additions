package com.bl4ues.scpclassifieddirective.inventory.network;

import com.bl4ues.scpclassifieddirective.inventory.capability.IScpInventory;
import com.bl4ues.scpclassifieddirective.inventory.capability.ScpInventoryCapability;
import com.bl4ues.scpclassifieddirective.inventory.container.StorageContainerSupport;
import com.bl4ues.scpclassifieddirective.inventory.event.ScpInventoryMaintenanceEvents;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpPickupRouter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;

import java.util.List;
import java.util.function.Supplier;

/**
 * Server-authoritative transfer bridge between the SCP capability inventory and
 * an already-open plain storage menu.
 */
public final class StorageContainerTransferPacket {
    public static final int DIRECTION_CONTAINER_TO_BACKPACK = 0;
    public static final int DIRECTION_BACKPACK_TO_CONTAINER = 1;

    public static final int SECTION_MAIN = 0;
    public static final int SECTION_KEYS = 1;
    public static final int SECTION_CODEX = 2;

    private final int containerId;
    private final int direction;
    private final int section;
    private final int sourceIndex;
    private final int targetMenuSlot;

    public StorageContainerTransferPacket(int containerId, int direction,
                                          int section, int sourceIndex,
                                          int targetMenuSlot) {
        this.containerId = containerId;
        this.direction = direction;
        this.section = section;
        this.sourceIndex = sourceIndex;
        this.targetMenuSlot = targetMenuSlot;
    }

    public static StorageContainerTransferPacket containerToBackpack(
            int containerId, int sourceMenuSlot) {
        return new StorageContainerTransferPacket(containerId,
                DIRECTION_CONTAINER_TO_BACKPACK, SECTION_MAIN,
                sourceMenuSlot, -1);
    }

    public static StorageContainerTransferPacket backpackToContainer(
            int containerId, int section, int sourceIndex,
            int targetMenuSlot) {
        return new StorageContainerTransferPacket(containerId,
                DIRECTION_BACKPACK_TO_CONTAINER, section, sourceIndex,
                targetMenuSlot);
    }

    public static void encode(StorageContainerTransferPacket message,
                              FriendlyByteBuf buffer) {
        buffer.writeInt(message.containerId);
        buffer.writeByte(message.direction);
        buffer.writeByte(message.section);
        buffer.writeInt(message.sourceIndex);
        buffer.writeInt(message.targetMenuSlot);
    }

    public static StorageContainerTransferPacket decode(
            FriendlyByteBuf buffer) {
        return new StorageContainerTransferPacket(
                buffer.readInt(),
                buffer.readByte(),
                buffer.readByte(),
                buffer.readInt(),
                buffer.readInt());
    }

    public static void handle(StorageContainerTransferPacket message,
                              Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (!ScpClassifiedDirectiveModulesConfig.get().inventory.enabled) {
                return;
            }

            ServerPlayer player = context.get().getSender();
            if (player == null) {
                return;
            }

            AbstractContainerMenu menu = player.containerMenu;
            if (menu == null
                    || menu.containerId != message.containerId
                    || !menu.stillValid(player)) {
                return;
            }

            List<Integer> storageSlots = StorageContainerSupport.storageSlotIds(
                    menu, player.getInventory());
            if (storageSlots.isEmpty()) {
                return;
            }

            player.getCapability(ScpInventoryCapability.INSTANCE)
                    .ifPresent(inventory -> {
                        boolean changed;
                        if (message.direction
                                == DIRECTION_CONTAINER_TO_BACKPACK) {
                            changed = moveContainerToBackpack(
                                    player, menu, storageSlots, inventory,
                                    message.sourceIndex);
                        } else if (message.direction
                                == DIRECTION_BACKPACK_TO_CONTAINER) {
                            changed = moveBackpackToContainer(
                                    player, menu, storageSlots, inventory,
                                    message.section, message.sourceIndex,
                                    message.targetMenuSlot);
                        } else {
                            return;
                        }

                        if (changed) {
                            menu.broadcastChanges();
                            ModNetwork.syncTo(player, inventory);
                        }
                    });
        });
        context.get().setPacketHandled(true);
    }

    private static boolean moveContainerToBackpack(ServerPlayer player,
                                                   AbstractContainerMenu menu,
                                                   List<Integer> storageSlots,
                                                   IScpInventory inventory,
                                                   int sourceMenuSlot) {
        if (!storageSlots.contains(sourceMenuSlot)
                || sourceMenuSlot < 0
                || sourceMenuSlot >= menu.slots.size()) {
            return false;
        }

        Slot slot = menu.slots.get(sourceMenuSlot);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return false;
        }

        ItemStack source = slot.getItem();
        if (source.isEmpty()) {
            return false;
        }

        ItemStack offered = source.copy();
        int accepted = ScpPickupRouter.accept(inventory, player, offered);
        accepted = Math.max(0, Math.min(accepted, source.getCount()));
        if (accepted <= 0) {
            ModNetwork.showInventoryFull(player);
            return false;
        }

        ItemStack removed = slot.remove(accepted);
        if (removed.isEmpty()) {
            return false;
        }

        slot.onTake(player, removed);
        slot.setChanged();
        slot.container.setChanged();
        return true;
    }

    private static boolean moveBackpackToContainer(ServerPlayer player,
                                                   AbstractContainerMenu menu,
                                                   List<Integer> storageSlots,
                                                   IScpInventory inventory,
                                                   int section,
                                                   int sourceIndex,
                                                   int preferredMenuSlot) {
        ItemStack source = getSourceStack(inventory, section, sourceIndex);
        if (source.isEmpty()) {
            return false;
        }

        int inserted = insertIntoStorage(menu, storageSlots, source,
                preferredMenuSlot);
        if (inserted <= 0) {
            return false;
        }

        removeFromSource(player, inventory, section, sourceIndex,
                source, inserted);
        ScpPickupRouter.reconcileCoinMirrors(player, inventory);
        ScpPickupRouter.reconcileHarmfulMirrors(player, inventory);
        return true;
    }

    private static ItemStack getSourceStack(IScpInventory inventory,
                                            int section,
                                            int sourceIndex) {
        if (inventory == null || sourceIndex < 0) {
            return ItemStack.EMPTY;
        }

        return switch (section) {
            case SECTION_MAIN -> inventory.isValidMainSlot(sourceIndex)
                    ? inventory.getInventoryItem(sourceIndex).copy()
                    : ItemStack.EMPTY;
            case SECTION_KEYS -> sourceIndex < inventory.getKeys().size()
                    ? inventory.getKeys().get(sourceIndex).copy()
                    : ItemStack.EMPTY;
            case SECTION_CODEX -> sourceIndex < inventory.getDocuments().size()
                    ? inventory.getDocumentItem(sourceIndex).copy()
                    : ItemStack.EMPTY;
            default -> ItemStack.EMPTY;
        };
    }

    private static void removeFromSource(ServerPlayer player,
                                         IScpInventory inventory,
                                         int section,
                                         int sourceIndex,
                                         ItemStack original,
                                         int amount) {
        int removed = Math.min(amount, original.getCount());
        ItemStack remainder = original.copy();
        remainder.shrink(removed);

        switch (section) {
            case SECTION_MAIN -> {
                if (!inventory.isValidMainSlot(sourceIndex)) {
                    return;
                }
                if (remainder.isEmpty()) {
                    ScpInventoryMaintenanceEvents
                            .discardActiveUsableFromSourceSlot(
                                    player, sourceIndex, original);
                    inventory.removeInventoryItem(sourceIndex);
                } else {
                    inventory.setInventoryItem(sourceIndex, remainder);
                }
            }
            case SECTION_KEYS -> {
                if (sourceIndex < 0
                        || sourceIndex >= inventory.getKeys().size()) {
                    return;
                }
                if (remainder.isEmpty()) {
                    inventory.extractKeyItem(sourceIndex);
                } else {
                    inventory.getKeys().set(sourceIndex, remainder);
                }
            }
            case SECTION_CODEX -> {
                if (sourceIndex < 0
                        || sourceIndex >= inventory.getDocuments().size()) {
                    return;
                }
                if (remainder.isEmpty()) {
                    inventory.extractDocumentItem(sourceIndex);
                } else {
                    inventory.getDocuments().set(sourceIndex, remainder);
                }
            }
            default -> {
            }
        }
    }

    private static int insertIntoStorage(AbstractContainerMenu menu,
                                         List<Integer> storageSlots,
                                         ItemStack source,
                                         int preferredMenuSlot) {
        ItemStack remaining = source.copy();
        int originalCount = remaining.getCount();

        if (preferredMenuSlot >= 0
                && storageSlots.contains(preferredMenuSlot)) {
            tryInsert(menu.slots.get(preferredMenuSlot), remaining);
        }

        // Merge into compatible occupied stacks before consuming empty slots.
        for (int slotId : storageSlots) {
            if (remaining.isEmpty() || slotId == preferredMenuSlot) {
                continue;
            }
            Slot slot = menu.slots.get(slotId);
            if (slot.hasItem()
                    && ItemStack.isSameItemSameTags(
                    slot.getItem(), remaining)) {
                tryInsert(slot, remaining);
            }
        }

        for (int slotId : storageSlots) {
            if (remaining.isEmpty() || slotId == preferredMenuSlot) {
                continue;
            }
            Slot slot = menu.slots.get(slotId);
            if (!slot.hasItem()) {
                tryInsert(slot, remaining);
            }
        }

        return originalCount - remaining.getCount();
    }

    private static void tryInsert(Slot slot, ItemStack remaining) {
        if (slot == null || remaining.isEmpty()
                || !slot.mayPlace(remaining)) {
            return;
        }

        ItemStack existing = slot.getItem();
        int max = Math.min(slot.getMaxStackSize(),
                remaining.getMaxStackSize());

        if (existing.isEmpty()) {
            int moved = Math.min(max, remaining.getCount());
            if (moved <= 0) {
                return;
            }
            ItemStack placed = remaining.copy();
            placed.setCount(moved);
            slot.set(placed);
            remaining.shrink(moved);
            slot.setChanged();
            slot.container.setChanged();
            return;
        }

        if (!ItemStack.isSameItemSameTags(existing, remaining)) {
            return;
        }

        int space = Math.max(0, max - existing.getCount());
        int moved = Math.min(space, remaining.getCount());
        if (moved <= 0) {
            return;
        }

        existing.grow(moved);
        remaining.shrink(moved);
        slot.setChanged();
        slot.container.setChanged();
    }
}
