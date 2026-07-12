package net.mcreator.scpadditions.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.ScpAdditionsMod;

/**
 * Keeps Unity door-button pairing optional and manual.
 *
 * Placement never creates a mirrored counterpart. However, when another
 * functional Unity button already exists in the original opposite position,
 * the integrated DoorButtonBlock logic is allowed to synchronize both buttons
 * through CLOSED, OPENING, OPEN and CLOSING. Without a matching counterpart,
 * only the clicked button changes state.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DoorButtonIndependentInteractionEvents {
    private static final int TRANSITION_TICKS = 21;

    private DoorButtonIndependentInteractionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getEntity().getItemInHand(event.getHand());

        // Let the dedicated smart-placement handler own clicks performed while
        // either public button item is held.
        if (held.is(FacilityModule.itemByPath("button_closed").get())
                || held.is(FacilityModule.itemByPath("button_locked").get())) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        Block block = state.getBlock();

        RegistryObject<Block> transition;
        RegistryObject<Block> endpoint;
        if (block == FacilityModule.BUTTON_CLOSED.get()) {
            transition = FacilityModule.BUTTON_OPENING;
            endpoint = FacilityModule.BUTTON_OPEN;
        } else if (block == FacilityModule.BUTTON_OPEN.get()) {
            transition = FacilityModule.BUTTON_CLOSING;
            endpoint = FacilityModule.BUTTON_CLOSED;
        } else {
            return;
        }

        if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return;
        }

        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        BlockPos counterpartPos = pos.relative(facing.getOpposite(), 2);
        BlockState counterpart = event.getLevel().getBlockState(counterpartPos);

        // A manually placed functional counterpart exists exactly where the old
        // mirrored placement used to put it. Let DoorButtonBlock#use execute its
        // original pair-aware state machine, which updates both sides together.
        if (isFunctionalButton(counterpart.getBlock())) {
            return;
        }

        // No matching counterpart: suppress the legacy pair logic and animate
        // only this button. Any mirror that DoorButtonBlock#onPlace attempts to
        // recreate during the state replacement is removed immediately.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        setSingleState(level, pos, facing, transition.get());

        ScpAdditionsMod.queueServerWork(TRANSITION_TICKS, () -> {
            BlockState current = level.getBlockState(pos);
            if (current.getBlock() == transition.get()
                    && current.hasProperty(HorizontalDirectionalBlock.FACING)) {
                Direction currentFacing = current.getValue(HorizontalDirectionalBlock.FACING);
                setSingleState(level, pos, currentFacing, endpoint.get());
            }
        });
    }

    private static void setSingleState(ServerLevel level, BlockPos pos,
            Direction facing, Block target) {
        BlockPos legacyCounterpartPos = pos.relative(facing.getOpposite(), 2);
        BlockState counterpartBefore = level.getBlockState(legacyCounterpartPos);

        level.setBlock(pos, target.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing), Block.UPDATE_ALL);

        // Preserve any manually placed button that was already opposite. Remove
        // only a new counterpart created as a side effect of the legacy onPlace.
        if (!isAnyDoorButton(counterpartBefore.getBlock())) {
            BlockState counterpartAfter = level.getBlockState(legacyCounterpartPos);
            if (isAnyDoorButton(counterpartAfter.getBlock())) {
                level.removeBlock(legacyCounterpartPos, false);
            }
        }
    }

    private static boolean isFunctionalButton(Block block) {
        return block == FacilityModule.BUTTON_CLOSED.get()
                || block == FacilityModule.BUTTON_OPENING.get()
                || block == FacilityModule.BUTTON_OPEN.get()
                || block == FacilityModule.BUTTON_CLOSING.get();
    }

    private static boolean isAnyDoorButton(Block block) {
        return block == FacilityModule.BUTTON_LOCKED.get() || isFunctionalButton(block);
    }
}
