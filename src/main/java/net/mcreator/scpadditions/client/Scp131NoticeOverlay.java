package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** SCP-styled notice panel used by the Eye Pods in the standalone mod. */
public final class Scp131NoticeOverlay {
    private static final ResourceLocation POD_ICON =
            new ResourceLocation("scpinventory", "textures/gui/131logo.png");

    private static final long VISIBLE_DURATION = 4000L;
    private static final long FADE_IN_TIME = 150L;
    private static final long FADE_OUT_TIME = 500L;
    private static final int X = 42;
    private static final int Y = 28;
    private static final int HEIGHT = 54;
    private static final int BORDER = 3;
    private static final int LEFT_PADDING = 18;
    private static final int RIGHT_PADDING = 32;
    private static final int ICON_SIZE = 38;
    private static final int ICON_TEXT_GAP = 20;
    private static final float TEXT_SCALE = 1.22F;
    private static final float ICON_ALPHA = 0.96F;
    private static final int BORDER_COLOR = 0xFF8E8E8E;
    private static final int PANEL_COLOR = 0xA61F211F;
    private static final int TEXT_COLOR = 0xFFE1DDD3;

    private static boolean active;
    private static long shownAt;
    private static long visibleUntil;
    private static String currentText = "SCP-131 has started following you. Hold G to dismiss";

    private Scp131NoticeOverlay() {
    }

    public static void show(boolean following) {
        long now = System.currentTimeMillis();
        currentText = following
                ? "SCP-131 has started following you. Hold G to dismiss"
                : "SCP-131 has stopped following you";
        active = true;
        shownAt = now;
        visibleUntil = now + VISIBLE_DURATION;
    }

    public static void hide() {
        active = false;
        shownAt = 0L;
        visibleUntil = 0L;
    }

    public static void render(GuiGraphics guiGraphics) {
        long now = System.currentTimeMillis();
        if (!active || now > visibleUntil) {
            active = false;
            return;
        }

        long visibleFor = now - shownAt;
        long remaining = visibleUntil - now;
        float alpha = fadeAlpha(visibleFor, remaining);
        Minecraft minecraft = Minecraft.getInstance();
        Component text = ScpFonts.montserrat(currentText);
        int scaledTextWidth = Math.round(minecraft.font.width(text) * TEXT_SCALE);
        int iconX = X + LEFT_PADDING;
        int iconY = Y + ((HEIGHT - ICON_SIZE) / 2);
        int textX = iconX + ICON_SIZE + ICON_TEXT_GAP;
        int scaledTextHeight = Math.round(minecraft.font.lineHeight * TEXT_SCALE);
        int textY = Y + Math.round((HEIGHT - scaledTextHeight) / 2.0F) + 3;
        int width = LEFT_PADDING + ICON_SIZE + ICON_TEXT_GAP + scaledTextWidth + RIGHT_PADDING;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 1000.0F);
        drawNoticeFrame(guiGraphics, X, Y, width, HEIGHT, alpha);
        drawIcon(guiGraphics, POD_ICON, iconX, iconY, alpha);
        drawText(guiGraphics, minecraft, text, textX, textY, alpha);
        guiGraphics.pose().popPose();
    }

    private static void drawNoticeFrame(GuiGraphics guiGraphics, int x, int y, int width, int height, float alpha) {
        int border = fadeColor(BORDER_COLOR, alpha);
        guiGraphics.fill(x, y, x + width, y + height, fadeColor(PANEL_COLOR, alpha));
        guiGraphics.fill(x, y, x + width, y + BORDER, border);
        guiGraphics.fill(x, y + height - BORDER, x + width, y + height, border);
        guiGraphics.fill(x, y, x + BORDER, y + height, border);
        guiGraphics.fill(x + width - BORDER, y, x + width, y + height, border);
    }

    private static void drawIcon(GuiGraphics guiGraphics, ResourceLocation icon, int x, int y, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha * ICON_ALPHA);
        guiGraphics.blit(icon, x, y, ICON_SIZE, ICON_SIZE, 0.0F, 0.0F, 256, 256, 256, 256);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void drawText(GuiGraphics guiGraphics, Minecraft minecraft, Component text, int x, int y, float alpha) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
        guiGraphics.drawString(minecraft.font, text, Math.round(x / TEXT_SCALE), Math.round(y / TEXT_SCALE),
                fadeColor(TEXT_COLOR, alpha), true);
        guiGraphics.pose().popPose();
    }

    private static float fadeAlpha(long visibleFor, long remaining) {
        float in = FADE_IN_TIME <= 0L ? 1.0F : Math.min(1.0F, (float) visibleFor / FADE_IN_TIME);
        float out = remaining < FADE_OUT_TIME ? Math.max(0.0F, (float) remaining / FADE_OUT_TIME) : 1.0F;
        return Math.min(in, out);
    }

    private static int fadeColor(int color, float alpha) {
        int a = (color >>> 24) & 255;
        int faded = Mth.clamp(Math.round(a * alpha), 0, 255);
        return (color & 0x00FFFFFF) | (faded << 24);
    }
}
