package net.mcreator.scpadditions.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.item.OffsetKeycardReaderItem;

import java.util.List;

public final class UnifiedReaderItems {
    public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, ScpAdditionsMod.MODID);

    /**
     * The only public reader item. It always places a level 1 reader; its level
     * is changed in-world with the screwdriver configuration screen.
     */
    public static final RegistryObject<Item> KEYCARD_READER = REGISTRY.register("keycard_reader",
            () -> new OffsetKeycardReaderItem(
                    ScpAdditionsModBlocks.LEFT_READER.get(),
                    ScpAdditionsModBlocks.RIGHT_READER,
                    () -> (BlockItem) ScpAdditionsModItems.RIGHT_READER.get(),
                    List.of(
                            ScpAdditionsModBlocks.LEFT_READER,
                            ScpAdditionsModBlocks.RIGHT_READER,
                            ScpAdditionsModBlocks.LV_2_LEFT_READER,
                            ScpAdditionsModBlocks.LV_2_RIGHT_READER,
                            ScpAdditionsModBlocks.LV_3_LEFT_READER,
                            ScpAdditionsModBlocks.LV_3_RIGHT_READER,
                            ScpAdditionsModBlocks.LV_4_LEFT_READER,
                            ScpAdditionsModBlocks.LV_4_RIGHT_READER,
                            ScpAdditionsModBlocks.LV_5_LEFT_READER,
                            ScpAdditionsModBlocks.LV_5_RIGHT_READER,
                            ScpAdditionsModBlocks.LV_6_LEFT_READER,
                            ScpAdditionsModBlocks.LV_6_RIGHT_READER
                    ),
                    new Item.Properties()
            ));

    /**
     * Texture/model intentionally deferred. Shift-right-clicking a reader with
     * this item opens the access-level configuration screen.
     */
    public static final RegistryObject<Item> SCREWDRIVER = REGISTRY.register("screwdriver",
            () -> new Item(new Item.Properties().stacksTo(1)));

    private UnifiedReaderItems() {
    }
}
