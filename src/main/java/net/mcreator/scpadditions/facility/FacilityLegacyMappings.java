package net.mcreator.scpadditions.facility;

import java.util.function.Supplier;

import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.MissingMappingsEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;

/**
 * Migrates worlds and inventories created by the standalone SCP Unity Extra
 * Blocks mod to the consolidated scp_additions registry IDs.
 */
@EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class FacilityLegacyMappings {
    private FacilityLegacyMappings() {
    }

    @SubscribeEvent
    public static void remapLegacyIds(MissingMappingsEvent event) {
        for (MissingMappingsEvent.Mapping<Block> mapping :
                event.getMappings(Registries.BLOCK, FacilityModule.LEGACY_MODID)) {
            Supplier<Block> replacement = FacilityModule.blockByPath(mapping.getKey().getPath());
            if (replacement != null) {
                mapping.remap(replacement.get());
            }
        }

        for (MissingMappingsEvent.Mapping<Block> mapping :
                event.getMappings(Registries.BLOCK, UBlocksModule.LEGACY_MODID)) {
            Supplier<Block> replacement = UBlocksModule.blockByPath(mapping.getKey().getPath());
            if (replacement != null) {
                mapping.remap(replacement.get());
            }
        }

        for (MissingMappingsEvent.Mapping<Item> mapping :
                event.getMappings(Registries.ITEM, FacilityModule.LEGACY_MODID)) {
            Supplier<Item> replacement = FacilityModule.itemByPath(mapping.getKey().getPath());
            if (replacement != null) {
                mapping.remap(replacement.get());
            }
        }

        for (MissingMappingsEvent.Mapping<Item> mapping :
                event.getMappings(Registries.ITEM, UBlocksModule.LEGACY_MODID)) {
            Supplier<Item> replacement = UBlocksModule.registeredItemByPath(mapping.getKey().getPath());
            if (replacement != null) {
                mapping.remap(replacement.get());
            }
        }
    }
}
