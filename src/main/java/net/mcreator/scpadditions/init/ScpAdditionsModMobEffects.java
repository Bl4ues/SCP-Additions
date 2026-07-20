package net.mcreator.scpadditions.init;

import java.util.function.Supplier;

import net.neoforged.neoforge.registries.ForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;

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
    public static final Supplier<MobEffect> EYE_SORE =
            REGISTRY.register("eye_sore", EyeSoreEffect::new);
    public static final Supplier<MobEffect> LUBRICATED_EYE =
            REGISTRY.register("lubricated_eye", LubricatedEyeEffect::new);
    public static final Supplier<MobEffect> SCP_1176_HONEYED =
            REGISTRY.register("scp_1176_honeyed", Scp1176HoneyedEffect::new);
    public static final Supplier<MobEffect> BLEEDING =
            REGISTRY.register("bleeding", BleedingEffect::new);
}
