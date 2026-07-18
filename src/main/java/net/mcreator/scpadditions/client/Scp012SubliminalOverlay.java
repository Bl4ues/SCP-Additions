package net.mcreator.scpadditions.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Code-generated subliminal flashes used until authored overlay textures exist. */
public final class Scp012SubliminalOverlay {
    private Scp012SubliminalOverlay() {
    }

    public static void render(GuiGraphics graphics, int width, int height,
                              float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null
                || !Scp012ClientState.isActive()) {
            return;
        }
        float progress = Scp012ClientState.contactProgress();
        if (progress <= 0.01F) return;

        long time = System.currentTimeMillis();
        float pulse = 0.45F + 0.55F * Mth.sin((time % 1400L) / 1400.0F
                * Mth.TWO_PI);
        int veilAlpha = Mth.clamp(Math.round((18.0F + 62.0F * progress)
                * (0.65F + pulse * 0.35F)), 0, 110);
        graphics.fill(0, 0, width, height,
                veilAlpha << 24 | 0x00100608);

        int bucket = (int) (time / 135L);
        int flashAlpha = Mth.clamp(Math.round(34.0F + progress * 105.0F),
                0, 150);
        for (int index = 0; index < 5; index++) {
            int seed = bucket * 7349 + index * 9151;
            int x = Math.floorMod(seed * 31, Math.max(1, width - 60));
            int y = Math.floorMod(seed * 17, Math.max(1, height - 24));
            int w = 30 + Math.floorMod(seed, Math.max(31, width / 4));
            int h = 2 + Math.floorMod(seed / 7, 8);
            int alpha = Math.max(8, flashAlpha - index * 16);
            graphics.fill(x, y, Math.min(width, x + w),
                    Math.min(height, y + h), alpha << 24 | 0x006A0F18);
        }

        int edgeAlpha = Mth.clamp(Math.round(progress * 115.0F), 0, 115);
        int edge = Math.max(8, Math.round(Math.min(width, height)
                * (0.025F + progress * 0.035F)));
        graphics.fill(0, 0, width, edge, edgeAlpha << 24 | 0x00180005);
        graphics.fill(0, height - edge, width, height,
                edgeAlpha << 24 | 0x00180005);
        graphics.fill(0, edge, edge, height - edge,
                edgeAlpha << 24 | 0x00180005);
        graphics.fill(width - edge, edge, width, height - edge,
                edgeAlpha << 24 | 0x00180005);
    }
}
