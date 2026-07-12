package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.mcreator.scpadditions.ScpAdditionsMod;

/**
 * Gives both public SCP Unity door-button items the same placement UX as the
 * unified keycard reader. The visible model is offset from its logical block;
 * the clicked half decides whether the logical anchor stays in place or moves
 * one block so the model appears on the requested side.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DoorButtonPlacementEvents {
    private DoorButtonPlacementEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(event.getHand());
        if (!stack.is(FacilityModule.itemByPath("button_closed").get())
                && !stack.is(FacilityModule.itemByPath("button_locked").get())) {
            return;
        }

        BlockHitResult hit = event.getHitVec();
        if (hit.getDirection().getAxis() == Direction.Axis.Y
                || !(stack.getItem() instanceof BlockItem blockItem)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        BlockPlaceContext originalPlacement = new BlockPlaceContext(
                new UseOnContext(player, event.getHand(), hit));
        BlockPos visualPosition = originalPlacement.getClickedPos();

        // Matches the horizontal facing used by the integrated Unity button.
        Direction buttonFacing = player.getDirection().getOpposite();
        Direction screenLeft = buttonFacing.getClockWise();

        Vec3 clickedCenter = Vec3.atCenterOf(hit.getBlockPos());
        Vec3 offsetFromCenter = hit.getLocation().subtract(clickedCenter);
        double leftCoordinate = offsetFromCenter.x * screenLeft.getStepX()
                + offsetFromCenter.z * screenLeft.getStepZ();
        boolean clickedLeftHalf = leftCoordinate >= 0.0D;

        // The Unity button uses the same right-offset geometry as the old right
        // reader. Moving the logical anchor left pulls the model into the clicked
        // block; leaving it in place exposes it on the opposite edge.
        BlockPos logicalPosition = clickedLeftHalf
                ? visualPosition.relative(screenLeft)
                : visualPosition;

        BlockHitResult shiftedHit = new BlockHitResult(
                Vec3.atCenterOf(logicalPosition),
                hit.getDirection(),
                logicalPosition,
                false);
        BlockPlaceContext shiftedPlacement = new BlockPlaceContext(
                player.level(),
                player,
                event.getHand(),
                stack,
                shiftedHit);

        if (!shiftedPlacement.getClickedPos().equals(logicalPosition)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        InteractionResult result = blockItem.place(shiftedPlacement);
        event.setCanceled(true);
        event.setCancellationResult(result);
    }
}
