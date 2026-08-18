package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.ScpFonts;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.mcreator.scpadditions.save.SaveMethod;

/** Client cache for the current save slot plus the subtle Saving... HUD. */
public final class SaveGameClientState {
    private static final long FADE_IN_MS = 180L;
    private static final long HOLD_MS = 620L;
    private static final long FADE_OUT_MS = 760L;
    private static final long TOTAL_MS = FADE_IN_MS + HOLD_MS + FADE_OUT_MS;

    private static volatile SaveMethod lastMethod = SaveMethod.WORLD_SPAWN;
    private static volatile long overlayStartedAt = -1L;

    private SaveGameClientState() {
    }

    /** Invoked reflectively by SaveStatePacket to keep client classes server-safe. */
    public static void receive(String methodId, boolean showOverlay) {
        lastMethod = SaveMethod.fromId(methodId);
        if (showOverlay) overlayStartedAt = Util.getMillis();
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
        alpha = alpha * alpha * (3.0F - 2.0F * alpha);
        int a = Mth.clamp(Math.round(alpha * 205.0F), 0, 255);
        int color = a << 24 | 0x00F2F2F2;

        graphics.pose().pushPose();
        graphics.pose().translate(18.0F, 18.0F, 900.0F);
        graphics.pose().scale(0.92F, 0.92F, 1.0F);
        graphics.drawString(minecraft.font, ScpFonts.roboto("Saving..."),
                0, 0, color, false);
        graphics.pose().popPose();
    }
}
