package com.bl4ues.scpinventory.events;

import com.bl4ues.scpinventory.item.ScpPickupRouter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.config.ScpAdditionsModulesConfig;

import java.util.List;

@Mod.EventBusSubscriber(modid = "scp_additions")
public class BlockPickupHandler {

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;

        Entity entity = event.getEntity();
        if (!(entity instanceof ItemEntity itemEntity) || itemEntity.getItem().isEmpty()) return;

        if (!ScpAdditionsModulesConfig.get().inventory.enabled) {
            ItemStack stack = itemEntity.getItem();
            if (stripTransientInventoryTags(stack)) itemEntity.setItem(stack);
            return;
        }

        ItemStack stack = itemEntity.getItem();
        // A controlled USABLE mirror stops being an inventory mirror as soon as
        // it becomes a real world item. Jukebox ejection and similar block/item
        // outputs may preserve the mirror NBT; leaving it attached makes the
        // pickup router reject the stack forever as an active session copy.
        if (ScpPickupRouter.isUsableSession(stack)) {
            ScpPickupRouter.stripUsableSession(stack);
            itemEntity.setItem(stack);
        }

        if (stack.getCount() > 1) {
            splitStackEntity(event, itemEntity, stack);
            return;
        }

        ScpPickupRouter.addNoMergeMarker(stack, itemEntity.getStringUUID());
    }

    private static void splitStackEntity(EntityJoinLevelEvent event, ItemEntity original, ItemStack stack) {
        event.setCanceled(true);

        int count = stack.getCount();
        for (int i = 0; i < count; i++) {
            ItemStack single = stack.copy();
            single.setCount(1);
            ScpPickupRouter.addNoMergeMarker(single, original.getStringUUID() + "-" + i);

            double angle = (Math.PI * 2.0D * i) / Math.max(1, count);
            double offsetX = Math.cos(angle) * 0.035D;
            double offsetZ = Math.sin(angle) * 0.035D;

            ItemEntity split = new ItemEntity(
                    event.getLevel(),
                    original.getX() + offsetX,
                    original.getY(),
                    original.getZ() + offsetZ,
                    single
            );
            split.setPickUpDelay(20);
            split.setDeltaMovement(original.getDeltaMovement().add(offsetX, 0.02D, offsetZ));
            event.getLevel().addFreshEntity(split);
        }
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        ItemStack stack = event.getItem().getItem();
        if (!ScpAdditionsModulesConfig.get().inventory.enabled || player.isCreative() || player.isSpectator()) {
            if (stripTransientInventoryTags(stack)) event.getItem().setItem(stack);
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()
                || ScpAdditionsModulesConfig.get().inventory.enabled
                || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 20 != 0) {
            return;
        }

        Inventory inventory = player.getInventory();
        boolean changed = stripTransientInventoryTags(inventory.items)
                | stripTransientInventoryTags(inventory.offhand)
                | stripTransientInventoryTags(inventory.armor);
        if (player.containerMenu != null) {
            ItemStack carried = player.containerMenu.getCarried();
            changed |= stripTransientInventoryTags(carried);
        }
        if (changed) {
            inventory.setChanged();
            player.inventoryMenu.broadcastChanges();
            if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
        }
    }

    private static boolean stripTransientInventoryTags(List<ItemStack> stacks) {
        boolean changed = false;
        for (ItemStack stack : stacks) changed |= stripTransientInventoryTags(stack);
        return changed;
    }

    private static boolean stripTransientInventoryTags(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) return false;
        CompoundTag tag = stack.getTag();
        if (tag == null) return false;
        boolean changed = tag.contains(ScpPickupRouter.NO_MERGE_TAG)
                || tag.contains(ScpPickupRouter.USABLE_SESSION_TAG)
                || tag.contains(ScpPickupRouter.USABLE_START_TICK_TAG);
        if (!changed) return false;
        ScpPickupRouter.stripNoMergeMarker(stack);
        ScpPickupRouter.stripUsableSession(stack);
        return true;
    }
}
