package net.mcreator.scpadditions.init;

import java.util.function.Supplier;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.ForgeRegistries;
import net.mcreator.scpadditions.ScpAdditionsMod;

public final class ScpAdditionsModParticleTypes {
    public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(
            ForgeRegistries.PARTICLE_TYPES, ScpAdditionsMod.MODID);

    public static final Supplier<SimpleParticleType> DECONTAMINATION_GAS = REGISTRY.register(
            "decontamination_gas", () -> new SimpleParticleType(false));

    private ScpAdditionsModParticleTypes() {
    }
}
