package com.bl4ues.scpclassifieddirective.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/** Sound events owned by SCP-106 and the optional world-entry cue. */
public final class Scp106Sounds {
    public static final DeferredRegister<SoundEvent> REGISTRY =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
                    ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<SoundEvent> PHASE = register("scp_106_phase");
    public static final RegistryObject<SoundEvent> CHASE = register("scp_106_chase");
    public static final RegistryObject<SoundEvent> STOP = register("scp_106_stop");
    public static final RegistryObject<SoundEvent> STEP = register("scp_106_step");
    public static final RegistryObject<SoundEvent> HIT = register("scp_106_hit");
    public static final RegistryObject<SoundEvent> RANGED_SPLASH =
            register("scp_106_ranged_splash");
    public static final RegistryObject<SoundEvent> WITHER = register("wither");
    public static final RegistryObject<SoundEvent> ENTER = register("enter");

    private Scp106Sounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = new ResourceLocation(ScpClassifiedDirectiveMod.MODID, name);
        return REGISTRY.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
