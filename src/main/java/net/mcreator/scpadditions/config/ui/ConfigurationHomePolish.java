package net.mcreator.scpadditions.config.ui;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Runs after the other UI extensions so the home screen keeps one stable layout. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ConfigurationHomePolish {
    private ConfigurationHomePolish() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MinecraftForge.EVENT_BUS.register(new Handler()));
    }

    private static final class Handler {
        private static final String HOME_SCREEN =
                "net.mcreator.scpadditions.config.ui.ConfigCenterClient$HomeScreen";
        private static final ResourceLocation CONFIG_LOGO = new ResourceLocation(
                ScpAdditionsMod.MODID, "textures/screens/logo.png");
        private static final int HEADER = 0xFF24282E;
        private static final int WHITE = 0xFFF7F8FC;
        private static final int MUTED = 0xFF9CA3AF;

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onInitPost(ScreenEvent.Init.Post event) {
            if (isHome(event.getScreen())) arrange(event.getScreen());
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onRenderPost(ScreenEvent.Render.Post event) {
            Screen screen = event.getScreen();
            if (!isHome(screen)) return;
            arrange(screen);
            renderHeader(event.getGuiGraphics(), screen);
        }

        private static boolean isHome(Screen screen) {
            return screen != null && HOME_SCREEN.equals(screen.getClass().getName());
        }

        private static void arrange(Screen screen) {
            Button general = find(screen, "General & Modules");
            if (general == null) return;

            Button crosshair = find(screen, "Crosshair");
            if (crosshair != null) {
                crosshair.visible = false;
                crosshair.active = false;
                crosshair.setX(-10_000);
            }

            int panelHeight = Math.min(310, screen.height - 20);
            int panelY = Math.max(10, (screen.height - panelHeight) / 2);
            int startY = panelY + 60;
            int step = 31;

            general.setY(startY);
            // Keep compatibility with both the original label and the renamed
            // mixed-content hub. Only one of these buttons exists at runtime.
            setY(screen, "Inventory, Equipment & Codex", startY + step);
            setY(screen, "Items, Entities & Codex", startY + step);
            setY(screen, "Contextual Interactions", startY + step * 2);
            setY(screen, "SCP-294 Drinks", startY + step * 3);
            setY(screen, "SCP-914 Recipes", startY + step * 4);
            setY(screen, "Accessibility", startY + step * 5);
            setY(screen, "Debug Tools", startY + step * 5);
            setY(screen, "Reload Snapshot", startY + step * 6 + 6);
            setY(screen, "Done", startY + step * 6 + 6);
        }

        private static Button find(Screen screen, String label) {
            for (GuiEventListener listener : screen.children()) {
                if (listener instanceof Button button
                        && label.equals(button.getMessage().getString())) {
                    return button;
                }
            }
            return null;
        }

        private static void setY(Screen screen, String label, int y) {
            Button button = find(screen, label);
            if (button != null) button.setY(y);
        }

        private static void renderHeader(GuiGraphics graphics, Screen screen) {
            int panelWidth = Math.min(420, screen.width - 20);
            int panelHeight = Math.min(310, screen.height - 20);
            int panelX = Math.max(8, (screen.width - panelWidth) / 2);
            int panelY = Math.max(10, (screen.height - panelHeight) / 2);
            Font font = Minecraft.getInstance().font;

            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 1200.0F);
            graphics.fill(panelX, panelY,
                    panelX + panelWidth, panelY + 44, HEADER);

            int titleX = panelX + 14;
            if (Minecraft.getInstance().getResourceManager()
                    .getResource(CONFIG_LOGO).isPresent()) {
                graphics.blit(CONFIG_LOGO, panelX + 10, panelY + 7,
                        30, 26, 0.0F, 0.0F,
                        960, 832, 960, 832);
                titleX = panelX + 48;
            }

            graphics.pose().pushPose();
            graphics.pose().translate(titleX, panelY + 8, 0.0F);
            graphics.pose().scale(1.08F, 1.08F, 1.0F);
            graphics.drawString(font,
                    ScpFonts.montserrat("SCP Additions Configuration"),
                    0, 0, WHITE, false);
            graphics.pose().popPose();
            graphics.drawString(font, ScpFonts.roboto(
                            "Gameplay, interface, and content settings."),
                    titleX, panelY + 27, MUTED, false);
            graphics.pose().popPose();
        }
    }
}
