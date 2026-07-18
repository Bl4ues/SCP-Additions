package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Code-generated green-black fatigue vignette for SCP-714 exposure. */
public final class Scp714VignetteOverlay {
    private static final int EDGE_LAYERS = 28;

    private Scp714VignetteOverlay() {
    }

    public static void render(GuiGraphics graphics, int width, int height,
            float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.isAlive()) {
            Scp714ClientState.clear();
            return;
        }
        if (!Scp714ClientState.isActive()) {
            return;
        }

        float progress = Scp714ClientState.getSmoothedProgress(partialTick);
        if (progress <= 0.001F) {
            return;
        }

        // The center darkens slowly at first and then collapses rapidly near the
        // two-minute mark. During the five-second coma grace period it is fully
        // black, while HUD/action-bar layers remain readable above this overlay.
        int centerAlpha = Scp714ClientState.isImmobilized()
                ? 255
                : Mth.clamp((int) (255.0F
                * Math.pow(progress, 2.35D)), 0, 250);
        if (centerAlpha > 0) {
            graphics.fill(0, 0, width, height,
                    argb(centerAlpha, 0, 2, 0));
        }

        int maxInset = Math.max(1, Math.min(width, height) / 2);
        int layerThickness = Math.max(2, maxInset / EDGE_LAYERS + 1);
        for (int layer = 0; layer < EDGE_LAYERS; layer++) {
            float normalized = 1.0F - layer / (float) EDGE_LAYERS;
            float edgeStrength = progress * normalized * normalized;
            int alpha = Mth.clamp((int) (118.0F * edgeStrength), 0, 118);
            if (alpha <= 0) {
                continue;
            }

            int inset = layer * maxInset / EDGE_LAYERS;
            int right = width - inset;
            int bottom = height - inset;
            if (right <= inset || bottom <= inset) {
                break;
            }

            // A restrained jade tint remains near the edge, matching the ring
            // without turning the whole screen into a flat green filter.
            int color = argb(alpha, 5, 20, 7);
            graphics.fill(inset, inset, right,
                    Math.min(bottom, inset + layerThickness), color);
            graphics.fill(inset, Math.max(inset, bottom - layerThickness),
                    right, bottom, color);
            graphics.fill(inset, inset,
                    Math.min(right, inset + layerThickness), bottom, color);
            graphics.fill(Math.max(inset, right - layerThickness), inset,
                    right, bottom, color);
        }
    }

    private static int argb(int alpha, int red, int green, int blue) {
        return (Mth.clamp(alpha, 0, 255) << 24)
                | (Mth.clamp(red, 0, 255) << 16)
                | (Mth.clamp(green, 0, 255) << 8)
                | Mth.clamp(blue, 0, 255);
    }
}
