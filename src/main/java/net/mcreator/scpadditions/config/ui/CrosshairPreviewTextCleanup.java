package net.mcreator.scpadditions.config.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Removes the implementation-size note from the Crosshair preview. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class CrosshairPreviewTextCleanup {
    private static final String CROSSHAIR_SCREEN =
            "net.mcreator.scpadditions.config.ui.Scp079ModulesScreenExtension$CrosshairScreen";
    private static final int PANEL = 0xEE111317;

    private CrosshairPreviewTextCleanup() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPost(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (screen == null
                || !CROSSHAIR_SCREEN.equals(screen.getClass().getName())) {
            return;
        }

        int panelWidth = Math.min(650, screen.width - 20);
        int panelHeight = Math.min(360, screen.height - 16);
        int panelX = Math.max(8, (screen.width - panelWidth) / 2);
        int panelY = Math.max(8, (screen.height - panelHeight) / 2);
        GuiGraphics graphics = event.getGuiGraphics();

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 1500.0F);
        graphics.fill(panelX + 44, panelY + 231,
                panelX + 212, panelY + 250, PANEL);
        graphics.pose().popPose();
    }
}
