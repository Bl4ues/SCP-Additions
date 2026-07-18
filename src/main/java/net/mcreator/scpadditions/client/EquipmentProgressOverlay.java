package net.mcreator.scpadditions.client;

import com.bl4ues.scpinventory.client.gui.ScpInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Reusable centered progress bar for timed equipment actions.
 *
 * <p>The server remains authoritative over the actual equipment state. Client
 * code may start local prediction with {@link #begin(int)}, correct it with
 * {@link #syncProgress(int, int)}, and explicitly finish or cancel the bar.</p>
 */
public final class EquipmentProgressOverlay {
    private static final int BAR_WIDTH = 240;
    private static final int BAR_HEIGHT = 8;
    private static final int BOTTOM_OFFSET = 104;
    private static final int COMPLETION_HOLD_TICKS = 3;

    private static final int TRACK = 0x7710181B;
    private static final int TRACK_DARK = 0xAA241B05;
    private static final int BORDER = 0xAA8F7422;
    private static final int FILL_LEFT = 0xDDBB831C;
    private static final int FILL_RIGHT = 0xFFFFD86A;

    private static boolean active;
    private static int elapsedTicks;
    private static int durationTicks = 1;
    private static int completionHoldTicks;

    private EquipmentProgressOverlay() {
    }

    public static void begin(int duration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ScpInventoryScreen) {
            minecraft.setScreen(null);
        }
        active = true;
        elapsedTicks = 0;
        durationTicks = Math.max(1, duration);
        completionHoldTicks = 0;
    }

    public static void syncProgress(int elapsed, int duration) {
        active = true;
        durationTicks = Math.max(1, duration);
        elapsedTicks = Math.max(0, Math.min(durationTicks, elapsed));
        completionHoldTicks = 0;
    }

    public static void complete() {
        if (!active) {
            return;
        }
        elapsedTicks = durationTicks;
        completionHoldTicks = COMPLETION_HOLD_TICKS;
    }

    public static void cancel() {
        reset();
    }

    public static boolean isActive() {
        return active;
    }

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            reset();
            return;
        }
        if (!active || minecraft.isPaused()) {
            return;
        }

        if (completionHoldTicks > 0) {
            completionHoldTicks--;
            if (completionHoldTicks <= 0) {
                reset();
            }
            return;
        }

        if (elapsedTicks < durationTicks) {
            elapsedTicks++;
        }
    }

    public static void render(GuiGraphics graphics, int screenWidth,
            int screenHeight, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active || minecraft.player == null || minecraft.options.hideGui
                || minecraft.screen != null) {
            return;
        }

        float interpolated = Math.min(durationTicks,
                elapsedTicks + Math.max(0.0F, partialTick));
        float ratio = Math.max(0.0F,
                Math.min(1.0F, interpolated / durationTicks));
        int x = (screenWidth - BAR_WIDTH) / 2;
        int y = screenHeight - BOTTOM_OFFSET;
        drawBar(graphics, x, y, BAR_WIDTH, BAR_HEIGHT, ratio);
    }

    private static void drawBar(GuiGraphics graphics, int x, int y,
            int width, int height, float ratio) {
        int right = x + width;
        int bottom = y + height;
        graphics.fill(x, y, right, bottom, TRACK);
        graphics.fill(x + 1, y + 1, right - 1, bottom - 1, TRACK_DARK);

        int fillWidth = Math.max(0,
                Math.min(width - 2, Math.round((width - 2) * ratio)));
        for (int i = 0; i < fillWidth; i++) {
            float progress = fillWidth <= 1
                    ? 1.0F : i / (float) (fillWidth - 1);
            graphics.fill(x + 1 + i, y + 1, x + 2 + i,
                    bottom - 1, lerpColor(FILL_LEFT, FILL_RIGHT, progress));
        }

        int markerX = Math.min(right - 2, x + 1 + fillWidth);
        graphics.fill(markerX, y - 2, markerX + 1, bottom + 2,
                withAlpha(FILL_RIGHT, 0.78F));
        graphics.fill(x, y, right, y + 1, BORDER);
        graphics.fill(x, bottom - 1, right, bottom, BORDER);
    }

    private static void reset() {
        active = false;
        elapsedTicks = 0;
        durationTicks = 1;
        completionHoldTicks = 0;
    }

    private static int withAlpha(int color, float alpha) {
        int value = Math.max(0,
                Math.min(255, Math.round(alpha * 255.0F)));
        return (value << 24) | (color & 0x00FFFFFF);
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
}
