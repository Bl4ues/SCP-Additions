package net.mcreator.scpadditions.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.item.OffsetKeycardReaderItem;

public final class UnifiedReaderItems {
    public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, ScpAdditionsMod.MODID);

    public static final RegistryObject<Item> LEVEL_1_READER = reader(
            "level_1_keycard_reader",
            ScpAdditionsModBlocks.LEFT_READER,
            ScpAdditionsModBlocks.RIGHT_READER,
            ScpAdditionsModItems.RIGHT_READER
    );
    public static final RegistryObject<Item> LEVEL_2_READER = reader(
            "level_2_keycard_reader",
            ScpAdditionsModBlocks.LV_2_LEFT_READER,
            ScpAdditionsModBlocks.LV_2_RIGHT_READER,
            ScpAdditionsModItems.LV_2_RIGHT_READER
    );
    public static final RegistryObject<Item> LEVEL_3_READER = reader(
            "level_3_keycard_reader",
            ScpAdditionsModBlocks.LV_3_LEFT_READER,
            ScpAdditionsModBlocks.LV_3_RIGHT_READER,
            ScpAdditionsModItems.LV_3_RIGHT_READER
    );
    public static final RegistryObject<Item> LEVEL_4_READER = reader(
            "level_4_keycard_reader",
            ScpAdditionsModBlocks.LV_4_LEFT_READER,
            ScpAdditionsModBlocks.LV_4_RIGHT_READER,
            ScpAdditionsModItems.LV_4_RIGHT_READER
    );
    public static final RegistryObject<Item> LEVEL_5_READER = reader(
            "level_5_keycard_reader",
            ScpAdditionsModBlocks.LV_5_LEFT_READER,
            ScpAdditionsModBlocks.LV_5_RIGHT_READER,
            ScpAdditionsModItems.LV_5_RIGHT_READER
    );
    public static final RegistryObject<Item> LEVEL_6_READER = reader(
            "level_6_keycard_reader",
            ScpAdditionsModBlocks.LV_6_LEFT_READER,
            ScpAdditionsModBlocks.LV_6_RIGHT_READER,
            ScpAdditionsModItems.LV_6_RIGHT_READER
    );

    private UnifiedReaderItems() {
    }

    private static RegistryObject<Item> reader(String id, RegistryObject<Block> leftBlock,
            RegistryObject<Block> rightBlock, RegistryObject<Item> legacyRightItem) {
        return REGISTRY.register(id, () -> new OffsetKeycardReaderItem(
                leftBlock.get(),
                rightBlock,
                () -> (BlockItem) legacyRightItem.get(),
                new Item.Properties()
        ));
    }
}
