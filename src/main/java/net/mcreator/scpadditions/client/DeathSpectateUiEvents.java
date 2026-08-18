package net.mcreator.scpadditions.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/**
 * Frame-rate camera updates and direct drag handling for the death live feed.
 *
 * The feed viewport is not a vanilla widget, so relying on Screen#mouseDragged
 * meant Minecraft could decide no component had captured the initial click and
 * never deliver a drag. Forge's screen input events let the viewport own that
 * interaction explicitly without changing the normal death-screen buttons.
 */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DeathSpectateUiEvents {
    private static boolean draggingFeed;

    private DeathSpectateUiEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPre(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof ScpDeathScreen)
                || !MineZeroSpectateClient.active()) {
            draggingFeed = false;
            return;
        }
        // The world is rendered immediately before its screen. Reapplying here
        // updates the rig once per rendered frame (one frame ahead of the next
        // world pass) rather than only at Minecraft's 20 Hz client tick.
        MineZeroSpectateClient.updateForRender(event.getPartialTick());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0
                || !(event.getScreen() instanceof ScpDeathScreen screen)
                || !MineZeroSpectateClient.active()
                || !insideFeed(screen, event.getMouseX(), event.getMouseY())) {
            return;
        }
        draggingFeed = true;
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!draggingFeed || event.getMouseButton() != 0
                || !(event.getScreen() instanceof ScpDeathScreen)
                || !MineZeroSpectateClient.active()) {
            return;
        }
        MineZeroSpectateClient.orbit(event.getDragX(), event.getDragY());
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getButton() != 0 || !draggingFeed) return;
        draggingFeed = false;
        if (event.getScreen() instanceof ScpDeathScreen
                && MineZeroSpectateClient.active()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        Screen next = event.getNewScreen();
        if (!(next instanceof ScpDeathScreen)) draggingFeed = false;
    }

    private static boolean insideFeed(ScpDeathScreen screen,
            double mouseX, double mouseY) {
        int left = Math.max(screen.width / 2 + 28,
                Math.round(screen.width * 0.53F));
        int right = Math.max(left + 120, screen.width - 28);
        int top = Math.max(46, Math.round(screen.height * 0.095F));
        int bottom = Math.max(top + 100, screen.height - 64);
        return mouseX >= left && mouseX < right
                && mouseY >= top && mouseY < bottom;
    }
}
