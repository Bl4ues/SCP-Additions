package com.bl4ues.scpinventory.client;

import com.bl4ues.scpinventory.ScpInventoryMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

public final class PlayerVitalsOverlay {

    private static final ResourceLocation STAMINA_ICON = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/stamina.png");
    private static final ResourceLocation HEALTH_ICON = new ResourceLocation(ScpInventoryMod.MODID, "textures/gui/healthlogo.png");

    private static final int ICON_SOURCE_SIZE = 128;
    private static final int ICON_SIZE = 17;
    private static final int BAR_WIDTH = 184;
    private static final int BAR_HEIGHT = 10;
    private static final int BAR_X = 52;
    private static final int ICON_X = 28;
    private static final int BOTTOM_MARGIN = 70;
    private static final int BAR_GAP = 18;

    private static final int TRACK = 0x7710181B;
    private static final int TRACK_DARK = 0xAA0B1012;
    private static final int BORDER = 0x996A6C6C;
    private static final int TEXT = 0xE8DDE3E0;
    private static final int STAMINA_LEFT = 0xAA4D6474;
    private static final int STAMINA_RIGHT = 0xCC7EA0B7;
    private static final int FLASH_RED = 0xFFE01010;

    private PlayerVitalsOverlay() {
    }

    public static void render(GuiGraphics g, int screenWidth, int screenHeight, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null || mc.options.hideGui) return;

        int staminaY = screenHeight - BOTTOM_MARGIN;
        int healthY = staminaY + BAR_GAP;

        drawIcon(g, STAMINA_ICON, ICON_X, staminaY - 4);
        drawIcon(g, HEALTH_ICON, ICON_X, healthY - 4);

        drawBar(g, BAR_X, staminaY, BAR_WIDTH, BAR_HEIGHT, PlayerVitalsClient.getStaminaRatio(), STAMINA_LEFT, STAMINA_RIGHT, 0.0F);

        float health = Math.max(0.0F, player.getHealth());
        float maxHealth = Math.max(1.0F, player.getMaxHealth());
        float healthRatio = Math.max(0.0F, Math.min(1.0F, health / maxHealth));
        int healthColor = getHealthColor(healthRatio);
        int healthDark = darken(healthColor, 0.62F);
        drawBar(g, BAR_X, healthY, BAR_WIDTH, BAR_HEIGHT, healthRatio, healthDark, healthColor, PlayerVitalsClient.getDamageFlashAlpha());

        String healthText = Math.round(health) + "/" + Math.round(maxHealth);
        g.drawString(mc.font, healthText, BAR_X + 6, healthY + 1, TEXT, false);
    }

    private static void drawBar(GuiGraphics g, int x, int y, int width, int height, float ratio, int leftColor, int rightColor, float flashAlpha) {
        int right = x + width;
        int bottom = y + height;
        g.fill(x, y, right, bottom, TRACK);
        g.fill(x + 1, y + 1, right - 1, bottom - 1, TRACK_DARK);

        int fillWidth = Math.max(0, Math.min(width - 2, Math.round((width - 2) * ratio)));
        if (fillWidth > 0) {
            for (int i = 0; i < fillWidth; i++) {
                float t = fillWidth <= 1 ? 1.0F : i / (float) (fillWidth - 1);
                g.fill(x + 1 + i, y + 1, x + 2 + i, bottom - 1, lerpColor(leftColor, rightColor, t));
            }

            int markerX = Math.min(right - 2, x + fillWidth);
            g.fill(markerX, y - 2, markerX + 1, bottom + 2, withAlpha(rightColor, 0.9F));

            if (flashAlpha > 0.0F) {
                g.fill(x + 1, y + 1, x + 1 + fillWidth, bottom - 1, withAlpha(FLASH_RED, flashAlpha));
            }
        }

        g.fill(x, y, right, y + 1, BORDER);
        g.fill(x, bottom - 1, right, bottom, BORDER);
        g.fill(x, y, x + 1, bottom, BORDER);
        g.fill(right - 1, y, right, bottom, BORDER);
    }

    private static int getHealthColor(float ratio) {
        int red = 0xCC8C1515;
        int orange = 0xCCAA6C24;
        int green = 0xCC7EA38A;

        if (ratio < 0.25F) return lerpColor(red, orange, ratio / 0.25F);
        if (ratio < 0.60F) return lerpColor(orange, green, (ratio - 0.25F) / 0.35F);
        return lerpColor(green, 0xD09BC0A0, (ratio - 0.60F) / 0.40F);
    }

    private static int darken(int color, float factor) {
        int a = color >>> 24;
        int r = Math.round(((color >> 16) & 0xFF) * factor);
        int g = Math.round(((color >> 8) & 0xFF) * factor);
        int b = Math.round((color & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerpColor(int from, int to, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int a = Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int withAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static void drawIcon(GuiGraphics g, ResourceLocation texture, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.88F);
        g.blit(texture, x, y, ICON_SIZE, ICON_SIZE, 0.0F, 0.0F, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
