package net.mcreator.scpadditions.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Sound registry kept separate from MCreator's regenerated sound holder. */
public final class MainMenuSounds {
    public static final String NAMESPACE = "scp_additions_menu";
    public static final DeferredRegister<SoundEvent> REGISTRY =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, NAMESPACE);
    public static final RegistryObject<SoundEvent> MAIN_MENU =
            REGISTRY.register("main_menu", () ->
                    SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(NAMESPACE, "main_menu")));

    private MainMenuSounds() {
    }
}
