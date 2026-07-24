package net.mcreator.scpadditions.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Sound events owned by SCP-106 and the optional world-entry cue. */
public final class Scp106Sounds {
    public static final DeferredRegister<SoundEvent> REGISTRY =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
                    ScpAdditionsMod.MODID);

    public static final RegistryObject<SoundEvent> PHASE = register("scp_106_phase");
    public static final RegistryObject<SoundEvent> CHASE = register("scp_106_chase");
    public static final RegistryObject<SoundEvent> STOP = register("scp_106_stop");
    public static final RegistryObject<SoundEvent> ENTER = register("enter");

    private Scp106Sounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = new ResourceLocation(ScpAdditionsMod.MODID, name);
        return REGISTRY.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
