package net.mcreator.scpadditions.facility;

import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.ForgeRegistries;
import net.neoforged.neoforge.registries.MissingMappingsEvent;
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
            Supplier<Block> replacement = FacilityModule.blockByPath(mapping.getKey().getPath());
            if (replacement != null && replacement.isPresent()) {
                mapping.remap(replacement.get());
            }
        }

        for (MissingMappingsEvent.Mapping<Block> mapping :
                event.getMappings(ForgeRegistries.Keys.BLOCKS, UBlocksModule.LEGACY_MODID)) {
            Supplier<Block> replacement = UBlocksModule.blockByPath(mapping.getKey().getPath());
            if (replacement != null && replacement.isPresent()) {
                mapping.remap(replacement.get());
            }
        }

        for (MissingMappingsEvent.Mapping<Item> mapping :
                event.getMappings(ForgeRegistries.Keys.ITEMS, FacilityModule.LEGACY_MODID)) {
            Supplier<Item> replacement = FacilityModule.itemByPath(mapping.getKey().getPath());
            if (replacement != null && replacement.isPresent()) {
                mapping.remap(replacement.get());
            }
        }

        for (MissingMappingsEvent.Mapping<Item> mapping :
                event.getMappings(ForgeRegistries.Keys.ITEMS, UBlocksModule.LEGACY_MODID)) {
            Supplier<Item> replacement = UBlocksModule.registeredItemByPath(mapping.getKey().getPath());
            if (replacement != null && replacement.isPresent()) {
                mapping.remap(replacement.get());
            }
        }
    }
}
