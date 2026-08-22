package com.bl4ues.scpclassifieddirective.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/** Sound registry kept separate from MCreator's regenerated sound holder. */
public final class MainMenuSounds {
    public static final DeferredRegister<SoundEvent> REGISTRY =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
                    ScpClassifiedDirectiveMod.MODID);
    public static final RegistryObject<SoundEvent> MAIN_MENU =
            REGISTRY.register("main_menu", () ->
                    SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                                    "main_menu")));
    public static final RegistryObject<SoundEvent> HOVER =
            REGISTRY.register("hover", () ->
                    SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(ScpClassifiedDirectiveMod.MODID,
                                    "hover")));

    private MainMenuSounds() {
    }
}
