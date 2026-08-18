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

/** Modern navigation composition for the Configuration Center home screen. */
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
            renderComposition(event.getGuiGraphics(), screen,
                    event.getMouseX(), event.getMouseY());
        }

        private static boolean isHome(Screen screen) {
            return screen != null && HOME_SCREEN.equals(screen.getClass().getName());
        }

        private static Layout layout(Screen screen) {
            int navWidth = clamp(Math.round(screen.width * 0.245F), 320, 410);
            int infoWidth = clamp(Math.round(screen.width * 0.245F), 300, 430);
            int columnGap = clamp(Math.round(screen.width * 0.014F), 16, 24);
            int total = navWidth + columnGap + infoWidth;
            int preferred = Math.round(screen.width * 0.365F);
            int navX = clamp(Math.max(preferred, screen.width - total - 54),
                    20, Math.max(20, screen.width - total - 20));
            int infoX = navX + navWidth + columnGap;
            int headerY = Math.max(35, Math.round(screen.height * 0.070F));
            int startY = Math.max(132, Math.round(screen.height * 0.225F));
            int rowHeight = clamp(Math.round(screen.height * 0.060F), 34, 46);
            int rowGap = clamp(Math.round(screen.height * 0.012F), 7, 10);
            return new Layout(navX, infoX, navWidth, infoWidth, headerY,
                    startY, rowHeight, rowGap);
        }

        private static void arrange(Screen screen) {
            Layout l = layout(screen);
            Button general = find(screen, "General & Modules");
            Button inventory = first(screen, "Items, Entities & Codex",
                    "Inventory, Equipment & Codex");
            Button interactions = find(screen, "Contextual Interactions");
            Button drinks = find(screen, "SCP-294 Drinks");
            Button recipes = find(screen, "SCP-914 Recipes");
            Button integrations = find(screen, "Mod Integrations");
            Button[] primary = {general, inventory, interactions, drinks, recipes,
                    integrations};

            int y = l.startY;
            for (Button button : primary) {
                if (button == null) continue;
                place(button, l.navX, y, l.navWidth, l.rowHeight);
                y += l.rowHeight + l.rowGap;
            }

            int toolY = l.startY + 178;
            Button crosshair = find(screen, "Crosshair");
            Button accessibility = find(screen, "Accessibility");
            Button debug = find(screen, "Debug Tools");
            Button reload = find(screen, "Reload Snapshot");
            Button done = find(screen, "Done");
            if (crosshair != null) {
                place(crosshair, l.infoX, toolY, l.infoWidth, l.rowHeight);
                toolY += l.rowHeight + l.rowGap;
            }
            if (accessibility != null) {
                place(accessibility, l.infoX, toolY, l.infoWidth, l.rowHeight);
                toolY += l.rowHeight + l.rowGap;
            }
            if (debug != null) {
                place(debug, l.infoX, toolY, l.infoWidth, l.rowHeight);
                toolY += l.rowHeight + l.rowGap + 7;
            }
            int footerHeight = Math.max(30, l.rowHeight - 6);
            if (reload != null) {
                place(reload, l.infoX, toolY, l.infoWidth, footerHeight);
                toolY += footerHeight + l.rowGap;
            }
            if (done != null) {
                place(done, l.infoX + l.infoWidth - 118,
                        l.headerY + 4, 118, 30);
            }
        }

        private static void renderComposition(GuiGraphics graphics, Screen screen,
                int mouseX, int mouseY) {
            Layout l = layout(screen);
            Font font = Minecraft.getInstance().font;
            float alpha = ConfigCenterVisuals.contentAlpha();
            int slide = ConfigCenterVisuals.contentOffsetX();
            int navX = l.navX + slide;
            int infoX = l.infoX + slide;

            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 1200.0F);

            ConfigCenterVisuals.drawScaledText(graphics, font,
                    ScpFonts.montserrat("CONFIGURATION CENTER"),
                    navX, l.headerY,
                    screen.height < 480 ? 1.34F : 1.58F,
                    ConfigCenterVisuals.fadeColor(ConfigCenterVisuals.TEXT));
            ConfigCenterVisuals.drawScaledText(graphics, font,
                    ScpFonts.titillium("SCP ADDITIONS / SYSTEM CONFIGURATION"),
                    navX, l.headerY + 27,
                    0.92F, ConfigCenterVisuals.fadeColor(ConfigCenterVisuals.ACCENT_BRIGHT));
            int lineY = l.headerY + 50;
            graphics.fill(navX, lineY, infoX + l.infoWidth, lineY + 2,
                    ConfigCenterVisuals.fadeColor(ConfigCenterVisuals.ACCENT));

            graphics.drawString(font, ScpFonts.titillium("SYSTEMS"),
                    navX, l.startY - 18,
                    ConfigCenterVisuals.fadeColor(ConfigCenterVisuals.ACCENT_BRIGHT), false);

            Button hovered = hoveredNavigation(screen, mouseX, mouseY);
            Info info = infoFor(hovered == null ? "General & Modules"
                    : hovered.getMessage().getString());
            int cardY = l.startY;
            int cardH = 146;
            graphics.fill(infoX, cardY, infoX + l.infoWidth, cardY + cardH,
                    fade(0x8A0B0E12, alpha));
            graphics.fill(infoX, cardY, infoX + 4, cardY + cardH,
                    ConfigCenterVisuals.fadeColor(ConfigCenterVisuals.ACCENT));
            graphics.drawString(font, ScpFonts.titillium("SECTION"),
                    infoX + 16, cardY + 16,
                    ConfigCenterVisuals.fadeColor(ConfigCenterVisuals.ACCENT_BRIGHT), false);
            ConfigCenterVisuals.drawScaledText(graphics, font,
                    ScpFonts.montserrat(info.title), infoX + 16, cardY + 37,
                    1.12F, ConfigCenterVisuals.fadeColor(ConfigCenterVisuals.TEXT));
            int bodyY = cardY + 67;
            for (var line : font.split(ScpFonts.roboto(info.description), l.infoWidth - 32)) {
                graphics.drawString(font, line, infoX + 16, bodyY,
                        ConfigCenterVisuals.fadeColor(ConfigCenterVisuals.MUTED), false);
                bodyY += font.lineHeight + 3;
            }

            graphics.drawString(font, ScpFonts.titillium("TOOLS & SESSION"),
                    infoX, l.startY + 160,
                    ConfigCenterVisuals.fadeColor(ConfigCenterVisuals.ACCENT_BRIGHT), false);
            graphics.pose().popPose();
        }

        private static Button hoveredNavigation(Screen screen, int mouseX, int mouseY) {
            for (GuiEventListener listener : screen.children()) {
                if (listener instanceof Button button && button.visible
                        && button.isMouseOver(mouseX, mouseY)) return button;
            }
            return null;
        }

        private static Info infoFor(String label) {
            return switch (label) {
                case "Items, Entities & Codex", "Inventory, Equipment & Codex" ->
                        new Info("Items, Entities & Codex",
                                "Edit inventory rules, equipment behavior, entity data and Codex content.");
                case "Contextual Interactions" -> new Info("Contextual Interactions",
                        "Control contextual prompts, targets and interaction behavior used throughout facilities.");
                case "SCP-294 Drinks" -> new Info("SCP-294 Drinks",
                        "Manage drink definitions, effects, colors and custom dispensing behavior.");
                case "SCP-914 Recipes" -> new Info("SCP-914 Recipes",
                        "Review and edit refinement recipes across SCP-914 settings and recipe files.");
                case "Mod Integrations" -> new Info("Mod Integrations",
                        "Enable deeper integrations that adapt supported mods to SCP Additions gameplay and systems.");
                case "Crosshair" -> new Info("Crosshair",
                        "Configure the custom crosshair, visibility, colour channels and opacity.");
                case "Accessibility" -> new Info("Accessibility",
                        "Client-side presentation options intended to improve comfort and readability.");
                case "Debug Tools" -> new Info("Debug Tools",
                        "Diagnostics and maintenance controls intended for development and troubleshooting.");
                case "Reload Snapshot" -> new Info("Reload Snapshot",
                        "Discard the current snapshot and request a fresh configuration state.");
                case "Done" -> new Info("Done", "Return to the previous screen.");
                default -> new Info("General & Modules",
                        "Configure the main gameplay systems, survival mechanics and feature modules used by SCP Additions.");
            };
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
                        && label.equals(button.getMessage().getString())) return button;
            }
            return null;
        }

        private static void place(Button button, int x, int y, int width, int height) {
            button.visible = true;
            button.setX(x);
            button.setY(y);
            button.setWidth(width);
            button.setHeight(height);
        }

        private static int fade(int color, float alpha) {
            int sourceAlpha = color >>> 24;
            int finalAlpha = clamp(Math.round(sourceAlpha * alpha), 0, 255);
            return finalAlpha << 24 | color & 0x00FFFFFF;
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }

        private record Layout(int navX, int infoX, int navWidth, int infoWidth,
                              int headerY, int startY, int rowHeight, int rowGap) {
        }

        private record Info(String title, String description) {
        }
    }
}
