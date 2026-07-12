package com.bl4ues.scpinventory.effect;

import com.bl4ues.scpinventory.ScpInventoryMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ScpInventoryMod.MODID);

    public static final RegistryObject<MobEffect> EYE_SORE = MOB_EFFECTS.register("eye_sore", EyeSoreEffect::new);

    private ModMobEffects() {
    }
}
