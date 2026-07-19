package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Code-generated green-black fatigue vignette for SCP-714 exposure. */
public final class Scp714VignetteOverlay {
    private Scp714VignetteOverlay() {
    }

    public static void render(GuiGraphics graphics, int width, int height,
                              float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.isAlive()) {
            Scp714ClientState.clear();
            return;
        }
        if (!Scp714ClientState.isActive() || width <= 0 || height <= 0) {
            return;
        }

        float progress = Scp714ClientState.getSmoothedProgress(partialTick);
        if (progress <= 0.001F) return;

        SmoothRadialVignetteRenderer.render(graphics, width, height,
                (x, y) -> colorAt(x, y, width, height, progress));
    }

    private static SmoothRadialVignetteRenderer.VertexColor colorAt(
            float x, float y, int width, int height, float progress) {
        if (Scp714ClientState.isImmobilized()) {
            return new SmoothRadialVignetteRenderer.VertexColor(0, 0, 0, 255);
        }

        float normalizedX = (x - width * 0.5F) / (width * 0.5F);
        float normalizedY = (y - height * 0.5F) / (height * 0.5F);
        float radius = Mth.sqrt(normalizedX * normalizedX
                + normalizedY * normalizedY);

        float easedProgress = SmoothRadialVignetteRenderer.smoothStep(
                0.0F, 1.0F, progress);
        float aperture = Mth.lerp(easedProgress, 1.20F, -0.05F);
        float feather = Mth.lerp(easedProgress, 0.30F, 0.55F);
        float edge = SmoothRadialVignetteRenderer.smoothStep(
                aperture - feather, aperture + feather, radius);

        float onset = (float) Math.pow(progress, 0.65D);
        float edgeOpacity = edge * onset
                * Mth.lerp(progress, 0.10F, 0.96F);
        float centerOpacity = (float) Math.pow(progress, 3.40D) * 0.92F;
        float alpha = Mth.clamp(Math.max(edgeOpacity, centerOpacity),
                0.0F, 0.985F);

        float jadeTint = (1.0F - progress)
                * (0.20F + edge * 0.80F);
        int green = Mth.clamp(Math.round(18.0F * jadeTint), 0, 18);
        int blue = Mth.clamp(Math.round(5.0F * jadeTint), 0, 5);
        return new SmoothRadialVignetteRenderer.VertexColor(0, green, blue,
                Mth.clamp(Math.round(alpha * 255.0F), 0, 251));
    }
}
