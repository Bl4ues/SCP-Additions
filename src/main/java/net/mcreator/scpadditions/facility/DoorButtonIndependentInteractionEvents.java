package net.mcreator.scpadditions.facility;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Optional manual pairing for Unity door buttons.
 *
 * Placement never creates a counterpart. If another functional button already
 * exists exactly two blocks through the wall, both preserve their own right- or
 * left-side geometry while changing state together.
 */
@EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class DoorButtonIndependentInteractionEvents {
    private static final int TRANSITION_TICKS = 21;
    private static final int HEAVY_DOOR_TRANSITION_TICKS = 24;

    private DoorButtonIndependentInteractionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getEntity().getItemInHand(event.getHand());
        if (held.is(FacilityModule.itemByPath("button_closed").get())
                || held.is(FacilityModule.itemByPath("button_locked").get())) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        Phase phase = phaseOf(state.getBlock());
        if (phase != Phase.CLOSED && phase != Phase.OPEN) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));

        if (event.getLevel() instanceof ServerLevel level) {
            activateButton(level, pos);
        }
    }

    /** Direct block-level fallback used by every visual variant. */
    public static boolean activateButton(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        Phase phase = phaseOf(state.getBlock());
        if ((phase != Phase.CLOSED && phase != Phase.OPEN)
                || !state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return false;
        }

        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        BlockPos counterpartPos = pos.relative(facing.getOpposite(), 2);
        BlockState counterpartState = level.getBlockState(counterpartPos);
        boolean linked = isFunctional(counterpartState.getBlock())
                && counterpartState.hasProperty(HorizontalDirectionalBlock.FACING)
                && counterpartState.getValue(HorizontalDirectionalBlock.FACING) == facing.getOpposite();

        Phase transition = phase == Phase.CLOSED ? Phase.OPENING : Phase.CLOSING;
        Phase endpoint = phase == Phase.CLOSED ? Phase.OPEN : Phase.CLOSED;
        boolean clickedLeft = usesLeftGeometry(state.getBlock());
        boolean counterpartLeft = linked && usesLeftGeometry(counterpartState.getBlock());
        Direction counterpartFacing = linked
                ? counterpartState.getValue(HorizontalDirectionalBlock.FACING)
                : facing.getOpposite();

        setState(level, pos, facing, clickedLeft, transition);
        if (linked) {
            setState(level, counterpartPos, counterpartFacing,
                    counterpartLeft, transition);
        }

        ScpAdditionsMod.queueServerWork(TRANSITION_TICKS, () -> {
            completeTransition(level, pos, endpoint);
            if (linked) {
                completeTransition(level, counterpartPos, endpoint);
            }
        });
        return true;
    }

    /**
     * Synchronizes only panels that can physically feed the selected heavy-door
     * controller or its upper redstone relay.
     */
    public static int synchronizeDoorPanels(ServerLevel level, BlockPos doorPos,
            boolean opening) {
        if (level == null || doorPos == null) {
            return 0;
        }

        Set<BlockPos> panelPositions = new LinkedHashSet<>();
        for (int yOffset = 0; yOffset <= 2; yOffset++) {
            BlockPos probe = doorPos.above(yOffset);
            for (Direction direction : Direction.values()) {
                BlockPos candidate = probe.relative(direction);
                BlockState candidateState = level.getBlockState(candidate);
                if (isFunctional(candidateState.getBlock())
                        && candidateState.hasProperty(HorizontalDirectionalBlock.FACING)) {
                    panelPositions.add(candidate.immutable());
                }
            }
        }

        if (panelPositions.isEmpty()) {
            return 0;
        }

        Phase transition = opening ? Phase.OPENING : Phase.CLOSING;
        Phase endpoint = opening ? Phase.OPEN : Phase.CLOSED;
        List<BlockPos> stablePositions = List.copyOf(panelPositions);

        for (BlockPos panelPos : stablePositions) {
            BlockState panelState = level.getBlockState(panelPos);
            if (!isFunctional(panelState.getBlock())
                    || !panelState.hasProperty(HorizontalDirectionalBlock.FACING)) {
                continue;
            }
            setState(level, panelPos,
                    panelState.getValue(HorizontalDirectionalBlock.FACING),
                    usesLeftGeometry(panelState.getBlock()),
                    transition);
        }

        ScpAdditionsMod.queueServerWork(HEAVY_DOOR_TRANSITION_TICKS, () -> {
            for (BlockPos panelPos : stablePositions) {
                completeTransition(level, panelPos, endpoint);
            }
        });
        return stablePositions.size();
    }

    private static void completeTransition(ServerLevel level, BlockPos pos, Phase endpoint) {
        BlockState current = level.getBlockState(pos);
        Phase currentPhase = phaseOf(current.getBlock());
        if ((endpoint == Phase.OPEN && currentPhase != Phase.OPENING)
                || (endpoint == Phase.CLOSED && currentPhase != Phase.CLOSING)
                || !current.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return;
        }

        setState(level, pos,
                current.getValue(HorizontalDirectionalBlock.FACING),
                usesLeftGeometry(current.getBlock()),
                endpoint);
    }

    private static void setState(ServerLevel level, BlockPos pos,
            Direction facing, boolean leftGeometry, Phase targetPhase) {
        Block target = blockFor(targetPhase, leftGeometry);
        level.setBlock(pos, target.defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing), Block.UPDATE_ALL);
        level.updateNeighborsAt(pos, target);
    }

    private static Block blockFor(Phase phase, boolean leftGeometry) {
        if (leftGeometry) {
            return switch (phase) {
                case LOCKED -> LeftDoorButtons.BUTTON_LOCKED.get();
                case CLOSED -> LeftDoorButtons.BUTTON_CLOSED.get();
                case OPENING -> LeftDoorButtons.BUTTON_OPENING.get();
                case OPEN -> LeftDoorButtons.BUTTON_OPEN.get();
                case CLOSING -> LeftDoorButtons.BUTTON_CLOSING.get();
            };
        }
        return switch (phase) {
            case LOCKED -> FacilityModule.BUTTON_LOCKED.get();
            case CLOSED -> FacilityModule.BUTTON_CLOSED.get();
            case OPENING -> FacilityModule.BUTTON_OPENING.get();
            case OPEN -> FacilityModule.BUTTON_OPEN.get();
            case CLOSING -> FacilityModule.BUTTON_CLOSING.get();
        };
    }

    private static Phase phaseOf(Block block) {
        if (block == FacilityModule.BUTTON_LOCKED.get()
                || block == LeftDoorButtons.BUTTON_LOCKED.get()
                || block == MirroredDoorButtons.BUTTON_LOCKED.get()) return Phase.LOCKED;
        if (block == FacilityModule.BUTTON_CLOSED.get()
                || block == LeftDoorButtons.BUTTON_CLOSED.get()
                || block == MirroredDoorButtons.BUTTON_CLOSED.get()) return Phase.CLOSED;
        if (block == FacilityModule.BUTTON_OPENING.get()
                || block == LeftDoorButtons.BUTTON_OPENING.get()
                || block == MirroredDoorButtons.BUTTON_OPENING.get()) return Phase.OPENING;
        if (block == FacilityModule.BUTTON_OPEN.get()
                || block == LeftDoorButtons.BUTTON_OPEN.get()
                || block == MirroredDoorButtons.BUTTON_OPEN.get()) return Phase.OPEN;
        if (block == FacilityModule.BUTTON_CLOSING.get()
                || block == LeftDoorButtons.BUTTON_CLOSING.get()
                || block == MirroredDoorButtons.BUTTON_CLOSING.get()) return Phase.CLOSING;
        return null;
    }

    private static boolean usesLeftGeometry(Block block) {
        return LeftDoorButtons.isAny(block) || MirroredDoorButtons.isAny(block);
    }

    private static boolean isFunctional(Block block) {
        Phase phase = phaseOf(block);
        return phase != null && phase != Phase.LOCKED;
    }

    private static boolean isAnyButton(Block block) {
        return phaseOf(block) != null;
    }

    private enum Phase {
        LOCKED,
        CLOSED,
        OPENING,
        OPEN,
        CLOSING
    }
}
