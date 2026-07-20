package net.mcreator.scpadditions.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.ForgeRegistries;
import net.neoforged.neoforge.registries.MissingMappingsEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;

import java.util.Set;

/**
 * Preserves old-world inventory loading after the pre-3.0 SCP-294 drink items
 * were consolidated into the configurable, NBT-backed generic cup.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LegacyDrinkItemMappings {
    private static final Set<String> LEGACY_DRINK_ITEMS = Set.of(
            "aloe",
            "amnesia",
            "anti_energy",
            "apple_cider",
            "aqua_regia",
            "beer",
            "bleach",
            "blood",
            "blood_of_christ",
            "cactus",
            "carbon",
            "carrot",
            "cassis_fanta",
            "champagne",
            "champion",
            "chim",
            "chocolate",
            "cider",
            "cocaine",
            "coconut",
            "cola",
            "cold",
            "corrosive_acid",
            "corrosive_black",
            "cosmopolitan",
            "courage",
            "cup_of_alcohol",
            "curry",
            "death",
            "eggs",
            "energy_drink",
            "espresso",
            "estus",
            "ethanol",
            "fear",
            "feces",
            "feces_and_blood",
            "frozen_yogurt",
            "gin",
            "glass",
            "gold_c",
            "grimace_shake",
            "grog",
            "happiness",
            "heroin",
            "honey",
            "hot",
            "ice_cream",
            "ink",
            "insulin",
            "ipecac",
            "iron_c",
            "lager",
            "morphine",
            "neutronium",
            "pear_cider",
            "quantum",
            "spirit",
            "tea",
            "vodka",
            "yogurt");

    private LegacyDrinkItemMappings() {
    }

    @SubscribeEvent
    public static void remapLegacyDrinkItems(MissingMappingsEvent event) {
        Item replacement = ScpAdditionsModItems.CUP_OF_COFFEE.get();
        for (MissingMappingsEvent.Mapping<Item> mapping :
                event.getMappings(ForgeRegistries.Keys.ITEMS, ScpAdditionsMod.MODID)) {
            if (LEGACY_DRINK_ITEMS.contains(mapping.getKey().getPath())) {
                mapping.remap(replacement);
            }
        }
    }
}
