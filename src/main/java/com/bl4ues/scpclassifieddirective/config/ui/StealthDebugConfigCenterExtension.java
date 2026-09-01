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
    private static final int CLIENT_SCOPE = 0xFF79D58B;
    private static final int BADGE_BACKGROUND = 0xE6081022;
    private static final int ON = 0xFF79D58B;
    private static final int OFF = 0xFFFF8B8B;
    private static final float BADGE_SCALE = 0.74F;

    private static final List<DebugPreference> PREFERENCES = List.of(
            DebugPreference.SCP_079_ENERGY,
            DebugPreference.SCP_079_DECISIONS,
            DebugPreference.SPAWN_TIMERS,
            DebugPreference.PLAYER_STEALTH,
            DebugPreference.SCP_426_EXPOSURE
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

        // The native screen supplies descriptions for the original three rows.
        // Draw descriptions only for local rows added beyond that base list.
        for (int index = 3; index < PREFERENCES.size(); index++) {
            DebugPreference preference = PREFERENCES.get(index);
            graphics.drawString(font, ScpFonts.roboto(preference.description),
                    layout.x() + 2,
                    layout.rowY() + index * ROW_HEIGHT + 24, MUTED, false);
        }
        graphics.drawString(font,
                ScpFonts.roboto("Client-side changes are saved immediately."),
                layout.x() + 106, layout.bottomY() + 6, MUTED, false);

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 1800.0F);
        for (int index = 0; index < PREFERENCES.size(); index++) {
            renderScopeAndState(graphics, font, layout,
                    layout.rowY() + index * ROW_HEIGHT, PREFERENCES.get(index));
        }
        graphics.pose().popPose();
    }

    private static void renderScopeAndState(GuiGraphics graphics, Font font,
            Layout layout, int top, DebugPreference preference) {
        int right = layout.x() + layout.width();
        int textY = top + Math.max(1, (22 - font.lineHeight) / 2);

        Component badge = ScpFonts.roboto("CLIENT");
        int scaledWidth = Math.round(font.width(badge) * BADGE_SCALE);
        int scaledHeight = Math.max(6,
                Math.round(font.lineHeight * BADGE_SCALE));
        int badgeRight = right - 58;
        int badgeX = badgeRight - scaledWidth;
        int badgeY = top + Math.max(1, (22 - scaledHeight) / 2);
        graphics.fill(badgeX - 4, top + 2, badgeRight + 3, top + 20,
                BADGE_BACKGROUND);
        graphics.pose().pushPose();
        graphics.pose().translate(badgeX, badgeY, 0.0F);
        graphics.pose().scale(BADGE_SCALE, BADGE_SCALE, 1.0F);
        graphics.drawString(font, badge, 0, 0, CLIENT_SCOPE, false);
        graphics.pose().popPose();

        boolean enabled = preference.enabled();
        Component state = ScpFonts.roboto(enabled ? "ON" : "OFF");
        graphics.drawString(font, state,
                right - 14 - font.width(state), textY,
                enabled ? ON : OFF, false);
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
        int panelHeight = Math.min(274, screen.height - 16);
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
                "Shows the local player's current stealth value."),
        SCP_426_EXPOSURE("SCP-426 Exposure HUD",
                "Shows the local player's SCP-426 exposure, percentage, and tier.");

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
                case SCP_426_EXPOSURE ->
                        StealthDebugClientPreferences.showScp426ExposureHud();
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
                case SCP_426_EXPOSURE ->
                        StealthDebugClientPreferences.toggleScp426ExposureHud();
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
            int textY = top + Math.max(1, (getHeight() - font.lineHeight) / 2);
            graphics.drawString(font, title, left + 16, textY, WHITE, false);
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
            StealthDebugClientPreferences.setShowScp426ExposureHud(false);
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
                    top + Math.max(1, (getHeight() - font.lineHeight) / 2),
                    WHITE, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
