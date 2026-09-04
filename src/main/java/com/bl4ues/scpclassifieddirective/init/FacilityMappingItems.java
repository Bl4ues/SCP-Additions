package com.bl4ues.scpclassifieddirective.init;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.item.FacilityMappingToolItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/** Direct registration for the map-maker-only facility floor authoring tool. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FacilityMappingItems {
    public static final ResourceLocation TOOL_ID = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "facility_mapping_tool");

    private FacilityMappingItems() {
    }

    public static Item getTool() {
        Item item = ForgeRegistries.ITEMS.getValue(TOOL_ID);
        if (item == null) {
            throw new IllegalStateException(
                    "Facility Mapping Tool is not registered yet");
        }
        return item;
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.ITEMS, TOOL_ID,
                FacilityMappingToolItem::new);
    }
}
