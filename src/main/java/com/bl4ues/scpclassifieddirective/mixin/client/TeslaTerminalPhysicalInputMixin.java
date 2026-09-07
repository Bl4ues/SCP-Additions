package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.TeslaTerminalFocusClient;
import com.bl4ues.scpclassifieddirective.client.gui.TeslaTerminalScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Maps the normal Screen cursor onto the real CRT rectangle. The terminal image
 * is stretched onto the authored monitor face, whose aspect ratio differs from
 * the legacy 1410x1080 GUI, so reusing the old fullscreen GUI rectangle makes
 * right-side controls miss their visible buttons.
 */
@Mixin(value = TeslaTerminalScreen.class, remap = false)
public abstract class TeslaTerminalPhysicalInputMixin {
    @Redirect(method = "mouseClicked",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/client/gui/TeslaTerminalScreen;textureX(D)D"))
    private double scpclassifieddirective$physicalTextureX(
            TeslaTerminalScreen screen, double mouseX) {
        Minecraft minecraft = Minecraft.getInstance();
        double guiWidth = minecraft.getWindow().getGuiScaledWidth();
        double guiHeight = minecraft.getWindow().getGuiScaledHeight();
        double physicalAspect = TeslaTerminalFocusClient.SCREEN_WIDTH
                / TeslaTerminalFocusClient.SCREEN_HEIGHT;
        double displayHeight = Math.min(
                guiHeight * TeslaTerminalFocusClient.VIEW_HEIGHT_FRACTION,
                guiWidth * 0.90D / physicalAspect);
        double displayWidth = displayHeight * physicalAspect;
        double left = (guiWidth - displayWidth) * 0.5D;
        return (mouseX - left) / displayWidth * TeslaTerminalScreen.TEX_W;
    }

    @Redirect(method = "mouseClicked",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/client/gui/TeslaTerminalScreen;textureY(D)D"))
    private double scpclassifieddirective$physicalTextureY(
            TeslaTerminalScreen screen, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        double guiWidth = minecraft.getWindow().getGuiScaledWidth();
        double guiHeight = minecraft.getWindow().getGuiScaledHeight();
        double physicalAspect = TeslaTerminalFocusClient.SCREEN_WIDTH
                / TeslaTerminalFocusClient.SCREEN_HEIGHT;
        double displayHeight = Math.min(
                guiHeight * TeslaTerminalFocusClient.VIEW_HEIGHT_FRACTION,
                guiWidth * 0.90D / physicalAspect);
        double top = (guiHeight - displayHeight) * 0.5D;
        return (mouseY - top) / displayHeight * TeslaTerminalScreen.TEX_H;
    }
}
