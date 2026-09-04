package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.NewGameScreenClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps the compact difficulty bullets square and optically centered on text. */
@Mixin(value = NewGameScreenClient.class, remap = false)
public abstract class NewGameDifficultyBulletMixin {
    @Redirect(method = "drawDifficultySummary",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V",
                    ordinal = 0,
                    remap = true))
    private static void scpClassifiedDirective$centerDifficultyBullet(
            GuiGraphics graphics, int left, int top, int right, int bottom,
            int color) {
        final int markerSize = 5;
        // The visible Roboto glyphs sit slightly above the nominal font box.
        // Recover lineY from the source marker and shift the geometric center
        // up one logical pixel to match the optical center seen on screen.
        int lineY = top - 3;
        int markerTop = lineY + Math.max(0,
                (Minecraft.getInstance().font.lineHeight - markerSize) / 2) - 1;
        graphics.fill(left, markerTop, left + markerSize,
                markerTop + markerSize, color);
    }
}
