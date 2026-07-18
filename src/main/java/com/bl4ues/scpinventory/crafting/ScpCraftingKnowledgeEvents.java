package com.bl4ues.scpinventory.crafting;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Learns vanilla or modded crafting recipes after a successful external craft. */
@Mod.EventBusSubscriber(modid = "scp_additions",
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ScpCraftingKnowledgeEvents {
    private ScpCraftingKnowledgeEvents() {
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getInventory() instanceof TransientCraftingContainer grid)) {
            return;
        }
        player.level().getRecipeManager().getRecipeFor(
                        RecipeType.CRAFTING, grid, player.level())
                .ifPresent(recipe -> ScpCraftingService.learn(player,
                        recipe.getId()));
    }
}
