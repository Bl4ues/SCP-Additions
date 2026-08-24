package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.ResponsiveUiScale;
import com.bl4ues.scpclassifieddirective.inventory.client.gui.components.CodexPanel;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Supplies the SCP Inventory Codex with the responsive screen canvas size. */
@Mixin(CodexPanel.class)
public abstract class CodexPanelResponsiveMixin {
    @Redirect(method = "renderExpanded",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledWidth()I"))
    private int scpClassifiedDirective$virtualCodexWidth(Window window) {
        return ResponsiveUiScale.current().virtualWidth();
    }

    @Redirect(method = "renderExpanded",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I"))
    private int scpClassifiedDirective$virtualCodexHeight(Window window) {
        return ResponsiveUiScale.current().virtualHeight();
    }

    @Redirect(method = "guessHeight",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I"))
    private static int scpClassifiedDirective$virtualLegacyCodexHeight(
            Window window) {
        return ResponsiveUiScale.current().virtualHeight();
    }
}
