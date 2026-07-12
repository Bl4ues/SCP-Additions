package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
 * Places one public Unity button item as either the original right-side model
 * or the authored left-side model. Both the orientation and the left/right test
 * are derived from the wall face that was clicked, never from player yaw.
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
        Direction clickedFace = hit.getDirection();
        if (clickedFace.getAxis() == Direction.Axis.Y) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        BlockPlaceContext originalPlacement = new BlockPlaceContext(
                new UseOnContext(player, event.getHand(), hit));
        BlockPos visualPosition = originalPlacement.getClickedPos();

        // Looking straight at the selected face, clockwise from its outward
        // normal is screen-left. This remains stable even when the player looks
        // diagonally or turns immediately before placing the panel.
        Direction screenLeft = clickedFace.getClockWise();
        Vec3 clickedCenter = Vec3.atCenterOf(hit.getBlockPos());
        Vec3 offsetFromCenter = hit.getLocation().subtract(clickedCenter);
        double leftCoordinate = offsetFromCenter.x * screenLeft.getStepX()
                + offsetFromCenter.z * screenLeft.getStepZ();
        boolean clickedLeftHalf = leftCoordinate >= 0.0D;

        BlockPos logicalPosition;
        Block target;
        if (clickedLeftHalf) {
            // The original model is offset from its logical block in one
            // direction, so its anchor lives one block to screen-left.
            logicalPosition = visualPosition.relative(screenLeft);
            target = locked ? FacilityModule.BUTTON_LOCKED.get()
                    : FacilityModule.BUTTON_CLOSED.get();
        } else {
            // The authored opposite-side model uses the inverse anchor. Matching
            // the reader placement contract keeps the visible panel on the face
            // half that was clicked while the redstone block sits on the other side.
            logicalPosition = visualPosition.relative(screenLeft.getOpposite());
            target = locked ? LeftDoorButtons.BUTTON_LOCKED.get()
                    : LeftDoorButtons.BUTTON_CLOSED.get();
        }

        InteractionResult result = placeButton(player.level(), player, event.getHand(),
                stack, logicalPosition, clickedFace, target);
        event.setCanceled(true);
        event.setCancellationResult(result);
    }

    private static InteractionResult placeButton(Level level, Player player,
            InteractionHand hand, ItemStack stack, BlockPos pos,
            Direction facing, Block target) {
        if (!player.mayUseItemAt(pos, facing, stack)) {
            return InteractionResult.FAIL;
        }

        BlockPlaceContext context = new BlockPlaceContext(
                level, player, hand, stack,
                new BlockHitResult(Vec3.atCenterOf(pos), facing, pos, false));
        if (!level.getBlockState(pos).canBeReplaced(context)) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide) {
            BlockPos legacyCounterpartPos = pos.relative(facing.getOpposite(), 2);
            BlockState counterpartBefore = level.getBlockState(legacyCounterpartPos);

            BlockState placed = target.defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, facing);
            if (!level.setBlock(pos, placed, Block.UPDATE_ALL)) {
                return InteractionResult.FAIL;
            }

            // Base Unity states still contain the old automatic counterpart
            // hook. Keep a manually placed panel, but remove only a panel that
            // was generated as a side effect of this placement.
            if (!isAnyDoorButton(counterpartBefore.getBlock())) {
                BlockState counterpartAfter = level.getBlockState(legacyCounterpartPos);
                if (isAnyDoorButton(counterpartAfter.getBlock())) {
                    level.removeBlock(legacyCounterpartPos, false);
                }
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
                || LeftDoorButtons.isAny(block)
                || MirroredDoorButtons.isAny(block);
    }
}
