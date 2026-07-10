package net.mcreator.scpadditions.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.entity.EntityType;

import net.mcreator.scpadditions.entity.Scp0591infected3Entity;
import net.mcreator.scpadditions.ScpAdditionsMod;

public class ScpAdditionsModEntities {
	public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ScpAdditionsMod.MODID);

	/**
	 * Temporary dead reference for untouched SCP-914 generated procedures.
	 * The SCP-059 entity is not registered anymore and cannot be spawned normally.
	 */
	@Deprecated(forRemoval = true)
	public static final RegistryObject<EntityType<Scp0591infected3Entity>> SCP_0591INFECTED_3 = null;
}