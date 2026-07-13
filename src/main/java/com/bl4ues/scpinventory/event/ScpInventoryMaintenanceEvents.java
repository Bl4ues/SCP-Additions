package com.bl4ues.scpinventory.event;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.capability.ScpInventoryCapability;
import com.bl4ues.scpinventory.item.ScpEquipmentSlot;
import com.bl4ues.scpinventory.item.ScpItemClassifier;
import com.bl4ues.scpinventory.item.ScpItemEffects;
import com.bl4ues.scpinventory.item.ScpItemType;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
import com.bl4ues.scpinventory.network.InventoryActionPacket;
import com.bl4ues.scpinventory.network.ModNetwork;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "scp_additions")
public final class ScpInventoryMaintenanceEvents {

    private static final int VANILLA_HOTBAR_START = 0;
    private static final int VANILLA_HOTBAR_END_EXCLUSIVE = 9;
    private static final int VANILLA_MAIN_START = 9;
    private static final int VANILLA_MAIN_END_EXCLUSIVE = 36;
    private static final int USABLE_RETURN_GRACE_TICKS = 20;
    private static final int USABLE_ACTIVATION_REARM_TICKS = 20;
    private static final int USABLE_ACTIVATION_STABLE_TICKS = 5;

    private static final Map<UUID, Integer> ACTIVE_USABLE_SLOTS = new HashMap<>();
    private static final Map<UUID, Integer> ACTIVE_USABLE_SOURCE_SLOTS = new HashMap<>();
    private static final Map<UUID, Integer> ACTIVE_USABLE_START_TICKS = new HashMap<>();
    private static final Map<UUID, Integer> ACTIVE_USABLE_STABLE_TICKS = new HashMap<>();
    private static final Map<UUID, ItemStack> ACTIVE_USABLE_STACKS = new HashMap<>();
    private static final Map<UUID, Boolean> ACTIVE_USABLE_CONFIRMED = new HashMap<>();

    private ScpInventoryMaintenanceEvents() {
    }

    public static boolean activateUsableSession(ServerPlayer player, IScpInventory inventory, int sourceSlot) {
        if (player == null || inventory == null || player.isCreative() || player.isSpectator() || !inventory.isValidMainSlot(sourceSlot)) {
            return false;
        }

        ItemStack alreadyActive = inventory.getActiveUsable();
        if (!alreadyActive.isEmpty()) {
            int activeSlot = syncActiveUsableMirror(player, inventory, alreadyActive, -1);
            if (activeSlot >= 0) {
                trackUsableSession(player, activeSlot, alreadyActive, -1);
                ModNetwork.activateUsableItem(player, activeSlot, -1, false, alreadyActive);
            }
            ModNetwork.syncTo(player, inventory);
            return true;
        }

        int hotbarSlot = findUsableHotbarSlot(player);
        if (hotbarSlot == -1) {
            ModNetwork.showInventoryFull(player);
            return false;
        }

        ItemStack usableStack = cleanForScpStorage(inventory.extractInventoryItem(sourceSlot));
        if (usableStack.isEmpty()) {
            return false;
        }
        usableStack.setCount(1);
        inventory.setActiveUsable(usableStack);

        int activeSlot = syncActiveUsableMirror(player, inventory, usableStack, hotbarSlot);
        if (activeSlot < 0) {
            replaceSourceOrFirstFree(inventory, sourceSlot, inventory.extractActiveUsable());
            ModNetwork.showInventoryFull(player);
            ModNetwork.syncTo(player, inventory);
            return false;
        }

        trackUsableSession(player, activeSlot, usableStack, sourceSlot);
        ModNetwork.activateUsableItem(player, activeSlot, sourceSlot, false, usableStack);
        ModNetwork.syncTo(player, inventory);
        return true;
    }

    public static void trackUsableSession(ServerPlayer player, int hotbarSlot) {
        if (player == null || hotbarSlot < VANILLA_HOTBAR_START || hotbarSlot >= VANILLA_HOTBAR_END_EXCLUSIVE || hotbarSlot >= player.getInventory().items.size()) {
            return;
        }
        trackUsableSession(player, hotbarSlot, player.getInventory().items.get(hotbarSlot), -1);
    }

    public static void trackUsableSession(ServerPlayer player, int hotbarSlot, ItemStack stack) {
        trackUsableSession(player, hotbarSlot, stack, -1);
    }

    public static void trackUsableSession(ServerPlayer player, int hotbarSlot, ItemStack stack, int sourceSlot) {
        if (player == null || hotbarSlot < VANILLA_HOTBAR_START || hotbarSlot >= VANILLA_HOTBAR_END_EXCLUSIVE || stack == null || stack.isEmpty()) {
            return;
        }

        UUID id = player.getUUID();
        ACTIVE_USABLE_SLOTS.put(id, hotbarSlot);
        ACTIVE_USABLE_SOURCE_SLOTS.put(id, sourceSlot);
        ACTIVE_USABLE_START_TICKS.put(id, player.tickCount);
        ACTIVE_USABLE_STABLE_TICKS.put(id, 0);
        ACTIVE_USABLE_STACKS.put(id, normalizeSingle(stack));
        ACTIVE_USABLE_CONFIRMED.put(id, false);
    }

    public static boolean returnTrackedUsableSession(ServerPlayer player, int hotbarSlot) {
        if (player == null || player.isCreative() || player.isSpectator()) {
            return false;
        }

        UUID id = player.getUUID();
        Integer activeSlot = ACTIVE_USABLE_SLOTS.get(id);
        if (activeSlot == null || activeSlot != hotbarSlot) {
            return false;
        }

        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
            ItemStack returning = cleanForScpStorage(inventory.extractActiveUsable());
            if (returning.isEmpty()) {
                returning = cleanForScpStorage(getHotbarOrTrackedSessionStack(player, hotbarSlot, id));
            }
            clearActiveUsableMirrors(player, returning);
            if (!returning.isEmpty()) {
                replaceSourceOrFirstFree(inventory, ACTIVE_USABLE_SOURCE_SLOTS.getOrDefault(id, -1), returning);
            }
            ModNetwork.syncTo(player, inventory);
        });

        clearUsableSession(player);
        return true;
    }

    public static boolean dropTrackedUsableSession(ServerPlayer player, int hotbarSlot) {
        if (player == null || player.isCreative() || player.isSpectator()) {
            return false;
        }

        UUID id = player.getUUID();
        Integer activeSlot = ACTIVE_USABLE_SLOTS.get(id);
        if (activeSlot == null || activeSlot != hotbarSlot) {
            return false;
        }

        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
            ItemStack dropStack = cleanForExternalWorld(inventory.extractActiveUsable());
            if (dropStack.isEmpty()) {
                dropStack = cleanForExternalWorld(getHotbarOrTrackedSessionStack(player, hotbarSlot, id));
            }
            clearActiveUsableMirrors(player, dropStack);
            clearHotbarSlot(player, hotbarSlot);
            if (!dropStack.isEmpty()) {
                player.drop(dropStack, false);
            }
            ModNetwork.syncTo(player, inventory);
        });
        clearUsableSession(player);
        return true;
    }

    public static boolean discardActiveUsableFromSourceSlot(ServerPlayer player, int sourceSlot, ItemStack sourceStack) {
        if (player == null || player.isCreative() || player.isSpectator()) {
            return false;
        }

        UUID id = player.getUUID();
        Integer activeHotbarSlot = ACTIVE_USABLE_SLOTS.get(id);
        if (activeHotbarSlot == null) {
            return false;
        }

        ItemStack activeStack = ACTIVE_USABLE_STACKS.getOrDefault(id, ItemStack.EMPTY);
        boolean matchingStack = sourceStack != null && !sourceStack.isEmpty() && !activeStack.isEmpty() && isSameSingleItem(sourceStack, activeStack);
        if (!matchingStack) {
            return false;
        }

        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
            inventory.clearActiveUsable();
            clearActiveUsableMirrors(player, activeStack);
            ModNetwork.syncTo(player, inventory);
        });
        clearUsableSession(player);
        return true;
    }

    private static int syncActiveUsableMirror(ServerPlayer player, IScpInventory inventory, ItemStack stack, int preferredSlot) {
        if (player == null || inventory == null || stack == null || stack.isEmpty()) {
            return -1;
        }

        ItemStack mirrorStack = cleanForExternalWorld(stack);
        mirrorStack.setCount(1);
        ScpPickupRouter.markUsableSession(mirrorStack, player.tickCount);
        clearActiveUsableMirrors(player, mirrorStack);

        Inventory vanillaInventory = player.getInventory();
        int mirrorSlot = -1;
        if (preferredSlot >= VANILLA_HOTBAR_START
                && preferredSlot < VANILLA_HOTBAR_END_EXCLUSIVE
                && preferredSlot < vanillaInventory.items.size()
                && vanillaInventory.items.get(preferredSlot).isEmpty()) {
            mirrorSlot = preferredSlot;
        }
        if (mirrorSlot < 0) {
            int selected = vanillaInventory.selected;
            if (selected >= VANILLA_HOTBAR_START
                    && selected < VANILLA_HOTBAR_END_EXCLUSIVE
                    && selected < vanillaInventory.items.size()
                    && vanillaInventory.items.get(selected).isEmpty()) {
                mirrorSlot = selected;
            }
        }
        if (mirrorSlot < 0) {
            mirrorSlot = findUsableHotbarSlot(player);
        }
        if (mirrorSlot < 0) {
            return -1;
        }

        vanillaInventory.setItem(mirrorSlot, mirrorStack.copy());
        vanillaInventory.selected = mirrorSlot;
        player.setItemInHand(InteractionHand.MAIN_HAND, mirrorStack.copy());
        player.connection.send(new ClientboundSetCarriedItemPacket(mirrorSlot));
        ScpPickupRouter.syncVanillaInventory(player);
        return mirrorSlot;
    }

    private static boolean clearActiveUsableMirrors(ServerPlayer player, ItemStack expectedStack) {
        if (player == null) {
            return false;
        }
        Inventory vanillaInventory = player.getInventory();
        boolean changed = false;
        int end = Math.min(VANILLA_MAIN_END_EXCLUSIVE, vanillaInventory.items.size());
        for (int i = VANILLA_HOTBAR_START; i < end; i++) {
            ItemStack stack = vanillaInventory.items.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (ScpPickupRouter.isUsableSession(stack)
                    || (expectedStack != null && !expectedStack.isEmpty() && isSameSingleItem(stack, expectedStack))) {
                vanillaInventory.items.set(i, ItemStack.EMPTY);
                changed = true;
            }
        }
        if (changed) {
            vanillaInventory.setChanged();
            ScpPickupRouter.syncVanillaInventory(player);
        }
        return changed;
    }

    private static int findUsableHotbarSlot(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int selected = inventory.selected;
        if (selected >= VANILLA_HOTBAR_START
                && selected < VANILLA_HOTBAR_END_EXCLUSIVE
                && selected < inventory.items.size()
                && inventory.items.get(selected).isEmpty()) {
            return selected;
        }

        int end = Math.min(VANILLA_HOTBAR_END_EXCLUSIVE, inventory.items.size());
        for (int i = VANILLA_HOTBAR_START; i < end; i++) {
            if (inventory.items.get(i).isEmpty()) return i;
        }
        return -1;
    }

    private static void replaceSourceOrFirstFree(IScpInventory inventory, int sourceSlot, ItemStack stack) {
        ItemStack restored = cleanForScpStorage(stack);
        if (inventory == null || restored.isEmpty()) {
            return;
        }

        int remaining = restored.getCount();
        if (sourceSlot >= 0 && inventory.isValidMainSlot(sourceSlot)) {
            ItemStack existing = inventory.getInventoryItem(sourceSlot);
            if (existing.isEmpty() || isSameSingleItem(existing, restored)) {
                ItemStack first = restored.copy();
                first.setCount(1);
                inventory.setInventoryItem(sourceSlot, first);
                remaining--;
            }
        }

        if (remaining > 0) {
            ItemStack rest = restored.copy();
            rest.setCount(remaining);
            inventory.addInventoryItems(rest);
        }
    }

    private static ItemStack getHotbarOrTrackedSessionStack(ServerPlayer player, int hotbarSlot, UUID id) {
        ItemStack stack = cleanForExternalWorld(getHotbarStack(player, hotbarSlot));
        if (stack.isEmpty()) {
            stack = cleanForExternalWorld(ACTIVE_USABLE_STACKS.getOrDefault(id, ItemStack.EMPTY));
        }
        if (!stack.isEmpty()) {
            stack.setCount(1);
        }
        return stack;
    }

    private static ItemStack getHotbarStack(ServerPlayer player, int hotbarSlot) {
        if (player == null) {
            return ItemStack.EMPTY;
        }
        Inventory vanillaInventory = player.getInventory();
        if (hotbarSlot < VANILLA_HOTBAR_START || hotbarSlot >= VANILLA_HOTBAR_END_EXCLUSIVE || hotbarSlot >= vanillaInventory.items.size()) {
            return ItemStack.EMPTY;
        }
        return vanillaInventory.items.get(hotbarSlot).copy();
    }

    private static void clearHotbarSlot(ServerPlayer player, int hotbarSlot) {
        if (player == null) {
            return;
        }
        Inventory vanillaInventory = player.getInventory();
        if (hotbarSlot < VANILLA_HOTBAR_START || hotbarSlot >= VANILLA_HOTBAR_END_EXCLUSIVE || hotbarSlot >= vanillaInventory.items.size()) {
            return;
        }
        if (!vanillaInventory.items.get(hotbarSlot).isEmpty()) {
            vanillaInventory.items.set(hotbarSlot, ItemStack.EMPTY);
            vanillaInventory.setChanged();
            ScpPickupRouter.syncVanillaInventory(player);
        }
    }

    private static void clearUsableSession(ServerPlayer player) {
        if (player == null) {
            return;
        }

        UUID id = player.getUUID();
        ACTIVE_USABLE_SLOTS.remove(id);
        ACTIVE_USABLE_SOURCE_SLOTS.remove(id);
        ACTIVE_USABLE_START_TICKS.remove(id);
        ACTIVE_USABLE_STABLE_TICKS.remove(id);
        ACTIVE_USABLE_STACKS.remove(id);
        ACTIVE_USABLE_CONFIRMED.remove(id);
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) {
            return;
        }

        ItemStack tossedStack = event.getEntity().getItem();
        if (finishActiveUsableVanillaToss(player, tossedStack)) {
            ItemStack cleanedToss = cleanForExternalWorld(tossedStack);
            event.getEntity().setItem(cleanedToss);
            return;
        }

        if (ScpPickupRouter.isUsableSession(tossedStack)) {
            ItemStack cleanedToss = cleanForExternalWorld(tossedStack);
            event.getEntity().setItem(cleanedToss);
            clearUsableSession(player);
            return;
        }

        if (ScpPickupRouter.isCoinMirror(tossedStack)) {
            ScpPickupRouter.stripCoinMirror(tossedStack);
            event.getEntity().setItem(tossedStack);
        }
    }

    private static boolean finishActiveUsableVanillaToss(ServerPlayer player, ItemStack tossedStack) {
        UUID id = player.getUUID();
        Integer activeSlot = ACTIVE_USABLE_SLOTS.get(id);
        if (activeSlot == null) {
            return false;
        }

        ItemStack activeStack = ACTIVE_USABLE_STACKS.getOrDefault(id, ItemStack.EMPTY);
        boolean matchesTrackedStack = tossedStack != null
                && !tossedStack.isEmpty()
                && !activeStack.isEmpty()
                && isSameSingleItem(tossedStack, activeStack);
        if (!matchesTrackedStack) {
            return false;
        }

        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
            inventory.clearActiveUsable();
            clearActiveUsableMirrors(player, activeStack);
            ModNetwork.syncTo(player, inventory);
        });
        clearUsableSession(player);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)
                || player.isSpectator()) {
            return;
        }

        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
            boolean changed = false;
            if (!player.isCreative()) {
                if (ScpItemEffects.hasNoStaminaModifierEquipped(player, inventory)) {
                    player.setSprinting(false);
                }
                changed |= ScpPickupRouter.reconcileCoinMirrors(player, inventory);
                changed |= ScpPickupRouter.reconcileHarmfulMirrors(player, inventory);
                changed |= maintainUsableSession(player, inventory);
                // Equipment mirrors must be reconciled before the generic collector.
                // A weapon hit changes durability NBT; collecting first interpreted
                // that legitimate mirror as a newly acquired WEAPON after one hit.
                changed |= reconcileAccessoryHand(player, inventory);
                changed |= reconcileWeaponHand(player, inventory);
                changed |= collectConfiguredVanillaItems(player, inventory, ACTIVE_USABLE_SLOTS.getOrDefault(player.getUUID(), -1));
            } else {
                changed |= reconcileAccessoryHand(player, inventory);
                changed |= reconcileWeaponHand(player, inventory);
            }
            if (changed) {
                ModNetwork.syncTo(player, inventory);
            }
        });
    }

    private static boolean maintainUsableSession(ServerPlayer player, IScpInventory inventory) {
        UUID id = player.getUUID();
        ItemStack activeUsable = inventory.getActiveUsable();
        Integer slot = ACTIVE_USABLE_SLOTS.get(id);
        if (activeUsable.isEmpty()) {
            if (slot != null) {
                clearActiveUsableMirrors(player, ACTIVE_USABLE_STACKS.getOrDefault(id, ItemStack.EMPTY));
                clearUsableSession(player);
                return true;
            }
            return false;
        }

        Inventory vanillaInventory = player.getInventory();
        if (slot == null || slot < VANILLA_HOTBAR_START || slot >= VANILLA_HOTBAR_END_EXCLUSIVE || slot >= vanillaInventory.items.size()) {
            int mirrorSlot = syncActiveUsableMirror(player, inventory, activeUsable, -1);
            if (mirrorSlot >= 0) {
                trackUsableSession(player, mirrorSlot, activeUsable, -1);
                return true;
            }
            inventory.clearActiveUsable();
            clearUsableSession(player);
            return true;
        }

        int startTick = ACTIVE_USABLE_START_TICKS.getOrDefault(id, player.tickCount);
        int elapsedTicks = player.tickCount - startTick;
        int stableTicks = ACTIVE_USABLE_STABLE_TICKS.getOrDefault(id, 0);
        boolean confirmed = ACTIVE_USABLE_CONFIRMED.getOrDefault(id, false);
        ItemStack mirror = vanillaInventory.items.get(slot);
        if (mirror.isEmpty()) {
            if (shouldPreserveVanishedUsable(activeUsable)) {
                return rearmTrackedUsableInHotbar(player, inventory, slot);
            }
            inventory.clearActiveUsable();
            clearUsableSession(player);
            ModNetwork.syncTo(player, inventory);
            return true;
        }

        stableTicks++;
        ACTIVE_USABLE_STABLE_TICKS.put(id, stableTicks);
        if (stableTicks >= USABLE_ACTIVATION_STABLE_TICKS) {
            ACTIVE_USABLE_CONFIRMED.put(id, true);
        }

        ItemStack normalizedMirror = normalizeSingle(mirror);
        if (!isSameSingleItem(normalizedMirror, activeUsable)) {
            inventory.setActiveUsable(normalizedMirror);
            ACTIVE_USABLE_STACKS.put(id, normalizedMirror);
            return true;
        }

        ACTIVE_USABLE_STACKS.put(id, normalizeSingle(activeUsable));
        if (elapsedTicks <= USABLE_RETURN_GRACE_TICKS && vanillaInventory.selected != slot) {
            vanillaInventory.selected = slot;
            player.connection.send(new ClientboundSetCarriedItemPacket(slot));
        }
        return false;
    }

    private static boolean rearmTrackedUsableInHotbar(ServerPlayer player, IScpInventory inventory, int activeSlot) {
        ItemStack active = inventory.getActiveUsable();
        if (active.isEmpty()) {
            inventory.clearActiveUsable();
            clearUsableSession(player);
            ModNetwork.syncTo(player, inventory);
            return true;
        }
        int mirrorSlot = syncActiveUsableMirror(player, inventory, active, activeSlot);
        if (mirrorSlot >= 0) {
            trackUsableSession(player, mirrorSlot, active, ACTIVE_USABLE_SOURCE_SLOTS.getOrDefault(player.getUUID(), -1));
            ModNetwork.syncTo(player, inventory);
            return true;
        }
        inventory.clearActiveUsable();
        clearUsableSession(player);
        ModNetwork.syncTo(player, inventory);
        return true;
    }

    private static boolean shouldPreserveVanishedUsable(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getUseAnimation() == net.minecraft.world.item.UseAnim.SPYGLASS;
    }

    private static boolean collectConfiguredVanillaItems(ServerPlayer player, IScpInventory inventory, int ignoredHotbarSlot) {
        if (player == null || inventory == null || player.isCreative() || player.isSpectator()) {
            return false;
        }
        boolean changed = false;
        Inventory vanillaInventory = player.getInventory();
        int end = Math.min(VANILLA_MAIN_END_EXCLUSIVE, vanillaInventory.items.size());
        for (int i = VANILLA_HOTBAR_START; i < end; i++) {
            if (i == ignoredHotbarSlot) {
                continue;
            }

            ItemStack stack = vanillaInventory.items.get(i);
            if (!shouldCollectVanillaStack(inventory, stack)) {
                continue;
            }

            ItemStack routing = cleanForScpStorage(stack);
            int accepted = ScpPickupRouter.accept(inventory, player, routing);
            if (accepted <= 0) {
                continue;
            }

            stack.shrink(accepted);
            if (stack.isEmpty()) {
                vanillaInventory.items.set(i, ItemStack.EMPTY);
            }
            vanillaInventory.setChanged();
            changed = true;
        }

        if (changed) {
            ScpPickupRouter.syncVanillaInventory(player);
        }
        return changed;
    }

    private static boolean shouldCollectVanillaStack(IScpInventory inventory, ItemStack stack) {
        if (inventory == null || stack == null || stack.isEmpty()
                || ScpPickupRouter.isCoinMirror(stack)
                || ScpPickupRouter.isHarmfulMirror(stack)
                || ScpPickupRouter.isUsableSession(stack)
                || isEquipmentMirror(inventory, stack)) {
            return false;
        }

        if (ScpItemClassifier.isCoin(stack)) {
            return true;
        }
        ScpItemType type = ScpItemClassifier.getType(stack);
        return type != ScpItemType.MISCELLANEOUS && type != ScpItemType.USABLE;
    }

    private static boolean isEquipmentMirror(IScpInventory inventory, ItemStack stack) {
        if (inventory == null || stack == null || stack.isEmpty()) {
            return false;
        }

        ItemStack activeUsable = inventory.getActiveUsable();
        if (!activeUsable.isEmpty() && isSameSingleItem(activeUsable, stack)) {
            return true;
        }
        for (ScpEquipmentSlot slot : ScpEquipmentSlot.values()) {
            ItemStack equipped = inventory.getEquipment(slot);
            if (!equipped.isEmpty() && isSameSingleItem(equipped, stack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean reconcileAccessoryHand(ServerPlayer player, IScpInventory inventory) {
        ItemStack equippedAccessory = inventory.getEquipment(ScpEquipmentSlot.ACCESSORY);
        ItemStack offhand = player.getOffhandItem();

        if (!equippedAccessory.isEmpty() && ScpItemClassifier.isAccessoryHand(equippedAccessory)) {
            if (offhand.isEmpty() || !ItemStack.isSameItemSameTags(normalizeSingle(offhand), normalizeSingle(equippedAccessory))) {
                InventoryActionPacket.syncVanillaEquipmentSlot(player, ScpEquipmentSlot.ACCESSORY, equippedAccessory);
                return true;
            }
            return false;
        }

        if (!offhand.isEmpty() && ScpItemClassifier.isAccessoryHand(offhand) && equippedAccessory.isEmpty()) {
            inventory.setEquipment(ScpEquipmentSlot.ACCESSORY, normalizeSingle(offhand));
            return true;
        }

        return false;
    }

    private static boolean reconcileWeaponHand(ServerPlayer player, IScpInventory inventory) {
        ItemStack equippedWeapon = inventory.getEquipment(ScpEquipmentSlot.WEAPON);
        if (equippedWeapon.isEmpty()) {
            return false;
        }

        Inventory vanillaInventory = player.getInventory();
        int mirrorSlot = findEquipmentMirrorSlot(vanillaInventory, ScpEquipmentSlot.WEAPON);
        if (mirrorSlot < 0) {
            inventory.clearEquipment(ScpEquipmentSlot.WEAPON);
            return true;
        }

        ItemStack mirror = vanillaInventory.items.get(mirrorSlot);
        ItemStack normalizedMirror = normalizeSingle(mirror);
        ItemStack normalizedEquipped = normalizeSingle(equippedWeapon);
        if (!ItemStack.isSameItemSameTags(normalizedMirror, normalizedEquipped)) {
            inventory.setEquipment(ScpEquipmentSlot.WEAPON, normalizedMirror);
            return true;
        }

        return false;
    }

    private static int findEquipmentMirrorSlot(Inventory inventory, ScpEquipmentSlot slot) {
        int hotbarEnd = Math.min(VANILLA_HOTBAR_END_EXCLUSIVE, inventory.items.size());
        for (int i = VANILLA_HOTBAR_START; i < hotbarEnd; i++) {
            ItemStack stack = inventory.items.get(i);
            if (!stack.isEmpty() && ScpItemClassifier.getEquipmentSlot(stack).orElse(null) == slot) {
                return i;
            }
        }

        int mainEnd = Math.min(VANILLA_MAIN_END_EXCLUSIVE, inventory.items.size());
        for (int i = VANILLA_MAIN_START; i < mainEnd; i++) {
            ItemStack stack = inventory.items.get(i);
            if (!stack.isEmpty() && ScpItemClassifier.getEquipmentSlot(stack).orElse(null) == slot) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isSameSingleItem(ItemStack left, ItemStack right) {
        return ItemStack.isSameItemSameTags(normalizeSingle(left), normalizeSingle(right));
    }

    private static ItemStack normalizeSingle(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = cleanForScpStorage(stack);
        copy.setCount(1);
        return copy;
    }

    private static ItemStack cleanForScpStorage(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        ScpPickupRouter.stripUsableSession(copy);
        ScpPickupRouter.stripNoMergeMarker(copy);
        ScpPickupRouter.stripHarmfulMirror(copy);
        ScpPickupRouter.stripCoinMirror(copy);
        return copy;
    }

    private static ItemStack cleanForExternalWorld(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        ScpPickupRouter.stripUsableSession(copy);
        ScpPickupRouter.stripNoMergeMarker(copy);
        return copy;
    }
}
