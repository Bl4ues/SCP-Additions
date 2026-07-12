package net.mcreator.scpadditions.facility;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.ScpAdditionsMod;

/**
 * Migrates worlds and inventories created by the standalone SCP Unity Extra
 * Blocks mod to the consolidated scp_additions registry IDs.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FacilityLegacyMappings {
    private FacilityLegacyMappings() {
    }

    @SubscribeEvent
    public static void remapLegacyIds(MissingMappingsEvent event) {
        for (MissingMappingsEvent.Mapping<Block> mapping :
                event.getMappings(ForgeRegistries.Keys.BLOCKS, FacilityModule.LEGACY_MODID)) {
            RegistryObject<Block> replacement = FacilityModule.blockByPath(mapping.getKey().getPath());
            if (replacement != null && replacement.isPresent()) {
                mapping.remap(replacement.get());
            }
        }

        for (MissingMappingsEvent.Mapping<Block> mapping :
                event.getMappings(ForgeRegistries.Keys.BLOCKS, UBlocksModule.LEGACY_MODID)) {
            RegistryObject<Block> replacement = UBlocksModule.blockByPath(mapping.getKey().getPath());
            if (replacement != null && replacement.isPresent()) {
                mapping.remap(replacement.get());
            }
        }

        for (MissingMappingsEvent.Mapping<Item> mapping :
                event.getMappings(ForgeRegistries.Keys.ITEMS, FacilityModule.LEGACY_MODID)) {
            RegistryObject<Item> replacement = FacilityModule.itemByPath(mapping.getKey().getPath());
            if (replacement != null && replacement.isPresent()) {
                mapping.remap(replacement.get());
            }
        }

        for (MissingMappingsEvent.Mapping<Item> mapping :
                event.getMappings(ForgeRegistries.Keys.ITEMS, UBlocksModule.LEGACY_MODID)) {
            RegistryObject<Item> replacement = UBlocksModule.itemByPath(mapping.getKey().getPath());
            if (replacement != null && replacement.isPresent()) {
                mapping.remap(replacement.get());
            }
        }
    }
}
