package net.mcreator.scpadditions.sound;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.ScpAdditionsMod;

public final class AchievementSounds {
    public static final DeferredRegister<SoundEvent> REGISTRY =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
                    ScpAdditionsMod.MODID);
    public static final RegistryObject<SoundEvent> ACHIEVEMENT =
            REGISTRY.register("achievement", () -> {
                ResourceLocation id = new ResourceLocation(
                        ScpAdditionsMod.MODID, "achievement");
                return SoundEvent.createVariableRangeEvent(id);
            });

    private AchievementSounds() {
    }
}
