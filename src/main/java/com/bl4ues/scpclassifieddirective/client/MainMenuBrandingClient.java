package com.bl4ues.scpclassifieddirective.client;

import com.bl4ues.scpclassifieddirective.ScpClassifiedDirectiveMod;
import com.bl4ues.scpclassifieddirective.inventory.client.ScpFonts;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.fml.ModList;

/** Draws the SCP: Classified Directive title branding. */
public final class MainMenuBrandingClient {
    private static final ResourceLocation LOGO = new ResourceLocation(
            ScpClassifiedDirectiveMod.MODID, "textures/screens/logo.png");
    private static final int TEXT = 0xFFF5F6F7;
    private static final int ACCENT_BRIGHT = 0xFFE3C865;

    private MainMenuBrandingClient() {
    }

    public static void render(CustomMainMenuScreen screen, GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;

        int left = Math.max(42, Math.round(screen.width * 0.055F));
        int top = Math.max(30, Math.round(screen.height * 0.064F));
        int logoHeight = Mth.clamp(Math.round(screen.height * 0.17F), 72, 122);
        int logoWidth = Math.round(logoHeight * (960.0F / 832.0F));

        int titleX = left;
        if (minecraft.getResourceManager().getResource(LOGO).isPresent()) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.blit(LOGO, left, top, logoWidth, logoHeight,
                    0.0F, 0.0F, 960, 832, 960, 832);
            RenderSystem.disableBlend();
            // Keep the wordmark visually attached to the logo instead of
            // leaving the large gap used by the previous single-size title.
            titleX += logoWidth + 5;
        }

        boolean compact = screen.height < 420;
        float scpScale = compact ? 2.80F : 4.25F;
        float directiveScale = compact ? 2.25F : 3.25F;
        float versionScale = compact ? 1.25F : 1.85F;
        int titleY = top - 1;

        Component scp = ScpFonts.montserrat("SCP:");
        drawScaledText(graphics, font, scp,
                titleX, titleY, scpScale, TEXT);

        float scpWidth = font.width(scp) * scpScale;
        drawScaledText(graphics, font,
                ScpFonts.montserrat("CLASSIFIED DIRECTIVE"),
                titleX + scpWidth + 5.0F, titleY,
                directiveScale, TEXT);

        int versionY = titleY
                + Math.round(font.lineHeight * scpScale) + 7;
        drawScaledText(graphics, font,
                ScpFonts.titillium("VERSION " + modVersion()),
                titleX, versionY, versionScale, ACCENT_BRIGHT);
    }

    /** Always follows the version declared by the loaded mod metadata. */
    private static String modVersion() {
        return ModList.get().getModContainerById(ScpClassifiedDirectiveMod.MODID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("4.0.0");
    }

    private static void drawScaledText(GuiGraphics graphics, Font font,
            Component text, float x, float y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }
}
