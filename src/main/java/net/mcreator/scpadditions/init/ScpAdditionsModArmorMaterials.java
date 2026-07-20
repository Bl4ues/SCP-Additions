package net.mcreator.scpadditions.init;

import java.util.EnumMap;
import java.util.List;

import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.mcreator.scpadditions.ScpAdditionsMod;

public final class ScpAdditionsModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> REGISTRY =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, ScpAdditionsMod.MODID);

    public static final Holder<ArmorMaterial> HAZMAT = Registry.registerForHolder(
            BuiltInRegistries.ARMOR_MATERIAL,
            ResourceLocation.fromNamespaceAndPath(ScpAdditionsMod.MODID, "hazmat_suit"),
            new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
                        defense.put(ArmorItem.Type.BOOTS, 1);
                        defense.put(ArmorItem.Type.LEGGINGS, 2);
                        defense.put(ArmorItem.Type.CHESTPLATE, 3);
                        defense.put(ArmorItem.Type.HELMET, 1);
                        defense.put(ArmorItem.Type.BODY, 3);
                    }),
                    0,
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.of(),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(
                            ScpAdditionsMod.MODID, "hazmat_suit"))),
                    0.0F,
                    0.0F));

    private ScpAdditionsModArmorMaterials() {}
}
