package com.bl4ues.scpclassifieddirective.sound;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/** Sound events for mapped-facility continuous and intermittent ambience. */
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

    public static final List<RegistryObject<SoundEvent>> FACILITY_NOISES =
            registerSeries("facility_noise_", 15);
    public static final List<RegistryObject<SoundEvent>> CORE_NOISES =
            registerSeries("core_noises_", 9);

    private FacilityAmbientSounds() {
    }

    public static int distantNoiseCount(boolean coreRoom) {
        return (coreRoom ? CORE_NOISES : FACILITY_NOISES).size();
    }

    public static SoundEvent distantNoise(boolean coreRoom, int index) {
        List<RegistryObject<SoundEvent>> sounds =
                coreRoom ? CORE_NOISES : FACILITY_NOISES;
        if (sounds.isEmpty()) return null;
        return sounds.get(Math.floorMod(index, sounds.size())).get();
    }

    private static List<RegistryObject<SoundEvent>> registerSeries(
            String prefix, int count) {
        List<RegistryObject<SoundEvent>> sounds =
                new ArrayList<>(Math.max(0, count));
        for (int index = 1; index <= count; index++) {
            sounds.add(register(prefix + index));
        }
        return List.copyOf(sounds);
    }

    private static RegistryObject<SoundEvent> register(String id) {
        return REGISTRY.register(id, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(ScpClassifiedDirectiveMod.MODID, id)));
    }
}
