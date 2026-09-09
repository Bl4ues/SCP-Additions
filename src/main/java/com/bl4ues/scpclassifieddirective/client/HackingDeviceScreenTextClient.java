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
    private static final int AMBER = 0xFFFFC857;

    private HackingDeviceScreenTextClient() {
    }

    /** Final forged-credential commit after the required breach chain is complete. */
    public static void renderSuccess(Font font, PoseStack poseStack,
            MultiBufferSource buffers) {
        double progress = HackingDeviceMinigameClient.phaseProgress();
        draw("CI//SCIPNET OVERRIDE COMMIT", 8, 9, GREEN_DIM);

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
            draw(lines[index], 8, 32 + index * 19, color);
        }

        if (progress > 0.72D) {
            centered("ACCESS BORROWED. MOVE.", 128, AMBER);
        }
        long noise = System.nanoTime() / 45_000_000L;
        int token = (int) ((noise * 31337L + 0x92FCL) & 0xFFFFL);
        draw(String.format("CI TOKEN %04X // TRUST US", token),
                8, 142, GREEN_DIM);
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
            centered("STOLEN PASS WINDOW", 48, GREEN_DIM);
            centered(String.format("[%d]", seconds), 75, GREEN_BRIGHT);
            centered("> CROSS NOW", 110, GREEN);
            centered("CI LINK WILL SELF-BURN", 132, AMBER);
            return;
        }

        long elapsed = now - countdownEnd;
        long phase = elapsed / HackingDeviceItem.BLINK_INTERVAL_TICKS;
        boolean visible = phase < HackingDeviceItem.BLINK_COUNT * 2L
                && (phase & 1L) == 0L;
        if (visible) {
            centered("0", 56, GREEN_BRIGHT);
            centered("BORROWED ACCESS EXPIRED", 86, GREEN_DIM);
        }
    }

    private static void centered(String text, float y, int color) {
        HackingDevicePixelFont.centered(text, y, color);
    }

    private static void draw(String text, float x, float y, int color) {
        HackingDevicePixelFont.draw(text, x, y, color);
    }
}
