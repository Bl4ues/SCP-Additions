package com.bl4ues.scpclassifieddirective.facility;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.item.ScrewdriverItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WallPanelInteractionEvents {
    private WallPanelInteractionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onRightClickBlock(
            PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getItemStack().getItem() instanceof ScrewdriverItem)) {
            return;
        }

        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (state.is(WallPanelModule.BLOCK.get())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(
                    event.getLevel().isClientSide));
            if (event.getLevel().isClientSide) return;
            if (event.getLevel().getBlockEntity(event.getPos())
                    instanceof WallPanelModule.WallPanelBlockEntity panel
                    && panel.hasMaterial()) {
                give(event, panel.removeMaterial());
            }
            return;
        }

        if (!state.is(DoubleWallPanelModule.BLOCK.get())) return;
        DoubleWallPanelModule.Side side =
                DoubleWallPanelModule.DoubleWallPanelBlock.sideForHit(
                        state, event.getHitVec().getDirection());
        if (side == null) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(
                event.getLevel().isClientSide));
        if (event.getLevel().isClientSide) return;
        if (event.getLevel().getBlockEntity(event.getPos())
                instanceof DoubleWallPanelModule.DoubleWallPanelBlockEntity panel
                && panel.hasMaterial(side)) {
            give(event, panel.removeMaterial(side));
        }
    }

    private static void give(PlayerInteractEvent.RightClickBlock event,
            ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!event.getEntity().getInventory().add(stack)) {
            event.getEntity().drop(stack, false);
        }
    }
}
