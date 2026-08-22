package com.bl4ues.scpclassifieddirective.init;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.item.NamedSpawnEggItem;

public final class Scp131Items {
    public static final DeferredRegister<Item> REGISTRY =
            DeferredRegister.create(ForgeRegistries.ITEMS,
                    ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<Item> SCP_106_SPAWN_EGG =
            REGISTRY.register("scp_106_spawn_egg", () ->
                    new NamedSpawnEggItem(
                            ScpClassifiedDirectiveModEntities.SCP_106,
                            0x0A0A0A, 0x5B3924,
                            new Item.Properties().rarity(Rarity.EPIC),
                            "SCP-106 Spawn Egg",
                            "tooltip.scp_classified_directive.scp_106_spawn_egg"));

    public static final RegistryObject<Item> SCP_131_A_SPAWN_EGG =
            REGISTRY.register("scp_131_a_spawn_egg", () ->
                    new NamedSpawnEggItem(
                            ScpClassifiedDirectiveModEntities.SCP_131_A,
                            0xD96724, 0x1F1B18,
                            new Item.Properties().rarity(Rarity.EPIC),
                            "SCP-131-A Spawn Egg",
                            "tooltip.scp_classified_directive.scp_131_a_spawn_egg"));

    public static final RegistryObject<Item> SCP_131_B_SPAWN_EGG =
            REGISTRY.register("scp_131_b_spawn_egg", () ->
                    new NamedSpawnEggItem(
                            ScpClassifiedDirectiveModEntities.SCP_131_B,
                            0xDDBB45, 0x342B18,
                            new Item.Properties().rarity(Rarity.EPIC),
                            "SCP-131-B Spawn Egg",
                            "tooltip.scp_classified_directive.scp_131_b_spawn_egg"));

    public static final RegistryObject<Item> SCP_173_SPAWN_EGG =
            REGISTRY.register("scp_173_spawn_egg", () ->
                    new NamedSpawnEggItem(
                            ScpClassifiedDirectiveModEntities.SCP_173,
                            0x8B8B82, 0x4A1712,
                            new Item.Properties().rarity(Rarity.EPIC),
                            "SCP-173 Spawn Egg",
                            "tooltip.scp_classified_directive.scp_173_spawn_egg"));

    public static final RegistryObject<Item> SCP_939_SPAWN_EGG =
            REGISTRY.register("scp_939_spawn_egg", () ->
                    new NamedSpawnEggItem(
                            ScpClassifiedDirectiveModEntities.SCP_939,
                            0x5A1615, 0xA64334,
                            new Item.Properties().rarity(Rarity.EPIC),
                            "SCP-939 Spawn Egg",
                            "tooltip.scp_classified_directive.scp_939_spawn_egg"));

    /**
     * Kept under the original registry ID for world compatibility, but exposed
     * as the handheld Roomba placement item rather than a vanilla-looking egg.
     */
    public static final RegistryObject<Item> ROOMBA_SPAWN_EGG =
            REGISTRY.register("roomba_spawn_egg", () ->
                    new NamedSpawnEggItem(
                            ScpClassifiedDirectiveModEntities.ROOMBA,
                            0x555B5E, 0x3D7FA8,
                            new Item.Properties().rarity(Rarity.UNCOMMON),
                            "Roomba",
                            "tooltip.scp_classified_directive.roomba_spawn_egg"));

    private Scp131Items() {
    }
}
