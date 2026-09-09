package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.item.HackingDeviceItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Locale;

/** Shared character-only overlays for the physical and hand-held device CRT. */
public final class HackingDeviceScreenTextClient {
    public static final float LOGICAL_WIDTH = 256.0F;
    public static final float LOGICAL_HEIGHT = 154.0F;
    public static final int GREEN = 0xFF49F06F;
    public static final int GREEN_BRIGHT = 0xFF78FF94;
    public static final int GREEN_DIM = 0xFF238A42;
    private static final int AMBER = 0xFFFFC857;
    private static final float PAD_X = 12.0F;

    private HackingDeviceScreenTextClient() {
    }

    /** Final forged-credential commit after the required breach chain is complete. */
    public static void renderSuccess(Font font, PoseStack poseStack,
            MultiBufferSource buffers) {
        double progress = HackingDeviceMinigameClient.phaseProgress();
        draw("CI//SCIPNET OVERRIDE COMMIT", PAD_X, 10, GREEN_DIM);

        String[] lines = {
                "> BREACH CHAIN.........VALID",
                "> FOUNDATION CERT......FORGED",
                "> ACL ENTRY.............LIED",
                "> READER TRUST..........YES",
                "> OPEN REQUEST.........QUEUED"
        };
        int visible = Math.max(1, Math.min(lines.length,
                1 + (int) Math.floor(progress * lines.length)));
        for (int index = 0; index < visible; index++) {
            int color = index == visible - 1 ? GREEN_BRIGHT : GREEN;
            draw(lines[index], PAD_X, 33 + index * 18, color);
        }

        if (progress > 0.72D) {
            centered("ACCESS BORROWED. MOVE.", 127, AMBER);
        }
        long noise = System.nanoTime() / 45_000_000L;
        int token = (int) ((noise * 31337L + 0x92FCL) & 0xFFFFL);
        draw(String.format(Locale.ROOT, "CI TOKEN %04X // SPOOF ACTIVE", token),
                PAD_X, 141, GREEN_DIM);
    }

    public static void renderAttachedCooldown(Font font, PoseStack poseStack,
            MultiBufferSource buffers) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) return;
        renderCountdown(level.getGameTime() + minecraft.getFrameTime(),
                HackingDeviceMinigameClient.cooldownEnd(),
                HackingDeviceMinigameClient.readyAt());
    }

    public static void renderItemCooldown(ItemStack stack, Font font,
            PoseStack poseStack, MultiBufferSource buffers) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || stack == null || stack.isEmpty()
                || !HackingDeviceItem.isCoolingDown(stack, level)) {
            return;
        }
        renderCountdown(level.getGameTime() + minecraft.getFrameTime(),
                HackingDeviceItem.countdownEnd(stack),
                HackingDeviceItem.readyAt(stack));
    }

    /**
     * Shows the stolen-reader window with frame-interpolated milliseconds. The
     * server still owns the real tick deadline; interpolation only makes the tiny
     * physical display readable instead of stepping down in 50 ms chunks.
     */
    private static void renderCountdown(double now, long countdownEnd,
            long readyAt) {
        if (countdownEnd <= 0L || readyAt <= 0L || now >= readyAt) return;

        if (now < countdownEnd) {
            double remainingSeconds = Math.max(0.0D,
                    Math.min(5.0D, (countdownEnd - now) / 20.0D));
            centered("ACCESS GRANTED", 12, GREEN_BRIGHT);
            centered("STOLEN PASS WINDOW", 27, GREEN_DIM);
            bigCentered(String.format(Locale.ROOT, "%.3f", remainingSeconds),
                    45, GREEN_BRIGHT);
            centered("> CROSS NOW", 103, GREEN);
            centered("READER RELOCKS AT ZERO", 124, AMBER);
            return;
        }

        double elapsed = now - countdownEnd;
        long phase = (long) Math.floor(elapsed
                / HackingDeviceItem.BLINK_INTERVAL_TICKS);
        boolean visible = phase < HackingDeviceItem.BLINK_COUNT * 2L
                && (phase & 1L) == 0L;
        if (visible) {
            bigCentered("EXPIRED", 43, GREEN_BRIGHT);
            centered("ACCESS WINDOW CLOSED", 108, GREEN_DIM);
            centered("READER LOCK RESTORED", 126, AMBER);
        }
    }

    /**
     * Large 3x5 display lettering built from the existing CRT raster glyphs.
     * It deliberately consumes much more of the physical screen so the timer is
     * actually legible when the device is back in the player's hand.
     */
    private static void bigCentered(String text, float y, int color) {
        String normalized = text == null ? "" : text.toUpperCase(Locale.ROOT);
        for (int row = 0; row < 5; row++) {
            StringBuilder line = new StringBuilder();
            for (int index = 0; index < normalized.length(); index++) {
                if (index > 0) line.append(' ');
                int bits = bigGlyph(normalized.charAt(index))[row];
                for (int column = 2; column >= 0; column--) {
                    line.append((bits & 1 << column) != 0 ? '#' : ' ');
                }
            }
            HackingDevicePixelFont.centered(line.toString(), y + row * 9.0F,
                    color);
        }
    }

    private static int[] bigGlyph(char character) {
        return switch (character) {
            case '0' -> b(7, 5, 5, 5, 7);
            case '1' -> b(2, 6, 2, 2, 7);
            case '2' -> b(7, 1, 7, 4, 7);
            case '3' -> b(7, 1, 7, 1, 7);
            case '4' -> b(5, 5, 7, 1, 1);
            case '5' -> b(7, 4, 7, 1, 7);
            case '6' -> b(7, 4, 7, 5, 7);
            case '7' -> b(7, 1, 1, 1, 1);
            case '8' -> b(7, 5, 7, 5, 7);
            case '9' -> b(7, 5, 7, 1, 7);
            case '.' -> b(0, 0, 0, 0, 2);
            case 'E' -> b(7, 4, 6, 4, 7);
            case 'X' -> b(5, 5, 2, 5, 5);
            case 'P' -> b(6, 5, 6, 4, 4);
            case 'I' -> b(7, 2, 2, 2, 7);
            case 'R' -> b(6, 5, 6, 5, 5);
            case 'D' -> b(6, 5, 5, 5, 6);
            default -> b(0, 0, 0, 0, 0);
        };
    }

    private static int[] b(int a, int b, int c, int d, int e) {
        return new int[]{a, b, c, d, e};
    }

    private static void centered(String text, float y, int color) {
        HackingDevicePixelFont.centered(text, y, color);
    }

    private static void draw(String text, float x, float y, int color) {
        HackingDevicePixelFont.draw(text, x, y, color);
    }
}
