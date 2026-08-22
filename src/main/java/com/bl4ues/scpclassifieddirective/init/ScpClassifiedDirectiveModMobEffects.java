package com.bl4ues.scpclassifieddirective.init;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import net.minecraft.world.effect.MobEffect;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.effect.BleedingEffect;
import com.bl4ues.scpclassifieddirective.effect.EyeSoreEffect;
import com.bl4ues.scpclassifieddirective.effect.LubricatedEyeEffect;
import com.bl4ues.scpclassifieddirective.effect.Scp1176HoneyedEffect;
import com.bl4ues.scpclassifieddirective.effect.Scp330HandLossEffect;

public class ScpClassifiedDirectiveModMobEffects {
    public static final DeferredRegister<MobEffect> REGISTRY =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,
                    ScpClassifiedDirectiveMod.MODID);
    public static final RegistryObject<MobEffect> SCP_330_HAND_LOSS =
            REGISTRY.register("scp_330_hand_loss", Scp330HandLossEffect::new);
    public static final RegistryObject<MobEffect> EYE_SORE =
            REGISTRY.register("eye_sore", EyeSoreEffect::new);
    public static final RegistryObject<MobEffect> LUBRICATED_EYE =
            REGISTRY.register("lubricated_eye", LubricatedEyeEffect::new);
    public static final RegistryObject<MobEffect> SCP_1176_HONEYED =
            REGISTRY.register("scp_1176_honeyed", Scp1176HoneyedEffect::new);
    public static final RegistryObject<MobEffect> BLEEDING =
            REGISTRY.register("bleeding", BleedingEffect::new);
}
