package com.bl4ues.scpclassifieddirective.procedures;

import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;

import com.bl4ues.scpclassifieddirective.network.ScpClassifiedDirectiveModVariables;
import com.bl4ues.scpclassifieddirective.init.ScpClassifiedDirectiveModBlocks;

import java.util.Map;

public class Scp294restock2Procedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		BlockPos pos = BlockPos.containing(x, y, z);
		BlockState oldState = world.getBlockState(pos);
		if (!oldState.is(ScpClassifiedDirectiveModBlocks.SCP_294_STOCKING.get())) {
			return;
		}

		BlockState newState = ScpClassifiedDirectiveModBlocks.SCP_294.get().defaultBlockState();
		for (Map.Entry<Property<?>, Comparable<?>> entry : oldState.getValues().entrySet()) {
			Property<?> property = newState.getBlock().getStateDefinition()
					.getProperty(entry.getKey().getName());
			if (property != null) {
				newState = copyProperty(newState, property, entry.getValue());
			}
		}

		BlockEntity oldEntity = world.getBlockEntity(pos);
		CompoundTag tag = oldEntity == null ? null : oldEntity.saveWithFullMetadata();
		if (oldEntity != null) oldEntity.setRemoved();
		world.setBlock(pos, newState, 3);
		if (tag != null) {
			BlockEntity replacement = world.getBlockEntity(pos);
			if (replacement != null) {
				try {
					replacement.load(tag);
				} catch (Exception ignored) {
				}
			}
		}

		ScpClassifiedDirectiveModVariables.WorldVariables variables =
				ScpClassifiedDirectiveModVariables.WorldVariables.get(world);
		variables.Scp294stock = 0;
		variables.syncData(world);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static BlockState copyProperty(BlockState state, Property property,
			Comparable value) {
		try {
			return state.setValue(property, value);
		} catch (Exception ignored) {
			return state;
		}
	}
}
