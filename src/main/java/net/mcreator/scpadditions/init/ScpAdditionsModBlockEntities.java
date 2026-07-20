
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.scpadditions.init;

import java.util.function.Supplier;

import net.neoforged.neoforge.registries.ForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;

import net.mcreator.scpadditions.block.entity.Scp294StockingBlockEntity;
import net.mcreator.scpadditions.block.entity.Scp294OutOfRangeBlockEntity;
import net.mcreator.scpadditions.block.entity.Scp294BlockEntity;
import net.mcreator.scpadditions.ScpAdditionsMod;

public class ScpAdditionsModBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ScpAdditionsMod.MODID);
	public static final Supplier<BlockEntityType<?>> SCP_294 = register("scp_294", ScpAdditionsModBlocks.SCP_294, Scp294BlockEntity::new);
	public static final Supplier<BlockEntityType<?>> SCP_294_OUT_OF_RANGE = register("scp_294_out_of_range", ScpAdditionsModBlocks.SCP_294_OUT_OF_RANGE, Scp294OutOfRangeBlockEntity::new);
	public static final Supplier<BlockEntityType<?>> SCP_294_STOCKING = register("scp_294_stocking", ScpAdditionsModBlocks.SCP_294_STOCKING, Scp294StockingBlockEntity::new);

	private static Supplier<BlockEntityType<?>> register(String registryname, Supplier<Block> block, BlockEntityType.BlockEntitySupplier<?> supplier) {
		return REGISTRY.register(registryname, () -> BlockEntityType.Builder.of(supplier, block.get()).build(null));
	}
}
