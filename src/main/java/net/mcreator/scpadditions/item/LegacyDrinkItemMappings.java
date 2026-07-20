package net.mcreator.scpadditions.item;

import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.init.ScpAdditionsModItems;

/** Preserves old SCP-294 drink ids through NeoForge registry aliases. */
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

    public static void registerAliases() {
        ResourceLocation replacement = ResourceLocation.fromNamespaceAndPath(
                ScpAdditionsMod.MODID, "cup_of_coffee");
        for (String oldPath : LEGACY_DRINK_ITEMS) {
            ScpAdditionsModItems.REGISTRY.addAlias(
                    ResourceLocation.fromNamespaceAndPath(
                            ScpAdditionsMod.MODID, oldPath),
                    replacement);
        }
    }
}
