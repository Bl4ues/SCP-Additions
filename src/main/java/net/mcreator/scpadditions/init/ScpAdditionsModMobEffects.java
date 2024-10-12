
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.scpadditions.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.effect.MobEffect;

import net.mcreator.scpadditions.potion.DeltaRadiationMobEffect;
import net.mcreator.scpadditions.ScpAdditionsMod;

public class ScpAdditionsModMobEffects {
	public static final DeferredRegister<MobEffect> REGISTRY = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ScpAdditionsMod.MODID);
	public static final RegistryObject<MobEffect> DELTA_RADIATION = REGISTRY.register("delta_radiation", () -> new DeltaRadiationMobEffect());
}
