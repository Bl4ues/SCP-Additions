package com.bl4ues.scpclassifieddirective.config.ui;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Keeps the Advanced Crouch & Stealth editor row visually identical to the
 * ordinary module toggles while retaining its split hit areas: the body opens
 * mob perception settings and the right side toggles the server-owned module.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class StealthModulesRowVisualFix {
    private static final String EXTENDED_TOGGLE_SCREEN =
            "com.bl4ues.scpclassifieddirective.config.ui.Scp079ModulesScreenExtension$ExtendedToggleScreen";
    private static final String MODULES_TITLE = "General & Modules";
    private static final String DESCRIPTION =
            "Smooth crouch/crawl movement and global visual-perception framework.";

    private static final int PANEL = 0xFF0B0E12;
    private static final int NAVY = 0xFF0D1116;
    private static final int NAVY_HOVER = 0xFF1A2028;
    private static final int ACCENT = 0xFFC99B18;
    private static final int WHITE = 0xFFF7F8FC;
    private static final int MUTED = 0xFF9CA3AF;
    private static final int SERVER_SCOPE = 0xFFFFC56D;
    private static final int BADGE_BACKGROUND = 0xE6081022;
    private static final int ON = 0xFF79D58B;
    private static final int OFF = 0xFFFF8B8B;
    private static final float BADGE_SCALE = 0.74F;

    private static Field labelsField;

    private StealthModulesRowVisualFix() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!isModulesScreen(screen)) return;

        Button editor = null;
        Button state = null;
        String stateLabel = "";
        try {
            Map<?, ?> labels = labels(screen);
            for (Map.Entry<?, ?> entry : labels.entrySet()) {
                if (!(entry.getKey() instanceof Button button)
                        || !(entry.getValue() instanceof Component label)) {
                    continue;
                }
                String text = label.getString();
                if ("Advanced Crouch & Stealth".equals(text)) {
                    editor = button;
                } else if (text.startsWith("State: ")) {
                    state = button;
                    stateLabel = text;
                }
            }
        } catch (ReflectiveOperationException exception) {
            ScpClassifiedDirectiveMod.LOGGER.debug(
                    "Could not normalize the Advanced Crouch module row", exception);
            return;
        }
        if (editor == null || state == null) return;

        boolean enabled = stateLabel.endsWith("ON");
        boolean editorHovered = editor.isHoveredOrFocused();
        boolean hovered = editorHovered || state.isHoveredOrFocused();
        int slide = ConfigCenterVisuals.contentOffsetX();
        int left = editor.getX() + slide;
        int top = editor.getY();
        int right = state.getX() + state.getWidth() + slide;
        int bottom = top + Math.max(editor.getHeight(), state.getHeight());

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;

        // The native row is intentionally split into two hit areas. Draw one
        // ordinary-looking module row above both widgets so the split remains
        // functional without being visible to the player.
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 1900.0F);
        graphics.fill(left, top, right, bottom, hovered ? NAVY_HOVER : NAVY);
        graphics.fill(left, top, left + (hovered ? 6 : 4), bottom, ACCENT);

        Component title = ScpFonts.roboto("Advanced Crouch & Stealth");
        int textY = top + Math.max(1, (bottom - top - font.lineHeight) / 2);
        graphics.drawString(font, title, left + 16, textY, WHITE, false);

        // Match ClientPreferenceModulesUi's SERVER badge exactly.
        Component scope = ScpFonts.roboto("SERVER");
        int scaledWidth = Math.round(font.width(scope) * BADGE_SCALE);
        int scaledHeight = Math.max(6,
                Math.round(font.lineHeight * BADGE_SCALE));
        int badgeRight = right - 58;
        int badgeX = badgeRight - scaledWidth;
        int badgeY = top + Math.max(1, (bottom - top - scaledHeight) / 2);

        // A quiet affordance that only brightens over the configurable body.
        Component configure = ScpFonts.roboto("CONFIG >");
        int configureX = badgeX - 12 - font.width(configure);
        int titleEnd = left + 16 + font.width(title);
        if (configureX >= titleEnd + 12) {
            graphics.drawString(font, configure, configureX, textY,
                    editorHovered ? SERVER_SCOPE : MUTED, false);
        }

        graphics.fill(badgeX - 4, top + 2, badgeRight + 3, bottom - 2,
                BADGE_BACKGROUND);
        graphics.pose().pushPose();
        graphics.pose().translate(badgeX, badgeY, 0.0F);
        graphics.pose().scale(BADGE_SCALE, BADGE_SCALE, 1.0F);
        graphics.drawString(font, scope, 0, 0, SERVER_SCOPE, false);
        graphics.pose().popPose();

        Component stateText = ScpFonts.roboto(enabled ? "ON" : "OFF");
        graphics.drawString(font, stateText,
                right - 14 - font.width(stateText), textY,
                enabled ? ON : OFF, false);

        // Replace the base screen's shortened description and obsolete
        // SERVER-SIDE annotation with the complete ordinary description line.
        graphics.fill(left, bottom, right, top + 34, PANEL);
        graphics.drawString(font, ScpFonts.roboto(DESCRIPTION),
                left + 2, top + 24, MUTED, false);
        graphics.pose().popPose();
    }

    private static boolean isModulesScreen(Screen screen) {
        return screen != null
                && EXTENDED_TOGGLE_SCREEN.equals(screen.getClass().getName())
                && MODULES_TITLE.equals(screen.getTitle().getString());
    }

    @SuppressWarnings("unchecked")
    private static Map<?, ?> labels(Screen screen) throws ReflectiveOperationException {
        if (labelsField == null) {
            labelsField = screen.getClass().getDeclaredField("labels");
            labelsField.setAccessible(true);
        }
        Object value = labelsField.get(screen);
        if (value instanceof Map<?, ?> map) return map;
        throw new IllegalStateException("ExtendedToggleScreen labels field is not a map");
    }
}
