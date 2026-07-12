package net.mcreator.scpadditions.facility;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Manual doors can move through the player, so every intermediate animation
 * frame must be walk-through. Only the stable closed endpoint is solid; the
 * stable open endpoint and both transition sequences are passable.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FacilityDoorCollisionOverrides {
    private FacilityDoorCollisionOverrides() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(FacilityDoorCollisionOverrides::apply);
    }

    @SuppressWarnings("unchecked")
    private static void apply() {
        try {
            Field field = FacilityModule.class.getDeclaredField("DOOR_FAMILIES");
            field.setAccessible(true);
            Map<String, FacilityModule.DoorFamily> families =
                    (Map<String, FacilityModule.DoorFamily>) field.get(null);

            families.replaceAll((id, family) -> {
                if (!family.directUse()) {
                    return family;
                }
                return new FacilityModule.DoorFamily(
                        family.id(),
                        family.closed(),
                        family.opening(),
                        family.open(),
                        family.closing(),
                        family.frameDelay(),
                        true,
                        0,
                        Integer.MAX_VALUE,
                        family.openingSound(),
                        family.closingSound());
            });
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Could not apply manual-door collision policy", exception);
        }
    }
}
