package net.mcreator.scpadditions.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Sound registry kept separate from MCreator's regenerated sound holder. */
public final class MainMenuSounds {
    public static final DeferredRegister<SoundEvent> REGISTRY =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
                    ScpAdditionsMod.MODID);
    public static final RegistryObject<SoundEvent> MAIN_MENU =
            REGISTRY.register("main_menu", () ->
                    SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(ScpAdditionsMod.MODID,
                                    "main_menu")));

    private MainMenuSounds() {
    }
}
