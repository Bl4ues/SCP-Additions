package net.mcreator.scpadditions.init;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.entity.EntityType;

import net.mcreator.scpadditions.ScpAdditionsMod;

public class ScpAdditionsModEntities {
	public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ScpAdditionsMod.MODID);
}