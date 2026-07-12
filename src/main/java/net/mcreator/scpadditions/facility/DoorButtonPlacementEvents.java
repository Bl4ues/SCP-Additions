package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
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
 * the clicked half decides where that one logical button is anchored.
 *
 * The standalone Unity implementation also created a mirrored counterpart on
 * the opposite side of the wall. That behavior is intentionally removed here:
 * every placement creates exactly one independent button.
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

        // The Unity model is offset from its logical block. Shift only the
        // logical anchor required to make the one visible button land on the
        // half that was clicked.
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

        // DoorButtonBlock#onPlace from the original Unity implementation still
        // attempts to create a mirrored counterpart. Snapshot that position so
        // we remove only a counterpart created by this placement, never a button
        // that was already there independently.
        BlockPos legacyCounterpartPos = logicalPosition.relative(buttonFacing.getOpposite(), 2);
        BlockState counterpartBefore = player.level().getBlockState(legacyCounterpartPos);

        InteractionResult result = blockItem.place(shiftedPlacement);

        if (!player.level().isClientSide && result.consumesAction()
                && !isDoorButton(counterpartBefore.getBlock())) {
            BlockState counterpartAfter = player.level().getBlockState(legacyCounterpartPos);
            if (isDoorButton(counterpartAfter.getBlock())) {
                player.level().removeBlock(legacyCounterpartPos, false);
            }
        }

        event.setCanceled(true);
        event.setCancellationResult(result);
    }

    private static boolean isDoorButton(Block block) {
        return block == FacilityModule.BUTTON_LOCKED.get()
                || block == FacilityModule.BUTTON_CLOSED.get()
                || block == FacilityModule.BUTTON_OPENING.get()
                || block == FacilityModule.BUTTON_OPEN.get()
                || block == FacilityModule.BUTTON_CLOSING.get();
    }
}
