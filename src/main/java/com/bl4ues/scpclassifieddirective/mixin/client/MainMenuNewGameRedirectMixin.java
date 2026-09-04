package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.client.MainMenuPlayPanelsClient;
import com.bl4ues.scpclassifieddirective.client.NewGameCreationClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replaces only the title flyout's vanilla world-creation handoff. */
@Mixin(MainMenuPlayPanelsClient.class)
public abstract class MainMenuNewGameRedirectMixin {
    @Redirect(method = "activateRow", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;openFresh(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/Screen;)V"))
    private static void scpclassifieddirective$openNewGameAnimated(
            Minecraft minecraft, Screen parent) {
        NewGameCreationClient.openAnimated(minecraft, parent);
    }
}
