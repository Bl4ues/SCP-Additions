package com.bl4ues.scpclassifieddirective.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import com.bl4ues.scpclassifieddirective.block.TeslaGateStructure;
import com.bl4ues.scpclassifieddirective.facility.FacilityStructureBreakGuard;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;

import java.util.Map;
import java.util.function.Supplier;

public final class TeslaGateTransitionHelper {
    private TeslaGateTransitionHelper() {
    }

    public static boolean transitionIfCurrent(LevelAccessor world, double x,
            double y, double z, Supplier<? extends Block> expectedBlock,
            Supplier<? extends Block> nextBlock) {
        BlockPos pos = BlockPos.containing(x, y, z);
        if (FacilityStructureBreakGuard.isBeingMined(world, pos)) {
            return false;
        }
        BlockState currentState = world.getBlockState(pos);
        if (currentState.getBlock() != expectedBlock.get()) {
            return false;
        }
        world.setBlock(pos, copyProperties(currentState,
                nextBlock.get().defaultBlockState()), 3);
        return true;
    }

    public static boolean resetStuckController(LevelAccessor world,
            BlockPos pos) {
        if (FacilityStructureBreakGuard.isBeingMined(world, pos)) {
            return false;
        }
        BlockState currentState = world.getBlockState(pos);
        if (!TeslaGateStructure.isController(currentState)
                || currentState.is(ScpClassifiedDirectiveModBlocks.TESLA_GATE.get())) {
            return false;
        }
        world.setBlock(pos, copyProperties(currentState,
                ScpClassifiedDirectiveModBlocks.TESLA_GATE.get().defaultBlockState()), 3);
        return true;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState copyProperties(BlockState from, BlockState to) {
        BlockState result = to;
        for (Map.Entry<Property<?>, Comparable<?>> entry
                : from.getValues().entrySet()) {
            Property property = result.getBlock().getStateDefinition()
                    .getProperty(entry.getKey().getName());
            if (property != null) {
                try {
                    result = result.setValue((Property) property,
                            (Comparable) entry.getValue());
                } catch (Exception ignored) {
                }
            }
        }
        return result;
    }
}
