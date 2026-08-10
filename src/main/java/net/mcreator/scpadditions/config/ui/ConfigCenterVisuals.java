package net.mcreator.scpadditions.config.ui;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.client.CustomMainMenuScreen;
import net.mcreator.scpadditions.init.MainMenuSounds;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

import java.util.Map;
import java.util.WeakHashMap;

/** Shared presentation layer for every Configuration Center screen. */
public final class ConfigCenterVisuals {
    public static final int TEXT = 0xFFF5F6F7;
    public static final int MUTED = 0xFF9FA6AD;
    public static final int ACCENT = 0xFFC99B18;
    public static final int ACCENT_BRIGHT = 0xFFE3C865;
    public static final int GREEN = 0xFF79D58B;
    public static final int RED = 0xFFFF8B8B;
    public static final int PANEL = 0xB80B0E12;
    public static final int PANEL_STRONG = 0xD20B0E12;
    public static final int BUTTON = 0xA60B0E12;
    public static final int BUTTON_HOVER = 0xD6161B22;
    public static final int BORDER = 0x663A424D;

    private static final ResourceLocation FALLBACK_BACKGROUND = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/screens/menu/loading_screen.png");
    private static final ResourceLocation SPINNER_OUTER = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/screens/menu/loading_1.png");
    private static final ResourceLocation SPINNER_INNER = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/screens/menu/loading_2.png");
    private static final ResourceLocation CONFIG_LOGO = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/screens/logo.png");
    private static final int REFERENCE_WIDTH = 1920;
    private static final int REFERENCE_HEIGHT = 1080;
    private static final int SPINNER_TEXTURE_SIZE = 512;

    private static final Map<Button, Float> BUTTON_HOVER_PROGRESS =
            new WeakHashMap<>();
    private static final Map<Button, Long> BUTTON_UPDATED_AT =
            new WeakHashMap<>();
    private static final Map<Button, Boolean> BUTTON_HOVERED =
            new WeakHashMap<>();

    private static ResourceLocation capturedBackground = FALLBACK_BACKGROUND;
    private static boolean titleBackdrop;
    private static float outerAngle;
    private static float innerAngle;
    private static float hoverBoost;
    private static long lastFrameAt = Util.getMillis();
    private static long enteredAt = Util.getMillis();

    private ConfigCenterVisuals() {
    }

    public static void prepare(Screen parent) {
        long now = Util.getMillis();
        enteredAt = now;
        lastFrameAt = now;
        hoverBoost = 0.0F;
        titleBackdrop = parent instanceof CustomMainMenuScreen;
        if (parent instanceof CustomMainMenuScreen menu) {
            capturedBackground = menu.configurationBackdropTexture();
            outerAngle = menu.configurationSpinnerOuterAngle();
            innerAngle = menu.configurationSpinnerInnerAngle();
        } else if (Minecraft.getInstance().level == null) {
            capturedBackground = FALLBACK_BACKGROUND;
        }
    }

    public static void renderBackdrop(Screen screen, GuiGraphics graphics,
            int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        int width = screen.width;
        int height = screen.height;
        long now = Util.getMillis();
        float delta = Math.min(0.10F,
                Math.max(0.0F, (now - lastFrameAt) / 1000.0F));
        lastFrameAt = now;

        boolean hovered = false;
        for (var listener : screen.children()) {
            if (listener instanceof AbstractWidget widget
                    && widget.visible && widget.active
                    && widget.isMouseOver(mouseX, mouseY)) {
                hovered = true;
                break;
            }
        }
        hoverBoost = approach(hoverBoost, hovered ? 1.0F : 0.0F,
                delta * 5.8F);
        float intro = Mth.clamp((now - enteredAt) / 520.0F, 0.0F, 1.0F);
        float speed = 6.0F + 32.0F * hoverBoost
                + 88.0F * (1.0F - smootherStep(intro));
        outerAngle = wrapAngle(outerAngle + speed * delta);
        innerAngle = wrapAngle(innerAngle - speed * 0.86F * delta);

        if (titleBackdrop || minecraft.level == null) {
            drawCoverTexture(graphics, capturedBackground, width, height);
        }

        int veilAlpha = titleBackdrop ? 0xB8
                : minecraft.level == null ? 0xE0 : 0xC8;
        graphics.fill(0, 0, width, height, veilAlpha << 24);

        // A second, very light veil behind the editor side improves legibility
        // while leaving the title artwork visibly alive underneath.
        int contentStart = Math.max(0, Math.round(width * 0.30F));
        graphics.fill(contentStart, 0, width, height, 0x32070A0E);

        int size = Mth.clamp(Math.round(height * 1.22F), 430, 980);
        int centerX = Math.round(width * 0.105F);
        int centerY = Math.round(height * 0.54F);
        float outerAlpha = 0.20F + hoverBoost * 0.055F;
        float innerAlpha = 0.15F + hoverBoost * 0.045F;
        drawRotatedTexture(graphics, SPINNER_OUTER, centerX, centerY,
                size, outerAngle, outerAlpha);
        drawRotatedTexture(graphics, SPINNER_INNER, centerX, centerY,
                size, innerAngle, innerAlpha);
    }

    public static int contentLeft(int width, int panelWidth) {
        int centered = Math.max(8, (width - panelWidth) / 2);
        if (width < 780) return centered;
        int preferred = Math.max(250, Math.round(width * 0.385F));
        return Mth.clamp(preferred, 12, Math.max(12, width - panelWidth - 22));
    }

    public static int navigationWidth(int width) {
        return Mth.clamp(Math.round(width * 0.34F), 390, 560);
    }

    public static void drawPanel(GuiGraphics graphics, Font font,
            int x, int y, int width, int height, String title) {
        graphics.fill(x, y, x + width, y + height, PANEL);
        graphics.fill(x, y, x + width, y + 2, ACCENT);
        graphics.fill(x, y, x + 3, y + height, 0xB8C99B18);
        graphics.fill(x + 3, y + height - 1, x + width, y + height, BORDER);
        drawScaledText(graphics, font, ScpFonts.montserrat(title),
                x + 16, y + 12, 1.10F, TEXT);
        graphics.fill(x + 16, y + 31, x + width - 16, y + 32, BORDER);
    }

    public static void drawButton(GuiGraphics graphics, Font font,
            Button button, Component rawLabel, int mouseX, int mouseY) {
        if (!button.visible) return;
        long now = Util.getMillis();
        boolean hovered = button.active
                && (button.isMouseOver(mouseX, mouseY) || button.isFocused());
        long previousAt = BUTTON_UPDATED_AT.getOrDefault(button, now);
        float delta = Math.min(0.10F,
                Math.max(0.0F, (now - previousAt) / 1000.0F));
        BUTTON_UPDATED_AT.put(button, now);
        float progress = BUTTON_HOVER_PROGRESS.getOrDefault(button, 0.0F);
        progress = approach(progress, hovered ? 1.0F : 0.0F, delta * 8.0F);
        BUTTON_HOVER_PROGRESS.put(button, progress);
        float eased = smootherStep(progress);

        boolean wasHovered = BUTTON_HOVERED.getOrDefault(button, false);
        if (hovered && !wasHovered) playHover();
        BUTTON_HOVERED.put(button, hovered);

        int left = button.getX();
        int top = button.getY();
        int right = left + button.getWidth();
        int bottom = top + button.getHeight();
        int background = !button.active ? 0xA014171C
                : blend(BUTTON, BUTTON_HOVER, eased);
        graphics.fill(left, top, right, bottom, background);
        graphics.fill(left, bottom - 1, right, bottom, BORDER);
        int stripe = Math.max(4, Math.round(4.0F + eased * 3.0F));
        graphics.fill(left, top, left + stripe, bottom,
                button.active ? ACCENT : 0xFF4D535C);

        String plain = rawLabel == null ? "" : rawLabel.getString();
        int stateLength = plain.endsWith(": ON") ? 2
                : plain.endsWith(": OFF") ? 3 : 0;
        int textY = top + Math.max(1,
                (button.getHeight() - font.lineHeight) / 2);
        int textColor = !button.active ? MUTED
                : eased > 0.45F ? ACCENT_BRIGHT : TEXT;
        int inset = 16 + Math.round(eased * 5.0F);

        if (stateLength > 0 && button.getWidth() >= 155) {
            String prefix = plain.substring(0, plain.length() - stateLength);
            String state = plain.substring(plain.length() - stateLength);
            int stateWidth = font.width(ScpFonts.roboto(state));
            int maxPrefix = Math.max(20,
                    button.getWidth() - inset - stateWidth - 28);
            Component prefixLabel = ScpFonts.roboto(
                    fitString(font, prefix, maxPrefix));
            graphics.drawString(font, prefixLabel, left + inset, textY,
                    textColor, false);
            graphics.drawString(font, ScpFonts.roboto(state),
                    right - 14 - stateWidth, textY,
                    !button.active ? MUTED
                            : "ON".equals(state) ? GREEN : RED,
                    false);
            return;
        }

        int maxWidth = Math.max(12, button.getWidth() - inset - 12);
        Component label = ScpFonts.roboto(fitString(font, plain, maxWidth));
        if (button.getWidth() >= 140) {
            graphics.drawString(font, label, left + inset, textY,
                    textColor, false);
        } else {
            graphics.drawCenteredString(font, label,
                    left + button.getWidth() / 2, textY, textColor);
        }
    }

    public static ResourceLocation logoTexture() {
        return CONFIG_LOGO;
    }

    private static String fitString(Font font, String value, int maxWidth) {
        Component component = ScpFonts.roboto(value);
        if (font.width(component) <= maxWidth) return value;
        String suffix = "...";
        int suffixWidth = font.width(ScpFonts.roboto(suffix));
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            String candidate = out.toString() + value.charAt(i);
            if (font.width(ScpFonts.roboto(candidate)) + suffixWidth
                    > maxWidth) break;
            out.append(value.charAt(i));
        }
        return out + suffix;
    }

    private static void drawCoverTexture(GuiGraphics graphics,
            ResourceLocation texture, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (texture == null || minecraft.getResourceManager()
                .getResource(texture).isEmpty()) {
            graphics.fill(0, 0, width, height, 0xFF090C11);
            return;
        }
        float screenAspect = width / (float) Math.max(1, height);
        float textureAspect = REFERENCE_WIDTH / (float) REFERENCE_HEIGHT;
        float u = 0.0F;
        float v = 0.0F;
        float regionWidth = REFERENCE_WIDTH;
        float regionHeight = REFERENCE_HEIGHT;
        if (screenAspect > textureAspect) {
            regionHeight = REFERENCE_WIDTH / screenAspect;
            v = (REFERENCE_HEIGHT - regionHeight) * 0.5F;
        } else if (screenAspect < textureAspect) {
            regionWidth = REFERENCE_HEIGHT * screenAspect;
            u = (REFERENCE_WIDTH - regionWidth) * 0.5F;
        }
        graphics.blit(texture, 0, 0, width, height,
                u, v, Math.round(regionWidth), Math.round(regionHeight),
                REFERENCE_WIDTH, REFERENCE_HEIGHT);
    }

    private static void drawRotatedTexture(GuiGraphics graphics,
            ResourceLocation texture, int centerX, int centerY, int size,
            float angle, float alpha) {
        if (Minecraft.getInstance().getResourceManager()
                .getResource(texture).isEmpty()) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        graphics.blit(texture, -size / 2, -size / 2, size, size,
                0.0F, 0.0F, SPINNER_TEXTURE_SIZE, SPINNER_TEXTURE_SIZE,
                SPINNER_TEXTURE_SIZE, SPINNER_TEXTURE_SIZE);
        graphics.pose().popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    public static void drawScaledText(GuiGraphics graphics, Font font,
            Component text, float x, float y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static int blend(int from, int to, float amount) {
        float t = Mth.clamp(amount, 0.0F, 1.0F);
        int fa = from >>> 24;
        int fr = from >> 16 & 255;
        int fg = from >> 8 & 255;
        int fb = from & 255;
        int ta = to >>> 24;
        int tr = to >> 16 & 255;
        int tg = to >> 8 & 255;
        int tb = to & 255;
        return (Math.round(Mth.lerp(t, fa, ta)) << 24)
                | (Math.round(Mth.lerp(t, fr, tr)) << 16)
                | (Math.round(Mth.lerp(t, fg, tg)) << 8)
                | Math.round(Mth.lerp(t, fb, tb));
    }

    private static float approach(float current, float target, float amount) {
        if (current < target) return Math.min(target, current + amount);
        if (current > target) return Math.max(target, current - amount);
        return current;
    }

    private static float smootherStep(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    private static float wrapAngle(float angle) {
        float wrapped = angle % 360.0F;
        return wrapped < 0.0F ? wrapped + 360.0F : wrapped;
    }

    private static void playHover() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(MainMenuSounds.HOVER.get(), 1.0F));
    }
}
