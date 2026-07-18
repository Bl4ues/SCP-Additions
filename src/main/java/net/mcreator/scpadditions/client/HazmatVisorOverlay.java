package net.mcreator.scpadditions.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.mcreator.scpadditions.equipment.HazmatSuitAccess;

/** First-person view through the sealed Hazmat Suit mask. */
public final class HazmatVisorOverlay {
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "scp_additions", "textures/gui/hazmat_suit_overlay.png");
    private static final int TEXTURE_WIDTH = 1920;
    private static final int TEXTURE_HEIGHT = 1080;

    private HazmatVisorOverlay() {
    }

    public static void render(GuiGraphics graphics, int screenWidth,
            int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.isAlive()
                || minecraft.options.hideGui || minecraft.screen != null
                || minecraft.options.getCameraType() != CameraType.FIRST_PERSON
                || !HazmatSuitAccess.isFullyEquipped(minecraft.player)) {
            return;
        }

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(TEXTURE, 0, 0, screenWidth, screenHeight,
                0.0F, 0.0F, TEXTURE_WIDTH, TEXTURE_HEIGHT,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
    }
}
