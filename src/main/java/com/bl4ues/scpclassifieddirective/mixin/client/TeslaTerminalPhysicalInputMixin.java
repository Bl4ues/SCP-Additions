package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.TeslaTerminalFocusClient;
import com.bl4ues.scpclassifieddirective.client.gui.TeslaTerminalScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Maps the normal Screen cursor onto the physical CRT. Once terminal focus is
 * complete the camera is centered on, and perpendicular to, the authored CRT
 * plane. Using that exact projected rectangle is both simpler and more robust
 * than reconstructing a second world-space ray from camera basis vectors.
 */
@Mixin(value = TeslaTerminalScreen.class, remap = false)
public abstract class TeslaTerminalPhysicalInputMixin {
    private double scpclassifieddirective$mouseX;
    private double scpclassifieddirective$mouseY;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void scpclassifieddirective$waitForStableFocus(double mouseX,
            double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        scpclassifieddirective$mouseX = mouseX;
        scpclassifieddirective$mouseY = mouseY;
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
        return scpclassifieddirective$physicalCoordinates(screen,
                mouseX, scpclassifieddirective$mouseY)[0];
    }

    @Redirect(method = "mouseClicked",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/client/gui/TeslaTerminalScreen;textureY(D)D"))
    private double scpclassifieddirective$physicalTextureY(
            TeslaTerminalScreen screen, double mouseY) {
        return scpclassifieddirective$physicalCoordinates(screen,
                scpclassifieddirective$mouseX, mouseY)[1];
    }

    private double[] scpclassifieddirective$physicalCoordinates(
            TeslaTerminalScreen screen, double mouseX, double mouseY) {
        double viewportWidth = Math.max(1.0D, screen.width);
        double viewportHeight = Math.max(1.0D, screen.height);

        // Focus camera ends exactly on the screen normal and looks at the CRT
        // centre. Project the real authored screen dimensions using the same
        // FOV/distance used by TeslaTerminalFocusClient, rather than the old
        // 1410x1080 aspect approximation.
        double projectedHeight = viewportHeight
                * TeslaTerminalFocusClient.projectedHeightFraction();
        double physicalAspect = TeslaTerminalFocusClient.SCREEN_WIDTH
                / TeslaTerminalFocusClient.SCREEN_HEIGHT;
        double projectedWidth = projectedHeight * physicalAspect;
        double left = (viewportWidth - projectedWidth) * 0.5D;
        double top = (viewportHeight - projectedHeight) * 0.5D;

        double u = (mouseX - left) / Math.max(1.0E-6D, projectedWidth);
        double v = (mouseY - top) / Math.max(1.0E-6D, projectedHeight);
        return new double[] {
                u * TeslaTerminalScreen.TEX_W,
                v * TeslaTerminalScreen.TEX_H
        };
    }

    /** Empty CRT space should not sound like a successful control press. */
    @Redirect(method = "mouseClicked",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/client/gui/TeslaTerminalScreen;playRandomClick()V"))
    private void scpclassifieddirective$onlySoundValidControls(
            TeslaTerminalScreen screen) {
        // Valid controls still emit playSelect().
    }
}
