package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.scp079.Scp079PlayableVisualsV2;
import com.bl4ues.scpclassifieddirective.client.scp079.Scp079UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The fullscreen Facility Map already has the intended Auxiliary Power anchor.
 * Feed HUDs are rendered through a different overlay path and land visibly
 * farther down/right after the CRT composite, so compensate that path only.
 */
@Mixin(value = Scp079PlayableVisualsV2.class, remap = false)
public abstract class Scp079FeedPowerAnchorMixin {
    private static final float FEED_POWER_LEFT = -40.0F;
    private static final float FEED_POWER_UP = -24.0F;

    @Redirect(method = {"renderLocalHud", "renderCameraHud"},
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/client/scp079/Scp079UiTheme;renderPower(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/Minecraft;I)V"))
    private static void scpclassifieddirective$matchMapPowerAnchor(
            GuiGraphics graphics, Minecraft minecraft, int power) {
        graphics.pose().pushPose();
        graphics.pose().translate(FEED_POWER_LEFT, FEED_POWER_UP, 0.0F);
        Scp079UiTheme.renderPower(graphics, minecraft, power);
        graphics.pose().popPose();
    }
}
