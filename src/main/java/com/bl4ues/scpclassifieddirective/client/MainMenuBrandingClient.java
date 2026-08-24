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
            titleX += logoWidth + 5;
        }

        boolean compact = screen.height < 420;
        float scpScale = compact ? 3.20F : 5.20F;
        float directiveBaseScale = compact ? 2.05F : 3.15F;
        float versionScale = compact ? 1.45F : 2.12F;
        int titleY = top + (compact ? 4 : 9);

        Component scp = ScpFonts.montserrat("SCP:");
        drawScaledText(graphics, font, scp,
                titleX, titleY, scpScale, TEXT);

        float scpWidth = font.width(scp) * scpScale;
        Component directive = ScpFonts.montserrat("CLASSIFIED DIRECTIVE");
        float directiveX = titleX + scpWidth + (compact ? 4.0F : 6.0F);
        float remainingWidth = Math.max(1.0F, screen.width - directiveX - 22.0F);
        float directiveScale = Math.min(directiveBaseScale,
                remainingWidth / Math.max(1.0F, font.width(directive)));
        directiveScale = Math.max(compact ? 1.55F : 2.20F, directiveScale);
        drawScaledText(graphics, font, directive,
                directiveX, titleY + (compact ? 2.0F : 4.0F),
                directiveScale, TEXT);

        int versionY = titleY
                + Math.round(font.lineHeight * scpScale)
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
