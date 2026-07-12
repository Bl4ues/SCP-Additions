package net.mcreator.scpadditions.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Client-only state and renderer for an authoritative blocked-pickup notice. */
final class ClientScpInventoryFullOverlay {
    private static final ResourceLocation ICON = new ResourceLocation(
            "scpinventory", "textures/gui/inventoryfull.png");
    private static final ResourceLocation ROBOTO = new ResourceLocation(
            "scpinventory", "roboto");

    private static final long VISIBLE_DURATION_MS = 4000L;
    private static final long FADE_IN_MS = 150L;
    private static final long FADE_OUT_MS = 500L;
    private static final int X = 42;
    private static final int Y = 28;
    private static final int HEIGHT = 54;
    private static final int ICON_SIZE = 38;
    private static final int PANEL = 0xA61F211F;
    private static final int BORDER = 0xFF8E8E8E;
    private static final int TEXT = 0xFFE1DDD3;

    private static long shownAt;
    private static long visibleUntil;

    private ClientScpInventoryFullOverlay() {
    }

    static void show() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isCreative()
                || minecraft.player.isSpectator()) {
            return;
        }
        long now = System.currentTimeMillis();
        shownAt = now;
        visibleUntil = now + VISIBLE_DURATION_MS;
    }

    static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        if (minecraft.player == null || minecraft.player.isCreative()
                || minecraft.player.isSpectator() || minecraft.options.hideGui
                || now >= visibleUntil) {
            return;
        }

        float alpha = fadeAlpha(now - shownAt, visibleUntil - now);
        Component message = Component.translatable(
                "overlay.scp_additions.scp_inventory_full")
                .withStyle(style -> style.withFont(ROBOTO));
        int textWidth = minecraft.font.width(message);
        int width = 18 + ICON_SIZE + 18 + textWidth + 28;
        int borderColor = withAlpha(BORDER, alpha);

        graphics.fill(X, Y, X + width, Y + HEIGHT, withAlpha(PANEL, alpha));
        graphics.fill(X, Y, X + width, Y + 3, borderColor);
        graphics.fill(X, Y + HEIGHT - 3, X + width, Y + HEIGHT, borderColor);
        graphics.fill(X, Y, X + 3, Y + HEIGHT, borderColor);
        graphics.fill(X + width - 3, Y, X + width, Y + HEIGHT, borderColor);

        int iconX = X + 18;
        int iconY = Y + (HEIGHT - ICON_SIZE) / 2;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha * 0.96F);
        graphics.blit(ICON, iconX, iconY, ICON_SIZE, ICON_SIZE,
                0.0F, 0.0F, 256, 256, 256, 256);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();

        int textY = Y + (HEIGHT - minecraft.font.lineHeight) / 2 + 1;
        graphics.drawString(minecraft.font, message,
                iconX + ICON_SIZE + 18, textY,
                withAlpha(TEXT, alpha), true);
    }

    private static float fadeAlpha(long visibleFor, long remaining) {
        float fadeIn = Math.min(1.0F, visibleFor / (float) FADE_IN_MS);
        float fadeOut = remaining < FADE_OUT_MS
                ? Math.max(0.0F, remaining / (float) FADE_OUT_MS) : 1.0F;
        return Math.min(fadeIn, fadeOut);
    }

    private static int withAlpha(int color, float alpha) {
        int sourceAlpha = (color >>> 24) & 0xFF;
        int resultAlpha = Mth.clamp(Math.round(sourceAlpha * alpha), 0, 255);
        return (color & 0x00FFFFFF) | (resultAlpha << 24);
    }
}
