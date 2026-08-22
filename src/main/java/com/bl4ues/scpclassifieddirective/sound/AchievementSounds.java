package com.bl4ues.scpclassifieddirective.sound;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

public final class AchievementSounds {
    public static final DeferredRegister<SoundEvent> REGISTRY =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
                    ScpClassifiedDirectiveMod.MODID);
    public static final RegistryObject<SoundEvent> ACHIEVEMENT =
            REGISTRY.register("achievement", () -> {
                ResourceLocation id = new ResourceLocation(
                        ScpClassifiedDirectiveMod.MODID, "achievement");
                return SoundEvent.createVariableRangeEvent(id);
            });

    private AchievementSounds() {
    }
}
