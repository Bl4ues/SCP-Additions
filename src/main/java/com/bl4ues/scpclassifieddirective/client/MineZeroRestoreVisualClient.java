package com.bl4ues.scpclassifieddirective.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;

/** Short FOV/tunnel transition after returning from any custom death screen. */
@Mod.EventBusSubscriber(modid = ScpClassifiedDirectiveMod.MODID, value = Dist.CLIENT)
public final class MineZeroRestoreVisualClient {
    private static final long DURATION_MS = 1250L;
    private static volatile long startedAt = -1L;
    private static volatile boolean pending;

    private MineZeroRestoreVisualClient() {
    }

    /** Queue the transition until the death screen is actually gone. */
    public static void start() {
        if (ClientModulePreferences.reduceRestoreMotion()) {
            pending = false;
            startedAt = -1L;
            return;
        }
        pending = true;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !pending) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || !minecraft.player.isAlive()
                || minecraft.screen instanceof ScpDeathScreen) {
            return;
        }
        pending = false;
        startedAt = Util.getMillis();
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        float remaining = remaining();
        if (remaining <= 0.0F) return;
        double normal = event.getFOV();
        double expanded = Math.max(normal, 120.0D);
        event.setFOV(Mth.lerp(remaining, normal, expanded));
    }

    /** Rendered from the SCP: Classified Directive above-all HUD overlay. */
    public static void render(GuiGraphics graphics, int width, int height) {
        float remaining = remaining();
        if (graphics == null || remaining <= 0.0F) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;

        int baseAlpha = Mth.clamp(Math.round(remaining * 126.0F), 0, 126);
        graphics.fill(0, 0, width, height, baseAlpha << 24 | 0x0005090D);
        int bands = 10;
        int band = Math.max(8, Math.min(width, height) / 42);
        for (int i = 0; i < bands; i++) {
            float edge = remaining * (bands - i) / (float) bands;
            int alpha = Mth.clamp(Math.round(edge * 82.0F), 0, 82);
            int color = alpha << 24 | 0x00141A22;
            int inset = i * band;
            graphics.fill(inset, inset, width - inset, inset + band, color);
            graphics.fill(inset, height - inset - band,
                    width - inset, height - inset, color);
            graphics.fill(inset, inset, inset + band, height - inset, color);
            graphics.fill(width - inset - band, inset,
                    width - inset, height - inset, color);
        }
    }

    private static float remaining() {
        long start = startedAt;
        if (start < 0L) return 0.0F;
        long elapsed = Util.getMillis() - start;
        if (elapsed < 0L || elapsed >= DURATION_MS) {
            startedAt = -1L;
            return 0.0F;
        }
        float t = Mth.clamp(elapsed / (float) DURATION_MS, 0.0F, 1.0F);
        float eased = t * t * (3.0F - 2.0F * t);
        return 1.0F - eased;
    }
}
