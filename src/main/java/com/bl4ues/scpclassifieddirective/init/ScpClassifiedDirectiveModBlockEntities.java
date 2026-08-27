
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package com.bl4ues.scpclassifieddirective.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;

import com.bl4ues.scpclassifieddirective.block.entity.DecontaminationBlockEntity;
import com.bl4ues.scpclassifieddirective.block.entity.Scp294StockingBlockEntity;
import com.bl4ues.scpclassifieddirective.block.entity.Scp294OutOfRangeBlockEntity;
import com.bl4ues.scpclassifieddirective.block.entity.Scp294BlockEntity;
import com.bl4ues.scpclassifieddirective.block.entity.Scp330BlockEntity;
import com.bl4ues.scpclassifieddirective.block.entity.Scp902BlockEntity;
import com.bl4ues.scpclassifieddirective.block.entity.Scp1176BlockEntity;
import com.bl4ues.scpclassifieddirective.block.entity.SystemTerminalBlockEntity;
import com.bl4ues.scpclassifieddirective.block.entity.TeslaGateBlockEntity;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

public class ScpClassifiedDirectiveModBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ScpClassifiedDirectiveMod.MODID);
	public static final RegistryObject<BlockEntityType<Scp330BlockEntity>> SCP_330 = REGISTRY.register("scp_330", () -> BlockEntityType.Builder.of(Scp330BlockEntity::new, ScpClassifiedDirectiveModBlocks.SCP_330.get()).build(null));
	public static final RegistryObject<BlockEntityType<Scp902BlockEntity>> SCP_902 = REGISTRY.register("scp_902", () -> BlockEntityType.Builder.of(Scp902BlockEntity::new, ScpClassifiedDirectiveModBlocks.SCP_902_CLOSED.get(), ScpClassifiedDirectiveModBlocks.SCP_902_OPEN.get()).build(null));
	public static final RegistryObject<BlockEntityType<Scp1176BlockEntity>> SCP_1176 = REGISTRY.register("scp_1176", () -> BlockEntityType.Builder.of(Scp1176BlockEntity::new, ScpClassifiedDirectiveModBlocks.SCP_1176.get()).build(null));
	public static final RegistryObject<BlockEntityType<TeslaGateBlockEntity>> TESLA_GATE = REGISTRY.register("tesla_gate", () -> BlockEntityType.Builder.of(TeslaGateBlockEntity::new,
			ScpClassifiedDirectiveModBlocks.TESLA_GATE.get(), ScpClassifiedDirectiveModBlocks.TESLA_ACTIVE.get(),
			ScpClassifiedDirectiveModBlocks.TESLA_ACTIVE_2.get(), ScpClassifiedDirectiveModBlocks.TESLA_ACTIVE_3.get(),
			ScpClassifiedDirectiveModBlocks.TESLA_ACTIVE_4.get(), ScpClassifiedDirectiveModBlocks.TESLA_RECHARGE.get()).build(null));
	public static final RegistryObject<BlockEntityType<DecontaminationBlockEntity>> DECONTAMINATION = REGISTRY.register("decontamination", () -> BlockEntityType.Builder.of(DecontaminationBlockEntity::new,
			ScpClassifiedDirectiveModBlocks.DECON_OPEN.get(), ScpClassifiedDirectiveModBlocks.DECON_CLOSED.get(), ScpClassifiedDirectiveModBlocks.DECON_OPEN_RELOAD.get()).build(null));
	public static final RegistryObject<BlockEntityType<SystemTerminalBlockEntity>> SCP_079_SYSTEM_CONTROL = REGISTRY.register("scp_079_system_control", () -> BlockEntityType.Builder.of(SystemTerminalBlockEntity::new, ScpClassifiedDirectiveModBlocks.SCP_079_SYSTEM_CONTROL.get()).build(null));
	public static final RegistryObject<BlockEntityType<?>> SCP_294 = register("scp_294", ScpClassifiedDirectiveModBlocks.SCP_294, Scp294BlockEntity::new);
	public static final RegistryObject<BlockEntityType<?>> SCP_294_OUT_OF_RANGE = register("scp_294_out_of_range", ScpClassifiedDirectiveModBlocks.SCP_294_OUT_OF_RANGE, Scp294OutOfRangeBlockEntity::new);
	public static final RegistryObject<BlockEntityType<?>> SCP_294_STOCKING = register("scp_294_stocking", ScpClassifiedDirectiveModBlocks.SCP_294_STOCKING, Scp294StockingBlockEntity::new);

	private static RegistryObject<BlockEntityType<?>> register(String registryname, RegistryObject<Block> block, BlockEntityType.BlockEntitySupplier<?> supplier) {
		return REGISTRY.register(registryname, () -> BlockEntityType.Builder.of(supplier, block.get()).build(null));
	}
}
