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
 * Feed HUDs pass through the curved CRT composite, which expands and shifts the
 * lower-right corner slightly. Counter-transform only that widget so its final
 * visible bounds match the map rather than merely sharing raw GUI coordinates.
 */
@Mixin(value = Scp079PlayableVisualsV2.class, remap = false)
public abstract class Scp079FeedPowerAnchorMixin {
    private static final float FEED_POWER_LEFT = -40.0F;
    private static final float FEED_POWER_UP = -26.0F;
    private static final float FEED_POWER_SCALE_X = 0.95F;
    private static final float FEED_POWER_SCALE_Y = 0.92F;

    @Redirect(method = {"renderLocalHud", "renderCameraHud"},
            at = @At(value = "INVOKE",
                    target = "Lcom/bl4ues/scpclassifieddirective/client/scp079/Scp079UiTheme;renderPower(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/Minecraft;I)V"))
    private static void scpclassifieddirective$matchMapPowerAnchor(
            GuiGraphics graphics, Minecraft minecraft, int power) {
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int barWidth = Math.min(260, Math.max(220, width / 4));
        int nativeX = width - 16 - barWidth;
        int nativeY = height - 19 - 14;

        graphics.pose().pushPose();
        // Scale around the bar's authored top-left anchor. This preserves the
        // corrected left edge while compensating the CRT pass' ~5% horizontal
        // and ~8% vertical expansion seen in the feed capture.
        graphics.pose().translate(nativeX + FEED_POWER_LEFT,
                nativeY + FEED_POWER_UP, 0.0F);
        graphics.pose().scale(FEED_POWER_SCALE_X, FEED_POWER_SCALE_Y, 1.0F);
        graphics.pose().translate(-nativeX, -nativeY, 0.0F);
        Scp079UiTheme.renderPower(graphics, minecraft, power);
        graphics.pose().popPose();
    }
}
