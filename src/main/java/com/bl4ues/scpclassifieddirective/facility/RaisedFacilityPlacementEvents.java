package com.bl4ues.scpclassifieddirective.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/**
 * The original Unity TV remains authored one block below its logical anchor.
 * Framed signs now carry corrected model coordinates and use ordinary placement.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RaisedFacilityPlacementEvents {
    private RaisedFacilityPlacementEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(
            PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(event.getHand());
        if (!stack.is(FacilityModule.itemByPath("tv").get())
                || !(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        BlockHitResult hit = event.getHitVec();
        BlockPlaceContext original = new BlockPlaceContext(
                new UseOnContext(player, event.getHand(), hit));
        BlockPos raisedTarget = original.getClickedPos().above();

        Vec3 raisedLocation = hit.getLocation().add(0.0D, 1.0D, 0.0D);
        BlockHitResult raisedHit = new BlockHitResult(raisedLocation,
                hit.getDirection(), raisedTarget, hit.isInside());
        BlockPlaceContext raisedContext = new BlockPlaceContext(
                player.level(), player, event.getHand(), stack, raisedHit);

        if (!raisedContext.getClickedPos().equals(raisedTarget)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        InteractionResult result = blockItem.place(raisedContext);
        event.setCanceled(true);
        event.setCancellationResult(result);
    }
}
