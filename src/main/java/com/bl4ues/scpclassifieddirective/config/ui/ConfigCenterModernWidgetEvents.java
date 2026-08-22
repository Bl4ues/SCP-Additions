package com.bl4ues.scpclassifieddirective.config.ui;

import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.client.UnityConfigurationUiEvents;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Final widget pass for the Configuration Center.
 *
 * A number of legacy screens still need real Minecraft widgets as their input
 * and narration surface. Their vanilla chrome, however, was being rendered
 * underneath the new presentation and became especially visible while the
 * Configuration Center was sliding in or out. This class keeps the widgets
 * alive for interaction while making the presentation consistently use the
 * modern flat SCP: Classified Directive language.
 */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class ConfigCenterModernWidgetEvents {
    private static final int SURFACE = 0xD20B0E12;
    private static final int SURFACE_HOVER = 0xE3171C23;
    private static final int TRACK = 0x66414A56;
    private static final int TRACK_FILLED = 0xFFC99B18;
    private static final int TEXT = 0xFFF5F6F7;
    private static final int MUTED = 0xFF9FA6AD;
    private static final int ACCENT = 0xFFC99B18;
    private static final int ACCENT_BRIGHT = 0xFFE3C865;
    private static final Map<AbstractWidget, Integer> SUPPRESSED_X = new WeakHashMap<>();

    private ConfigCenterModernWidgetEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!isConfigurationScreen(event.getScreen())) return;
        for (GuiEventListener listener : event.getListenersList()) {
            prepare(listener);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPre(ScreenEvent.Render.Pre event) {
        Screen screen = event.getScreen();
        if (!isConfigurationScreen(screen)) return;
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof AbstractSliderButton slider) {
                suppressNative(slider);
            } else if (listener instanceof AbstractButton button
                    && shouldSuppressNativeButton(button)) {
                suppressNative(button);
            }
            prepare(listener);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPost(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!isConfigurationScreen(screen)) return;

        restoreSuppressed(screen);

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 1450.0F);
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof AbstractSliderButton slider
                    && slider.visible) {
                drawSlider(graphics, font, slider,
                        event.getMouseX(), event.getMouseY());
            } else if (listener instanceof EditBox edit && edit.visible) {
                drawEditChrome(graphics, edit);
            } else if (listener instanceof AbstractButton button
                    && isSelfRenderedButton(button)
                    && button.visible) {
                drawContextRow(graphics, font, button,
                        event.getMouseX(), event.getMouseY());
            } else if (listener instanceof AbstractButton button
                    && !(button instanceof Button)
                    && button.visible) {
                drawAbstractButton(graphics, font, button,
                        event.getMouseX(), event.getMouseY());
            }
        }
        polishLegacySummaryRows(graphics, screen,
                event.getMouseX(), event.getMouseY());
        graphics.pose().popPose();
    }

    private static void prepare(GuiEventListener listener) {
        if (listener instanceof EditBox editBox) {
            editBox.setBordered(true);
            editBox.setFormatter((value, cursor) ->
                    ScpFonts.roboto(value).getVisualOrderText());
            return;
        }
        if (listener instanceof AbstractButton button
                && !isSelfRenderedButton(button)) {
            // Keep hitbox, focus and narration, but never let vanilla/Forge
            // chrome leak through the modern renderer during transitions.
            button.setAlpha(0.0F);
        }
    }

    private static void drawSlider(GuiGraphics graphics, Font font,
            AbstractSliderButton slider, int mouseX, int mouseY) {
        float alpha = ConfigCenterVisuals.contentAlpha();
        if (alpha <= 0.01F) return;

        int left = slider.getX();
        int top = slider.getY();
        int right = left + slider.getWidth();
        int bottom = top + slider.getHeight();
        boolean crosshairLegacy = "CrosshairSlider".equals(
                slider.getClass().getSimpleName());
        int coverTop = crosshairLegacy ? top - 11 : top;
        boolean hovered = slider.active
                && (slider.isMouseOver(mouseX, mouseY) || slider.isFocused());

        graphics.fill(left, coverTop, right, bottom,
                ConfigCenterVisuals.fadeColor(
                        hovered ? SURFACE_HOVER : SURFACE));
        graphics.fill(left, coverTop, left + (hovered ? 4 : 3), bottom,
                ConfigCenterVisuals.fadeColor(
                        slider.active ? ACCENT : 0xFF4D535C));

        String plain = slider.getMessage() == null
                ? "" : slider.getMessage().getString().trim();
        String label = plain;
        String valueLabel = "";
        int split = plain.lastIndexOf(':');
        if (split > 0 && split < plain.length() - 1) {
            String candidate = plain.substring(split + 1).trim();
            if (candidate.length() <= 12) {
                label = plain.substring(0, split).trim();
                valueLabel = candidate;
            }
        }

        int textY = top + Math.max(2,
                (slider.getHeight() - font.lineHeight) / 2 - 1);
        int maxLabelWidth = Math.max(16, slider.getWidth() - 74);
        graphics.drawString(font,
                ScpFonts.roboto(fit(font, label, maxLabelWidth)),
                left + 12, textY,
                ConfigCenterVisuals.fadeColor(
                        slider.active ? TEXT : MUTED), false);
        if (!valueLabel.isBlank()) {
            Component value = ScpFonts.roboto(valueLabel);
            graphics.drawString(font, value,
                    right - 12 - font.width(value), textY,
                    ConfigCenterVisuals.fadeColor(
                            hovered ? ACCENT_BRIGHT : MUTED), false);
        }

        int trackLeft = left + 12;
        int trackRight = right - 12;
        int trackY = bottom - 4;
        graphics.fill(trackLeft, trackY, trackRight, trackY + 2,
                ConfigCenterVisuals.fadeColor(TRACK));
        double value = Mth.clamp(sliderValue(slider), 0.0D, 1.0D);
        int knobX = trackLeft + (int) Math.round(
                (trackRight - trackLeft) * value);
        graphics.fill(trackLeft, trackY,
                Math.max(trackLeft, knobX), trackY + 2,
                ConfigCenterVisuals.fadeColor(TRACK_FILLED));
        graphics.fill(knobX - 2, bottom - 7, knobX + 2, bottom - 1,
                ConfigCenterVisuals.fadeColor(
                        hovered ? ACCENT_BRIGHT : TEXT));
    }

    private static void drawEditChrome(GuiGraphics graphics, EditBox edit) {
        float alpha = ConfigCenterVisuals.contentAlpha();
        if (alpha <= 0.01F) return;
        int left = edit.getX();
        int top = edit.getY();
        int right = left + edit.getWidth();
        int bottom = top + edit.getHeight();
        int line = edit.isFocused() ? ACCENT : 0x88414A56;
        graphics.fill(left, bottom - 1, right, bottom,
                ConfigCenterVisuals.fadeColor(line));
        graphics.fill(left, top + 3, left + (edit.isFocused() ? 3 : 2),
                bottom - 1,
                ConfigCenterVisuals.fadeColor(edit.isFocused()
                        ? ACCENT : 0x88414A56));
    }

    private static void drawAbstractButton(GuiGraphics graphics, Font font,
            AbstractButton button, int mouseX, int mouseY) {
        float alpha = ConfigCenterVisuals.contentAlpha();
        if (alpha <= 0.01F) return;
        int left = button.getX() + ConfigCenterVisuals.contentOffsetX();
        int top = button.getY();
        int right = left + button.getWidth();
        int bottom = top + button.getHeight();
        boolean hovered = button.active
                && (button.isMouseOver(mouseX, mouseY) || button.isFocused());
        graphics.fill(left, top, right, bottom,
                ConfigCenterVisuals.fadeColor(
                        hovered ? SURFACE_HOVER : SURFACE));
        graphics.fill(left, top, left + (hovered ? 5 : 3), bottom,
                ConfigCenterVisuals.fadeColor(
                        button.active ? ACCENT : 0xFF4D535C));

        Component message = button.getMessage() == null
                ? Component.empty() : ScpFonts.roboto(button.getMessage());
        int maxWidth = Math.max(12, button.getWidth() - 28);
        String fitted = fit(font, message.getString(), maxWidth);
        int textY = top + Math.max(1,
                (button.getHeight() - font.lineHeight) / 2);
        graphics.drawString(font, ScpFonts.roboto(fitted), left + 14, textY,
                ConfigCenterVisuals.fadeColor(!button.active ? MUTED
                        : hovered ? ACCENT_BRIGHT : TEXT), false);
    }

    private static void drawContextRow(GuiGraphics graphics, Font font,
            AbstractButton button, int mouseX, int mouseY) {
        Object row = readField(button, "row");
        Object rawRule = readField(row, "rule");
        if (!(rawRule instanceof JsonObject rule)) return;

        boolean enabled = jsonBoolean(rule, "enabled", true);
        boolean configured = Boolean.TRUE.equals(readField(row, "configured"));
        Object view = readField(row, "view");
        Object sourceObject = invokeNoArgs(view, "source");
        String source = sourceObject == null ? (configured ? "CUSTOM" : "INTEGRATED")
                : sourceObject.toString().toUpperCase(java.util.Locale.ROOT);
        int sourceColor = switch (source) {
            case "OVERRIDE" -> 0xFFFFC56D;
            case "CUSTOM" -> 0xFF79D58B;
            default -> ACCENT;
        };

        int left = button.getX();
        int top = button.getY();
        int right = left + button.getWidth();
        int bottom = top + button.getHeight();
        boolean hovered = enabled
                && (button.isMouseOver(mouseX, mouseY) || button.isFocused());
        graphics.fill(left, top, right, bottom,
                hovered ? 0xED171C23 : 0xE80B0E12);
        graphics.fill(left, top, left + (hovered ? 5 : 3), bottom,
                enabled ? sourceColor : 0xFF4D535C);

        String type = jsonString(rule, "type", "target")
                .toUpperCase(java.util.Locale.ROOT);
        int tagWidth = Math.max(42, Math.min(62,
                font.width(ScpFonts.titillium(type)) + 14));
        int tagX = left + 10;
        graphics.drawString(font, ScpFonts.titillium(type), tagX, top + 6,
                enabled ? sourceColor : MUTED, false);

        String target = button.getMessage() == null
                ? "" : button.getMessage().getString();
        JsonObject text = rule.has("text") && rule.get("text").isJsonObject()
                ? rule.getAsJsonObject("text") : null;
        String action = jsonString(text, "action", "Use");
        int iconX = tagX + tagWidth;
        int iconY = top + 8;
        boolean hasTargetIcon = renderContextTargetIcon(graphics, rule, iconX, iconY);
        int textX = iconX + (hasTargetIcon ? 20 : 0);
        int available = Math.max(20, right - textX - 12);
        String main = target + "  ·  " + action
                + (enabled ? "" : "  [DISABLED]");
        graphics.drawString(font, ScpFonts.roboto(
                        fit(font, main, available)),
                textX, top + 5, enabled ? TEXT : MUTED, false);

        String id = jsonString(rule, "id", "");
        String meta = source + (configured ? "  ·  explicit" : "  ·  runtime default")
                + (id.isBlank() ? "" : "  ·  " + id);
        graphics.drawString(font, ScpFonts.titillium(
                        fit(font, meta, available)),
                textX, top + 18, enabled ? MUTED : 0xFF707680, false);
    }

    private static boolean renderContextTargetIcon(GuiGraphics graphics,
            JsonObject rule, int x, int y) {
        String idText = jsonString(rule, "id", "");
        if (idText.isBlank()) return false;
        try {
            ResourceLocation id = new ResourceLocation(idText);
            String type = jsonString(rule, "type", "");
            if ("block".equalsIgnoreCase(type)) {
                var block = ForgeRegistries.BLOCKS.getValue(id);
                if (block == null || block.asItem() == Items.AIR) return false;
                graphics.renderItem(new ItemStack(block.asItem()), x, y);
                return true;
            }
            if ("entity".equalsIgnoreCase(type)) {
                if (UnityConfigurationUiEvents.renderEntityPreview(graphics, id, x, y)) {
                    return true;
                }
                graphics.renderItem(new ItemStack(Items.SPAWNER), x, y);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static void polishLegacySummaryRows(GuiGraphics graphics,
            Screen screen, int mouseX, int mouseY) {
        String name = screen.getClass().getSimpleName();
        int size = listSize(readField(screen, "filtered"));
        int scroll = (int) numberValue(readField(screen, "scroll"), 0.0D);
        int left;
        int rowRight;
        int listY;
        int visible;

        switch (name) {
            case "DrinkListScreen" -> {
                int w = Math.min(700, screen.width - 16);
                left = ConfigCenterVisuals.contentLeft(screen.width, w) + 12;
                int top = Math.max(8, (screen.height
                        - Math.min(410, screen.height - 16)) / 2) + 38;
                listY = top + 30;
                visible = Math.max(5, Math.min(11, (screen.height - 132) / 24));
                rowRight = left + w - 238;
            }
            case "ItemRulesScreen" -> {
                int w = Math.min(650, screen.width - 16);
                left = ConfigCenterVisuals.contentLeft(screen.width, w) + 12;
                int top = Math.max(8, (screen.height
                        - Math.min(400, screen.height - 16)) / 2) + 38;
                listY = top + 30;
                visible = Math.max(4, Math.min(10, (screen.height - 128) / 24));
                rowRight = left + w - 164;
            }
            case "RecipeListScreen" -> {
                int w = Math.min(760, screen.width - 12);
                left = ConfigCenterVisuals.contentLeft(screen.width, w) + 12;
                int top = Math.max(6, (screen.height
                        - Math.min(440, screen.height - 12)) / 2) + 38;
                listY = top + 30;
                visible = Math.max(5, Math.min(12, (screen.height - 130) / 24));
                rowRight = left + w - 242;
            }
            case "IdListScreen" -> {
                int w = Math.min(650, screen.width - 18);
                left = ConfigCenterVisuals.contentLeft(screen.width, w) + 12;
                int top = Math.max(8, (screen.height
                        - Math.min(390, screen.height - 16)) / 2) + 38;
                listY = top + 56;
                visible = Math.max(4, Math.min(9, (screen.height - 146) / 24));
                rowRight = left + w - 178;
            }
            case "DrinkEffectsScreen" -> {
                Object drinkRaw = readField(screen, "drink");
                if (drinkRaw instanceof JsonObject drink
                        && drink.has("effects") && drink.get("effects").isJsonArray()) {
                    size = drink.getAsJsonArray("effects").size();
                }
                int w = Math.min(680, screen.width - 16);
                left = ConfigCenterVisuals.contentLeft(screen.width, w) + 12;
                int top = Math.max(8, (screen.height
                        - Math.min(410, screen.height - 16)) / 2) + 42;
                listY = top + 30;
                visible = Math.max(4, Math.min(10, (screen.height - 130) / 24));
                rowRight = left + w - 82;
            }
            default -> {
                return;
            }
        }

        int count = Math.min(visible, Math.max(0, size - scroll));
        for (int row = 0; row < count; row++) {
            int top = listY + row * 24;
            int bottom = top + 20;
            boolean hovered = mouseX >= left && mouseX < rowRight
                    && mouseY >= top && mouseY < bottom;
            int seam = hovered ? 0xFF131820 : 0xFF0B0E12;
            graphics.fill(left, top, rowRight, top + 1, seam);
            graphics.fill(left, bottom - 1, rowRight, bottom, seam);
            graphics.fill(left, top, left + (hovered ? 4 : 3), bottom,
                    hovered ? ACCENT_BRIGHT : ACCENT);
        }
    }

    private static boolean shouldSuppressNativeButton(AbstractButton button) {
        return isSelfRenderedButton(button) || !(button instanceof Button);
    }

    private static void suppressNative(AbstractWidget button) {
        if (button.getX() <= -9000) return;
        SUPPRESSED_X.put(button, button.getX());
        button.setX(-10000);
    }

    private static void restoreSuppressed(Screen screen) {
        for (GuiEventListener listener : screen.children()) {
            if (!(listener instanceof AbstractWidget button)) continue;
            Integer x = SUPPRESSED_X.get(button);
            if (x != null && button.getX() <= -9000) button.setX(x);
        }
    }

    private static Object invokeNoArgs(Object target, String name) {
        if (target == null) return null;
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                var method = type.getDeclaredMethod(name);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    private static int listSize(Object value) {
        return value instanceof java.util.List<?> list ? list.size() : 0;
    }

    private static double numberValue(Object value, double fallback) {
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private static String jsonString(JsonObject root, String key, String fallback) {
        if (root == null || !root.has(key) || root.get(key).isJsonNull()) return fallback;
        try {
            return root.get(key).getAsString();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean jsonBoolean(JsonObject root, String key, boolean fallback) {
        if (root == null || !root.has(key) || root.get(key).isJsonNull()) return fallback;
        try {
            return root.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double sliderValue(AbstractSliderButton slider) {
        Object value = readField(slider, "value");
        return value instanceof Number number ? number.doubleValue() : 0.0D;
    }

    private static Object readField(Object target, String name) {
        Class<?> type = target == null ? null : target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    private static String fit(Font font, String value, int maxWidth) {
        String safe = value == null ? "" : value;
        if (font.width(ScpFonts.roboto(safe)) <= maxWidth) return safe;
        String suffix = "...";
        int suffixWidth = font.width(ScpFonts.roboto(suffix));
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < safe.length(); i++) {
            String candidate = out.toString() + safe.charAt(i);
            if (font.width(ScpFonts.roboto(candidate)) + suffixWidth > maxWidth) {
                break;
            }
            out.append(safe.charAt(i));
        }
        return out + suffix;
    }

    private static boolean isSelfRenderedButton(AbstractButton button) {
        return button != null && "ContextRowButton".equals(
                button.getClass().getSimpleName());
    }

    private static boolean isConfigurationScreen(Screen screen) {
        if (screen == null) return false;
        String name = screen.getClass().getName();
        if (name.startsWith("com.bl4ues.scpclassifieddirective.config.ui.ConfigCenterClient$")
                || name.startsWith(
                "com.bl4ues.scpclassifieddirective.config.ui.Scp079ModulesScreenExtension$")
                || name.startsWith(
                "com.bl4ues.scpclassifieddirective.client.RoombaConfigCenterEnhancements$")) {
            return true;
        }
        String simple = screen.getClass().getSimpleName();
        return "ItemConfigScreen".equals(simple)
                || "ContextConfigScreen".equals(simple)
                || "UnityColorPickerScreen".equals(simple)
                || "CodexImageDropScreen".equals(simple)
                || "CodexTextEditorScreen".equals(simple);
    }
}
