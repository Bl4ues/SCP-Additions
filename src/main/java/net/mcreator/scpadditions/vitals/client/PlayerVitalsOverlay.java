package net.mcreator.scpadditions.vitals.client;

import net.minecraft.core.registries.BuiltInRegistries;

import com.mojang.blaze3d.systems.RenderSystem;
import com.bl4ues.scpinventory.client.ReferenceGuiScale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.equipment.HazmatSuitAccess;
import net.mcreator.scpadditions.init.ScpAdditionsModMobEffects;
import net.mcreator.scpadditions.vitals.VitalsModule;

/** SCP Inventory's lower-left health and stamina HUD, with independent toggles. */
public final class PlayerVitalsOverlay {
    private static final ResourceLocation STAMINA_ICON =
            ResourceLocation.fromNamespaceAndPath("scpinventory", "textures/gui/stamina.png");
    private static final ResourceLocation HEALTH_ICON =
            ResourceLocation.fromNamespaceAndPath("scpinventory", "textures/gui/healthlogo.png");
    private static final ResourceLocation ROBOTO_FONT =
            ResourceLocation.fromNamespaceAndPath("scpinventory", "roboto");

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
    private static final int HAZMAT_STAMINA_LEFT = 0xCC7B1717;
    private static final int HAZMAT_STAMINA_RIGHT = 0xFFE04A3F;
    private static final int BLEEDING_HEALTH_LEFT = 0xCC5A080C;
    private static final int BLEEDING_HEALTH_RIGHT = 0xFFF02632;
    private static final int FLASH_RED = 0xFFE01010;

    private PlayerVitalsOverlay() {
    }

    public static void render(GuiGraphics graphics, int screenWidth,
            int screenHeight, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isCreative() || player.isSpectator()
                || minecraft.screen != null || minecraft.options.hideGui
                || !VitalsModule.anyHudEnabled()) {
            return;
        }

        float uiScale = ReferenceGuiScale.factor(minecraft);
        int logicalScreenHeight = ReferenceGuiScale.logicalSize(
                screenHeight, uiScale);
        graphics.pose().pushPose();
        graphics.pose().scale(uiScale, uiScale, 1.0F);

        int rowY = logicalScreenHeight - BOTTOM_MARGIN;

        if (VitalsModule.staminaHudEnabled()) {
            drawIcon(graphics, STAMINA_ICON, ICON_X, rowY - 4);
            boolean hazmatEquipped = HazmatSuitAccess.isFullyEquipped(player);
            drawBar(graphics, BAR_X, rowY, BAR_WIDTH, BAR_HEIGHT,
                    PlayerVitalsClient.getStaminaRatio(),
                    hazmatEquipped ? HAZMAT_STAMINA_LEFT : STAMINA_LEFT,
                    hazmatEquipped ? HAZMAT_STAMINA_RIGHT : STAMINA_RIGHT,
                    0.0F);
            rowY += BAR_GAP;
        }

        if (VitalsModule.healthHudEnabled()) {
            drawIcon(graphics, HEALTH_ICON, ICON_X, rowY - 4);

            float health = Math.max(0.0F, player.getHealth());
            float maxHealth = Math.max(1.0F, player.getMaxHealth());
            float healthRatio = Math.max(0.0F, Math.min(1.0F, health / maxHealth));
            boolean bleeding = player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ScpAdditionsModMobEffects.BLEEDING.get()));
            int healthColor = bleeding
                    ? BLEEDING_HEALTH_RIGHT : getHealthColor(healthRatio);
            int healthDark = bleeding
                    ? BLEEDING_HEALTH_LEFT : darken(healthColor, 0.62F);

            drawBar(graphics, BAR_X, rowY, BAR_WIDTH, BAR_HEIGHT,
                    healthRatio, healthDark, healthColor,
                    PlayerVitalsClient.getDamageFlashAlpha());

            Component healthText = Component.literal(
                    Math.round(health) + "/" + Math.round(maxHealth))
                    .withStyle(style -> style.withFont(ROBOTO_FONT));
            graphics.drawString(minecraft.font, healthText,
                    BAR_X + 6, rowY + 3, TEXT, false);
        }

        graphics.pose().popPose();
    }

    private static void drawBar(GuiGraphics graphics, int x, int y,
            int width, int height, float ratio, int leftColor,
            int rightColor, float flashAlpha) {
        int right = x + width;
        int bottom = y + height;
        graphics.fill(x, y, right, bottom, TRACK);
        graphics.fill(x + 1, y + 1, right - 1, bottom - 1, TRACK_DARK);

        int fillWidth = Math.max(0,
                Math.min(width - 2, Math.round((width - 2) * ratio)));
        if (fillWidth > 0) {
            for (int i = 0; i < fillWidth; i++) {
                float progress = fillWidth <= 1
                        ? 1.0F : i / (float) (fillWidth - 1);
                graphics.fill(x + 1 + i, y + 1, x + 2 + i,
                        bottom - 1, lerpColor(leftColor, rightColor, progress));
            }

            int markerX = Math.min(right - 2, x + fillWidth);
            graphics.fill(markerX, y - 2, markerX + 1, bottom + 2,
                    withAlpha(rightColor, 0.9F));

            if (flashAlpha > 0.0F) {
                graphics.fill(x + 1, y + 1, x + 1 + fillWidth,
                        bottom - 1, withAlpha(FLASH_RED, flashAlpha));
            }
        }

        graphics.fill(x, y, right, y + 1, BORDER);
        graphics.fill(x, bottom - 1, right, bottom, BORDER);
        graphics.fill(x, y, x + 1, bottom, BORDER);
        graphics.fill(right - 1, y, right, bottom, BORDER);
    }

    private static int getHealthColor(float ratio) {
        int red = 0xCC8C1515;
        int orange = 0xCCAA6C24;
        int green = 0xCC7EA38A;

        if (ratio < 0.25F) {
            return lerpColor(red, orange, ratio / 0.25F);
        }
        if (ratio < 0.60F) {
            return lerpColor(orange, green, (ratio - 0.25F) / 0.35F);
        }
        return lerpColor(green, 0xD09BC0A0,
                (ratio - 0.60F) / 0.40F);
    }

    private static int darken(int color, float factor) {
        int alpha = color >>> 24;
        int red = Math.round(((color >> 16) & 0xFF) * factor);
        int green = Math.round(((color >> 8) & 0xFF) * factor);
        int blue = Math.round((color & 0xFF) * factor);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int lerpColor(int from, int to, float amount) {
        float t = Math.max(0.0F, Math.min(1.0F, amount));
        int alpha = Math.round(((from >>> 24) & 0xFF)
                + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
        int red = Math.round(((from >> 16) & 0xFF)
                + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int green = Math.round(((from >> 8) & 0xFF)
                + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int blue = Math.round((from & 0xFF)
                + ((to & 0xFF) - (from & 0xFF)) * t);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int withAlpha(int color, float alpha) {
        int value = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        return (value << 24) | (color & 0x00FFFFFF);
    }

    private static void drawIcon(GuiGraphics graphics,
            ResourceLocation texture, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.88F);
        graphics.blit(texture, x, y, ICON_SIZE, ICON_SIZE,
                0.0F, 0.0F, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE,
                ICON_SOURCE_SIZE, ICON_SOURCE_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
