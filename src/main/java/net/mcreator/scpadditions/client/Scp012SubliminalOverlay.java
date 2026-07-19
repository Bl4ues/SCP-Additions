package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;

/** Code-generated psychosis plus optional authored full-screen subliminals. */
public final class Scp012SubliminalOverlay {
    private static final int TEXTURE_WIDTH = 1920;
    private static final int TEXTURE_HEIGHT = 1080;
    private static final List<ResourceLocation> AUTHORED = List.of(
            texture(1), texture(2), texture(3), texture(4), texture(5));

    private Scp012SubliminalOverlay() {
    }

    public static void render(GuiGraphics graphics, int width, int height,
                              float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null
                || !Scp012ClientState.shouldRenderOverlay()) {
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

        renderAuthoredFlash(minecraft, graphics, width, height, progress, time);

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

    private static void renderAuthoredFlash(Minecraft minecraft,
                                            GuiGraphics graphics,
                                            int width, int height,
                                            float progress, long time) {
        long window = 520L;
        long within = Math.floorMod(time, window);
        long visibleFor = 70L + Math.round(progress * 120.0F);
        if (within > visibleFor) return;

        int bucket = (int) (time / window);
        ResourceLocation texture = AUTHORED.get(
                Math.floorMod(bucket * 7 + 3, AUTHORED.size()));
        if (minecraft.getResourceManager().getResource(texture).isEmpty()) {
            return;
        }

        float alpha = Mth.clamp(0.18F + progress * 0.62F, 0.0F, 0.82F);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.blit(texture, 0, 0, width, height, 0.0F, 0.0F,
                TEXTURE_WIDTH, TEXTURE_HEIGHT,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }

    private static ResourceLocation texture(int index) {
        return new ResourceLocation("scp_additions",
                "textures/gui/012_overlay_" + index + ".png");
    }
}
