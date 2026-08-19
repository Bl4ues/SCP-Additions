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
import net.mcreator.scpadditions.network.SaveFeedbackPacket;
import net.mcreator.scpadditions.save.SaveMethod;

/** Client cache for the current save slot plus the animated save-feedback HUD. */
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
    private static final long ANY_SAVE_REPLAY_GUARD_MS = TOTAL_MS + 1400L;
    private static final long SAME_SAVE_REPLAY_GUARD_MS = TOTAL_MS + 4200L;
    private static final long GENERIC_ECHO_GUARD_MS = TOTAL_MS + 7000L;
    private static final long POST_FADE_SUPPRESSION_MS = 1200L;
    private static final long LOAD_GAME_SUPPRESSION_MS = 6500L;
    private static final float FINAL_RENDER_CUTOFF = 0.055F;

    private static volatile SaveMethod lastMethod = SaveMethod.WORLD_SPAWN;
    private static volatile SaveMethod lastOverlayMethod = SaveMethod.WORLD_SPAWN;
    private static volatile OverlayKind overlayKind = OverlayKind.SAVING;
    private static volatile long overlayStartedAt = -1L;
    private static volatile long lastOverlayRequestAt = -1L;
    private static volatile long overlaySuppressedUntil = -1L;

    private SaveGameClientState() {
    }

    /** Invoked reflectively by SaveStatePacket to keep client classes server-safe. */
    public static void receive(String methodId, boolean showOverlay) {
        SaveMethod method = SaveMethod.fromId(methodId);
        lastMethod = method;
        if (!showOverlay) return;

        long now = Util.getMillis();
        if (now < overlaySuppressedUntil) return;

        long sinceLast = lastOverlayRequestAt < 0L
                ? Long.MAX_VALUE : now - lastOverlayRequestAt;
        if (sinceLast < ANY_SAVE_REPLAY_GUARD_MS) return;
        if (lastOverlayMethod == method
                && sinceLast < SAME_SAVE_REPLAY_GUARD_MS) return;
        if (method == SaveMethod.RESPAWN_POINT
                && lastOverlayMethod != SaveMethod.RESPAWN_POINT
                && sinceLast < GENERIC_ECHO_GUARD_MS) return;

        lastOverlayMethod = method;
        lastOverlayRequestAt = now;
        overlayKind = OverlayKind.SAVING;
        overlayStartedAt = now;
    }

    /** Invoked reflectively by SaveFeedbackPacket for feedback that is not a save. */
    public static void receiveFeedback(int kind) {
        if (kind != SaveFeedbackPacket.SAVE_BLOCKED) return;
        long now = Util.getMillis();
        if (now < overlaySuppressedUntil) return;

        // A blocked attempt is immediate user feedback, not another save-state
        // transition. Repeated attempts simply restart the same presentation.
        overlayKind = OverlayKind.SAVE_BLOCKED;
        overlayStartedAt = now;
    }

    /**
     * A respawn or world rewind is loading an existing save, not creating one.
     * Silence delayed setRespawnPosition echoes and kill any overlay still alive.
     */
    public static void suppressForLoadGame() {
        long now = Util.getMillis();
        overlayStartedAt = -1L;
        overlaySuppressedUntil = Math.max(overlaySuppressedUntil,
                now + LOAD_GAME_SUPPRESSION_MS);
    }

    public static SaveMethod lastMethod() {
        return lastMethod;
    }

    public static void render(GuiGraphics graphics, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (graphics == null || minecraft.player == null
                || overlayStartedAt < 0L) return;

        long now = Util.getMillis();
        long elapsed = now - overlayStartedAt;
        if (elapsed < 0L || elapsed >= TOTAL_MS) {
            finishOverlay(now);
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

        // Stop the entire presentation together at the tail instead of allowing
        // one nearly-transparent font frame to look like a second flash.
        if (elapsed > FADE_IN_MS + HOLD_MS && alpha <= FINAL_RENDER_CUTOFF) {
            finishOverlay(now);
            return;
        }

        float entrance = smootherStep(Mth.clamp(
                elapsed / (float) FADE_IN_MS, 0.0F, 1.0F));
        float slideY = (1.0F - entrance) * 4.0F;
        int iconSize = Mth.clamp(Math.round(Mth.lerp(entrance, 27.0F, 32.0F)),
                27, 32);
        int iconX = 20;
        int iconY = Math.round(22.0F + slideY);
        int centerX = iconX + iconSize / 2;
        int centerY = iconY + iconSize / 2;
        boolean saving = overlayKind == OverlayKind.SAVING;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        if (saving) {
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

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha * 0.92F);
            drawRotatedTexture(graphics, SAVE_OUTER, centerX, centerY,
                    iconSize, outerAngle);
            drawRotatedTexture(graphics, SAVE_INNER, centerX, centerY,
                    iconSize, innerAngle);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        int a = Mth.clamp(Math.round(alpha * 224.0F), 0, 255);
        int color = a << 24 | 0x00F2F2F2;
        Component message = ScpFonts.roboto(saving
                ? "Saving..." : "You can't save right now");
        float textScale = 1.50F;

        // Keep both messages on the exact same baseline and text origin. The
        // failure state intentionally omits the rotating save emblem because no
        // checkpoint was created.
        float textX = iconX + iconSize + 11.0F;
        float scaledLineHeight = minecraft.font.lineHeight * textScale;
        float textY = centerY - scaledLineHeight * 0.5F + 2.25F;

        graphics.pose().pushPose();
        graphics.pose().translate(textX, textY, 900.0F);
        graphics.pose().scale(textScale, textScale, 1.0F);
        graphics.drawString(minecraft.font, message, 0, 0, color, false);
        graphics.pose().popPose();

        RenderSystem.disableBlend();
    }

    private static void finishOverlay(long now) {
        boolean wasSaving = overlayKind == OverlayKind.SAVING;
        overlayStartedAt = -1L;
        if (wasSaving) {
            overlaySuppressedUntil = Math.max(overlaySuppressedUntil,
                    now + POST_FADE_SUPPRESSION_MS);
        }
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

    private enum OverlayKind {
        SAVING,
        SAVE_BLOCKED
    }
}
