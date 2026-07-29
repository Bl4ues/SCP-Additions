package net.mcreator.scpadditions.sound;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Sound events for general gameplay feedback. */
public final class GameplaySounds {
    public static final DeferredRegister<SoundEvent> REGISTRY =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
                    ScpAdditionsMod.MODID);

    public static final RegistryObject<SoundEvent> SAVE_GAME =
            register("save_game");

    private GameplaySounds() {
    }

    private static RegistryObject<SoundEvent> register(String path) {
        ResourceLocation id = new ResourceLocation(ScpAdditionsMod.MODID, path);
        return REGISTRY.register(path,
                () -> SoundEvent.createVariableRangeEvent(id));
    }
}
