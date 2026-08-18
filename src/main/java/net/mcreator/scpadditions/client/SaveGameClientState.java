package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.mcreator.scpadditions.ScpAdditionsMod;
import net.mcreator.scpadditions.save.SaveMethod;

/** Client cache for the current save slot plus the animated Saving... HUD. */
public final class SaveGameClientState {
    private static final ResourceLocation SAVE_OUTER = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/screens/menu/loading_1.png");
    private static final ResourceLocation SAVE_INNER = new ResourceLocation(
            ScpAdditionsMod.MODID, "textures/screens/menu/loading_2.png");
    private static final int SAVE_TEXTURE_SIZE = 512;

    private static final long FADE_IN_MS = 260L;
    private static final long HOLD_MS = 2500L;
    private static final long FADE_OUT_MS = 840L;
    private static final long TOTAL_MS = FADE_IN_MS + HOLD_MS + FADE_OUT_MS;
    private static final long ROTATION_START_MS = 480L;
    private static final long ROTATION_END_MS = 2450L;
    private static final long DUPLICATE_REQUEST_GUARD_MS = 900L;

    private static volatile SaveMethod lastMethod = SaveMethod.WORLD_SPAWN;
    private static volatile SaveMethod lastOverlayMethod = SaveMethod.WORLD_SPAWN;
    private static volatile long overlayStartedAt = -1L;
    private static volatile long lastOverlayRequestAt = -1L;

    private SaveGameClientState() {
    }

    /** Invoked reflectively by SaveStatePacket to keep client classes server-safe. */
    public static void receive(String methodId, boolean showOverlay) {
        SaveMethod method = SaveMethod.fromId(methodId);
        lastMethod = method;
        if (!showOverlay) return;

        long now = Util.getMillis();
        boolean overlayActive = overlayStartedAt >= 0L
                && now - overlayStartedAt < TOTAL_MS;
        if (lastOverlayMethod == method
                && (overlayActive
                || now - lastOverlayRequestAt < DUPLICATE_REQUEST_GUARD_MS)) {
            return;
        }

        lastOverlayMethod = method;
        lastOverlayRequestAt = now;
        overlayStartedAt = now;
    }

    public static SaveMethod lastMethod() {
        return lastMethod;
    }

    public static void render(GuiGraphics graphics, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (graphics == null || minecraft.player == null
                || overlayStartedAt < 0L) return;

        long elapsed = Util.getMillis() - overlayStartedAt;
        if (elapsed < 0L || elapsed >= TOTAL_MS) {
            overlayStartedAt = -1L;
            return;
        }

        float alpha;
        if (elapsed < FADE_IN_MS) {
            alpha = elapsed / (float) FADE_IN_MS;
        } else if (elapsed < FADE_IN_MS + HOLD_MS) {
            alpha = 1.0F;
        } else {
            alpha = 1.0F - (elapsed - FADE_IN_MS - HOLD_MS)
                    / (float) FADE_OUT_MS;
        }
        alpha = smootherStep(alpha);

        float entrance = smootherStep(Mth.clamp(
                elapsed / (float) FADE_IN_MS, 0.0F, 1.0F));
        float slideY = (1.0F - entrance) * 4.0F;
        int iconSize = Mth.clamp(Math.round(Mth.lerp(entrance, 27.0F, 32.0F)),
                27, 32);
        int iconX = 20;
        int iconY = Math.round(17.0F + slideY);
        int centerX = iconX + iconSize / 2;
        int centerY = iconY + iconSize / 2;

        float rotationProgress;
        if (elapsed <= ROTATION_START_MS) {
            rotationProgress = 0.0F;
        } else if (elapsed >= ROTATION_END_MS) {
            rotationProgress = 1.0F;
        } else {
            rotationProgress = (elapsed - ROTATION_START_MS)
                    / (float) (ROTATION_END_MS - ROTATION_START_MS);
        }
        float easedRotation = smootherStep(rotationProgress);
        float outerAngle = 360.0F * easedRotation;
        float innerAngle = -360.0F * easedRotation;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha * 0.92F);
        drawRotatedTexture(graphics, SAVE_OUTER, centerX, centerY,
                iconSize, outerAngle);
        drawRotatedTexture(graphics, SAVE_INNER, centerX, centerY,
                iconSize, innerAngle);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();

        int a = Mth.clamp(Math.round(alpha * 224.0F), 0, 255);
        int color = a << 24 | 0x00F2F2F2;
        Component saving = ScpFonts.roboto("Saving...");
        float textScale = 1.30F;
        float textX = iconX + iconSize + 11.0F;
        float textY = iconY + (iconSize
                - minecraft.font.lineHeight * textScale) / 2.0F + 0.5F;

        graphics.pose().pushPose();
        graphics.pose().translate(textX, textY, 900.0F);
        graphics.pose().scale(textScale, textScale, 1.0F);
        graphics.drawString(minecraft.font, saving, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static void drawRotatedTexture(GuiGraphics graphics,
            ResourceLocation texture, int centerX, int centerY, int size,
            float angleDegrees) {
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 900.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(angleDegrees));
        int half = size / 2;
        graphics.blit(texture, -half, -half, size, size, 0.0F, 0.0F,
                SAVE_TEXTURE_SIZE, SAVE_TEXTURE_SIZE, SAVE_TEXTURE_SIZE,
                SAVE_TEXTURE_SIZE);
        graphics.pose().popPose();
    }

    private static float smootherStep(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }
}
