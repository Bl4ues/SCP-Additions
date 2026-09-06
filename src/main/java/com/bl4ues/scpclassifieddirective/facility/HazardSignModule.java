package com.bl4ues.scpclassifieddirective.facility;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/** Registration for the portrait, editable facility hazard sign. */
public final class HazardSignModule {
    public static final String PATH = "hazard_sign";

    private static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS,
                    ScpClassifiedDirectiveMod.MODID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS,
                    ScpClassifiedDirectiveMod.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,
                    ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<HazardSignBlock> BLOCK =
            BLOCKS.register(PATH, HazardSignBlock::new);
    public static final RegistryObject<Item> ITEM = ITEMS.register(PATH,
            () -> new BlockItem(BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<HazardSignBlockEntity>>
            BLOCK_ENTITY = BLOCK_ENTITIES.register(PATH,
                    () -> BlockEntityType.Builder.of(
                            HazardSignBlockEntity::new, BLOCK.get()).build(null));

    private HazardSignModule() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
