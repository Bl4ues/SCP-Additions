package com.bl4ues.scpclassifieddirective.inventory.event;

import com.bl4ues.scpclassifieddirective.config.ScpClassifiedDirectiveModulesConfig;
import com.bl4ues.scpclassifieddirective.inventory.capability.IScpInventory;
import com.bl4ues.scpclassifieddirective.inventory.capability.ScpInventoryCapability;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpItemClassifier;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpItemType;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpPickupRouter;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side owner for PLACEABLE-style temporary hand entries.
 *
 * The real one-item stack lives in the vanilla hotbar while it is active, which
 * lets block placement and ordinary held-item interactions work normally while
 * the authoritative item remains part of the SCP Inventory flow. Equipment is
 * admitted only through the explicit Hold Item action and keeps its equipment
 * classification for quick-equip and slot behavior.
 */
@Mod.EventBusSubscriber(modid = "scp_classified_directive")
public final class PlaceableHotbarSessionEvents {
    private static final int HOTBAR_START = 0;
    private static final int HOTBAR_END_EXCLUSIVE = 9;
    private static final Map<UUID, Session> ACTIVE = new HashMap<>();

    private PlaceableHotbarSessionEvents() {
    }

    public static boolean activatePlaceableSession(ServerPlayer player,
            IScpInventory inventory, int sourceSlot) {
        return activateSession(player, inventory, sourceSlot, false);
    }

    public static boolean activateHeldEquipmentSession(ServerPlayer player,
            IScpInventory inventory, int sourceSlot) {
        return activateSession(player, inventory, sourceSlot, true);
    }

    private static boolean activateSession(ServerPlayer player,
            IScpInventory inventory, int sourceSlot,
            boolean allowEquipment) {
        if (!ScpClassifiedDirectiveModulesConfig.get().inventory.enabled
                || player == null || inventory == null || player.isCreative()
                || player.isSpectator() || !inventory.isValidMainSlot(sourceSlot)) {
            return false;
        }

        ItemStack requested = inventory.getInventoryItem(sourceSlot);
        if (requested.isEmpty()) return false;

        ScpItemType type = ScpItemClassifier.getType(requested);
        boolean ordinaryPlaceable = type == ScpItemType.PLACEABLE;
        boolean heldEquipment = allowEquipment
                && ScpItemClassifier.getEquipmentSlot(requested).isPresent();
        if (!ordinaryPlaceable && !heldEquipment) return false;

        int oldSlot = findTrackedPlaceableSlot(player);
        if (oldSlot >= 0) {
            returnTrackedPlaceableSession(player, oldSlot);
        }

        int hotbarSlot = findEmptyHotbarSlot(player);
        if (hotbarSlot < 0) {
            ModNetwork.showInventoryFull(player);
            ModNetwork.syncTo(player, inventory);
            return false;
        }

        ItemStack placeable = cleanSessionStack(
                inventory.extractInventoryItem(sourceSlot));
        if (placeable.isEmpty()) {
            ModNetwork.syncTo(player, inventory);
            return false;
        }
        placeable.setCount(1);

        ItemStack hotbarStack = placeable.copy();
        ScpPickupRouter.markUsableSession(hotbarStack, player.tickCount);
        Inventory vanilla = player.getInventory();
        vanilla.setItem(hotbarSlot, hotbarStack);
        vanilla.selected = hotbarSlot;
        vanilla.setChanged();
        ACTIVE.put(player.getUUID(), new Session(hotbarSlot, sourceSlot,
                normalized(placeable)));

        player.connection.send(new ClientboundSetCarriedItemPacket(hotbarSlot));
        ScpPickupRouter.syncVanillaInventory(player);
        ModNetwork.activateUsableItem(player, hotbarSlot, sourceSlot, false,
                placeable);
        ModNetwork.syncTo(player, inventory);
        return true;
    }

    public static boolean returnTrackedPlaceableSession(ServerPlayer player,
            int hotbarSlot) {
        if (player == null || player.isCreative() || player.isSpectator()) {
            return false;
        }

        Session session = resolveSession(player, hotbarSlot);
        if (session == null) {
            return false;
        }

        ItemStack returning = cleanSessionStack(getHotbarStack(player,
                hotbarSlot));
        clearHotbarSlot(player, hotbarSlot);
        ACTIVE.remove(player.getUUID());

        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
            if (!returning.isEmpty()) {
                restoreOrDrop(player, inventory, session.sourceSlot(), returning);
            }
            ModNetwork.syncTo(player, inventory);
        });
        return true;
    }

    public static boolean dropTrackedPlaceableSession(ServerPlayer player,
            int hotbarSlot) {
        if (player == null || player.isCreative() || player.isSpectator()) {
            return false;
        }

        Session session = resolveSession(player, hotbarSlot);
        if (session == null) {
            return false;
        }

        ItemStack dropped = cleanSessionStack(getHotbarStack(player,
                hotbarSlot));
        clearHotbarSlot(player, hotbarSlot);
        ACTIVE.remove(player.getUUID());
        if (!dropped.isEmpty()) {
            player.drop(dropped, false);
        }
        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(
                inventory -> ModNetwork.syncTo(player, inventory));
        return true;
    }

    public static boolean isTrackedPlaceableSlot(ServerPlayer player,
            int hotbarSlot) {
        return resolveSession(player, hotbarSlot) != null;
    }

    private static Session resolveSession(ServerPlayer player, int hotbarSlot) {
        if (player == null || hotbarSlot < HOTBAR_START
                || hotbarSlot >= HOTBAR_END_EXCLUSIVE) {
            return null;
        }

        UUID id = player.getUUID();
        Session current = ACTIVE.get(id);
        if (current != null && current.hotbarSlot() == hotbarSlot) {
            ItemStack stack = getHotbarStack(player, hotbarSlot);
            if (isPlaceableSessionStack(stack)
                    || (!stack.isEmpty()
                    && isSupportedSessionItem(stack)
                    && isSameSingleItem(stack, current.stack()))) {
                return current;
            }
            ACTIVE.remove(id);
        }

        ItemStack stack = getHotbarStack(player, hotbarSlot);
        if (!isPlaceableSessionStack(stack)) {
            return null;
        }
        Session reconstructed = new Session(hotbarSlot, -1,
                normalized(stack));
        ACTIVE.put(id, reconstructed);
        return reconstructed;
    }

    private static int findTrackedPlaceableSlot(ServerPlayer player) {
        if (player == null) return -1;
        Session current = ACTIVE.get(player.getUUID());
        if (current != null
                && resolveSession(player, current.hotbarSlot()) != null) {
            return current.hotbarSlot();
        }

        Inventory inventory = player.getInventory();
        int end = Math.min(HOTBAR_END_EXCLUSIVE, inventory.items.size());
        for (int slot = HOTBAR_START; slot < end; slot++) {
            if (isPlaceableSessionStack(inventory.items.get(slot))) {
                ACTIVE.put(player.getUUID(), new Session(slot, -1,
                        normalized(inventory.items.get(slot))));
                return slot;
            }
        }
        return -1;
    }

    private static int findEmptyHotbarSlot(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int selected = inventory.selected;
        if (selected >= HOTBAR_START && selected < HOTBAR_END_EXCLUSIVE
                && selected < inventory.items.size()
                && inventory.items.get(selected).isEmpty()) {
            return selected;
        }

        int end = Math.min(HOTBAR_END_EXCLUSIVE, inventory.items.size());
        for (int slot = HOTBAR_START; slot < end; slot++) {
            if (inventory.items.get(slot).isEmpty()) return slot;
        }
        return -1;
    }

    private static boolean isPlaceableSessionStack(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && ScpPickupRouter.isUsableSession(stack)
                && isSupportedSessionItem(stack);
    }

    private static boolean isSupportedSessionItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return ScpItemClassifier.getType(stack) == ScpItemType.PLACEABLE
                || ScpItemClassifier.getEquipmentSlot(stack).isPresent();
    }

    private static void restoreOrDrop(ServerPlayer player,
            IScpInventory inventory, int sourceSlot, ItemStack stack) {
        ItemStack restored = cleanSessionStack(stack);
        if (restored.isEmpty()) return;
        restored.setCount(1);

        if (sourceSlot >= 0 && inventory.isValidMainSlot(sourceSlot)
                && inventory.getInventoryItem(sourceSlot).isEmpty()) {
            inventory.setInventoryItem(sourceSlot, restored);
            return;
        }
        if (inventory.addInventoryItem(restored)) {
            return;
        }
        player.drop(restored, false);
    }

    private static ItemStack getHotbarStack(ServerPlayer player, int slot) {
        if (player == null || slot < HOTBAR_START
                || slot >= HOTBAR_END_EXCLUSIVE
                || slot >= player.getInventory().items.size()) {
            return ItemStack.EMPTY;
        }
        return player.getInventory().items.get(slot).copy();
    }

    private static void clearHotbarSlot(ServerPlayer player, int slot) {
        if (player == null || slot < HOTBAR_START
                || slot >= HOTBAR_END_EXCLUSIVE
                || slot >= player.getInventory().items.size()) {
            return;
        }
        player.getInventory().items.set(slot, ItemStack.EMPTY);
        player.getInventory().setChanged();
        ScpPickupRouter.syncVanillaInventory(player);
    }

    private static ItemStack cleanSessionStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        ScpPickupRouter.stripUsableSession(copy);
        ScpPickupRouter.stripNoMergeMarker(copy);
        ScpPickupRouter.stripCoinMirror(copy);
        ScpPickupRouter.stripHarmfulMirror(copy);
        copy.setCount(1);
        return copy;
    }

    private static ItemStack normalized(ItemStack stack) {
        return cleanSessionStack(stack);
    }

    private static boolean isSameSingleItem(ItemStack left, ItemStack right) {
        ItemStack normalizedLeft = normalized(left);
        ItemStack normalizedRight = normalized(right);
        return !normalizedLeft.isEmpty() && !normalizedRight.isEmpty()
                && ItemStack.isSameItemSameTags(normalizedLeft,
                        normalizedRight);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)
                || player.isCreative() || player.isSpectator()
                || !ScpClassifiedDirectiveModulesConfig.get().inventory.enabled) {
            return;
        }

        int slot = findTrackedPlaceableSlot(player);
        if (slot < 0) return;

        ItemStack stack = getHotbarStack(player, slot);
        if (stack.isEmpty()) {
            ACTIVE.remove(player.getUUID());
            return;
        }
        if (!isSupportedSessionItem(stack)) {
            ACTIVE.remove(player.getUUID());
            return;
        }
        if (!ScpPickupRouter.isUsableSession(stack)) {
            ScpPickupRouter.markUsableSession(stack, player.tickCount);
            player.getInventory().setItem(slot, stack);
            ScpPickupRouter.syncVanillaInventory(player);
        }

        Session current = ACTIVE.get(player.getUUID());
        if (current != null && !isSameSingleItem(current.stack(), stack)) {
            ACTIVE.put(player.getUUID(), new Session(slot,
                    current.sourceSlot(), normalized(stack)));
        }
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        ItemStack tossed = event.getEntity().getItem();
        if (tossed == null || tossed.isEmpty()
                || !isSupportedSessionItem(tossed)
                || !ScpPickupRouter.isUsableSession(tossed)) {
            return;
        }
        ItemStack cleaned = cleanSessionStack(tossed);
        event.getEntity().setItem(cleaned);
        ACTIVE.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ACTIVE.remove(player.getUUID());
        }
    }

    private record Session(int hotbarSlot, int sourceSlot, ItemStack stack) {
    }
}
