package com.bl4ues.scpclassifieddirective.world.inventory;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import com.bl4ues.scpclassifieddirective.entity.PlayerCorpseEntity;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModMenus;

/**
 * Server-authoritative storage menu backed by a physical player corpse.
 *
 * <p>Corpse slots always precede the vanilla player inventory. That ordering is
 * intentional: it lets the existing SCP Inventory storage adapter recognize
 * this menu as ordinary external storage and replace the vanilla presentation
 * with the SCP container UI when the inventory module is enabled.</p>
 */
public final class PlayerCorpseMenu extends AbstractContainerMenu {
    private final Container storage;
    private final PlayerCorpseEntity corpse;
    private final int corpseEntityId;
    private final int storageSize;
    private final int storageRows;
    private final boolean scpInventoryMode;

    /** Forge client factory. */
    public PlayerCorpseMenu(int containerId, Inventory playerInventory,
            FriendlyByteBuf data) {
        this(containerId, playerInventory,
                resolveCorpse(playerInventory.player, data.readInt()),
                normalizeSize(data.readVarInt()), data.readBoolean());
    }

    /** Server factory used by the corpse MenuProvider. */
    public PlayerCorpseMenu(int containerId, Inventory playerInventory,
            PlayerCorpseEntity corpse) {
        this(containerId, playerInventory, corpse,
                corpse == null ? 9 : corpse.containerSize(),
                corpse != null && corpse.scpInventoryMode());
    }

    private PlayerCorpseMenu(int containerId, Inventory playerInventory,
            PlayerCorpseEntity corpse, int requestedSize,
            boolean scpInventoryMode) {
        super(ScpClassifiedDirectiveModMenus.PLAYER_CORPSE.get(), containerId);
        this.corpse = corpse;
        this.corpseEntityId = corpse == null ? -1 : corpse.getId();
        this.storageSize = normalizeSize(requestedSize);
        this.storageRows = Math.max(1, (storageSize + 8) / 9);
        this.scpInventoryMode = scpInventoryMode;
        this.storage = corpse != null
                && corpse.containerSize() == storageSize
                ? corpse.container() : new SimpleContainer(storageSize);

        storage.startOpen(playerInventory.player);
        addStorageSlots();
        addPlayerSlots(playerInventory);
    }

    private void addStorageSlots() {
        for (int slot = 0; slot < storageSize; slot++) {
            int row = slot / 9;
            int column = slot % 9;
            addSlot(new Slot(storage, slot,
                    8 + column * 18, 18 + row * 18));
        }
    }

    private void addPlayerSlots(Inventory inventory) {
        int inventoryY = 31 + storageRows * 18;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        8 + column * 18, inventoryY + row * 18));
            }
        }
        int hotbarY = inventoryY + 58;
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column,
                    8 + column * 18, hotbarY));
        }
    }

    public int storageSize() {
        return storageSize;
    }

    public int storageRows() {
        return storageRows;
    }

    public int corpseEntityId() {
        return corpseEntityId;
    }

    public boolean scpInventoryMode() {
        return scpInventoryMode;
    }

    @Override
    public boolean stillValid(Player player) {
        if (corpse == null) {
            // The menu packet can arrive one client tick before the entity spawn
            // packet. Validation that matters is server-side, where corpse is
            // always the real backing entity.
            return player.level().isClientSide;
        }
        return corpse.isAlive() && player.distanceToSqr(corpse) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        if (index < 0 || index >= slots.size()) return result;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < storageSize) {
            if (!moveItemStackTo(stack, storageSize, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, storageSize, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        storage.stopOpen(player);
    }

    private static PlayerCorpseEntity resolveCorpse(Player player, int entityId) {
        if (player == null || player.level() == null || entityId < 0) return null;
        Entity entity = player.level().getEntity(entityId);
        return entity instanceof PlayerCorpseEntity corpse ? corpse : null;
    }

    private static int normalizeSize(int requested) {
        int size = Math.max(9, requested);
        return ((size + 8) / 9) * 9;
    }
}
