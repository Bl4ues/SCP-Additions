package com.bl4ues.scpclassifieddirective.mixin.client;

import com.bl4ues.scpclassifieddirective.inventory.client.gui.ScpInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets the physical PDA finish its stow animation before any screen replaces it. */
@Mixin(Minecraft.class)
public abstract class InventoryPdaScreenTransitionMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void scpClassifiedDirective$stowPdaBeforeChangingScreen(
            Screen nextScreen, CallbackInfo callback) {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (minecraft.screen instanceof ScpInventoryScreen inventory
                && nextScreen != inventory
                && inventory.beginPdaClose(nextScreen)) {
            callback.cancel();
        }
    }
}
