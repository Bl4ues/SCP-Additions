package net.mcreator.scpadditions.init;

import java.util.function.Supplier;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.mcreator.scpadditions.ScpAdditionsMod;

public final class ScpAdditionsModParticleTypes {
    public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(
            BuiltInRegistries.PARTICLE_TYPE, ScpAdditionsMod.MODID);

    public static final Supplier<SimpleParticleType> DECONTAMINATION_GAS = REGISTRY.register(
            "decontamination_gas", () -> FabricParticleTypes.simple(false));

    private ScpAdditionsModParticleTypes() {
    }
}
