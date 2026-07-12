package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.bl4ues.scpinventory.capability.IScpInventory;
import com.bl4ues.scpinventory.item.ScpPickupRouter;
import com.bl4ues.scpinventory.network.ModNetwork;
import com.bl4ues.scpinventory.network.UsableSessionDropPacket;
import com.bl4ues.scpinventory.network.UsableSessionReturnPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "scp_additions", value = Dist.CLIENT)
public final class UsableHotbarSessionClient {

    private static final int STARTUP_SELECT_SYNC_TICKS = 3;
    private static final int RETURN_ARM_TICKS = 30;

    private static int activeSlot = -1;
    private static int activeSourceSlot = -1;
    private static int syncTicks = 0;
    private static int returnArmTicks = 0;
    private static ItemStack activeStack = ItemStack.EMPTY;

    private UsableHotbarSessionClient() {
    }

    public static void start(int hotbarSlot) {
        start(hotbarSlot, -1, ItemStack.EMPTY);
    }

    public static void start(int hotbarSlot, ItemStack stack) {
        start(hotbarSlot, -1, stack);
    }

    public static void start(int hotbarSlot, int sourceSlot, ItemStack stack) {
        if (hotbarSlot < 0 || hotbarSlot >= 9) {
            clear();
            return;
        }

        activeSlot = hotbarSlot;
        activeSourceSlot = sourceSlot;
        syncTicks = STARTUP_SELECT_SYNC_TICKS;
        returnArmTicks = RETURN_ARM_TICKS;
        activeStack = stack == null ? ItemStack.EMPTY : stack.copy();
    }

    public static void filterActiveSourceSlot(IScpInventory inventory) {
        if (inventory == null || activeSlot < 0 || activeSourceSlot < 0 || activeStack.isEmpty()) {
            return;
        }

        if (!isHotbarMirrorStillActive()) {
            clear();
            return;
        }

        if (!inventory.isValidMainSlot(activeSourceSlot)) {
            return;
        }

        ItemStack sourceStack = inventory.getInventoryItem(activeSourceSlot);
        if (!sourceStack.isEmpty() && isSameMutableUsableItem(sourceStack, activeStack)) {
            inventory.setInventoryItem(activeSourceSlot, ItemStack.EMPTY);
        }
    }

    public static void discardIfSourceSlot(int sourceSlot) {
        if (activeSlot < 0 || sourceSlot < 0 || sourceSlot != activeSourceSlot) {
            return;
        }

        clearClientSessionCopy();
        clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            handleDropKey();
            return;
        }

        if (event.phase != TickEvent.Phase.END || activeSlot < 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || player.isCreative() || player.isSpectator()) {
            clear();
            return;
        }

        if (activeSlot >= player.getInventory().items.size()) {
            clear();
            return;
        }

        ensureClientSessionCopy(player);

        if (returnArmTicks > 0) {
            returnArmTicks--;
        }

        if (syncTicks > 0) {
            syncTicks--;
        }

        if (player.getInventory().selected != activeSlot) {
            forceSelectedSlot(player);
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (activeSlot < 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null || player.isCreative() || player.isSpectator()) {
            return;
        }

        if (returnArmTicks > 0) {
            event.setCanceled(true);
            return;
        }

        sendReturn();
        event.setCanceled(true);
    }

    private static void handleDropKey() {
        if (activeSlot < 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null || player.isCreative() || player.isSpectator()) {
            return;
        }

        if (!minecraft.options.keyDrop.consumeClick()) {
            return;
        }

        ModNetwork.CHANNEL.sendToServer(new UsableSessionDropPacket(activeSlot));
        clearClientSessionCopy();
        clear();
    }

    private static void sendReturn() {
        if (activeSlot < 0) {
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new UsableSessionReturnPacket(activeSlot));
        clearClientSessionCopy();
        clear();
    }

    private static void ensureClientSessionCopy(LocalPlayer player) {
        if (player == null || activeSlot < 0 || activeSlot >= player.getInventory().items.size() || activeStack.isEmpty()) {
            return;
        }

        if (!shouldClientReapplySessionCopy(activeStack)) {
            return;
        }

        ItemStack active = player.getInventory().items.get(activeSlot);
        if (!active.isEmpty() && isSameSingleItem(active, activeStack)) {
            return;
        }

        ItemStack copy = activeStack.copy();
        copy.setCount(1);
        player.getInventory().setItem(activeSlot, copy);
        player.getInventory().selected = activeSlot;
        player.getInventory().setChanged();
        if (player.connection != null) {
            player.connection.send(new ServerboundSetCarriedItemPacket(activeSlot));
        }
    }

    private static boolean shouldClientReapplySessionCopy(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getUseAnimation() == UseAnim.SPYGLASS;
    }

    private static boolean isHotbarMirrorStillActive() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || activeSlot < 0 || activeSlot >= player.getInventory().items.size()) {
            return false;
        }

        ItemStack hotbarStack = player.getInventory().items.get(activeSlot);
        if (hotbarStack.isEmpty() || activeStack.isEmpty() || !isSameMutableUsableItem(hotbarStack, activeStack)) {
            return false;
        }

        activeStack = normalizedSingle(hotbarStack);
        return true;
    }

    private static void forceSelectedSlot(LocalPlayer player) {
        if (player.getInventory().selected != activeSlot) {
            player.getInventory().selected = activeSlot;
        }
        if (player.connection != null) {
            player.connection.send(new ServerboundSetCarriedItemPacket(activeSlot));
        }
    }

    private static void clearClientSessionCopy() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || activeSlot < 0 || activeSlot >= player.getInventory().items.size()) {
            return;
        }

        ItemStack expected = activeStack == null ? ItemStack.EMPTY : activeStack.copy();
        ItemStack active = player.getInventory().items.get(activeSlot);
        boolean removedExpectedCopy = !active.isEmpty() && !expected.isEmpty() && isSameMutableUsableItem(active, expected);
        player.getInventory().setItem(activeSlot, ItemStack.EMPTY);

        if (!removedExpectedCopy && !expected.isEmpty()) {
            int end = Math.min(36, player.getInventory().items.size());
            for (int i = 0; i < end; i++) {
                if (i == activeSlot) {
                    continue;
                }
                ItemStack stack = player.getInventory().items.get(i);
                if (stack.isEmpty() || !isSameMutableUsableItem(stack, expected)) {
                    continue;
                }
                stack.shrink(1);
                if (stack.isEmpty()) {
                    player.getInventory().items.set(i, ItemStack.EMPTY);
                }
                break;
            }
        }

        player.getInventory().setChanged();
    }

    private static boolean isSameMutableUsableItem(ItemStack left, ItemStack right) {
        ItemStack normalizedLeft = normalizedSingle(left);
        ItemStack normalizedRight = normalizedSingle(right);
        return !normalizedLeft.isEmpty() && !normalizedRight.isEmpty() && ItemStack.isSameItem(normalizedLeft, normalizedRight);
    }

    private static boolean isSameSingleItem(ItemStack left, ItemStack right) {
        return ItemStack.isSameItemSameTags(normalizedSingle(left), normalizedSingle(right));
    }

    private static ItemStack normalizedSingle(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        ScpPickupRouter.stripUsableSession(copy);
        ScpPickupRouter.stripNoMergeMarker(copy);
        return copy;
    }

    private static void clear() {
        activeSlot = -1;
        activeSourceSlot = -1;
        syncTicks = 0;
        returnArmTicks = 0;
        activeStack = ItemStack.EMPTY;
    }
}
