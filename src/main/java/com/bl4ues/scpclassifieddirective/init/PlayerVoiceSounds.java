package com.bl4ues.scpclassifieddirective.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/** Sound events for selectable player voices and shared breathing audio. */
public final class PlayerVoiceSounds {
    public static final String MODID = ScpClassifiedDirectiveMod.MODID;

    public static final DeferredRegister<SoundEvent> REGISTRY =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);

    public static final RegistryObject<SoundEvent> VOICE_PROFILE_B_HURT =
            register("voice_profile_b_hurt");
    public static final RegistryObject<SoundEvent> VOICE_PROFILE_A_GASP =
            register("voice_profile_a_gasp");
    public static final RegistryObject<SoundEvent> VOICE_PROFILE_B_GASP =
            register("voice_profile_b_gasp");
    public static final RegistryObject<SoundEvent> DROWNING_LOOP =
            register("drowning_loop");

    private PlayerVoiceSounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = new ResourceLocation(MODID, name);
        return REGISTRY.register(name,
                () -> SoundEvent.createVariableRangeEvent(id));
    }
}
