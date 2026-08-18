package net.mcreator.scpadditions.client;

import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

import java.util.Map;
import java.util.WeakHashMap;

/** Swaps only the presentation layer while preserving DeathScreen behavior. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class CustomDeathScreenEvents {
    private static final Map<DeathScreen, DeathData> CAPTURED =
            new WeakHashMap<>();

    private CustomDeathScreenEvents() {
    }

    public static void remember(DeathScreen screen, Component cause,
            boolean hardcore) {
        if (screen != null) {
            CAPTURED.put(screen, new DeathData(cause, hardcore));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onOpening(ScreenEvent.Opening event) {
        Screen incoming = event.getNewScreen();
        if (!(incoming instanceof DeathScreen death)
                || incoming instanceof ScpDeathScreen
                || !CustomDeathScreenPreferences.enabled()) {
            return;
        }

        DeathData data = CAPTURED.remove(death);
        Component cause = data == null || data.cause() == null
                ? Component.literal("Unknown cause of death.") : data.cause();
        boolean hardcore = data != null && data.hardcore();
        event.setNewScreen(new ScpDeathScreen(cause, hardcore));
    }

    private record DeathData(Component cause, boolean hardcore) {
    }
}
