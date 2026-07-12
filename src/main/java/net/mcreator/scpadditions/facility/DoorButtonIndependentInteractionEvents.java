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
 * Replaces the standalone Unity button's paired state machine with a local one.
 * Each placed button opens, closes, emits redstone and drops independently,
 * even when another button happens to exist directly across the wall.
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

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));

        if (!(event.getLevel() instanceof ServerLevel level)
                || !state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return;
        }

        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        setIndependentState(level, pos, facing, transition.get());

        ScpAdditionsMod.queueServerWork(TRANSITION_TICKS, () -> {
            BlockState current = level.getBlockState(pos);
            if (current.getBlock() == transition.get()
                    && current.hasProperty(HorizontalDirectionalBlock.FACING)) {
                Direction currentFacing = current.getValue(HorizontalDirectionalBlock.FACING);
                setIndependentState(level, pos, currentFacing, endpoint.get());
            }
        });
    }

    /**
     * DoorButtonBlock#onPlace still contains the legacy mirrored-placement code.
     * Preserve a pre-existing independent opposite button, but remove any new
     * counterpart that onPlace creates as a side effect of this state change.
     */
    private static void setIndependentState(ServerLevel level, BlockPos pos,
            Direction facing, Block target) {
        BlockPos legacyCounterpartPos = pos.relative(facing.getOpposite(), 2);
        BlockState counterpartBefore = level.getBlockState(legacyCounterpartPos);

        level.setBlock(pos, target.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing), Block.UPDATE_ALL);

        if (!isDoorButton(counterpartBefore.getBlock())) {
            BlockState counterpartAfter = level.getBlockState(legacyCounterpartPos);
            if (isDoorButton(counterpartAfter.getBlock())) {
                level.removeBlock(legacyCounterpartPos, false);
            }
        }
    }

    private static boolean isDoorButton(Block block) {
        return block == FacilityModule.BUTTON_LOCKED.get()
                || block == FacilityModule.BUTTON_CLOSED.get()
                || block == FacilityModule.BUTTON_OPENING.get()
                || block == FacilityModule.BUTTON_OPEN.get()
                || block == FacilityModule.BUTTON_CLOSING.get();
    }
}
