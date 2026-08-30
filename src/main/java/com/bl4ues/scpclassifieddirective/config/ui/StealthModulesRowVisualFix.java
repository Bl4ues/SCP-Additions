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

    private static final int PANEL = 0xF00B0E12;
    private static final int NAVY = 0xFF0D1116;
    private static final int NAVY_HOVER = 0xFF1A2028;
    private static final int ACCENT = 0xFFC99B18;
    private static final int WHITE = 0xFFF7F8FC;
    private static final int PALE_GOLD = 0xFFE3C865;
    private static final int SERVER_BADGE = 0xFF071524;
    private static final int ON = 0xFF79D58B;
    private static final int OFF = 0xFFFF8B8B;

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
        boolean hovered = editor.isHoveredOrFocused() || state.isHoveredOrFocused();
        int left = editor.getX();
        int top = editor.getY();
        int right = state.getX() + state.getWidth();
        int bottom = top + Math.max(editor.getHeight(), state.getHeight());

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;

        // Replace the visually split editor/state buttons with one ordinary
        // module row. The original widgets remain underneath and keep their
        // separate click behavior.
        graphics.fill(left, top, right, bottom, hovered ? NAVY_HOVER : NAVY);
        graphics.fill(left, top, left + (hovered ? 5 : 4), bottom, ACCENT);

        Component title = ScpFonts.roboto("Advanced Crouch & Stealth");
        int textY = top + Math.max(1, (bottom - top - 8) / 2);
        graphics.drawString(font, title, left + 16, textY, WHITE, false);

        Component scope = ScpFonts.roboto("SERVER");
        int scopeWidth = font.width(scope) + 12;
        int stateReserve = 54;
        int scopeX = right - stateReserve - scopeWidth - 12;
        graphics.fill(scopeX, top + 2, scopeX + scopeWidth, bottom - 2,
                SERVER_BADGE);
        graphics.drawString(font, scope,
                scopeX + (scopeWidth - font.width(scope)) / 2,
                textY, PALE_GOLD, false);

        Component stateText = ScpFonts.roboto(enabled ? "ON" : "OFF");
        graphics.drawString(font, stateText,
                right - 14 - font.width(stateText), textY,
                enabled ? ON : OFF, false);

        // The base screen used serverOwned as a reason to print a second,
        // oversized SERVER-SIDE marker on the description line. Erase only that
        // redundant right-hand area and leave the normal description intact.
        graphics.fill(right - 150, bottom, right, top + 34, PANEL);
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
