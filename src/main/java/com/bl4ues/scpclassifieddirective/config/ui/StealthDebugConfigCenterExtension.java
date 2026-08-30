package com.bl4ues.scpclassifieddirective.config.ui;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.StealthDebugClientPreferences;
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Replaces Debug Tools module rows with client-local HUD preferences. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class StealthDebugConfigCenterExtension {
    private static final String EXTENDED_TOGGLE_SCREEN =
            "com.bl4ues.scpclassifieddirective.config.ui.Scp079ModulesScreenExtension$ExtendedToggleScreen";
    private static final String DEBUG_TITLE = "Debug Tools";
    private static final int ROW_HEIGHT = 34;

    private static final int NAVY = 0xFF0D1116;
    private static final int NAVY_HOVER = 0xFF1A2028;
    private static final int BORDER = 0xFF3A424D;
    private static final int BORDER_HOVER = 0xFFC99B18;
    private static final int ACCENT = 0xFFC99B18;
    private static final int WHITE = 0xFFF7F8FC;
    private static final int MUTED = 0xFF9CA3AF;
    private static final int PALE_GOLD = 0xFFE3C865;
    private static final int CLIENT_BADGE = 0xFF071524;
    private static final int ON = 0xFF79D58B;
    private static final int OFF = 0xFFFF8B8B;

    private static final List<DebugPreference> PREFERENCES = List.of(
            DebugPreference.SCP_079_ENERGY,
            DebugPreference.SCP_079_DECISIONS,
            DebugPreference.SPAWN_TIMERS,
            DebugPreference.PLAYER_STEALTH
    );

    private StealthDebugConfigCenterExtension() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!isDebugTools(screen)) return;

        Layout layout = layout(screen);
        hideServerBackedControls(event, layout);
        for (int index = 0; index < PREFERENCES.size(); index++) {
            event.addListener(new LocalDebugButton(layout.x(),
                    layout.rowY() + index * ROW_HEIGHT, layout.width(), 22,
                    PREFERENCES.get(index)));
        }
        event.addListener(new LocalDefaultsButton(layout.x(), layout.bottomY(),
                90, 20));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!isDebugTools(screen)) return;

        Layout layout = layout(screen);
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        DebugPreference stealth = DebugPreference.PLAYER_STEALTH;
        graphics.drawString(font, ScpFonts.roboto(stealth.description),
                layout.x() + 2,
                layout.rowY() + 3 * ROW_HEIGHT + 24, MUTED, false);
        graphics.drawString(font,
                ScpFonts.roboto("Client-side changes are saved immediately."),
                layout.x() + 106, layout.bottomY() + 6, MUTED, false);
    }

    private static void hideServerBackedControls(ScreenEvent.Init.Post event,
            Layout layout) {
        for (GuiEventListener listener : event.getListenersList()) {
            if (!(listener instanceof Button button)) continue;

            int relativeY = button.getY() - layout.rowY();
            boolean oldDebugRow = button.getX() == layout.x()
                    && button.getWidth() == layout.width()
                    && button.getHeight() == 22
                    && relativeY >= 0
                    && relativeY < 3 * ROW_HEIGHT
                    && relativeY % ROW_HEIGHT == 0;
            boolean oldDefaults = button.getX() == layout.x()
                    && button.getY() == layout.bottomY()
                    && button.getWidth() == 90;
            boolean oldSave = button.getY() == layout.bottomY()
                    && button.getWidth() == 108;
            if (oldDebugRow || oldDefaults || oldSave) {
                button.active = false;
                button.visible = false;
            }
        }
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
        return new Layout(panelX + 16, panelY + 44, panelWidth - 32,
                panelY + panelHeight - 30);
    }

    private record Layout(int x, int rowY, int width, int bottomY) {
    }

    private enum DebugPreference {
        SCP_079_ENERGY("SCP-079 Energy HUD",
                "Shows SCP-079 processing power in the upper-right corner."),
        SCP_079_DECISIONS("SCP-079 Decision Log HUD",
                "Shows recent SCP-079 decisions, costs, context, and manipulated devices."),
        SPAWN_TIMERS("SCP Spawn Timers HUD",
                "Shows each roamer's state, countdown, and latest scheduler result."),
        PLAYER_STEALTH("Player Stealth HUD",
                "Shows the local player's current stealth value.");

        private final String label;
        private final String description;

        DebugPreference(String label, String description) {
            this.label = label;
            this.description = description;
        }

        private boolean enabled() {
            return switch (this) {
                case SCP_079_ENERGY ->
                        StealthDebugClientPreferences.showScp079EnergyHud();
                case SCP_079_DECISIONS ->
                        StealthDebugClientPreferences.showScp079DecisionLogHud();
                case SPAWN_TIMERS ->
                        StealthDebugClientPreferences.showScpSpawnTimersHud();
                case PLAYER_STEALTH ->
                        StealthDebugClientPreferences.showStealthHud();
            };
        }

        private void toggle() {
            switch (this) {
                case SCP_079_ENERGY ->
                        StealthDebugClientPreferences.toggleScp079EnergyHud();
                case SCP_079_DECISIONS ->
                        StealthDebugClientPreferences.toggleScp079DecisionLogHud();
                case SPAWN_TIMERS ->
                        StealthDebugClientPreferences.toggleScpSpawnTimersHud();
                case PLAYER_STEALTH ->
                        StealthDebugClientPreferences.toggleStealthHud();
            }
        }
    }

    private static final class LocalDebugButton extends AbstractButton {
        private final DebugPreference preference;

        private LocalDebugButton(int x, int y, int width, int height,
                DebugPreference preference) {
            super(x, y, width, height, ScpFonts.roboto(preference.label));
            this.preference = preference;
        }

        @Override
        public void onPress() {
            preference.toggle();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            boolean hovered = isHoveredOrFocused();
            int left = getX();
            int top = getY();
            int right = left + getWidth();
            int bottom = top + getHeight();
            int border = hovered ? BORDER_HOVER : BORDER;

            graphics.fill(left, top, right, bottom,
                    hovered ? NAVY_HOVER : NAVY);
            graphics.fill(left, top, right, top + 1, border);
            graphics.fill(left, bottom - 1, right, bottom, border);
            graphics.fill(left, top, left + 1, bottom, border);
            graphics.fill(right - 1, top, right, bottom, border);
            graphics.fill(left + 1, top + 1, left + (hovered ? 5 : 4),
                    bottom - 1, ACCENT);

            Font font = Minecraft.getInstance().font;
            Component title = ScpFonts.roboto(preference.label);
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

            boolean enabled = preference.enabled();
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

    private static final class LocalDefaultsButton extends AbstractButton {
        private LocalDefaultsButton(int x, int y, int width, int height) {
            super(x, y, width, height, ScpFonts.roboto("Defaults"));
        }

        @Override
        public void onPress() {
            StealthDebugClientPreferences.setShowScp079EnergyHud(false);
            StealthDebugClientPreferences.setShowScp079DecisionLogHud(false);
            StealthDebugClientPreferences.setShowScpSpawnTimersHud(false);
            StealthDebugClientPreferences.setShowStealthHud(false);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX,
                int mouseY, float partialTick) {
            boolean hovered = isHoveredOrFocused();
            int left = getX();
            int top = getY();
            int right = left + getWidth();
            int bottom = top + getHeight();
            int border = hovered ? BORDER_HOVER : BORDER;
            graphics.fill(left, top, right, bottom,
                    hovered ? NAVY_HOVER : NAVY);
            graphics.fill(left, top, right, top + 1, border);
            graphics.fill(left, bottom - 1, right, bottom, border);
            graphics.fill(left, top, left + 1, bottom, border);
            graphics.fill(right - 1, top, right, bottom, border);
            graphics.fill(left + 1, top + 1, left + (hovered ? 4 : 2),
                    bottom - 1, ACCENT);

            Font font = Minecraft.getInstance().font;
            Component label = getMessage();
            graphics.drawString(font, label,
                    left + Math.max(5, (getWidth() - font.width(label)) / 2),
                    top + Math.max(1, (getHeight() - 8) / 2), WHITE, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
