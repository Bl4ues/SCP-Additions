package net.mcreator.scpadditions.facility;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Registry bootstrap for the wall-mounted Document Holder. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DocumentHolderModule {
    public static final ResourceLocation BLOCK_ID = new ResourceLocation(
            ScpAdditionsMod.MODID, "document_holder");
    public static final ResourceLocation ITEM_ID = BLOCK_ID;
    public static final ResourceLocation BLOCK_ENTITY_ID = BLOCK_ID;

    private static DocumentHolderBlock block;
    private static DocumentHolderBlockItem item;
    private static BlockEntityType<DocumentHolderBlockEntity> blockEntityType;

    private DocumentHolderModule() {
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, BLOCK_ID, () -> {
            block = new DocumentHolderBlock();
            return block;
        });
        event.register(Registries.ITEM, ITEM_ID, () -> {
            item = new DocumentHolderBlockItem(block());
            return item;
        });
        event.register(Registries.BLOCK_ENTITY_TYPE, BLOCK_ENTITY_ID, () -> {
            blockEntityType = BlockEntityType.Builder.of(
                    DocumentHolderBlockEntity::new, block()).build(null);
            return blockEntityType;
        });
    }

    @SubscribeEvent
    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(
                FacilityModule.SCP_FACILITY_BLOCKS.getKey())) {
            event.accept(item());
        }
    }

    public static DocumentHolderBlock block() {
        if (block == null) {
            throw new IllegalStateException(
                    "Document Holder block requested before registration");
        }
        return block;
    }

    public static Item item() {
        if (item == null) {
            throw new IllegalStateException(
                    "Document Holder item requested before registration");
        }
        return item;
    }

    public static BlockEntityType<DocumentHolderBlockEntity> blockEntityType() {
        if (blockEntityType == null) {
            throw new IllegalStateException(
                    "Document Holder block entity requested before registration");
        }
        return blockEntityType;
    }
}
