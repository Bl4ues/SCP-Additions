package com.bl4ues.scpclassifieddirective.facility.transform;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.LeftDoorButtons;
import com.bl4ues.scpclassifieddirective.facility.MirroredDoorButtons;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import com.bl4ues.scpclassifieddirective.init.UnifiedReaderItems;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Placement convention for wall fixtures whose logical block anchor is not
 * where their visible model sits.
 *
 * <p>Unity door buttons/readers intentionally keep their controller cell next
 * to the visible panel. Vanilla placement already honors that authored offset.
 * Transformed construction must preserve the same contract in its local grid
 * rather than "fixing" the model back to the controller center.</p>
 */
public final class TransformWallFixturePlacement {
    private TransformWallFixturePlacement() {
    }

    public static Placement resolve(BlockItem item, Direction outward,
            double localX, double localZ) {
        if (item == null || outward == null
                || outward.getAxis() == Direction.Axis.Y) return null;

        Direction screenLeft = outward.getClockWise();
        double leftCoordinate = localX * screenLeft.getStepX()
                + localZ * screenLeft.getStepZ();
        boolean clickedLeftHalf = leftCoordinate >= 0.0D;
        Direction logicalShift = clickedLeftHalf
                ? screenLeft : screenLeft.getOpposite();

        Block target = null;
        Block publicBlock = item.getBlock();
        if (publicBlock == FacilityModule.BUTTON_CLOSED.get()) {
            target = clickedLeftHalf ? FacilityModule.BUTTON_CLOSED.get()
                    : LeftDoorButtons.BUTTON_CLOSED.get();
        } else if (publicBlock == FacilityModule.BUTTON_LOCKED.get()) {
            target = clickedLeftHalf ? FacilityModule.BUTTON_LOCKED.get()
                    : LeftDoorButtons.BUTTON_LOCKED.get();
        } else if (item == UnifiedReaderItems.KEYCARD_READER.get()) {
            target = clickedLeftHalf
                    ? ScpClassifiedDirectiveModBlocks.RIGHT_READER.get()
                    : ScpClassifiedDirectiveModBlocks.LEFT_READER.get();
        }
        if (target == null) return null;

        BlockState state = target.defaultBlockState();
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            state = state.setValue(HorizontalDirectionalBlock.FACING, outward);
        }
        return new Placement(logicalShift, state);
    }

    /**
     * Returns the local offset from controller/anchor cell to the cell the
     * player visually perceives as containing this fixture.
     */
    public static Direction visualShift(BlockState state) {
        if (state == null
                || !state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return null;
        }
        Block block = state.getBlock();
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        Direction screenLeft = facing.getClockWise();

        if (isOriginalDoorButton(block)) {
            return screenLeft.getOpposite();
        }
        if (LeftDoorButtons.isAny(block) || MirroredDoorButtons.isAny(block)) {
            return screenLeft;
        }

        ReaderSide reader = readerSide(block);
        if (reader == ReaderSide.RIGHT) return screenLeft.getOpposite();
        if (reader == ReaderSide.LEFT) return screenLeft;
        return null;
    }

    public static boolean isDoorButton(BlockState state) {
        if (state == null) return false;
        Block block = state.getBlock();
        return isOriginalDoorButton(block)
                || LeftDoorButtons.isAny(block)
                || MirroredDoorButtons.isAny(block);
    }

    public static boolean isKeycardReader(BlockState state) {
        return state != null && readerSide(state.getBlock()) != ReaderSide.NONE;
    }

    public static DoorButtonPhase doorButtonPhase(BlockState state) {
        if (!isDoorButton(state)) return null;
        Block block = state.getBlock();
        if (block == FacilityModule.BUTTON_LOCKED.get()
                || block == LeftDoorButtons.BUTTON_LOCKED.get()
                || block == MirroredDoorButtons.BUTTON_LOCKED.get()) {
            return DoorButtonPhase.LOCKED;
        }
        if (block == FacilityModule.BUTTON_CLOSED.get()
                || block == LeftDoorButtons.BUTTON_CLOSED.get()
                || block == MirroredDoorButtons.BUTTON_CLOSED.get()) {
            return DoorButtonPhase.CLOSED;
        }
        if (block == FacilityModule.BUTTON_OPENING.get()
                || block == LeftDoorButtons.BUTTON_OPENING.get()
                || block == MirroredDoorButtons.BUTTON_OPENING.get()) {
            return DoorButtonPhase.OPENING;
        }
        if (block == FacilityModule.BUTTON_OPEN.get()
                || block == LeftDoorButtons.BUTTON_OPEN.get()
                || block == MirroredDoorButtons.BUTTON_OPEN.get()) {
            return DoorButtonPhase.OPEN;
        }
        if (block == FacilityModule.BUTTON_CLOSING.get()
                || block == LeftDoorButtons.BUTTON_CLOSING.get()
                || block == MirroredDoorButtons.BUTTON_CLOSING.get()) {
            return DoorButtonPhase.CLOSING;
        }
        return null;
    }

    public static BlockState doorButtonState(BlockState current,
            DoorButtonPhase target) {
        if (current == null || target == null
                || !current.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return current;
        }
        boolean left = LeftDoorButtons.isAny(current.getBlock())
                || MirroredDoorButtons.isAny(current.getBlock());
        Block block;
        if (left) {
            block = switch (target) {
                case LOCKED -> LeftDoorButtons.BUTTON_LOCKED.get();
                case CLOSED -> LeftDoorButtons.BUTTON_CLOSED.get();
                case OPENING -> LeftDoorButtons.BUTTON_OPENING.get();
                case OPEN -> LeftDoorButtons.BUTTON_OPEN.get();
                case CLOSING -> LeftDoorButtons.BUTTON_CLOSING.get();
            };
        } else {
            block = switch (target) {
                case LOCKED -> FacilityModule.BUTTON_LOCKED.get();
                case CLOSED -> FacilityModule.BUTTON_CLOSED.get();
                case OPENING -> FacilityModule.BUTTON_OPENING.get();
                case OPEN -> FacilityModule.BUTTON_OPEN.get();
                case CLOSING -> FacilityModule.BUTTON_CLOSING.get();
            };
        }
        BlockState result = block.defaultBlockState();
        if (result.hasProperty(HorizontalDirectionalBlock.FACING)) {
            result = result.setValue(HorizontalDirectionalBlock.FACING,
                    current.getValue(HorizontalDirectionalBlock.FACING));
        }
        return result;
    }

    private static boolean isOriginalDoorButton(Block block) {
        return block == FacilityModule.BUTTON_LOCKED.get()
                || block == FacilityModule.BUTTON_CLOSED.get()
                || block == FacilityModule.BUTTON_OPENING.get()
                || block == FacilityModule.BUTTON_OPEN.get()
                || block == FacilityModule.BUTTON_CLOSING.get();
    }

    private static ReaderSide readerSide(Block block) {
        if (block == null) return ReaderSide.NONE;
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        if (id == null
                || !ScpClassifiedDirectiveMod.MODID.equals(id.getNamespace())) {
            return ReaderSide.NONE;
        }
        String path = id.getPath();
        if (!path.contains("reader")) return ReaderSide.NONE;
        if (path.contains("right")) return ReaderSide.RIGHT;
        if (path.contains("left")) return ReaderSide.LEFT;
        return ReaderSide.NONE;
    }

    public record Placement(Direction logicalShift, BlockState state) {
    }

    public enum DoorButtonPhase {
        LOCKED, CLOSED, OPENING, OPEN, CLOSING
    }

    private enum ReaderSide {
        NONE, LEFT, RIGHT
    }
}
