package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.TeslaTerminalFocusClient;
import com.bl4ues.scpclassifieddirective.client.gui.TeslaTerminalScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Maps the normal Screen cursor onto the real CRT rectangle. The terminal image
 * is stretched onto the authored monitor face, whose aspect ratio differs from
 * the legacy 1410x1080 GUI. Projection follows the focused camera's actual FOV
 * and physical distance so visible controls and hitboxes share one coordinate
 * system.
 */
@Mixin(value = TeslaTerminalScreen.class, remap = false)
public abstract class TeslaTerminalPhysicalInputMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void scpclassifieddirective$waitForStableFocus(double mouseX,
            double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == 0 && TeslaTerminalFocusClient.active()
                && !TeslaTerminalFocusClient.inputReady()) {
            cir.setReturnValue(true);
        }
    }

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
                guiHeight * TeslaTerminalFocusClient.projectedHeightFraction(),
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
                guiHeight * TeslaTerminalFocusClient.projectedHeightFraction(),
                guiWidth * 0.90D / physicalAspect);
        double top = (guiHeight - displayHeight) * 0.5D;
        return (mouseY - top) / displayHeight * TeslaTerminalScreen.TEX_H;
    }

    /**
     * The legacy screen played its generic click for every LMB press before it
     * knew whether a button was actually hit. On a physical terminal that is
     * actively misleading: empty CRT space sounded successful. Every real
     * control already calls playSelect(), so suppress only the unconditional
     * pre-hit-test sound and keep valid-button feedback intact.
     */
    @Redirect(method = "mouseClicked",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/client/gui/TeslaTerminalScreen;playRandomClick()V"))
    private void scpclassifieddirective$onlySoundValidControls(
            TeslaTerminalScreen screen) {
        // Intentionally empty. Valid controls still emit playSelect().
    }
}
