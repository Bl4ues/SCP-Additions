package com.bl4ues.scpclassifieddirective.inventory.event;

import com.bl4ues.scpclassifieddirective.inventory.capability.ScpInventoryCapability;
import com.bl4ues.scpclassifieddirective.inventory.item.ScpPickupRouter;
import com.bl4ues.scpclassifieddirective.inventory.network.ModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One-way cleanup for player data written by the removed portable crafting UI. */
@Mod.EventBusSubscriber(modid = "scp_classified_directive")
public final class LegacyInventoryDataCleanupEvents {
    private static final String LEGACY_ROOT_KEY = "ScpInventoryCrafting";
    private static final String LEGACY_GRID_KEY = "Grid";

    private LegacyInventoryDataCleanupEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(LEGACY_ROOT_KEY, Tag.TAG_COMPOUND)) return;

        CompoundTag legacy = persisted.getCompound(LEGACY_ROOT_KEY);
        ListTag grid = legacy.getList(LEGACY_GRID_KEY, Tag.TAG_COMPOUND);

        player.getCapability(ScpInventoryCapability.INSTANCE).ifPresent(inventory -> {
            for (int slot = 0; slot < grid.size(); slot++) {
                ItemStack recovered = ItemStack.of(grid.getCompound(slot));
                if (recovered.isEmpty()) continue;

                ScpPickupRouter.stripUsableSession(recovered);
                ScpPickupRouter.stripNoMergeMarker(recovered);
                ScpPickupRouter.stripCoinMirror(recovered);
                ScpPickupRouter.stripHarmfulMirror(recovered);

                int accepted = Math.max(0, ScpPickupRouter.accept(
                        inventory, player, recovered.copy()));
                if (accepted > 0) recovered.shrink(accepted);
                if (!recovered.isEmpty()) player.drop(recovered, false);
            }

            persisted.remove(LEGACY_ROOT_KEY);
            root.put(Player.PERSISTED_NBT_TAG, persisted);
            ModNetwork.syncTo(player, inventory);
        });
    }
}