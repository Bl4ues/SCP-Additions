package net.mcreator.scpadditions.block.entity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.mcreator.scpadditions.init.ScpAdditionsModBlockEntities;

/** NeoForge item-handler providers for all three SCP-294 block states. */
public final class Scp294Capabilities {
    private Scp294Capabilities() {
    }

    @SuppressWarnings("unchecked")
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                (BlockEntityType<Scp294BlockEntity>) (BlockEntityType<?>)
                        ScpAdditionsModBlockEntities.SCP_294.get(),
                (blockEntity, side) -> blockEntity.getItemHandler(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                (BlockEntityType<Scp294StockingBlockEntity>) (BlockEntityType<?>)
                        ScpAdditionsModBlockEntities.SCP_294_STOCKING.get(),
                (blockEntity, side) -> blockEntity.getItemHandler(side));
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                (BlockEntityType<Scp294OutOfRangeBlockEntity>) (BlockEntityType<?>)
                        ScpAdditionsModBlockEntities.SCP_294_OUT_OF_RANGE.get(),
                (blockEntity, side) -> blockEntity.getItemHandler(side));
    }
}
