package net.mcreator.scpadditions.init;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import net.minecraft.world.effect.MobEffect;

import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.effect.BleedingEffect;
import net.mcreator.scpadditions.effect.EyeSoreEffect;
import net.mcreator.scpadditions.effect.LubricatedEyeEffect;
import net.mcreator.scpadditions.effect.Scp1176HoneyedEffect;

public class ScpAdditionsModMobEffects {
    public static final DeferredRegister<MobEffect> REGISTRY =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,
                    ScpAdditionsMod.MODID);
    public static final RegistryObject<MobEffect> EYE_SORE =
            REGISTRY.register("eye_sore", EyeSoreEffect::new);
    public static final RegistryObject<MobEffect> LUBRICATED_EYE =
            REGISTRY.register("lubricated_eye", LubricatedEyeEffect::new);
    public static final RegistryObject<MobEffect> SCP_1176_HONEYED =
            REGISTRY.register("scp_1176_honeyed", Scp1176HoneyedEffect::new);
    public static final RegistryObject<MobEffect> BLEEDING =
            REGISTRY.register("bleeding", BleedingEffect::new);
}
