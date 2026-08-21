package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mcreator.scpadditions.ScpAdditionsMod;

/** Red impact vignette layered under the SCP-939 maul QTE. */
@Mod.EventBusSubscriber(modid = ScpAdditionsMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Scp939MaulImpactOverlay {
    private static final int VIGNETTE_BANDS = 30;
    private static final float TARGET_DECAY_PER_TICK = 0.78F;
    private static final float HIT_IMPULSE = 0.62F;
    private static final float MAX_ALPHA = 0.68F;
    private static final float RESPONSE = 17.0F;

    private static int previousHurtTime;
    private static float targetStrength;
    private static float visualStrength;
    private static long lastFrameNanos;

    private Scp939MaulImpactOverlay() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !Scp939ClientState.pinned()) {
            previousHurtTime = player == null ? 0 : player.hurtTime;
            targetStrength = 0.0F;
            return;
        }

        int hurtTime = player.hurtTime;
        // hurtTime jumps back up when a new damage event lands, then counts
        // down. During a pin this gives us a local, packet-free maul impact cue.
        if (hurtTime > previousHurtTime) {
            targetStrength = Mth.clamp(targetStrength + HIT_IMPULSE,
                    0.0F, 1.0F);
        } else {
            targetStrength *= TARGET_DECAY_PER_TICK;
            if (targetStrength < 0.004F) targetStrength = 0.0F;
        }
        previousHurtTime = hurtTime;
    }

    public static void render(GuiGraphics graphics, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!Scp939ClientState.pinned() || minecraft.player == null
                || minecraft.options.hideGui || minecraft.screen != null) {
            visualStrength = 0.0F;
            lastFrameNanos = 0L;
            return;
        }

        long now = System.nanoTime();
        float delta = frameDelta(now);
        float amount = 1.0F - (float) Math.exp(-RESPONSE * delta);
        visualStrength += (targetStrength - visualStrength) * amount;
        if (visualStrength <= 0.002F) return;

        drawRedVignette(graphics, width, height, visualStrength);
    }

    private static void drawRedVignette(GuiGraphics graphics, int width,
            int height, float strength) {
        int maximumInset = Math.max(30, Math.min(width, height) / 3);
        for (int band = 0; band < VIGNETTE_BANDS; band++) {
            int outer = Math.round(maximumInset
                    * band / (float) VIGNETTE_BANDS);
            int inner = Math.round(maximumInset
                    * (band + 1) / (float) VIGNETTE_BANDS);
            if (inner <= outer) continue;

            float edge = 1.0F - band / (float) (VIGNETTE_BANDS - 1);
            float alpha = strength * MAX_ALPHA * edge * edge;
            int color = argb(alpha, 122, 6, 12);
            graphics.fill(outer, outer, width - outer, inner, color);
            graphics.fill(outer, height - inner,
                    width - outer, height - outer, color);
            graphics.fill(outer, inner, inner, height - inner, color);
            graphics.fill(width - inner, inner,
                    width - outer, height - inner, color);
        }
    }

    private static float frameDelta(long now) {
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
            return 1.0F / 60.0F;
        }
        float seconds = (now - lastFrameNanos) / 1_000_000_000.0F;
        lastFrameNanos = now;
        return Mth.clamp(seconds, 0.0F, 0.10F);
    }

    private static int argb(float alpha, int red, int green, int blue) {
        int a = Mth.clamp(Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F),
                0, 255);
        return (a << 24) | ((red & 0xFF) << 16)
                | ((green & 0xFF) << 8) | (blue & 0xFF);
    }
}
