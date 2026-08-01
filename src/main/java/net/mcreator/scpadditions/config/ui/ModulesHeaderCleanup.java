package net.mcreator.scpadditions.config.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Keeps the General & Modules header clear now that scope lives on each row. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class ModulesHeaderCleanup {
    private static final String EXTENDED_SCREEN =
            "net.mcreator.scpadditions.config.ui.Scp079ModulesScreenExtension$ExtendedToggleScreen";
    private static final int PANEL = 0xEE111317;

    private ModulesHeaderCleanup() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPost(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (screen == null
                || !EXTENDED_SCREEN.equals(screen.getClass().getName())
                || !"General & Modules".equals(screen.getTitle().getString())) {
            return;
        }

        int panelWidth = Math.min(560, screen.width - 20);
        int panelHeight = Math.min(380, screen.height - 16);
        int panelX = Math.max(8, (screen.width - panelWidth) / 2);
        int panelY = Math.max(8, (screen.height - panelHeight) / 2);
        GuiGraphics graphics = event.getGuiGraphics();

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 1400.0F);
        graphics.fill(panelX + 10, panelY + 27,
                panelX + panelWidth - 10, panelY + 42, PANEL);
        graphics.pose().popPose();
    }
}
