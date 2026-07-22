package net.mcreator.scpadditions.init;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.ScpAdditionsMod;

public final class ScpAdditionsModParticleTypes {
    public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(
            ForgeRegistries.PARTICLE_TYPES, ScpAdditionsMod.MODID);

    public static final RegistryObject<SimpleParticleType> DECONTAMINATION_GAS = REGISTRY.register(
            "decontamination_gas", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> SCP_106_CORROSION = REGISTRY.register(
            "scp_106_corrosion", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> SCP_106_PORTAL = REGISTRY.register(
            "scp_106_portal", () -> new SimpleParticleType(false));

    private ScpAdditionsModParticleTypes() {
    }
}
