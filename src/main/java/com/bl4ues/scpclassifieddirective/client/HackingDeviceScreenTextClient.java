package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.item.HackingDeviceItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Shared character-only overlays for the physical and hand-held device CRT. */
public final class HackingDeviceScreenTextClient {
    public static final float LOGICAL_WIDTH = 256.0F;
    public static final float LOGICAL_HEIGHT = 154.0F;
    public static final int GREEN = 0xFF49F06F;
    public static final int GREEN_BRIGHT = 0xFF78FF94;
    public static final int GREEN_DIM = 0xFF238A42;

    private HackingDeviceScreenTextClient() {
    }

    /** Replaces the old ASCII lock with a fast SCP-079-style terminal commit. */
    public static void renderSuccess(Font font, PoseStack poseStack,
            MultiBufferSource buffers) {
        double progress = HackingDeviceMinigameClient.phaseProgress();
        draw("BREACH COMMIT // FINAL", 8, 10, GREEN_DIM);

        String[] lines = {
                "> CRC CHAIN............VALID",
                "> FORGE AUTH FRAME........OK",
                "> INJECT CREDENTIAL........OK",
                "> OVERRIDE ACCESS LIST.....OK",
                "> READER HANDSHAKE........WAIT"
        };
        int visible = Math.max(1, Math.min(lines.length,
                1 + (int) Math.floor(progress * lines.length)));
        for (int index = 0; index < visible; index++) {
            int color = index == visible - 1 ? GREEN_BRIGHT : GREEN;
            draw(lines[index], 8, 34 + index * 19, color);
        }

        long noise = System.nanoTime() / 45_000_000L;
        int a = (int) ((noise * 37L + 0xA7L) & 0xFFL);
        int b = (int) ((noise * 91L + 0x31L) & 0xFFL);
        int token = (int) ((noise * 31337L + 0x92FCL) & 0xFFFFL);
        draw(String.format("AUTH TRACE %02X:%02X  TOKEN %04X", a, b, token),
                8, 137, GREEN_DIM);
    }

    public static void renderAttachedCooldown(Font font, PoseStack poseStack,
            MultiBufferSource buffers) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) return;
        renderCountdown(level.getGameTime(),
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
        renderCountdown(level.getGameTime(),
                HackingDeviceItem.countdownEnd(stack),
                HackingDeviceItem.readyAt(stack));
    }

    private static void renderCountdown(long now, long countdownEnd,
            long readyAt) {
        if (countdownEnd <= 0L || readyAt <= 0L || now >= readyAt) return;

        if (now < countdownEnd) {
            long remaining = countdownEnd - now;
            int seconds = (int) Math.min(5L,
                    Math.max(1L, (remaining + 19L) / 20L));
            centered("ACCESS GRANTED", 24, GREEN_BRIGHT);
            centered("PASS WINDOW", 52, GREEN_DIM);
            centered(String.format("[%d]", seconds), 76, GREEN_BRIGHT);
            centered("> CROSS NOW", 113, GREEN);
            return;
        }

        long elapsed = now - countdownEnd;
        long phase = elapsed / HackingDeviceItem.BLINK_INTERVAL_TICKS;
        boolean visible = phase < HackingDeviceItem.BLINK_COUNT * 2L
                && (phase & 1L) == 0L;
        if (visible) {
            centered("0", 56, GREEN_BRIGHT);
            centered("LINK CLOSED", 86, GREEN_DIM);
        }
    }

    private static void centered(String text, float y, int color) {
        HackingDevicePixelFont.centered(text, y, color);
    }

    private static void draw(String text, float x, float y, int color) {
        HackingDevicePixelFont.draw(text, x, y, color);
    }
}
