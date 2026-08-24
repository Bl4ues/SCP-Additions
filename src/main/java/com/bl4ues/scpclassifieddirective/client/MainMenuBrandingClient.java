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

    /*
     * The title uses a dedicated 32 px TTF provider rather than magnifying the
     * normal 11 px UI face by five times. Keep these scales comparatively low:
     * the apparent size is intentionally almost unchanged, while the source
     * glyph raster is substantially denser and therefore stays clean.
     */
    private static final float TITLE_LAYOUT_CAP_HEIGHT = 26.0F;

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
            titleX += logoWidth + 5;
        }

        boolean compact = screen.height < 420;
        float scpScale = compact ? 1.10F : 1.80F;
        float directiveBaseScale = compact ? 0.72F : 1.10F;
        float versionScale = compact ? 1.45F : 2.12F;
        int titleY = top + (compact ? 4 : 9);

        Component scp = ScpFonts.montserratTitle("SCP:");
        drawScaledText(graphics, font, scp,
                titleX, titleY, scpScale, TEXT);

        float scpWidth = font.width(scp) * scpScale;
        Component directive = ScpFonts.montserratTitle("CLASSIFIED DIRECTIVE");
        float directiveX = titleX + scpWidth + (compact ? 4.0F : 6.0F);
        float remainingWidth = Math.max(1.0F, screen.width - directiveX - 22.0F);
        float directiveScale = Math.min(directiveBaseScale,
                remainingWidth / Math.max(1.0F, font.width(directive)));
        directiveScale = Math.max(compact ? 0.54F : 0.78F, directiveScale);

        // Align the smaller wordmark by its lower visual edge, not its top.
        // Montserrat's cap-height makes a small fixed correction more stable
        // here than using Minecraft's global 9 px Font.lineHeight.
        float directiveY = titleY + (compact ? 7.0F : 14.0F);
        drawScaledText(graphics, font, directive,
                directiveX, directiveY, directiveScale, TEXT);

        int versionY = titleY
                + Math.round(TITLE_LAYOUT_CAP_HEIGHT * scpScale)
                + (compact ? 8 : 12);
        drawScaledText(graphics, font,
                ScpFonts.titillium("VERSION " + modVersion()),
                titleX + (compact ? 1.0F : 3.0F), versionY,
                versionScale, ACCENT_BRIGHT);
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
