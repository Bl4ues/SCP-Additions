package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.item.ScrewdriverItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Gives the Wall Panel's Screwdriver reset priority over generic right-click
 * handlers. The block use path keeps the same behavior as a fallback, but this
 * event guarantees the dedicated tool interaction is not swallowed first.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WallPanelInteractionEvents {
    private WallPanelInteractionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().getBlockState(event.getPos())
                .is(WallPanelModule.BLOCK.get())
                || !(event.getItemStack().getItem() instanceof ScrewdriverItem)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(
                event.getLevel().isClientSide));

        if (event.getLevel().isClientSide
                || !(event.getLevel().getBlockEntity(event.getPos())
                    instanceof WallPanelModule.WallPanelBlockEntity panel)
                || !panel.hasMaterial()) {
            return;
        }

        ItemStack returned = panel.removeMaterial();
        if (!returned.isEmpty()
                && !event.getEntity().getInventory().add(returned)) {
            event.getEntity().drop(returned, false);
        }
    }
}
