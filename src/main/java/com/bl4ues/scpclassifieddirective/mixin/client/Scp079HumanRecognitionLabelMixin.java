package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableVisualsV2;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps human recognition as a clean 2D box while SCP labels remain visible. */
@Mixin(Scp079PlayableVisualsV2.class)
public abstract class Scp079HumanRecognitionLabelMixin {
    @Redirect(method = "renderRecognition",
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/client/scp079/Scp079UiTheme;drawCentered(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;Ljava/lang/String;FFFI)V"),
            remap = false)
    private static void scpclassifieddirective$hideHumanLabel(
            GuiGraphics graphics, Font font, String value, float centerX,
            float y, float scale, int color) {
        if (!"HUMAN".equals(value)) {
            Scp079UiTheme.drawCentered(graphics, font, value, centerX, y,
                    scale, color);
        }
    }
}
