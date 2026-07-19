package net.mcreator.scpadditions.scp012;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * SCP-012 audio uses a dedicated resource namespace so its authored sound
 * definitions remain isolated from the large legacy sounds.json.
 */
public final class Scp012Sounds {
    public static final String NAMESPACE = "scp_additions_012";
    public static final DeferredRegister<SoundEvent> REGISTRY =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, NAMESPACE);

    public static final RegistryObject<SoundEvent> TRANCE = sound("trance");
    public static final RegistryObject<SoundEvent> DAMAGE = sound("damage");
    public static final RegistryObject<SoundEvent> OPEN = sound("open");
    public static final RegistryObject<SoundEvent> CLOSE = sound("close");
    public static final RegistryObject<SoundEvent> BLEED_1 = sound("bleed_1");
    public static final RegistryObject<SoundEvent> BLEED_2 = sound("bleed_2");
    public static final RegistryObject<SoundEvent> BLEED_3 = sound("bleed_3");
    public static final RegistryObject<SoundEvent> ON_MOUNT_GOLGOTHA =
            sound("on_mount_golgotha");

    private Scp012Sounds() {
    }

    public static void register(IEventBus bus) {
        REGISTRY.register(bus);
    }

    private static RegistryObject<SoundEvent> sound(String id) {
        return REGISTRY.register(id, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(NAMESPACE, id)));
    }
}
