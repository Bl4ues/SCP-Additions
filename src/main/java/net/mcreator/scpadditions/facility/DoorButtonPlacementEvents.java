package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
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
 * unified keycard reader. The clicked half chooses between the original offset
 * model and a true mirrored internal model; placement never creates the old
 * automatic counterpart on the other side of the wall.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DoorButtonPlacementEvents {
    private DoorButtonPlacementEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(event.getHand());
        boolean locked = stack.is(FacilityModule.itemByPath("button_locked").get());
        boolean functional = stack.is(FacilityModule.itemByPath("button_closed").get());
        if (!locked && !functional) {
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

        Direction buttonFacing = player.getDirection().getOpposite();
        Direction screenLeft = buttonFacing.getClockWise();

        Vec3 clickedCenter = Vec3.atCenterOf(hit.getBlockPos());
        Vec3 offsetFromCenter = hit.getLocation().subtract(clickedCenter);
        double leftCoordinate = offsetFromCenter.x * screenLeft.getStepX()
                + offsetFromCenter.z * screenLeft.getStepZ();
        boolean clickedLeftHalf = leftCoordinate >= 0.0D;

        InteractionResult result;
        if (clickedLeftHalf) {
            // The original model extends toward the selected visual position, so
            // its logical anchor remains one block to screen-left.
            BlockPos logicalPosition = visualPosition.relative(screenLeft);
            BlockHitResult shiftedHit = new BlockHitResult(
                    Vec3.atCenterOf(logicalPosition),
                    hit.getDirection(),
                    logicalPosition,
                    false);
            BlockPlaceContext shiftedPlacement = new BlockPlaceContext(
                    player.level(), player, event.getHand(), stack, shiftedHit);

            if (!shiftedPlacement.getClickedPos().equals(logicalPosition)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                return;
            }

            BlockPos legacyCounterpartPos = logicalPosition.relative(
                    buttonFacing.getOpposite(), 2);
            BlockState counterpartBefore = player.level().getBlockState(legacyCounterpartPos);

            result = blockItem.place(shiftedPlacement);

            if (!player.level().isClientSide && result.consumesAction()
                    && !isAnyDoorButton(counterpartBefore.getBlock())) {
                BlockState counterpartAfter = player.level().getBlockState(legacyCounterpartPos);
                if (isAnyDoorButton(counterpartAfter.getBlock())) {
                    player.level().removeBlock(legacyCounterpartPos, false);
                }
            }
        } else {
            // The right side needs mirrored geometry rather than the same model
            // moved to another block. Its logical anchor is already correct.
            BlockPos logicalPosition = visualPosition;
            result = placeMirrored(player.level(), player, stack, logicalPosition,
                    buttonFacing, locked, hit.getDirection());
        }

        event.setCanceled(true);
        event.setCancellationResult(result);
    }

    private static InteractionResult placeMirrored(Level level, Player player,
            ItemStack stack, BlockPos pos, Direction facing, boolean locked,
            Direction clickedFace) {
        if (!player.mayUseItemAt(pos, clickedFace, stack)) {
            return InteractionResult.FAIL;
        }

        BlockPlaceContext context = new BlockPlaceContext(
                level, player, player.getUsedItemHand(), stack,
                new BlockHitResult(Vec3.atCenterOf(pos), clickedFace, pos, false));
        if (!level.getBlockState(pos).canBeReplaced(context)) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide) {
            Block target = locked
                    ? MirroredDoorButtons.BUTTON_LOCKED.get()
                    : MirroredDoorButtons.BUTTON_CLOSED.get();
            BlockState placed = target.defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, facing);
            if (!level.setBlock(pos, placed, Block.UPDATE_ALL)) {
                return InteractionResult.FAIL;
            }

            SoundType sound = placed.getSoundType(level, pos, player);
            level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
                    (sound.getVolume() + 1.0F) / 2.0F,
                    sound.getPitch() * 0.8F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean isAnyDoorButton(Block block) {
        return block == FacilityModule.BUTTON_LOCKED.get()
                || block == FacilityModule.BUTTON_CLOSED.get()
                || block == FacilityModule.BUTTON_OPENING.get()
                || block == FacilityModule.BUTTON_OPEN.get()
                || block == FacilityModule.BUTTON_CLOSING.get()
                || MirroredDoorButtons.isAny(block);
    }
}
