package com.bl4ues.scpclassifieddirective.block;

import com.bl4ues.scpclassifieddirective.block.entity.DecontaminationBlockEntity;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.FacilityStructureBreakGuard;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import com.bl4ues.scpclassifieddirective.procedures.DecontaminationCheckpointController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Placement, ownership, collision and transforms for the rebuilt checkpoint. */
public final class DecontaminationStructure {
    /** Entrance at 0, five chamber cells at 1..5, exit at 6. */
    public static final int EXIT_FORWARD = 6;
    /** 4.75 seconds after the decontamination sequence begins. */
    public static final int DOOR_RELEASE_TICK = 95;
    private static final int FIRST_INTERIOR_FORWARD = 1;
    private static final int LAST_INTERIOR_FORWARD = 5;
    private static final int LIGHT_FORWARD = 3;
    private static final int LIGHT_HEIGHT = 3;

    private DecontaminationStructure() {
    }

    public static boolean hasPlacementSupport(Level level,
            BlockPos controllerPos, Direction facing) {
        BlockPos support = entranceDoorPosition(controllerPos, facing).below();
        return level.getWorldBorder().isWithinBounds(support)
                && level.getBlockState(support).isFaceSturdy(level, support,
                Direction.UP);
    }

    public static boolean canPlace(Level level, BlockPos controllerPos,
            Direction facing) {
        return canPlace(level, controllerPos, facing, null);
    }

    public static boolean canPlace(Level level, BlockPos controllerPos,
            Direction facing, @Nullable BlockPos allowedOccupiedPos) {
        return hasPlacementSupport(level, controllerPos, facing)
                && collectObstructions(level, controllerPos, facing,
                allowedOccupiedPos).isEmpty();
    }

    /**
     * Checks the whole authored visual envelope, not only the helper blocks.
     * Interior cells are deliberately not reserved after placement, so map
     * builders can still add content to the chamber later.
     */
    public static List<BlockPos> collectObstructions(Level level,
            BlockPos controllerPos, Direction facing,
            @Nullable BlockPos allowedOccupiedPos) {
        Set<BlockPos> blockers = new LinkedHashSet<>();

        // The GeckoLib body occupies the five chamber cells. Its sloped roof
        // reaches into height 4 even though collision uses a simple height-3
        // rectangular ceiling, so placement reserves that visual clearance too.
        for (int forward = FIRST_INTERIOR_FORWARD;
                forward <= LAST_INTERIOR_FORWARD; forward++) {
            for (int height = -1; height <= 4; height++) {
                for (int side = -1; side <= 1; side++) {
                    addBlockerIfOccupied(level, controllerPos, facing,
                            side, height, forward, allowedOccupiedPos, blockers);
                }
            }
        }

        // Door side cells stay completely free so pre-existing or later walls
        // can meet the checkpoint. Only the two-block-tall centerline occupied
        // by each heavy BLACK_DOOR is required to be clear.
        for (int height = -1; height <= 0; height++) {
            addBlockerIfOccupied(level, controllerPos, facing,
                    0, height, 0, allowedOccupiedPos, blockers);
            addBlockerIfOccupied(level, controllerPos, facing,
                    0, height, EXIT_FORWARD, allowedOccupiedPos, blockers);
        }

        return List.copyOf(blockers);
    }

    private static void addBlockerIfOccupied(Level level,
            BlockPos controllerPos, Direction facing, int side, int height,
            int forward, @Nullable BlockPos allowedOccupiedPos,
            Set<BlockPos> blockers) {
        BlockPos candidate = partPosition(controllerPos, facing,
                side, height, forward);
        if (candidate.equals(allowedOccupiedPos)) return;
        if (!level.getWorldBorder().isWithinBounds(candidate)
                || !level.getBlockState(candidate).canBeReplaced()) {
            blockers.add(candidate.immutable());
        }
    }

    /** Places all owned helpers atomically after the controller exists. */
    public static boolean placeStructure(Level level, BlockPos controllerPos,
            Direction facing) {
        if (!isController(level.getBlockState(controllerPos))) return false;

        BlockPos lightPos = lightPosition(controllerPos, facing);
        BlockPos entrance = entranceDoorPosition(controllerPos, facing);
        BlockPos exit = exitDoorPosition(controllerPos, facing);
        if (!canPlaceCollisionParts(level, controllerPos, facing)
                || !level.getBlockState(lightPos).canBeReplaced()
                || !level.getBlockState(entrance).canBeReplaced()
                || !level.getBlockState(exit).canBeReplaced()) {
            return false;
        }

        if (!placeCollisionParts(level, controllerPos, facing)) return false;
        level.setBlock(lightPos, DecontaminationStructureBlocks.light()
                .defaultBlockState().setValue(DecontaminationLightBlock.FACING,
                        facing), Block.UPDATE_ALL);

        if (!placeDoor(level, facing, entrance)
                || !placeDoor(level, facing, exit)) {
            removeStructureParts(level, controllerPos,
                    level.getBlockState(controllerPos));
            return false;
        }

        nudgeOwnedDoors(level, controllerPos, facing);
        return true;
    }

    public static void ensureStructure(Level level, BlockPos controllerPos,
            Direction facing) {
        if (!isController(level.getBlockState(controllerPos))) return;
        ensureCollisionParts(level, controllerPos, facing);

        BlockPos lightPos = lightPosition(controllerPos, facing);
        BlockState lightState = level.getBlockState(lightPos);
        if (!isValidLight(level, lightPos, lightState)
                && lightState.canBeReplaced()) {
            level.setBlock(lightPos, DecontaminationStructureBlocks.light()
                    .defaultBlockState().setValue(DecontaminationLightBlock.FACING,
                            facing), Block.UPDATE_ALL);
        }

        ensureDoor(level, facing, entranceDoorPosition(controllerPos, facing));
        ensureDoor(level, facing, exitDoorPosition(controllerPos, facing));
        nudgeOwnedDoors(level, controllerPos, facing);
    }

    private static boolean canPlaceCollisionParts(Level level,
            BlockPos controllerPos, Direction facing) {
        for (PartAddress part : collisionParts()) {
            BlockPos pos = partPosition(controllerPos, facing,
                    part.side, part.height, part.forward);
            BlockState current = level.getBlockState(pos);
            if (current.getBlock() == DecontaminationStructureBlocks.collision()
                    && isExpectedCollisionState(current, facing, part)
                    && controllerPosition(pos, current).equals(controllerPos)) {
                continue;
            }
            if (!current.canBeReplaced()) return false;
        }
        return true;
    }

    public static boolean placeCollisionParts(Level level,
            BlockPos controllerPos, Direction facing) {
        if (!canPlaceCollisionParts(level, controllerPos, facing)) return false;
        for (PartAddress part : collisionParts()) {
            BlockPos pos = partPosition(controllerPos, facing,
                    part.side, part.height, part.forward);
            BlockState current = level.getBlockState(pos);
            if (current.getBlock() == DecontaminationStructureBlocks.collision()
                    && isExpectedCollisionState(current, facing, part)
                    && controllerPosition(pos, current).equals(controllerPos)) {
                continue;
            }
            level.setBlock(pos, collisionState(level, pos, facing, part),
                    Block.UPDATE_ALL);
        }
        return true;
    }

    public static void ensureCollisionParts(Level level,
            BlockPos controllerPos, Direction facing) {
        if (!isController(level.getBlockState(controllerPos))) return;
        for (PartAddress part : collisionParts()) {
            BlockPos pos = partPosition(controllerPos, facing,
                    part.side, part.height, part.forward);
            BlockState current = level.getBlockState(pos);
            if (current.getBlock() == DecontaminationStructureBlocks.collision()
                    && isExpectedCollisionState(current, facing, part)
                    && controllerPosition(pos, current).equals(controllerPos)) {
                continue;
            }
            if (current.canBeReplaced()) {
                level.setBlock(pos, collisionState(level, pos, facing, part),
                        Block.UPDATE_ALL);
            }
        }
    }

    public static void removeStructureParts(Level level,
            BlockPos controllerPos, BlockState controllerState) {
        if (!controllerState.hasProperty(HorizontalDirectionalBlock.FACING)) return;
        Direction facing = controllerState.getValue(HorizontalDirectionalBlock.FACING);

        for (PartAddress part : collisionParts()) {
            BlockPos pos = partPosition(controllerPos, facing,
                    part.side, part.height, part.forward);
            BlockState current = level.getBlockState(pos);
            if (current.getBlock() == DecontaminationStructureBlocks.collision()
                    && controllerPosition(pos, current).equals(controllerPos)) {
                clearBlock(level, pos, current);
            }
        }

        BlockPos lightPos = lightPosition(controllerPos, facing);
        BlockState light = level.getBlockState(lightPos);
        if (isValidLight(level, lightPos, light)) {
            clearBlock(level, lightPos, light);
        }

        removeOwnedDoor(level, controllerPos,
                entranceDoorPosition(controllerPos, facing));
        removeOwnedDoor(level, controllerPos,
                exitDoorPosition(controllerPos, facing));
    }

    /** Legacy alias retained while old generated classes still exist. */
    public static void removeCollisionParts(Level level,
            BlockPos controllerPos, BlockState controllerState) {
        removeStructureParts(level, controllerPos, controllerState);
    }

    public static void destroyFromCollision(Level level, BlockPos partPos,
            BlockState partState, boolean dropCheckpoint) {
        destroyController(level, controllerPosition(partPos, partState),
                dropCheckpoint);
    }

    public static void destroyFromDoor(Level level, BlockPos doorPos,
            BlockState doorState, boolean dropCheckpoint) {
        BlockPos controllerPos = controllerForDoor(level, doorPos, doorState);
        if (controllerPos != null) {
            destroyController(level, controllerPos, dropCheckpoint);
        }
    }

    private static void destroyController(Level level, BlockPos controllerPos,
            boolean dropCheckpoint) {
        BlockState controllerState = level.getBlockState(controllerPos);
        if (!isController(controllerState)) return;

        DecontaminationCheckpointController.forget(level, controllerPos);
        FacilityStructureBreakGuard.clear(level, controllerPos);
        removeStructureParts(level, controllerPos, controllerState);
        if (dropCheckpoint) {
            Block.popResource(level, controllerPos,
                    new ItemStack(ScpClassifiedDirectiveModBlocks.DECON_OPEN.get()));
        }
        clearBlock(level, controllerPos, controllerState);
    }

    public static boolean isValidCollisionPart(BlockGetter level,
            BlockPos partPos, BlockState partState) {
        if (partState.getBlock() != DecontaminationStructureBlocks.collision()) {
            return false;
        }
        int side = DecontaminationCollisionBlock.decodeSide(
                partState.getValue(DecontaminationCollisionBlock.SIDE));
        int height = DecontaminationCollisionBlock.decodeHeight(
                partState.getValue(DecontaminationCollisionBlock.HEIGHT));
        int forward = partState.getValue(DecontaminationCollisionBlock.FORWARD);
        if (!isCollisionPart(side, height, forward)) return false;

        BlockPos controller = controllerPosition(partPos, partState);
        BlockState controllerState = level.getBlockState(controller);
        if (!isController(controllerState)
                || !controllerState.hasProperty(
                HorizontalDirectionalBlock.FACING)) {
            return false;
        }
        Direction facing = partState.getValue(DecontaminationCollisionBlock.FACING);
        return controllerState.getValue(HorizontalDirectionalBlock.FACING) == facing
                && partPosition(controller, facing, side, height, forward)
                .equals(partPos);
    }

    public static boolean isCollisionPart(int side, int height, int forward) {
        if (forward < FIRST_INTERIOR_FORWARD
                || forward > LAST_INTERIOR_FORWARD
                || side < -1 || side > 1 || height < -1 || height > 3) {
            return false;
        }
        if (height == -1) return true;
        if (height == 3) return !(side == 0 && forward == LIGHT_FORWARD);
        return (side == -1 || side == 1) && height >= 0 && height <= 2;
    }

    /** The center floor cells at both chamber ends power the owned doors. */
    public static boolean isDoorPowerPart(BlockState state) {
        if (state.getBlock() != DecontaminationStructureBlocks.collision()) {
            return false;
        }
        return DecontaminationCollisionBlock.decodeSide(
                state.getValue(DecontaminationCollisionBlock.SIDE)) == 0
                && DecontaminationCollisionBlock.decodeHeight(
                state.getValue(DecontaminationCollisionBlock.HEIGHT)) == -1
                && (state.getValue(DecontaminationCollisionBlock.FORWARD)
                == FIRST_INTERIOR_FORWARD
                || state.getValue(DecontaminationCollisionBlock.FORWARD)
                == LAST_INTERIOR_FORWARD);
    }

    public static int ownedDoorPowerSignal(BlockGetter level, BlockPos partPos,
            BlockState partState) {
        if (!isDoorPowerPart(partState)
                || !isValidCollisionPart(level, partPos, partState)) return 0;
        BlockPos controllerPos = controllerPosition(partPos, partState);
        return shouldPowerOwnedDoors(level, controllerPos) ? 15 : 0;
    }

    public static boolean shouldPowerOwnedDoors(BlockGetter level,
            BlockPos controllerPos) {
        BlockState controllerState = level.getBlockState(controllerPos);
        if (!isController(controllerState)) return false;
        if (level.getBlockEntity(controllerPos)
                instanceof DecontaminationBlockEntity decon) {
            return !decon.isActive()
                    || decon.sequenceElapsedTicks() >= DOOR_RELEASE_TICK;
        }
        return !isClosedController(controllerState);
    }

    public static BlockPos controllerPosition(BlockPos partPos,
            BlockState partState) {
        Direction facing = partState.getValue(DecontaminationCollisionBlock.FACING);
        int side = DecontaminationCollisionBlock.decodeSide(
                partState.getValue(DecontaminationCollisionBlock.SIDE));
        int height = DecontaminationCollisionBlock.decodeHeight(
                partState.getValue(DecontaminationCollisionBlock.HEIGHT));
        int forward = partState.getValue(DecontaminationCollisionBlock.FORWARD);
        Direction right = facing.getClockWise();
        Direction forwardDirection = facing.getOpposite();
        return partPos.offset(
                -right.getStepX() * side - forwardDirection.getStepX() * forward,
                -height,
                -right.getStepZ() * side - forwardDirection.getStepZ() * forward);
    }

    public static BlockPos partPosition(BlockPos controllerPos, Direction facing,
            int side, int height, int forward) {
        Direction right = facing.getClockWise();
        Direction forwardDirection = facing.getOpposite();
        return controllerPos.offset(
                right.getStepX() * side + forwardDirection.getStepX() * forward,
                height,
                right.getStepZ() * side + forwardDirection.getStepZ() * forward);
    }

    public static BlockPos entranceDoorPosition(BlockPos controllerPos,
            Direction facing) {
        return partPosition(controllerPos, facing, 0, -1, 0);
    }

    public static BlockPos exitDoorPosition(BlockPos controllerPos,
            Direction facing) {
        return partPosition(controllerPos, facing, 0, -1, EXIT_FORWARD);
    }

    public static BlockPos lightPosition(BlockPos controllerPos,
            Direction facing) {
        return partPosition(controllerPos, facing, 0,
                LIGHT_HEIGHT, LIGHT_FORWARD);
    }

    public static BlockPos controllerPositionForLight(BlockPos lightPos,
            BlockState lightState) {
        Direction facing = lightState.getValue(DecontaminationLightBlock.FACING);
        Direction forward = facing.getOpposite();
        return lightPos.offset(-forward.getStepX() * LIGHT_FORWARD,
                -LIGHT_HEIGHT, -forward.getStepZ() * LIGHT_FORWARD);
    }

    public static boolean isValidLight(BlockGetter level, BlockPos lightPos,
            BlockState lightState) {
        if (lightState.getBlock() != DecontaminationStructureBlocks.light()
                || !lightState.hasProperty(DecontaminationLightBlock.FACING)) {
            return false;
        }
        Direction facing = lightState.getValue(DecontaminationLightBlock.FACING);
        BlockPos controller = controllerPositionForLight(lightPos, lightState);
        BlockState controllerState = level.getBlockState(controller);
        return isController(controllerState)
                && controllerState.hasProperty(HorizontalDirectionalBlock.FACING)
                && controllerState.getValue(HorizontalDirectionalBlock.FACING) == facing
                && lightPosition(controller, facing).equals(lightPos);
    }

    public static boolean isOwnedDoor(BlockGetter level, BlockPos doorPos,
            BlockState doorState) {
        return controllerForDoor(level, doorPos, doorState) != null;
    }

    @Nullable
    public static BlockPos controllerForDoor(BlockGetter level, BlockPos doorPos,
            BlockState doorState) {
        if (!isBlackDoor(doorState.getBlock())
                || !doorState.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return null;
        }
        Direction facing = doorState.getValue(HorizontalDirectionalBlock.FACING);

        BlockPos entranceController = doorPos.above();
        if (matchesDoorController(level, entranceController, doorPos, facing)) {
            return entranceController;
        }

        BlockPos exitController = doorPos.relative(facing, EXIT_FORWARD).above();
        if (matchesDoorController(level, exitController, doorPos, facing)) {
            return exitController;
        }
        return null;
    }

    public static boolean shouldOwnedDoorBeOpen(Level level, BlockPos doorPos) {
        BlockState doorState = level.getBlockState(doorPos);
        BlockPos controller = controllerForDoor(level, doorPos, doorState);
        return controller != null && shouldPowerOwnedDoors(level, controller);
    }

    /** Refreshes the two hidden floor relays and the actual BLACK_DOOR ticks. */
    public static void nudgeOwnedDoors(Level level, BlockPos controllerPos,
            Direction facing) {
        for (int forward : new int[]{FIRST_INTERIOR_FORWARD,
                LAST_INTERIOR_FORWARD}) {
            BlockPos powerPos = partPosition(controllerPos, facing,
                    0, -1, forward);
            BlockState powerState = level.getBlockState(powerPos);
            if (powerState.getBlock()
                    == DecontaminationStructureBlocks.collision()) {
                level.updateNeighborsAt(powerPos, powerState.getBlock());
            }
        }
        nudgeDoor(level, entranceDoorPosition(controllerPos, facing));
        nudgeDoor(level, exitDoorPosition(controllerPos, facing));
    }

    public static boolean isController(BlockState state) {
        Block block = state.getBlock();
        return block == ScpClassifiedDirectiveModBlocks.DECON_OPEN.get()
                || block == ScpClassifiedDirectiveModBlocks.DECON_CLOSED.get()
                || block == ScpClassifiedDirectiveModBlocks.DECON_OPEN_RELOAD.get();
    }

    public static boolean isClosedController(BlockState state) {
        return state.getBlock() == ScpClassifiedDirectiveModBlocks.DECON_CLOSED.get();
    }

    /** Interior player-detection volume, inset from the authored side walls. */
    public static AABB chamberBox(BlockPos controllerPos, Direction facing) {
        Vec3 a = modelPointToWorld(controllerPos, facing,
                -20.0D, 0.0D, 10.0D);
        Vec3 b = modelPointToWorld(controllerPos, facing,
                20.0D, 32.0D, 86.0D);
        return new AABB(Math.min(a.x, b.x), Math.min(a.y, b.y),
                Math.min(a.z, b.z), Math.max(a.x, b.x),
                Math.max(a.y, b.y), Math.max(a.z, b.z));
    }

    /** Converts Blockbench/GeckoLib local model units to oriented world space. */
    public static Vec3 modelPointToWorld(BlockPos controllerPos,
            Direction facing, double modelX, double modelY, double modelZ) {
        Direction right = facing.getClockWise();
        Direction forward = facing.getOpposite();
        double x = controllerPos.getX() + 0.5D
                + right.getStepX() * modelX / 16.0D
                + forward.getStepX() * modelZ / 16.0D;
        double y = controllerPos.getY() + modelY / 16.0D;
        double z = controllerPos.getZ() + 0.5D
                + right.getStepZ() * modelX / 16.0D
                + forward.getStepZ() * modelZ / 16.0D;
        return new Vec3(x, y, z);
    }

    public static void clearBlock(Level level, BlockPos pos,
            BlockState state) {
        BlockState replacement = state.getFluidState().isEmpty()
                ? Blocks.AIR.defaultBlockState()
                : state.getFluidState().createLegacyBlock();
        level.setBlock(pos, replacement, Block.UPDATE_ALL);
    }

    private static boolean placeDoor(Level level, Direction facing,
            BlockPos doorPos) {
        if (!level.getBlockState(doorPos).canBeReplaced()) return false;
        BlockState closed = FacilityModule.BLACK_DOOR.closed().get()
                .defaultBlockState().setValue(
                HorizontalDirectionalBlock.FACING, facing);
        if (!level.setBlock(doorPos, closed, Block.UPDATE_ALL)) return false;
        level.scheduleTick(doorPos, closed.getBlock(), 1);
        return true;
    }

    private static void ensureDoor(Level level, Direction facing,
            BlockPos doorPos) {
        BlockState current = level.getBlockState(doorPos);
        if (isBlackDoor(current.getBlock())
                && current.hasProperty(HorizontalDirectionalBlock.FACING)
                && current.getValue(HorizontalDirectionalBlock.FACING) == facing) {
            return;
        }
        if (current.canBeReplaced()) placeDoor(level, facing, doorPos);
    }

    private static void removeOwnedDoor(Level level, BlockPos controllerPos,
            BlockPos doorPos) {
        BlockState state = level.getBlockState(doorPos);
        BlockPos owner = controllerForDoor(level, doorPos, state);
        if (controllerPos.equals(owner)) clearBlock(level, doorPos, state);
    }

    private static void nudgeDoor(Level level, BlockPos doorPos) {
        BlockState state = level.getBlockState(doorPos);
        if (!isBlackDoor(state.getBlock())) return;
        level.scheduleTick(doorPos, state.getBlock(), 1);
        level.updateNeighborsAt(doorPos, state.getBlock());
    }

    private static boolean matchesDoorController(BlockGetter level,
            BlockPos controllerPos, BlockPos doorPos, Direction facing) {
        BlockState controllerState = level.getBlockState(controllerPos);
        if (!isController(controllerState)
                || !controllerState.hasProperty(
                HorizontalDirectionalBlock.FACING)
                || controllerState.getValue(
                HorizontalDirectionalBlock.FACING) != facing) {
            return false;
        }
        return entranceDoorPosition(controllerPos, facing).equals(doorPos)
                || exitDoorPosition(controllerPos, facing).equals(doorPos);
    }

    private static boolean isBlackDoor(Block block) {
        FacilityModule.DoorFamily family = FacilityModule.BLACK_DOOR;
        if (block == family.closed().get() || block == family.open().get()) {
            return true;
        }
        return family.opening().stream().anyMatch(value -> block == value.get())
                || family.closing().stream().anyMatch(value -> block == value.get());
    }

    private static BlockState collisionState(Level level, BlockPos pos,
            Direction facing, PartAddress part) {
        return DecontaminationStructureBlocks.collision().defaultBlockState()
                .setValue(DecontaminationCollisionBlock.FACING, facing)
                .setValue(DecontaminationCollisionBlock.SIDE,
                        DecontaminationCollisionBlock.encodeSide(part.side))
                .setValue(DecontaminationCollisionBlock.HEIGHT,
                        DecontaminationCollisionBlock.encodeHeight(part.height))
                .setValue(DecontaminationCollisionBlock.FORWARD, part.forward)
                .setValue(DecontaminationCollisionBlock.WATERLOGGED,
                        level.getFluidState(pos).getType() == Fluids.WATER);
    }

    private static boolean isExpectedCollisionState(BlockState state,
            Direction facing, PartAddress part) {
        return state.getValue(DecontaminationCollisionBlock.FACING) == facing
                && DecontaminationCollisionBlock.decodeSide(
                state.getValue(DecontaminationCollisionBlock.SIDE)) == part.side
                && DecontaminationCollisionBlock.decodeHeight(
                state.getValue(DecontaminationCollisionBlock.HEIGHT)) == part.height
                && state.getValue(DecontaminationCollisionBlock.FORWARD)
                == part.forward;
    }

    private static List<PartAddress> collisionParts() {
        List<PartAddress> parts = new ArrayList<>();
        for (int forward = FIRST_INTERIOR_FORWARD;
                forward <= LAST_INTERIOR_FORWARD; forward++) {
            for (int side = -1; side <= 1; side++) {
                parts.add(new PartAddress(side, -1, forward));
                if (!(side == 0 && forward == LIGHT_FORWARD)) {
                    parts.add(new PartAddress(side, 3, forward));
                }
            }
            for (int height = 0; height <= 2; height++) {
                parts.add(new PartAddress(-1, height, forward));
                parts.add(new PartAddress(1, height, forward));
            }
        }
        return parts;
    }

    private record PartAddress(int side, int height, int forward) {
    }
}
