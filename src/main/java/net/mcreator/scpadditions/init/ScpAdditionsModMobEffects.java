package net.mcreator.scpadditions.init;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import net.minecraft.world.effect.MobEffect;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.effect.EyeSoreEffect;

public class ScpAdditionsModMobEffects {
	public static final DeferredRegister<MobEffect> REGISTRY = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ScpAdditionsMod.MODID);
	public static final RegistryObject<MobEffect> EYE_SORE = REGISTRY.register("eye_sore", EyeSoreEffect::new);
}
