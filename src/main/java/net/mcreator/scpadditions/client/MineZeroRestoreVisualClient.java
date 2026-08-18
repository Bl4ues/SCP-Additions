package net.mcreator.scpadditions.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Short post-rewind FOV/tunnel transition after a MineZero checkpoint restore. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID, value = Dist.CLIENT)
public final class MineZeroRestoreVisualClient {
    private static final long DURATION_MS = 1050L;
    private static volatile long startedAt = -1L;

    private MineZeroRestoreVisualClient() {
    }

    public static void start() {
        if (ClientModulePreferences.reduceRestoreMotion()) {
            startedAt = -1L;
            return;
        }
        startedAt = Util.getMillis();
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        float remaining = remaining();
        if (remaining <= 0.0F) return;
        double normal = event.getFOV();
        double expanded = Math.max(normal, 110.0D);
        event.setFOV(Mth.lerp(remaining, normal, expanded));
    }

    @SubscribeEvent
    public static void onGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CHAT.id())) return;
        float remaining = remaining();
        if (remaining <= 0.0F) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        // A layered radial veil approximates the first blurred instant without
        // forcing a post-processing shader onto shader packs such as Oculus.
        int baseAlpha = Mth.clamp(Math.round(remaining * 96.0F), 0, 96);
        graphics.fill(0, 0, width, height, baseAlpha << 24 | 0x0006090C);
        int bands = 8;
        int band = Math.max(8, Math.min(width, height) / 38);
        for (int i = 0; i < bands; i++) {
            float edge = remaining * (bands - i) / (float) bands;
            int alpha = Mth.clamp(Math.round(edge * 62.0F), 0, 62);
            int color = alpha << 24 | 0x00151A21;
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
