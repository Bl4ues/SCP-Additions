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
        Layout layout = scpclassifieddirective$layout();
        return (mouseX - layout.left) / layout.width
                * TeslaTerminalScreen.TEX_W;
    }

    @Redirect(method = "mouseClicked",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/client/gui/TeslaTerminalScreen;textureY(D)D"))
    private double scpclassifieddirective$physicalTextureY(
            TeslaTerminalScreen screen, double mouseY) {
        Layout layout = scpclassifieddirective$layout();
        return (mouseY - layout.top) / layout.height
                * TeslaTerminalScreen.TEX_H;
    }

    private static Layout scpclassifieddirective$layout() {
        Minecraft minecraft = Minecraft.getInstance();
        double screenWidth = minecraft.getWindow().getGuiScaledWidth();
        double screenHeight = minecraft.getWindow().getGuiScaledHeight();
        double physicalAspect = TeslaTerminalFocusClient.SCREEN_WIDTH
                / TeslaTerminalFocusClient.SCREEN_HEIGHT;
        double height = Math.min(
                screenHeight * TeslaTerminalFocusClient.VIEW_HEIGHT_FRACTION,
                screenWidth * 0.90D / physicalAspect);
        double width = height * physicalAspect;
        return new Layout((screenWidth - width) * 0.5D,
                (screenHeight - height) * 0.5D, width, height);
    }

    private record Layout(double left, double top, double width, double height) {
    }
}
