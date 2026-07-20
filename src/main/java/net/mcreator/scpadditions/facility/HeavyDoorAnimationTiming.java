package net.mcreator.scpadditions.facility;

import java.util.function.Supplier;

import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Retimes the three redstone-controlled heavy door families to exactly 24 game
 * ticks (1.2 seconds) from the first visible animation frame to the endpoint.
 *
 * The imported Unity sequence contains thirteen transitional states. Minecraft
 * block ticks are integer-valued, so thirteen equal delays cannot total 24.
 * The final transitional state is visually adjacent to the fully open/closed
 * endpoint; omitting only that state leaves twelve transitions at two ticks
 * each, preserving the sequence while reaching an exact 24-tick duration.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class HeavyDoorAnimationTiming {
    private static final int FRAME_DELAY_TICKS = 2;
    private static boolean applied;

    private HeavyDoorAnimationTiming() {
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(HeavyDoorAnimationTiming::apply);
    }

    @SuppressWarnings("unchecked")
    private static synchronized void apply() {
        if (applied) {
            return;
        }

        try {
            Field familiesField = FacilityModule.class.getDeclaredField("DOOR_FAMILIES");
            familiesField.setAccessible(true);
            Map<String, FacilityModule.DoorFamily> families =
                    (Map<String, FacilityModule.DoorFamily>) familiesField.get(null);

            retime(families, FacilityModule.DEFAULT_DOOR);
            retime(families, FacilityModule.YELLOW_DOOR);
            retime(families, FacilityModule.BLACK_DOOR);
            applied = true;

            ScpAdditionsMod.LOGGER.info(
                    "Retimed redstone heavy doors to 24 ticks (1.2 seconds)");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            ScpAdditionsMod.LOGGER.error(
                    "Failed to apply exact heavy-door animation timing", exception);
        }
    }

    private static void retime(Map<String, FacilityModule.DoorFamily> families,
            FacilityModule.DoorFamily original) {
        List<Supplier<Block>> opening = withoutFinalTransition(original.opening());
        List<Supplier<Block>> closing = withoutFinalTransition(original.closing());

        if (opening.size() != 12 || closing.size() != 12) {
            throw new IllegalStateException("Heavy door family " + original.id()
                    + " does not have the expected 13-frame source sequence");
        }

        FacilityModule.DoorFamily timed = new FacilityModule.DoorFamily(
                original.id(),
                original.closed(),
                opening,
                original.open(),
                closing,
                FRAME_DELAY_TICKS,
                original.directUse(),
                original.openingPassableFrame(),
                original.closingSolidFrame(),
                original.openingSound(),
                original.closingSound());
        families.put(original.id(), timed);
    }

    private static List<Supplier<Block>> withoutFinalTransition(
            List<Supplier<Block>> frames) {
        if (frames == null || frames.size() < 2) {
            throw new IllegalStateException("Door animation frame list is incomplete");
        }
        return List.copyOf(frames.subList(0, frames.size() - 1));
    }
}
