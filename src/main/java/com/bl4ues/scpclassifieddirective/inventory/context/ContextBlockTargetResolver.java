package com.bl4ues.scpclassifieddirective.inventory.context;

import com.bl4ues.scpclassifieddirective.block.DecontaminationStructure;
import com.bl4ues.scpclassifieddirective.block.DecontaminationStructureBlocks;
import com.bl4ues.scpclassifieddirective.block.TeslaGateStructure;
import com.bl4ues.scpclassifieddirective.block.TeslaGateStructureBlocks;
import com.bl4ues.scpclassifieddirective.facility.FacilityLargePropStructure;
import com.bl4ues.scpclassifieddirective.facility.FacilityModule;
import com.bl4ues.scpclassifieddirective.facility.elevator.CoreRoomElevatorModule;
import com.bl4ues.scpclassifieddirective.facility.blastdoor.BlastDoorModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Resolves invisible/auxiliary multiblock cells to the public block that owns
 * the interaction. Context prompts must describe the machine or prop the
 * player sees, never an implementation-only collision/placeholder block.
 */
public final class ContextBlockTargetResolver {
    private ContextBlockTargetResolver() {
    }

    public static ResolvedBlock resolve(Level level, BlockPos pos) {
        if (level == null || pos == null || !level.isLoaded(pos)) {
            return null;
        }

        BlockState state = level.getBlockState(pos);

        if (state.getBlock() == FacilityModule.FACILITY_PROP_PART.get()
                && FacilityLargePropStructure.isValidPart(level, pos, state)) {
            return root(level, FacilityLargePropStructure.controllerPosition(
                    pos, state), pos, state);
        }

        if (level.getBlockEntity(pos)
                instanceof CoreRoomElevatorModule.StructurePartBlockEntity part) {
            return root(level, part.masterPos(), pos, state);
        }

        if (state.getBlock() == TeslaGateStructureBlocks.collision()
                && TeslaGateStructure.isValidCollisionPart(level, pos, state)) {
            return root(level, TeslaGateStructure.controllerPosition(
                    pos, state), pos, state);
        }

        if (state.getBlock() == DecontaminationStructureBlocks.collision()
                && DecontaminationStructure.isValidCollisionPart(
                        level, pos, state)) {
            return root(level, DecontaminationStructure.controllerPosition(
                    pos, state), pos, state);
        }

        if (BlastDoorModule.isPart(state)) {
            BlockPos controller = BlastDoorModule.controllerPosition(
                    level, pos, state);
            if (controller != null) {
                return root(level, controller, pos, state);
            }
        }

        return new ResolvedBlock(pos.immutable(), state);
    }

    public static boolean belongsTo(Level level, BlockPos candidate,
            BlockPos controller) {
        if (controller == null || candidate == null) return false;
        ResolvedBlock resolved = resolve(level, candidate);
        return resolved != null && controller.equals(resolved.pos());
    }

    private static ResolvedBlock root(Level level, BlockPos controller,
            BlockPos fallbackPos, BlockState fallbackState) {
        if (controller == null || !level.isLoaded(controller)) {
            return new ResolvedBlock(fallbackPos.immutable(), fallbackState);
        }
        BlockState controllerState = level.getBlockState(controller);
        if (controllerState.isAir()) {
            return new ResolvedBlock(fallbackPos.immutable(), fallbackState);
        }
        return new ResolvedBlock(controller.immutable(), controllerState);
    }

    public record ResolvedBlock(BlockPos pos, BlockState state) {
    }
}
