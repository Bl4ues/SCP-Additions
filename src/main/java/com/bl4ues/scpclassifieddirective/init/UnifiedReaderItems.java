package com.bl4ues.scpclassifieddirective.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.item.OffsetKeycardReaderItem;
import com.bl4ues.scpclassifieddirective.item.ScrewdriverItem;

import java.util.List;
import java.util.function.Supplier;

public final class UnifiedReaderItems {
    public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, ScpClassifiedDirectiveMod.MODID);

    /**
     * The only public reader item. It always places a level 1 reader; its level
     * is changed in-world with the screwdriver configuration screen.
     */
    public static final RegistryObject<Item> KEYCARD_READER = REGISTRY.register("keycard_reader",
            () -> new OffsetKeycardReaderItem(
                    ScpClassifiedDirectiveModBlocks.LEFT_READER.get(),
                    ScpClassifiedDirectiveModBlocks.RIGHT_READER,
                    () -> (BlockItem) ScpClassifiedDirectiveModItems.RIGHT_READER.get(),
                    List.<Supplier<? extends Block>>of(
                            ScpClassifiedDirectiveModBlocks.LEFT_READER,
                            ScpClassifiedDirectiveModBlocks.RIGHT_READER,
                            ScpClassifiedDirectiveModBlocks.LV_2_LEFT_READER,
                            ScpClassifiedDirectiveModBlocks.LV_2_RIGHT_READER,
                            ScpClassifiedDirectiveModBlocks.LV_3_LEFT_READER,
                            ScpClassifiedDirectiveModBlocks.LV_3_RIGHT_READER,
                            ScpClassifiedDirectiveModBlocks.LV_4_LEFT_READER,
                            ScpClassifiedDirectiveModBlocks.LV_4_RIGHT_READER,
                            ScpClassifiedDirectiveModBlocks.LV_5_LEFT_READER,
                            ScpClassifiedDirectiveModBlocks.LV_5_RIGHT_READER,
                            ScpClassifiedDirectiveModBlocks.LV_6_LEFT_READER,
                            ScpClassifiedDirectiveModBlocks.LV_6_RIGHT_READER
                    ),
                    new Item.Properties()
            ));

    /**
     * General-purpose facility tool used by supported interactive blocks.
     */
    public static final RegistryObject<Item> SCREWDRIVER = REGISTRY.register("screwdriver",
            () -> new ScrewdriverItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)));

    private UnifiedReaderItems() {
    }
}
