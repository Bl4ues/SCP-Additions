package net.mcreator.scpadditions.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.ScpAdditionsMod;

public final class Scp173Sounds {
    public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ScpAdditionsMod.MODID);

    public static final RegistryObject<SoundEvent> SCARE = register("scare");
    public static final RegistryObject<SoundEvent> HORROR = register("horror");
    public static final RegistryObject<SoundEvent> STATUE_DEATH = register("statue_death");
    public static final RegistryObject<SoundEvent> STONE_SCRAP = register("stone_scrap");
    public static final RegistryObject<SoundEvent> NECK_SNAP = register("neck_snap");
    public static final RegistryObject<SoundEvent> RATTLE = register("rattle");

    private Scp173Sounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = new ResourceLocation(ScpAdditionsMod.MODID, name);
        return REGISTRY.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
