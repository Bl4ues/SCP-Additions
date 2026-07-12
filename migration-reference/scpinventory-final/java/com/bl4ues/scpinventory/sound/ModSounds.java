package com.bl4ues.scpinventory.sound;

import com.bl4ues.scpinventory.ScpInventoryMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ScpInventoryMod.MODID);

    public static final RegistryObject<SoundEvent> SCARE = register("scare");
    public static final RegistryObject<SoundEvent> HORROR = register("horror");
    public static final RegistryObject<SoundEvent> STATUE_DEATH = register("statue_death");
    public static final RegistryObject<SoundEvent> STONE_SCRAP = register("stone_scrap");
    public static final RegistryObject<SoundEvent> STONE_DRAG = register("stone_drag");
    public static final RegistryObject<SoundEvent> NECK_SNAP = register("neck_snap");
    public static final RegistryObject<SoundEvent> RATTLE = register("rattle");
    public static final RegistryObject<SoundEvent> EYE_POD_IDLE = register("eye_pod_idle");
    public static final RegistryObject<SoundEvent> EYE_POD_MOVE = register("eye_pod_move");
    public static final RegistryObject<SoundEvent> EYE_POD_VOICE = register("eye_pod_voice");

    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = new ResourceLocation(ScpInventoryMod.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
