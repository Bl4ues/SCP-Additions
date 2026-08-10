package net.mcreator.scpadditions.config.ui;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Final responsive layout pass for the Configuration Center home screen. */
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
            int navWidth = Math.min(ConfigCenterVisuals.navigationWidth(screen.width),
                    screen.width - 24);
            int navX = ConfigCenterVisuals.contentLeft(screen.width, navWidth);
            int rowHeight = MthCompat.clamp(Math.round(screen.height * 0.058F),
                    30, 42);
            int gap = MthCompat.clamp(Math.round(screen.height * 0.012F),
                    6, 10);
            int startY = Math.max(90, Math.round(screen.height * 0.205F));
            int usableBottom = screen.height - 54;

            Button crosshair = find(screen, "Crosshair");
            Button general = find(screen, "General & Modules");
            Button inventory = first(screen, "Items, Entities & Codex",
                    "Inventory, Equipment & Codex");
            Button interactions = find(screen, "Contextual Interactions");
            Button drinks = find(screen, "SCP-294 Drinks");
            Button recipes = find(screen, "SCP-914 Recipes");
            Button accessibility = find(screen, "Accessibility");
            Button debug = find(screen, "Debug Tools");
            Button reload = find(screen, "Reload Snapshot");
            Button done = find(screen, "Done");

            Button[] primary = {crosshair, general, inventory, interactions, drinks, recipes};
            int rows = 0;
            for (Button button : primary) if (button != null) rows++;
            int required = rows * rowHeight + Math.max(0, rows - 1) * gap
                    + rowHeight + gap + 44;
            if (startY + required > usableBottom) {
                startY = Math.max(70, usableBottom - required);
                rowHeight = Math.max(28, rowHeight - 2);
                gap = Math.max(5, gap - 1);
            }

            int y = startY;
            for (Button button : primary) {
                if (button == null) continue;
                place(button, navX, y, navWidth, rowHeight);
                y += rowHeight + gap;
            }

            if (accessibility != null || debug != null) {
                int splitGap = 8;
                int half = (navWidth - splitGap) / 2;
                if (accessibility != null)
                    place(accessibility, navX, y, half, rowHeight);
                if (debug != null)
                    place(debug, navX + half + splitGap, y,
                            navWidth - half - splitGap, rowHeight);
                y += rowHeight + gap + 8;
            }

            int footerHeight = Math.max(26, rowHeight - 6);
            int footerGap = 8;
            int half = (navWidth - footerGap) / 2;
            if (reload != null)
                place(reload, navX, y, half, footerHeight);
            if (done != null)
                place(done, navX + half + footerGap, y,
                        navWidth - half - footerGap, footerHeight);
        }

        private static Button first(Screen screen, String... labels) {
            for (String label : labels) {
                Button button = find(screen, label);
                if (button != null) return button;
            }
            return null;
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

        private static void place(Button button, int x, int y,
                int width, int height) {
            button.visible = true;
            button.setX(x);
            button.setY(y);
            button.setWidth(width);
            button.setHeight(height);
        }

        private static void renderHeader(GuiGraphics graphics, Screen screen) {
            int navWidth = Math.min(ConfigCenterVisuals.navigationWidth(screen.width),
                    screen.width - 24);
            int navX = ConfigCenterVisuals.contentLeft(screen.width, navWidth);
            Font font = Minecraft.getInstance().font;

            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 1200.0F);
            ConfigCenterVisuals.drawScaledText(graphics, font,
                    ScpFonts.montserrat("CONFIGURATION CENTER"),
                    navX, Math.max(28, screen.height * 0.075F),
                    screen.height < 480 ? 1.35F : 1.62F,
                    ConfigCenterVisuals.TEXT);
            ConfigCenterVisuals.drawScaledText(graphics, font,
                    ScpFonts.titillium("SCP ADDITIONS / SYSTEM CONFIGURATION"),
                    navX, Math.max(50, screen.height * 0.075F + 25),
                    0.92F, ConfigCenterVisuals.ACCENT_BRIGHT);
            int lineY = Math.max(69, Math.round(screen.height * 0.075F) + 47);
            graphics.fill(navX, lineY, navX + navWidth, lineY + 2,
                    ConfigCenterVisuals.ACCENT);
            graphics.drawString(font, ScpFonts.roboto(
                            "Gameplay, interface, content, and developer controls."),
                    navX, lineY + 10, ConfigCenterVisuals.MUTED, false);
            graphics.pose().popPose();
        }
    }

    /** Tiny local clamp helper to keep this layout independent of mappings. */
    private static final class MthCompat {
        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
