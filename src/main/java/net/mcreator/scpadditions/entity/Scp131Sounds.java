package net.mcreator.scpadditions.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Sound events backed by the migrated SCP Inventory resource namespace.
 */
public final class Scp131Sounds {
    public static final String RESOURCE_NAMESPACE = "scpinventory";
    public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RESOURCE_NAMESPACE);

    public static final RegistryObject<SoundEvent> EYE_POD_VOICE = REGISTRY.register("eye_pod_voice", () ->
            SoundEvent.createVariableRangeEvent(new ResourceLocation(RESOURCE_NAMESPACE, "eye_pod_voice")));

    private Scp131Sounds() {
    }
}
