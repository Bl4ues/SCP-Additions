package com.bl4ues.scpclassifieddirective.procedures;

import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;

import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

import java.util.Map;

public class Scp294OutOfRangeProcedureProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		ScpClassifiedDirectiveMod.queueServerWork(20, () -> {
			BlockPos pos = BlockPos.containing(x, y, z);
			BlockState newState = copyProperties(world.getBlockState(pos), ScpClassifiedDirectiveModBlocks.SCP_294.get().defaultBlockState());
			BlockEntity blockEntity = world.getBlockEntity(pos);
			CompoundTag blockEntityTag = null;
			if (blockEntity != null) {
				blockEntityTag = blockEntity.saveWithFullMetadata();
				blockEntity.setRemoved();
			}
			world.setBlock(pos, newState, 3);
			if (blockEntityTag != null) {
				BlockEntity newBlockEntity = world.getBlockEntity(pos);
				if (newBlockEntity != null) {
					try {
						newBlockEntity.load(blockEntityTag);
					} catch (Exception ignored) {
					}
				}
			}
		});
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static BlockState copyProperties(BlockState from, BlockState to) {
		BlockState result = to;
		for (Map.Entry<Property<?>, Comparable<?>> entry : from.getValues().entrySet()) {
			Property property = result.getBlock().getStateDefinition().getProperty(entry.getKey().getName());
			if (property != null) {
				try {
					result = result.setValue((Property) property, (Comparable) entry.getValue());
				} catch (Exception ignored) {
				}
			}
		}
		return result;
	}
}