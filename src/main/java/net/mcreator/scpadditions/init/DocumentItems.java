package net.mcreator.scpadditions.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.document.DocumentData;
import net.mcreator.scpadditions.item.DocumentItem;

@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DocumentItems {
    public static final ResourceLocation DOCUMENT_ID = new ResourceLocation(
            ScpAdditionsMod.MODID, "document");

    private DocumentItems() {
    }

    public static Item getDocument() {
        Item item = ForgeRegistries.ITEMS.getValue(DOCUMENT_ID);
        if (item == null) {
            throw new IllegalStateException("Document item is not registered yet");
        }
        return item;
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.ITEMS, DOCUMENT_ID,
                DocumentItem::new);
    }

    @SubscribeEvent
    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(ScpAdditionsModTabs.SCP_ADDITIONS.getKey())) {
            event.accept(DocumentData.createDefaultStack());
        }
    }
}
