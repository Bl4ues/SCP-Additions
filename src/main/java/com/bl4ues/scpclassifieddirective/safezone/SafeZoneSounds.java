package com.bl4ues.scpclassifieddirective.safezone;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Sound events are registered before their OGG files are supplied. */
public final class SafeZoneSounds {
    public static final DeferredRegister<SoundEvent> REGISTRY =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
                    ScpClassifiedDirectiveMod.MODID);

    public static final RegistryObject<SoundEvent> SCP_914 =
            register("scp914soundtrack");
    public static final RegistryObject<SoundEvent> SCP_1176 =
            register("scp1176soundtrack");
    public static final RegistryObject<SoundEvent> SCP_079 =
            register("scp079soundtrack");
    public static final RegistryObject<SoundEvent> SCP_012 =
            register("scp012soundtrack");
    public static final RegistryObject<SoundEvent> SCP_426 =
            register("scp426soundtrack");
    public static final RegistryObject<SoundEvent> SCP_294 =
            register("scp294soundtrack");

    private SafeZoneSounds() {
    }

    public static SoundEvent forTrack(SafeZoneTrack track) {
        if (track == null) return null;
        return switch (track) {
            case SCP_914 -> SCP_914.get();
            case SCP_1176 -> SCP_1176.get();
            case SCP_079 -> SCP_079.get();
            case SCP_012 -> SCP_012.get();
            case SCP_426 -> SCP_426.get();
            case SCP_294 -> SCP_294.get();
        };
    }

    private static RegistryObject<SoundEvent> register(String id) {
        return REGISTRY.register(id, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(ScpClassifiedDirectiveMod.MODID, id)));
    }
}
