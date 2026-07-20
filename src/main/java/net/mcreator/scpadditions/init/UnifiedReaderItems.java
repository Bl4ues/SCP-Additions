package net.mcreator.scpadditions.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.item.OffsetKeycardReaderItem;
import net.mcreator.scpadditions.item.ScrewdriverItem;

import java.util.List;
import java.util.function.Supplier;

public final class UnifiedReaderItems {
    public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(BuiltInRegistries.ITEM, ScpAdditionsMod.MODID);

    /**
     * The only public reader item. It always places a level 1 reader; its level
     * is changed in-world with the screwdriver configuration screen.
     */
    public static final Supplier<Item> KEYCARD_READER = REGISTRY.register("keycard_reader",
            () -> new OffsetKeycardReaderItem(
                    ScpAdditionsModBlocks.LEFT_READER.get(),
                    ScpAdditionsModBlocks.RIGHT_READER,
                    () -> (BlockItem) ScpAdditionsModItems.RIGHT_READER.get(),
                    List.<Supplier<? extends Block>>of(
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
     * Using a reader opens its configuration screen. Crouching copies its
     * level, while Control-use applies the copied level to another reader.
     */
    public static final Supplier<Item> SCREWDRIVER = REGISTRY.register("screwdriver",
            () -> new ScrewdriverItem(new Item.Properties().stacksTo(1)));

    private UnifiedReaderItems() {
    }
}
