package com.bl4ues.scpclassifieddirective.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.item.DocumentItem;

@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DocumentItems {
    public static final ResourceLocation DOCUMENT_ID = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "document");

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
        if (event.getTabKey().equals(ScpClassifiedDirectiveModTabs.SCP_CLASSIFIED_DIRECTIVE.getKey())) {
            // Keep the catalog stack uninitialized. Each copy receives its own
            // Codex ID on first use instead of cloning one tab-wide UUID.
            event.accept(new ItemStack(getDocument()));
        }
    }
}
