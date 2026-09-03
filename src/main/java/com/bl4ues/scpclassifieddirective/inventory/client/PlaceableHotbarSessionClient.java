package com.bl4ues.scpclassifieddirective.inventory.client;

import com.bl4ues.scpclassifieddirective.inventory.item.ScpItemClassifier;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpItemType;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpPickupRouter;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
import com.bl4ues.scpclassifieddirective.inventory.network.UsableSessionDropPacket;
import com.bl4ues.scpclassifieddirective.inventory.network.UsableSessionReturnPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client owner for the independent PLACEABLE-style entry in the custom hotbar. */
@Mod.EventBusSubscriber(modid = "scp_classified_directive", value = Dist.CLIENT)
public final class PlaceableHotbarSessionClient {
    private static int activeSlot = -1;
    private static int activeSourceSlot = -1;
    private static ItemStack activeStack = ItemStack.EMPTY;

    private PlaceableHotbarSessionClient() {
    }

    public static void start(int hotbarSlot, int sourceSlot, ItemStack stack) {
        if (hotbarSlot < 0 || hotbarSlot >= 9 || stack == null
                || stack.isEmpty() || !isSupportedSessionItem(stack)) {
            clear();
            return;
        }

        if (activeSlot >= 0 && (activeSlot != hotbarSlot
                || !isSameSingleItem(activeStack, stack))) {
            clearClientSessionCopy();
        }
        activeSlot = hotbarSlot;
        activeSourceSlot = sourceSlot;
        activeStack = normalized(stack);
    }

    public static boolean returnSelectedSession(int selectedSlot) {
        if (activeSlot < 0 || selectedSlot != activeSlot) return false;
        ModNetwork.CHANNEL.sendToServer(new UsableSessionReturnPacket(activeSlot));
        clearClientSessionCopy();
        clear();
        return true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            handleDropKey();
            return;
        }
        if (event.phase != TickEvent.Phase.END || activeSlot < 0) return;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || player.isCreative()
                || player.isSpectator() || activeSlot >= player.getInventory().items.size()) {
            clear();
            return;
        }

        ItemStack actual = player.getInventory().items.get(activeSlot);
        // A placed/consumed real hotbar item must never be recreated client-side
        // after vanilla has removed it.
        if (actual.isEmpty()) {
            clear();
            return;
        }
        if (!isSupportedSessionItem(actual)
                || !isSameSingleItem(actual, activeStack)) {
            clear();
            return;
        }
        activeStack = normalized(actual);
    }

    private static void handleDropKey() {
        if (activeSlot < 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null
                || player.isCreative() || player.isSpectator()
                || player.getInventory().selected != activeSlot) {
            return;
        }
        if (!minecraft.options.keyDrop.consumeClick()) return;

        ModNetwork.CHANNEL.sendToServer(new UsableSessionDropPacket(activeSlot));
        clearClientSessionCopy();
        clear();
    }

    private static void clearClientSessionCopy() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || activeSlot < 0
                || activeSlot >= player.getInventory().items.size()) {
            return;
        }
        ItemStack actual = player.getInventory().items.get(activeSlot);
        if (!actual.isEmpty() && !activeStack.isEmpty()
                && isSupportedSessionItem(actual)
                && isSameSingleItem(actual, activeStack)) {
            player.getInventory().setItem(activeSlot, ItemStack.EMPTY);
            player.getInventory().setChanged();
        }
    }

    private static boolean isSupportedSessionItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return ScpItemClassifier.getType(stack) == ScpItemType.PLACEABLE
                || ScpItemClassifier.getEquipmentSlot(stack).isPresent();
    }

    private static boolean isSameSingleItem(ItemStack left, ItemStack right) {
        ItemStack normalizedLeft = normalized(left);
        ItemStack normalizedRight = normalized(right);
        return !normalizedLeft.isEmpty() && !normalizedRight.isEmpty()
                && ItemStack.isSameItemSameTags(normalizedLeft, normalizedRight);
    }

    private static ItemStack normalized(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        ScpPickupRouter.stripUsableSession(copy);
        ScpPickupRouter.stripNoMergeMarker(copy);
        return copy;
    }

    private static void clear() {
        activeSlot = -1;
        activeSourceSlot = -1;
        activeStack = ItemStack.EMPTY;
    }
}
