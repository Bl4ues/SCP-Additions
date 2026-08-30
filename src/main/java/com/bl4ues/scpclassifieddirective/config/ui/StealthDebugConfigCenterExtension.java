package com.bl4ues.scpclassifieddirective.config.ui;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.StealthDebugClientPreferences;
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Adds the client-local player stealth readout option to Debug Tools. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class StealthDebugConfigCenterExtension {
    private static final String EXTENDED_TOGGLE_SCREEN =
            "com.bl4ues.scpclassifieddirective.config.ui.Scp079ModulesScreenExtension$ExtendedToggleScreen";
    private static final String DEBUG_TITLE = "Debug Tools";

    private static final int NAVY = 0xFF0D1116;
    private static final int NAVY_HOVER = 0xFF1A2028;
    private static final int ACCENT = 0xFFC99B18;
    private static final int WHITE = 0xFFF7F8FC;
    private static final int MUTED = 0xFF9CA3AF;
    private static final int PALE_GOLD = 0xFFE3C865;
    private static final int CLIENT_BADGE = 0xFF071524;
    private static final int ON = 0xFF79D58B;
    private static final int OFF = 0xFFFF8B8B;

    private StealthDebugConfigCenterExtension() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!isDebugTools(screen)) return;

        Layout layout = layout(screen);
        event.addListener(new LocalStealthButton(layout.x(), layout.rowY(),
                layout.width(), 22));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!isDebugTools(screen)) return;

        Layout layout = layout(screen);
        event.getGuiGraphics().drawString(Minecraft.getInstance().font,
                ScpFonts.roboto("Shows the local player's current stealth value."),
                layout.x() + 2, layout.rowY() + 24, MUTED, false);
    }

    private static boolean isDebugTools(Screen screen) {
        return screen != null
                && EXTENDED_TOGGLE_SCREEN.equals(screen.getClass().getName())
                && DEBUG_TITLE.equals(screen.getTitle().getString());
    }

    private static Layout layout(Screen screen) {
        int panelWidth = Math.min(620, screen.width - 20);
        int panelHeight = Math.min(240, screen.height - 16);
        int panelX = ConfigCenterVisuals.contentLeft(screen.width, panelWidth);
        int panelY = Math.max(8, (screen.height - panelHeight) / 2);
        int x = panelX + 16;
        int rowY = panelY + 44 + 3 * 34;
        return new Layout(x, rowY, panelWidth - 32);
    }

    private record Layout(int x, int rowY, int width) {
    }

    private static final class LocalStealthButton extends AbstractButton {
        private LocalStealthButton(int x, int y, int width, int height) {
            super(x, y, width, height, ScpFonts.roboto("Player Stealth HUD"));
        }

        @Override
        public void onPress() {
            StealthDebugClientPreferences.toggleStealthHud();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            boolean hovered = isHoveredOrFocused();
            int left = getX();
            int top = getY();
            int right = left + getWidth();
            int bottom = top + getHeight();

            graphics.fill(left, top, right, bottom,
                    hovered ? NAVY_HOVER : NAVY);
            graphics.fill(left, top, left + (hovered ? 5 : 4), bottom, ACCENT);

            Font font = Minecraft.getInstance().font;
            Component title = ScpFonts.roboto("Player Stealth HUD");
            int textY = top + Math.max(1, (getHeight() - 8) / 2);
            graphics.drawString(font, title, left + 16, textY, WHITE, false);

            Component scope = ScpFonts.roboto("CLIENT");
            int scopeWidth = font.width(scope) + 12;
            int stateReserve = 54;
            int scopeX = right - stateReserve - scopeWidth - 12;
            graphics.fill(scopeX, top + 2, scopeX + scopeWidth, bottom - 2,
                    CLIENT_BADGE);
            graphics.drawString(font, scope,
                    scopeX + (scopeWidth - font.width(scope)) / 2,
                    textY, PALE_GOLD, false);

            boolean enabled = StealthDebugClientPreferences.showStealthHud();
            Component state = ScpFonts.roboto(enabled ? "ON" : "OFF");
            graphics.drawString(font, state,
                    right - 14 - font.width(state), textY,
                    enabled ? ON : OFF, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
