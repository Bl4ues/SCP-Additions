package net.mcreator.scpadditions.init;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.item.NamedSpawnEggItem;

public final class Scp131Items {
    public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, ScpAdditionsMod.MODID);

    public static final RegistryObject<Item> SCP_131_A_SPAWN_EGG = REGISTRY.register("scp_131_a_spawn_egg", () ->
            new NamedSpawnEggItem(ScpAdditionsModEntities.SCP_131_A, 0xD96724, 0x1F1B18,
                    new Item.Properties(), "SCP-131-A Spawn Egg"));

    public static final RegistryObject<Item> SCP_131_B_SPAWN_EGG = REGISTRY.register("scp_131_b_spawn_egg", () ->
            new NamedSpawnEggItem(ScpAdditionsModEntities.SCP_131_B, 0xDDBB45, 0x342B18,
                    new Item.Properties(), "SCP-131-B Spawn Egg"));

    public static final RegistryObject<Item> SCP_173_SPAWN_EGG = REGISTRY.register("scp_173_spawn_egg", () ->
            new NamedSpawnEggItem(ScpAdditionsModEntities.SCP_173, 0x8B8B82, 0x4A1712,
                    new Item.Properties(), "SCP-173 Spawn Egg"));

    private Scp131Items() {
    }
}
