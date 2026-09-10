package com.bl4ues.scpclassifieddirective.sound;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Sound events for the mapped facility's continuous environmental ambience. */
public final class FacilityAmbientSounds {
    public static final DeferredRegister<SoundEvent> REGISTRY =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
                    ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<SoundEvent> CORE_ROOM =
            register("coreroom_ambient");
    public static final RegistryObject<SoundEvent> LCZ_SL1 =
            register("sl1_ambient");
    public static final RegistryObject<SoundEvent> LCZ_SL2 =
            register("sl2_ambient");
    public static final RegistryObject<SoundEvent> LCZ_SL3 =
            register("sl3_ambient");

    private FacilityAmbientSounds() {
    }

    private static RegistryObject<SoundEvent> register(String id) {
        return REGISTRY.register(id, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(ScpClassifiedDirectiveMod.MODID, id)));
    }
}
