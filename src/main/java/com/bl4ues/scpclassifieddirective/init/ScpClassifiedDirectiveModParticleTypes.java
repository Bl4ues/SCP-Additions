package com.bl4ues.scpclassifieddirective.init;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

public final class ScpClassifiedDirectiveModParticleTypes {
    public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(
            ForgeRegistries.PARTICLE_TYPES, ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<SimpleParticleType> DECONTAMINATION_GAS = REGISTRY.register(
            "decontamination_gas", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> SCP_106_CORROSION = REGISTRY.register(
            "scp_106_corrosion", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> SCP_106_PORTAL = REGISTRY.register(
            "scp_106_portal", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> DAMAGE_SPLATTER = REGISTRY.register(
            "damage_splatter", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> TESLA_ARC = REGISTRY.register(
            "tesla_arc", () -> new SimpleParticleType(false));

    private ScpClassifiedDirectiveModParticleTypes() {
    }
}
